package com.moajjem.myduuo.ui.welcome

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.moajjem.myduuo.R
import com.moajjem.myduuo.data.AppRepository
import com.moajjem.myduuo.ui.setup.SetupActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var permissionTitle: TextView
    private lateinit var permissionDesc: TextView
    private lateinit var btnAction: Button
    private lateinit var dotUsage: android.view.View
    private lateinit var dotNotif: android.view.View
    private lateinit var dotForeground: android.view.View
    private lateinit var dotBattery: android.view.View

    private enum class PermissionStep {
        USAGE_ACCESS,
        NOTIFICATIONS,
        FOREGROUND_SERVICE,
        BATTERY_OPTIMIZATION,
        COMPLETE
    }

    private var currentStep = PermissionStep.USAGE_ACCESS

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications are needed to receive partner updates.", Toast.LENGTH_LONG).show()
        }
        evaluateStepAndConfigureUI()
    }

    private var hasAcceptedForegroundService = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Start background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.welcome_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1500)
            it.setExitFadeDuration(1500)
            it.start()
        }

        permissionTitle = findViewById(R.id.permission_title)
        permissionDesc = findViewById(R.id.permission_desc)
        btnAction = findViewById(R.id.btn_action)
        dotUsage = findViewById(R.id.dot_usage)
        dotNotif = findViewById(R.id.dot_notif)
        dotForeground = findViewById(R.id.dot_foreground)
        dotBattery = findViewById(R.id.dot_battery)

        btnAction.setOnClickListener {
            handlePermissionAction()
        }

        findViewById<TextView>(R.id.tv_welcome_credit_text).apply {
            text = android.text.Html.fromHtml("Made with ❤️ by <font color='#FF4081'><u>Moajjem</u></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }
        findViewById<android.view.View>(R.id.tv_welcome_credit).setOnClickListener {
            startActivity(Intent(this, com.moajjem.myduuo.ui.profile.ProfileActivity::class.java))
        }

        findViewById<TextView>(R.id.txt_footer).setOnClickListener {
            showPrivacyPolicyDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        evaluateStepAndConfigureUI()
    }

    private fun evaluateStepAndConfigureUI() {
        val hasUsage = hasUsageStatsPermission()
        val hasNotif = hasNotificationPermission()
        val hasForeground = hasForegroundServicePermission()
        val hasBattery = isIgnoringBatteryOptimizations()

        // Update indicators
        dotUsage.setBackgroundColor(
            ContextCompat.getColor(this, if (hasUsage) R.color.romantic_light_pink else R.color.glass_card_stroke)
        )
        dotNotif.setBackgroundColor(
            ContextCompat.getColor(this, if (hasNotif) R.color.romantic_light_pink else R.color.glass_card_stroke)
        )
        dotForeground.setBackgroundColor(
            ContextCompat.getColor(this, if (hasForeground) R.color.romantic_light_pink else R.color.glass_card_stroke)
        )
        dotBattery.setBackgroundColor(
            ContextCompat.getColor(this, if (hasBattery) R.color.romantic_light_pink else R.color.glass_card_stroke)
        )

        when {
            !hasUsage -> {
                currentStep = PermissionStep.USAGE_ACCESS
                permissionTitle.text = "Usage Stats Access"
                permissionDesc.text = "Required to monitor the active application change (e.g. YouTube, Chrome). MyDuo only checks the current foreground app label and will never inspect contents, messages, or keys."
                btnAction.text = "Grant Usage Access"
            }
            !hasNotif -> {
                currentStep = PermissionStep.NOTIFICATIONS
                permissionTitle.text = "Notification Permission"
                permissionDesc.text = "Allows MyDuo to instantly notify you when your partner changes the active application, so you stay connected in real-time."
                btnAction.text = "Grant Notification Access"
            }
            !hasForeground -> {
                currentStep = PermissionStep.FOREGROUND_SERVICE
                permissionTitle.text = "Foreground Service"
                permissionDesc.text = "Allows MyDuo to run its activity monitor in the background. A small persistent status bar icon will show when it's active."
                btnAction.text = "Enable Background Service"
            }
            !hasBattery -> {
                currentStep = PermissionStep.BATTERY_OPTIMIZATION
                permissionTitle.text = "Battery Optimization"
                permissionDesc.text = "Recommended to stop Android from pausing the background service when the phone is idle, ensuring continuous real-time sync."
                btnAction.text = "Disable Battery Optimization"
            }
            else -> {
                currentStep = PermissionStep.COMPLETE
                navigateToSetup()
            }
        }
    }

    private fun handlePermissionAction() {
        when (currentStep) {
            PermissionStep.USAGE_ACCESS -> {
                try {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open settings. Please enable manually.", Toast.LENGTH_SHORT).show()
                }
            }
            PermissionStep.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    evaluateStepAndConfigureUI()
                }
            }
            PermissionStep.FOREGROUND_SERVICE -> {
                hasAcceptedForegroundService = true
                Toast.makeText(this, "Background service enabled!", Toast.LENGTH_SHORT).show()
                evaluateStepAndConfigureUI()
            }
            PermissionStep.BATTERY_OPTIMIZATION -> {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Please disable battery optimization for MyDuo manually in Settings.", Toast.LENGTH_LONG).show()
                }
            }
            PermissionStep.COMPLETE -> {
                navigateToSetup()
            }
        }
    }

    private fun navigateToSetup() {
        startActivity(Intent(this, SetupActivity::class.java))
        finish()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasForegroundServicePermission(): Boolean {
        // Foreground service is normal permission. We check manifest declaration and user approval in wizard.
        val normalGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return normalGranted && hasAcceptedForegroundService
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun showPrivacyPolicyDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Privacy Policy & Data Collection")
            .setMessage("At MyDuo, your privacy is our absolute priority. We want to be completely transparent about why we request permissions and what data is shared:\n\n" +
                    "1. Foreground App Monitoring (Usage Access):\n" +
                    "Why: We check the name/label of the active foreground app (e.g., Chrome or YouTube) to share it with your partner. This is the core functionality of the couple app.\n" +
                    "Privacy Guarantee: We never inspect, collect, or upload your messages, contacts, photos, videos, passwords, keystrokes, location, camera, or microphone data. No private content leaves your device.\n\n" +
                    "2. Silent Real-time Synced Alerts (Notifications):\n" +
                    "Why: To update your partner's status silently in your notification tray and to keep the background synchronization service active.\n\n" +
                    "3. Persistent Service (Ignore Battery Optimization):\n" +
                    "Why: To prevent Android from force-closing or sleeping the background monitoring service when your phone goes idle, ensuring you stay connected forever.\n\n" +
                    "4. Network Syncing (Internet):\n" +
                    "Why: To securely transmit the active app name to your shared Telegram group over encrypted HTTPS. No third-party clouds or external databases are used.")
            .setPositiveButton("I Understand") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
