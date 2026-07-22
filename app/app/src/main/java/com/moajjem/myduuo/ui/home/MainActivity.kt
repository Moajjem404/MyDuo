package com.moajjem.myduuo.ui.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import java.io.File
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.moajjem.myduuo.R
import com.moajjem.myduuo.data.AppRepository
import com.moajjem.myduuo.data.PartnerState
import com.moajjem.myduuo.data.VersionChecker
import com.moajjem.myduuo.service.ActivityMonitorService
import com.moajjem.myduuo.ui.profile.ProfileActivity
import com.moajjem.myduuo.ui.settings.SettingsActivity
import com.moajjem.myduuo.ui.welcome.SplashActivity
import com.moajjem.myduuo.ui.welcome.WelcomeActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var appRepository: AppRepository

    private lateinit var tvPartnerOnlineStatus: TextView
    private lateinit var tvPartnerCurrentApp: TextView
    private lateinit var tvPartnerDuration: TextView
    
    private lateinit var ivAppIcon: ImageView
    private lateinit var tvAppName: TextView
    
    private lateinit var ivLastAppIcon: ImageView
    private lateinit var tvLastAppName: TextView
    private lateinit var tvLastAppTime: TextView
    private lateinit var viewOnlineIndicator: android.view.View
    
    private lateinit var pulseRing: android.view.View
    private lateinit var tvFooterAuthor: android.view.View

    private var activePartnerState: PartnerState? = null
    private val handler = Handler(Looper.getMainLooper())
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    
    // UI duration ticker
    private val durationRunnable = object : Runnable {
        override fun run() {
            updateDurationUI()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appRepository = AppRepository.getInstance(this)

        // Route to SplashActivity or WelcomeActivity if configuration is incomplete
        if (!appRepository.isSetupComplete()) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Start background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.main_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1500)
            it.setExitFadeDuration(1500)
            it.start()
        }

        // Bind views
        tvPartnerOnlineStatus = findViewById(R.id.tv_partner_online_status)
        tvPartnerCurrentApp = findViewById(R.id.tv_partner_current_app)
        tvPartnerDuration = findViewById(R.id.tv_partner_duration)
        
        ivAppIcon = findViewById(R.id.iv_app_icon)
        tvAppName = findViewById(R.id.tv_app_name)
        
        ivLastAppIcon = findViewById(R.id.iv_last_app_icon)
        tvLastAppName = findViewById(R.id.tv_last_app_name)
        tvLastAppTime = findViewById(R.id.tv_last_app_time)
        viewOnlineIndicator = findViewById(R.id.view_online_indicator)
        
        pulseRing = findViewById(R.id.pulse_ring)
        tvFooterAuthor = findViewById(R.id.tv_footer_author)
        findViewById<TextView>(R.id.tv_footer_credit_text).apply {
            text = android.text.Html.fromHtml("Made with ❤️ by <font color='#FF4081'><u>Moajjem</u></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }

        // Heart pulse ring animation
        startPulseAnimation()

        // Couple Avatar Loading
        val ivPartnerAvatar = findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.iv_partner_avatar)
        val coupleCache = File(filesDir, "couple_profile.jpg")
        if (coupleCache.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(coupleCache.absolutePath)
                if (bitmap != null) {
                    ivPartnerAvatar.setImageBitmap(bitmap)
                } else {
                    downloadAndCacheCoupleImage(coupleCache, ivPartnerAvatar)
                }
            } catch (e: Exception) {
                downloadAndCacheCoupleImage(coupleCache, ivPartnerAvatar)
            }
        } else {
            downloadAndCacheCoupleImage(coupleCache, ivPartnerAvatar)
        }

        // Populate initial Last App view from prefs
        val prefs = getSharedPreferences("MainActivity", Context.MODE_PRIVATE)
        val lastAppName = prefs.getString("last_app", "Facebook") ?: "Facebook"
        val lastAppTime = prefs.getLong("last_app_time", System.currentTimeMillis() / 1000 - 720)
        tvLastAppName.text = "Last app: $lastAppName"
        mapAppIcon(lastAppName, ivLastAppIcon)
        
        val diffSec = (System.currentTimeMillis() / 1000) - lastAppTime
        val diffMin = if (diffSec > 0) diffSec / 60 else 12
        tvLastAppTime.text = "$diffMin min ago"

        // Bottom Navigation Tab listeners
        findViewById<android.view.View>(R.id.tab_home).setOnClickListener {
            // Already here
        }
        findViewById<android.view.View>(R.id.tab_history).setOnClickListener {
            startActivity(Intent(this, com.moajjem.myduuo.ui.history.HistoryActivity::class.java))
        }
        findViewById<android.view.View>(R.id.tab_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.tab_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Footer action -> Profile Page
        tvFooterAuthor.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Network check and dynamic offline overlay toggles
        val layoutNoInternet = findViewById<android.view.View>(R.id.layout_no_internet)
        val btnRetryInternet = findViewById<android.widget.Button>(R.id.btn_retry_internet)

        fun checkInternet() {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            val isOnline = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            layoutNoInternet.visibility = if (isOnline) android.view.View.GONE else android.view.View.VISIBLE
        }

        btnRetryInternet.setOnClickListener {
            checkInternet()
        }

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                runOnUiThread {
                    layoutNoInternet.visibility = android.view.View.GONE
                }
            }

            override fun onLost(network: android.net.Network) {
                runOnUiThread {
                    layoutNoInternet.visibility = android.view.View.VISIBLE
                }
            }
        }
        cm.registerNetworkCallback(networkRequest, networkCallback!!)
        checkInternet()

        // Check for updates
        checkForAppUpdates()

        // Observe live partner status
        lifecycleScope.launch {
            appRepository.partnerState.collectLatest { state ->
                activePartnerState = state
                updatePartnerStatusUI(state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically ensure Background service is active
        checkAndStartForegroundService()
        // Start live duration counting
        handler.post(durationRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(durationRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            try {
                cm.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkAndStartForegroundService() {
        if (!isServiceRunning(ActivityMonitorService::class.java)) {
            val intent = Intent(this, ActivityMonitorService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }

    private fun startPulseAnimation() {
        val anim = AlphaAnimation(0.2f, 0.8f).apply {
            duration = 1500
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        pulseRing.startAnimation(anim)
    }

    private fun updatePartnerStatusUI(state: PartnerState?) {
        val partnerName = appRepository.getPartnerName()

        if (state == null) {
            tvPartnerOnlineStatus.text = "Offline"
            viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
            tvPartnerCurrentApp.text = "$partnerName is Offline"
            tvPartnerDuration.text = "Waiting for partner..."
            
            tvAppName.text = "Locked / Screen Off"
            mapAppIcon("Locked", ivAppIcon)
            return
        }

        // Map active app dynamically
        tvAppName.text = state.app
        mapAppIcon(state.app, ivAppIcon)

        // Save last app if changed
        val prefs = getSharedPreferences("MainActivity", Context.MODE_PRIVATE)
        val currentStoredApp = prefs.getString("current_app", "") ?: ""
        val currentStoredTime = prefs.getLong("current_app_time", 0L)
        
        if (state.app != currentStoredApp && state.app.isNotEmpty()) {
            if (currentStoredApp.isNotEmpty() && !currentStoredApp.contains("Locked") && !currentStoredApp.contains("Off")) {
                saveLastApp(currentStoredApp, currentStoredTime)
                tvLastAppName.text = "Last app: $currentStoredApp"
                mapAppIcon(currentStoredApp, ivLastAppIcon)
                
                val diffSecLast = (System.currentTimeMillis() / 1000) - currentStoredTime
                val diffMinLast = if (diffSecLast > 0) diffSecLast / 60 else 1
                tvLastAppTime.text = "$diffMinLast min ago"
            }
            prefs.edit().putString("current_app", state.app).putLong("current_app_time", state.time).apply()
        }

        val isLocked = state.app.contains("Off") || state.app.contains("Locked")
        val isOnline = (System.currentTimeMillis() / 1000 - state.time < 60) && !isLocked

        if (isOnline) {
            tvPartnerOnlineStatus.text = "Online"
            viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GREEN)
            tvPartnerCurrentApp.text = "$partnerName is using"
        } else {
            tvPartnerOnlineStatus.text = if (isLocked) "Away" else "Offline"
            viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isLocked) android.graphics.Color.YELLOW else android.graphics.Color.RED)
            tvPartnerCurrentApp.text = if (isLocked) "$partnerName is Idle" else "$partnerName was using"
        }

        updateDurationUI()
    }

    private fun updateDurationUI() {
        val state = activePartnerState ?: return
        
        if (state.app.contains("Off") || state.app.contains("Locked")) {
            tvPartnerDuration.text = "Status: Idle"
            return
        }

        val diffSec = (System.currentTimeMillis() / 1000) - state.time
        if (diffSec < 0) return

        val seconds = diffSec % 60
        val minutes = (diffSec / 60) % 60
        val hours = (diffSec / 3600) % 24

        val durationString = if (diffSec < 60) {
            "Active now"
        } else if (hours > 0) {
            "Since $hours hr $minutes min ago"
        } else {
            "Since $minutes min ago"
        }
        tvPartnerDuration.text = durationString
    }

    private fun saveLastApp(app: String, time: Long) {
        val prefs = getSharedPreferences("MainActivity", Context.MODE_PRIVATE)
        prefs.edit().putString("last_app", app).putLong("last_app_time", time).apply()
    }

    private fun mapAppIcon(appName: String, imageView: ImageView) {
        val url = when (appName) {
            "Chrome" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Google_Chrome_icon_%28February_2022%29.svg/120px-Google_Chrome_icon_%28February_2022%29.svg.png"
            "Facebook" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/Facebook_Logo_%282019%29.png/120px-Facebook_Logo_%282019%29.png"
            "Telegram" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Telegram_logo.svg/120px-Telegram_logo.svg.png"
            "Messenger" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Facebook_Messenger_logo_2020.svg/120px-Facebook_Messenger_logo_2020.svg.png"
            "Instagram" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Instagram_logo_2016.png/120px-Instagram_logo_2016.png"
            "YouTube" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/YouTube_full-color_icon_%282017%29.png/120px-YouTube_full-color_icon_%282017%29.png"
            "WhatsApp" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6b/WhatsApp.svg/120px-WhatsApp.svg.png"
            else -> null
        }

        if (url == null) {
            imageView.setImageResource(android.R.drawable.ic_secure)
            return
        }

        val cacheFile = File(filesDir, "${appName.lowercase()}_icon.png")
        if (cacheFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            } catch (e: Exception) {
                // download
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            cacheFile.writeBytes(bytes)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                withContext(Dispatchers.Main) {
                                    imageView.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkForAppUpdates() {
        lifecycleScope.launch {
            val updateVersion = withContext(Dispatchers.IO) {
                VersionChecker.checkVersion(0.3) // Current version: 0.3
            }

            if (updateVersion != null) {
                showUpdateDialog(updateVersion)
            }
        }
    }

    private fun showUpdateDialog(newVersion: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("New Update Available! 💖")
            .setMessage("A newer version (v$newVersion) of MyDuo is available. Update now to stay connected with your partner!")
            .setCancelable(false)
            .setNeutralButton("Later") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Update") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Moajjem404/MyDuo"))
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .show()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun downloadAndCacheCoupleImage(destFile: File, ivAvatar: com.google.android.material.imageview.ShapeableImageView) {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = "https://raw.githubusercontent.com/Moajjem404/MyDuo/refs/heads/main/img/couple.jpeg"
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            destFile.writeBytes(bytes)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                withContext(Dispatchers.Main) {
                                    ivAvatar.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
