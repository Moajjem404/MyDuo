package com.moajjem.myduuo.service

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.moajjem.myduuo.MyDuoApplication
import com.moajjem.myduuo.data.AppRepository
import com.moajjem.myduuo.data.TelegramBotManager
import com.moajjem.myduuo.ui.home.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class ActivityMonitorService : Service() {

    private lateinit var appRepository: AppRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var isScreenOn = true
    private var lastSharedApp: String? = null
    
    private var monitoringJob: kotlinx.coroutines.Job? = null
    private var partnerUpdatesJob: kotlinx.coroutines.Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    isScreenOn = true
                    Log.d(TAG, "Screen ON/Unlocked - Resume monitoring loop")
                    startMonitoringLoop()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    Log.d(TAG, "Screen OFF - Suspend monitoring loop")
                    // Share Locked status when screen goes off
                    shareAppUpdate("Locked")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Foreground Service created")

        appRepository = AppRepository.getInstance(this)

        // Register Broadcast Receiver for Screen On/Off/Unlock
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Foreground Service starting")
        
        // Show persistent foreground notification
        startForegroundServiceNotification()

        // Start loops
        startMonitoringLoop()
        startPartnerUpdatesLoop()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Foreground Service destroyed")
        unregisterReceiver(screenReceiver)
        serviceJob.cancel() // Cancel all active coroutines
    }

    private fun buildForegroundNotification(statusText: String, contentText: String, whenTimestamp: Long? = null): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, MyDuoApplication.CHANNEL_SERVICE_ID)
            .setContentTitle(statusText)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (whenTimestamp != null) {
            builder.setShowWhen(true).setWhen(whenTimestamp)
        }

        return builder.build()
    }

    private fun getPartnerStatusTitle(partnerName: String, appName: String): String {
        return when (appName) {
            "Facebook" -> "💕 $partnerName opened Facebook"
            "YouTube" -> "💙 $partnerName is browsing YouTube"
            "Chrome" -> "❤️ $partnerName opened Chrome"
            "Telegram" -> "💙 $partnerName is using Telegram"
            "Locked" -> "💔 $partnerName phone has lock now"
            else -> "❤️ $partnerName is using $appName"
        }
    }

    private fun startForegroundServiceNotification() {
        val partnerState = appRepository.partnerState.value
        val partnerName = appRepository.getPartnerName() ?: "Partner"
        val statusText: String
        val contentText: String
        var whenTimestamp: Long? = null

        if (partnerState != null) {
            statusText = getPartnerStatusTitle(partnerName, partnerState.app)
            val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val formattedTime = sdf.format(java.util.Date(partnerState.time * 1000))
            contentText = "Active since $formattedTime. Tap to view details."
            whenTimestamp = partnerState.time * 1000
        } else {
            statusText = "Waiting for $partnerName's updates..."
            contentText = "Tap to open MyDuo."
        }

        val notification = buildForegroundNotification(statusText, contentText, whenTimestamp)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // --- Foreground App Monitoring Loop ---

    private fun startMonitoringLoop() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isScreenOn) {
                if (appRepository.isSetupComplete()) {
                    checkForegroundApp()
                }
                delay(3000) // Poll every 3 seconds (battery optimized)
            }
        }
    }

    private fun checkForegroundApp() {
        if (!hasUsageStatsPermission(this)) {
            Log.w(TAG, "Usage Stats Permission not granted!")
            return
        }

        val currentPackage = getForegroundPackage()
        if (currentPackage != null) {
            // Avoid sharing "MyDuo" itself to prevent circular app tracking loops
            if (currentPackage == packageName) {
                return
            }

            val appName = getAppNameFromPackage(currentPackage)
            if (appName != lastSharedApp) {
                shareAppUpdate(appName)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        
        // 1. Query events in the last 15 seconds
        val events = usageStatsManager.queryEvents(time - 15000, time)
        val event = UsageEvents.Event()
        var lastResumedPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumedPackage = event.packageName
            }
        }
        
        if (lastResumedPackage != null) {
            return lastResumedPackage
        }

        // 2. Fallback to queryUsageStats
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 60000, time)
        if (!stats.isNullOrEmpty()) {
            var activeStats = stats[0]
            for (stat in stats) {
                if (stat.lastTimeUsed > activeStats.lastTimeUsed) {
                    activeStats = stat
                }
            }
            if (activeStats.lastTimeUsed > 0) {
                return activeStats.packageName
            }
        }
        return null
    }

    private fun getAppNameFromPackage(packageName: String): String {
        val knownApps = mapOf(
            "com.facebook.katana" to "Facebook",
            "com.facebook.lite" to "Facebook",
            "com.android.chrome" to "Chrome",
            "com.chrome.beta" to "Chrome",
            "com.facebook.orca" to "Messenger",
            "com.facebook.mlite" to "Messenger",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.instagram.android" to "Instagram",
            "com.google.android.youtube" to "YouTube",
            "com.whatsapp" to "WhatsApp",
            "com.tencent.mm" to "WeChat",
            "org.telegram.messenger" to "Telegram"
        )
        knownApps[packageName]?.let { return it }

        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            "Other App"
        }
    }

    private fun shareAppUpdate(appName: String) {
        lastSharedApp = appName
        val timestamp = System.currentTimeMillis() / 1000 // UNIX timestamp in seconds
        val senderId = appRepository.getSenderId() ?: return
        val botToken = appRepository.getBotToken() ?: return
        val groupId = appRepository.getGroupId() ?: return

        serviceScope.launch {
            // Generate clean JSON payload
            val payload = JSONObject().apply {
                put("app", appName)
                put("time", timestamp.toString())
                put("sender", senderId)
            }.toString()

            val msgId = TelegramBotManager.sendMessage(botToken, groupId, payload)
            if (msgId > 0) {
                Log.d(TAG, "Sent app update: $payload (Message ID: $msgId)")
                // Pin the message so the partner can read it statelessly even if privacy mode / bot-to-bot block exists
                TelegramBotManager.pinChatMessage(botToken, groupId, msgId)
            } else {
                Log.e(TAG, "Failed to send update to Telegram")
            }
        }
    }

    // --- Partner Telegram Updates Checking Loop ---

    private fun startPartnerUpdatesLoop() {
        partnerUpdatesJob?.cancel()
        partnerUpdatesJob = serviceScope.launch {
            while (true) {
                val botToken = appRepository.getBotToken()
                val groupId = appRepository.getGroupId()

                if (!botToken.isNullOrEmpty() && !groupId.isNullOrEmpty()) {
                    // A. Poll updates (stateless fetch of last 30 updates, avoids competing-consumer queue deletes)
                    val localOffset = appRepository.getTelegramOffset()
                    val updates = TelegramBotManager.getUpdates(botToken, -30)

                    var maxUpdateId = localOffset
                    for (update in updates) {
                        if (update.updateId >= localOffset) {
                            processIncomingMessage(update.text, update.date)
                        }
                        if (update.updateId >= maxUpdateId) {
                            maxUpdateId = update.updateId + 1
                        }
                    }

                    if (maxUpdateId > localOffset) {
                        appRepository.saveTelegramOffset(maxUpdateId)
                    }

                    // B. Poll latest pinned message in the group (stateless fallback, bypasses bot-to-bot blockage)
                    val pinnedMsg = TelegramBotManager.getLatestPinnedMessage(botToken, groupId)
                    if (pinnedMsg != null) {
                        processIncomingMessage(pinnedMsg.text, pinnedMsg.date)
                    }
                }
                delay(4000) // Poll Telegram every 4 seconds (battery optimized)
            }
        }
    }

    private fun processIncomingMessage(text: String, telegramTimestamp: Long) {
        try {
            val trimmedText = text.trim()
            if (!trimmedText.startsWith("{") || !trimmedText.endsWith("}")) {
                return // Not our JSON payload format
            }

            val json = JSONObject(trimmedText)
            if (!json.has("app") || !json.has("time") || !json.has("sender")) {
                return // Missing required fields
            }

            val sender = json.getString("sender")
            val mySenderId = appRepository.getSenderId() ?: ""

            // Ignore our own updates
            if (sender == mySenderId) {
                return
            }

            val appName = json.getString("app")
            val timestampStr = json.getString("time")
            val timestamp = timestampStr.toLongOrNull() ?: telegramTimestamp

            // Avoid duplicates
            val currentPartnerState = appRepository.partnerState.value
            if (currentPartnerState != null && 
                currentPartnerState.sender == sender && 
                currentPartnerState.app == appName && 
                Math.abs(currentPartnerState.time - timestamp) < 2) {
                return
            }

            // Update state
            appRepository.updatePartnerState(sender, appName, timestamp)

            // Trigger immediate notification
            showPartnerUpdateNotification(appName, timestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming message: ${e.message}", e)
        }
    }

    private fun showPartnerUpdateNotification(appName: String, timestamp: Long) {
        val partnerName = appRepository.getPartnerName() ?: "Partner"
        val statusText = getPartnerStatusTitle(partnerName, appName)
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val formattedTime = sdf.format(java.util.Date(timestamp * 1000))
        val contentText = "Active since $formattedTime. Tap to view details."

        val notification = buildForegroundNotification(statusText, contentText, timestamp * 1000)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    companion object {
        private const val TAG = "ActivityMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val PARTNER_NOTIF_ID = 1002
    }
}
