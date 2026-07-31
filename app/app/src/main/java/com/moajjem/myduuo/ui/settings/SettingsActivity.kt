package com.moajjem.myduuo.ui.settings

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.moajjem.myduuo.R
import com.moajjem.myduuo.data.AppRepository
import com.moajjem.myduuo.service.ActivityMonitorService
import com.moajjem.myduuo.ui.welcome.SplashActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var appRepository: AppRepository

    private lateinit var etBotToken: EditText
    private lateinit var etGroupId: EditText
    private lateinit var etPartnerName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Prevent screenshots on sensitive pages for security
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        appRepository = AppRepository.getInstance(this)

        setContentView(R.layout.activity_settings)

        // Start breathing background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.settings_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1500)
            it.setExitFadeDuration(1500)
            it.start()
        }

        etBotToken = findViewById(R.id.et_settings_bot_token)
        etGroupId = findViewById(R.id.et_settings_group_id)
        etPartnerName = findViewById(R.id.et_settings_partner_name)
        btnSave = findViewById(R.id.btn_save_settings)
        btnReset = findViewById(R.id.btn_reset_app)

        // Prepopulate current fields
        etBotToken.setText(appRepository.getBotToken() ?: "")
        etGroupId.setText(appRepository.getGroupId() ?: "")
        etPartnerName.setText(appRepository.getPartnerName())

        // Setup Partner Connection Code card
        val myGender = appRepository.getGender() ?: "Male"
        val creatorName = if (myGender.equals("Female", ignoreCase = true)) "Mun" else "Moajjem"
        val pairingCode = com.moajjem.myduuo.util.PairingCodeManager.generatePairingCode(
            botToken = appRepository.getBotToken() ?: "",
            groupId = appRepository.getGroupId() ?: "",
            creatorName = creatorName,
            targetPartnerName = appRepository.getPartnerName(),
            creatorGender = myGender
        )
        val tvPairingCode = findViewById<android.widget.TextView>(R.id.tv_settings_pairing_code)
        val btnCopyPairingCode = findViewById<Button>(R.id.btn_copy_pairing_code)
        val btnSharePairingCode = findViewById<Button>(R.id.btn_share_pairing_code)

        tvPairingCode.text = if (pairingCode.isNotEmpty()) pairingCode else "No active connection credentials"

        btnCopyPairingCode.setOnClickListener { view ->
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (ignored: Exception) {}
            view.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                if (pairingCode.isNotEmpty()) {
                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Partner Connection Code", pairingCode)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Partner Connection Code Copied! 📋", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No connection code available!", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }

        btnSharePairingCode.setOnClickListener { view ->
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (ignored: Exception) {}
            view.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                if (pairingCode.isNotEmpty()) {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "MyDuo Connection Code")
                            putExtra(Intent.EXTRA_TEXT, "Here is my MyDuo Partner Connection Code: $pairingCode\n\nPaste this code into your MyDuo app to connect with me! 💗")
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Connection Code via"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "No connection code available!", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }

        // Back button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Save action
        btnSave.setOnClickListener { view ->
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (ignored: Exception) {}
            view.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                saveChanges()
            }.start()
        }

        findViewById<android.widget.TextView>(R.id.tv_settings_footer_text).apply {
            text = android.text.Html.fromHtml("Made with ❤️ by <font color='#FF4081'><u>Moajjem</u></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }
        findViewById<android.view.View>(R.id.tv_settings_footer).setOnClickListener {
            startActivity(Intent(this, com.moajjem.myduuo.ui.profile.ProfileActivity::class.java))
        }

        // Live partner status observing for header toolbar
        val viewOnlineIndicator = findViewById<android.view.View>(R.id.view_online_indicator)
        val tvPartnerOnlineStatus = findViewById<android.widget.TextView>(R.id.tv_partner_online_status)
        
        CoroutineScope(Dispatchers.Main).launch {
            appRepository.partnerState.collect { state ->
                if (state == null) {
                    tvPartnerOnlineStatus.text = "Offline"
                    viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
                } else {
                    val isLocked = state.app.contains("Off") || state.app.contains("Locked")
                    val isOnline = (System.currentTimeMillis() / 1000 - state.time < 60) && !isLocked
                    if (isOnline) {
                        tvPartnerOnlineStatus.text = "Online"
                        viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GREEN)
                    } else {
                        tvPartnerOnlineStatus.text = if (isLocked) "Away" else "Offline"
                        viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isLocked) android.graphics.Color.YELLOW else android.graphics.Color.RED)
                    }
                }
            }
        }

        // Bottom Navigation Tab listeners
        findViewById<android.view.View>(R.id.tab_home).setOnClickListener {
            val intent = Intent(this, com.moajjem.myduuo.ui.home.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        findViewById<android.view.View>(R.id.tab_history).setOnClickListener {
            val intent = Intent(this, com.moajjem.myduuo.ui.history.HistoryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        findViewById<android.view.View>(R.id.tab_settings).setOnClickListener {
            // Already here
        }
        findViewById<android.view.View>(R.id.tab_profile).setOnClickListener {
            val intent = Intent(this, com.moajjem.myduuo.ui.profile.ProfileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Footer Credit
        findViewById<android.widget.TextView>(R.id.tv_settings_footer_text).apply {
            text = android.text.Html.fromHtml("Made with <font color='#FF4081'>❤️</font> by <font color='#FF80AB'><b><u>Moajjem</u></b></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }
        findViewById<android.view.View>(R.id.tv_settings_footer).setOnClickListener { view ->
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (ignored: Exception) {}
            view.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                startActivity(Intent(this@SettingsActivity, com.moajjem.myduuo.ui.profile.ProfileActivity::class.java))
            }.start()
        }

        // Reset action
        btnReset.setOnClickListener {
            showResetConfirmation()
        }
    }

    private fun saveChanges() {
        val botToken = etBotToken.text.toString().trim()
        val groupId = etGroupId.text.toString().trim()
        val partnerName = etPartnerName.text.toString().trim()

        if (botToken.isEmpty()) {
            etBotToken.error = "Bot Token cannot be empty"
            return
        }
        if (groupId.isEmpty()) {
            etGroupId.error = "Group ID cannot be empty"
            return
        }
        if (partnerName.isEmpty()) {
            etPartnerName.error = "Partner Name cannot be empty"
            return
        }

        appRepository.saveBotToken(botToken)
        appRepository.saveGroupId(groupId)
        appRepository.savePartnerName(partnerName)

        Toast.makeText(this, "Settings Saved! ❤️", Toast.LENGTH_SHORT).show()

        // Restart background service to apply new credentials
        val serviceIntent = Intent(this, ActivityMonitorService::class.java)
        stopService(serviceIntent)
        ContextCompat.startForegroundService(this, serviceIntent)

        finish()
    }

    private fun showResetConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset MyDuo?")
            .setMessage("Are you sure you want to reset the application? This will permanently erase your Telegram credentials, partner name, local SQLite databases, and stop background tracking.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset App") { _, _ ->
                performReset()
            }
            .show()
    }

    private fun performReset() {
        // Stop service
        val serviceIntent = Intent(this, ActivityMonitorService::class.java)
        stopService(serviceIntent)

        // Clear all data
        appRepository.clearAllData()

        Toast.makeText(this, "Application Reset Completed.", Toast.LENGTH_SHORT).show()

        // Restart app by launching SplashActivity
        val splashIntent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(splashIntent)
        finish()
    }
}
