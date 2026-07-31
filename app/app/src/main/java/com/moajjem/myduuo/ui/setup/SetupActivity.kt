package com.moajjem.myduuo.ui.setup

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.moajjem.myduuo.R
import com.moajjem.myduuo.data.AppRepository
import com.moajjem.myduuo.data.TelegramBotManager
import com.moajjem.myduuo.ui.home.MainActivity
import com.moajjem.myduuo.util.PairingCodeManager
import com.moajjem.myduuo.util.ParsedPairingCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : AppCompatActivity() {

    enum class SetupMode { CREATE, JOIN }

    private var currentMode = SetupMode.CREATE
    private val randomSuffix = (1000..9999).random()
    private var generatedSenderId = ""

    // Mode Selector Views
    private lateinit var btnTabCreate: Button
    private lateinit var btnTabJoin: Button
    private lateinit var panelCreateConnection: LinearLayout
    private lateinit var panelJoinCode: LinearLayout

    // Form 1: Create Connection (1. Token -> 2. Chat ID -> 3. Your Name -> 4. Partner Name -> 5. Your Gender)
    private lateinit var etBotToken: EditText
    private lateinit var etGroupId: EditText
    private lateinit var etMyName: EditText
    private lateinit var etPartnerName: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var tvSenderId: TextView

    // Form 2: Join with Partner Code
    private lateinit var etPartnerCode: EditText
    private lateinit var btnPasteCode: Button
    private lateinit var tvCodeStatus: TextView
    private lateinit var etJoinMyName: EditText
    private lateinit var tvJoinSenderId: TextView

    // Decoded credentials from code paste
    private var decodedCode: ParsedPairingCode? = null

    // General Action
    private lateinit var btnSave: Button
    private lateinit var pbLoader: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Start background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.setup_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1500)
            it.setExitFadeDuration(1500)
            it.start()
        }

        // Mode switchers
        btnTabCreate = findViewById(R.id.btn_tab_create)
        btnTabJoin = findViewById(R.id.btn_tab_join)
        panelCreateConnection = findViewById(R.id.panel_create_connection)
        panelJoinCode = findViewById(R.id.panel_join_code)

        // Bind Form 1
        etBotToken = findViewById(R.id.et_bot_token)
        etGroupId = findViewById(R.id.et_group_id)
        etMyName = findViewById(R.id.et_my_name)
        etPartnerName = findViewById(R.id.et_partner_name)
        rgGender = findViewById(R.id.rg_gender)
        tvSenderId = findViewById(R.id.tv_generated_sender_id)

        // Bind Form 2
        etPartnerCode = findViewById(R.id.et_partner_code)
        btnPasteCode = findViewById(R.id.btn_paste_code)
        tvCodeStatus = findViewById(R.id.tv_code_status)
        etJoinMyName = findViewById(R.id.et_join_my_name)
        tvJoinSenderId = findViewById(R.id.tv_join_generated_sender_id)

        // Bind Common
        btnSave = findViewById(R.id.btn_save)
        pbLoader = findViewById(R.id.pb_loader)

        // Prepopulate existing configuration if available
        val repository = AppRepository.getInstance(this)
        etBotToken.setText(repository.getBotToken() ?: "")
        etGroupId.setText(repository.getGroupId() ?: "")
        etPartnerName.setText(repository.getPartnerName())

        val existingSenderId = repository.getSenderId() ?: ""
        if (existingSenderId.startsWith("Moajjem", ignoreCase = true) || existingSenderId.startsWith("Mun", ignoreCase = true)) {
            generatedSenderId = existingSenderId
        }

        val gender = repository.getGender() ?: "Male"
        if (gender == "Female") {
            rgGender.check(R.id.rb_female)
        } else {
            rgGender.check(R.id.rb_male)
        }

        // Mode switch tab listeners
        btnTabCreate.setOnClickListener { switchMode(SetupMode.CREATE) }
        btnTabJoin.setOnClickListener { switchMode(SetupMode.JOIN) }

        // Update Sender IDs based on gender
        updateSenderId()
        rgGender.setOnCheckedChangeListener { _, _ -> updateSenderId() }

        // Paste Code Action
        btnPasteCode.setOnClickListener { pasteFromClipboard() }

        // Live text watcher for Partner Code input
        etPartnerCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateCodeInput(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Tutorial video and help
        findViewById<View>(R.id.card_watch_tutorial)?.setOnClickListener { openTutorialVideo() }
        findViewById<TextView>(R.id.tv_setup_help).setOnClickListener {
            startActivity(Intent(this, com.moajjem.myduuo.ui.instructions.InstructionsActivity::class.java))
        }

        // Footer Credit
        findViewById<TextView>(R.id.tv_setup_footer_text).apply {
            text = android.text.Html.fromHtml("Made with ❤️ by <font color='#FF4081'><u>Moajjem</u></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }
        findViewById<View>(R.id.tv_setup_footer).setOnClickListener {
            startActivity(Intent(this, com.moajjem.myduuo.ui.profile.ProfileActivity::class.java))
        }

        btnSave.setOnClickListener { saveSettings() }
    }

    private fun switchMode(mode: SetupMode) {
        val isFirstRun = panelCreateConnection.visibility == View.VISIBLE && panelJoinCode.visibility == View.GONE && mode == SetupMode.CREATE
        currentMode = mode

        val createActiveColor = ContextCompat.getColor(this, R.color.romantic_pink)
        val joinActiveColor = ContextCompat.getColor(this, R.color.stroke_purple_glow)
        val inactiveColor = ContextCompat.getColor(this, R.color.glass_card_bg_dark)
        val white = Color.WHITE
        val grayLight = ContextCompat.getColor(this, R.color.gray_light)

        // Haptic feedback
        try {
            btnTabCreate.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (ignored: Exception) {}

        if (mode == SetupMode.CREATE) {
            btnTabCreate.backgroundTintList = android.content.res.ColorStateList.valueOf(createActiveColor)
            btnTabCreate.setTextColor(white)
            btnTabCreate.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).withEndAction {
                btnTabCreate.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()

            btnTabJoin.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnTabJoin.setTextColor(grayLight)
            btnTabJoin.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).withEndAction {
                btnTabJoin.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()

            if (!isFirstRun) {
                panelJoinCode.animate().alpha(0f).translationY(15f).setDuration(120).withEndAction {
                    panelJoinCode.visibility = View.GONE
                    panelCreateConnection.visibility = View.VISIBLE
                    panelCreateConnection.alpha = 0f
                    panelCreateConnection.translationY = -15f
                    panelCreateConnection.animate().alpha(1f).translationY(0f).setDuration(220).start()
                }.start()
            } else {
                panelCreateConnection.visibility = View.VISIBLE
                panelJoinCode.visibility = View.GONE
            }

            btnSave.text = "Save & Create Connection"
            btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(createActiveColor)
        } else {
            btnTabJoin.backgroundTintList = android.content.res.ColorStateList.valueOf(joinActiveColor)
            btnTabJoin.setTextColor(white)
            btnTabJoin.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).withEndAction {
                btnTabJoin.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()

            btnTabCreate.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnTabCreate.setTextColor(grayLight)
            btnTabCreate.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).withEndAction {
                btnTabCreate.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()

            panelCreateConnection.animate().alpha(0f).translationY(15f).setDuration(120).withEndAction {
                panelCreateConnection.visibility = View.GONE
                panelJoinCode.visibility = View.VISIBLE
                panelJoinCode.alpha = 0f
                panelJoinCode.translationY = -15f
                panelJoinCode.animate().alpha(1f).translationY(0f).setDuration(220).start()
            }.start()

            btnSave.text = "Connect with Partner Code"
            btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(joinActiveColor)
        }
        updateSenderId()
    }

    private fun updateSenderId() {
        val isFemale = if (currentMode == SetupMode.CREATE) {
            rgGender.checkedRadioButtonId == R.id.rb_female
        } else {
            val creatorGender = decodedCode?.creatorGender ?: ""
            creatorGender.equals("Male", ignoreCase = true)
        }

        generatedSenderId = if (isFemale) {
            "Mun$randomSuffix"
        } else {
            "Moajjem$randomSuffix"
        }

        tvSenderId.text = "Sender ID: $generatedSenderId"
        tvJoinSenderId.text = "Sender ID: $generatedSenderId"
    }

    private fun pasteFromClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pastedText = clip.getItemAt(0).text.toString().trim()
                if (pastedText.isNotEmpty()) {
                    etPartnerCode.setText(pastedText)
                    Toast.makeText(this, "Partner code pasted! 📋", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No text found in clipboard!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to read clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateCodeInput(code: String) {
        if (code.isBlank()) {
            decodedCode = null
            tvCodeStatus.text = "Enter or paste the connection code sent by your partner."
            tvCodeStatus.setTextColor(ContextCompat.getColor(this, R.color.gray_light))
            updateSenderId()
            return
        }

        val decoded = PairingCodeManager.decodePairingCode(code)
        if (decoded != null) {
            decodedCode = decoded
            var statusMsg = "✅ Connection Code Verified!"

            if (decoded.creatorName.isNotBlank()) {
                statusMsg += "\nPartner Name: ${decoded.creatorName}"
            }
            if (decoded.targetPartnerName.isNotBlank()) {
                etJoinMyName.setText(decoded.targetPartnerName)
            }
            if (decoded.creatorGender.isNotBlank()) {
                statusMsg += " (${decoded.creatorGender})"
            }
            statusMsg += " Auto-Loaded 💕"

            tvCodeStatus.text = statusMsg
            tvCodeStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
        } else {
            decodedCode = null
            tvCodeStatus.text = "❌ Invalid Connection Code. Please check with your partner."
            tvCodeStatus.setTextColor(Color.parseColor("#FF5252")) // Red
        }
        updateSenderId()
    }

    private fun openTutorialVideo() {
        Toast.makeText(this, "Loading tutorial link...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val videoUrl = withContext(Dispatchers.IO) {
                try {
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url("https://raw.githubusercontent.com/Moajjem404/MyDuo/refs/heads/main/v/tut.txt")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.string()?.trim()
                        } else null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val targetUrl = if (!videoUrl.isNullOrEmpty() && (videoUrl.startsWith("http://") || videoUrl.startsWith("https://"))) {
                videoUrl
            } else {
                "https://raw.githubusercontent.com/Moajjem404/MyDuo/refs/heads/main/v/tut.txt"
            }

            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this@SetupActivity, "Unable to open video tutorial link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSettings() {
        val botToken: String
        val groupId: String
        val partnerName: String
        val genderStr: String

        if (currentMode == SetupMode.CREATE) {
            botToken = etBotToken.text.toString().trim()
            groupId = etGroupId.text.toString().trim()
            partnerName = etPartnerName.text.toString().trim()
            val isFemale = rgGender.checkedRadioButtonId == R.id.rb_female
            genderStr = if (isFemale) "Female" else "Male"

            if (botToken.isEmpty()) {
                etBotToken.error = "Bot token is required"
                return
            }
            if (groupId.isEmpty()) {
                etGroupId.error = "Chat ID is required"
                return
            }
            if (partnerName.isEmpty()) {
                etPartnerName.error = "Partner name is required"
                return
            }
        } else {
            val rawCode = etPartnerCode.text.toString().trim()
            val decoded = decodedCode ?: PairingCodeManager.decodePairingCode(rawCode)

            if (decoded == null) {
                etPartnerCode.error = "Invalid or empty Partner Connection Code"
                Toast.makeText(this, "Please paste a valid Partner Code!", Toast.LENGTH_SHORT).show()
                return
            }

            botToken = decoded.botToken
            groupId = decoded.groupId
            // For person B (join mode), person B's partner is creatorName (person A)!
            partnerName = if (decoded.creatorName.isNotBlank()) decoded.creatorName else "Partner"

            // Auto-assign my gender to opposite of partner gender
            val partnerGender = decoded.creatorGender
            genderStr = if (partnerGender.equals("Female", ignoreCase = true)) "Male" else "Female"
        }

        // Ensure generatedSenderId is initialized correctly
        updateSenderId()

        // Show loading state
        pbLoader.visibility = View.VISIBLE
        btnSave.visibility = View.INVISIBLE

        lifecycleScope.launch {
            // Validate bot token with Telegram API
            val isValid = withContext(Dispatchers.IO) {
                TelegramBotManager.validateToken(botToken)
            }

            if (isValid) {
                val repository = AppRepository.getInstance(this@SetupActivity)
                repository.saveBotToken(botToken)
                repository.saveGroupId(groupId)
                repository.saveSenderId(generatedSenderId)
                repository.savePartnerName(partnerName)
                repository.saveGender(genderStr)

                Toast.makeText(this@SetupActivity, "Setup Completed! ❤️", Toast.LENGTH_SHORT).show()

                // Launch Main Screen
                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                finish()
            } else {
                pbLoader.visibility = View.GONE
                btnSave.visibility = View.VISIBLE
                if (currentMode == SetupMode.CREATE) {
                    etBotToken.error = "Invalid Telegram Bot Token!"
                } else {
                    etPartnerCode.error = "Connection Code contains invalid Bot Token!"
                }
                Toast.makeText(this@SetupActivity, "Validation failed. Please check credentials.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
