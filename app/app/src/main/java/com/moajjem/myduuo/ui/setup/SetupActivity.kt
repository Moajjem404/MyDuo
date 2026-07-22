package com.moajjem.myduuo.ui.setup

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.moajjem.myduuo.R
import com.moajjem.myduuo.data.AppRepository
import com.moajjem.myduuo.data.TelegramBotManager
import com.moajjem.myduuo.ui.home.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : AppCompatActivity() {

    private lateinit var etBotToken: EditText
    private lateinit var etGroupId: EditText
    private lateinit var etPartnerName: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var tvSenderId: TextView
    private lateinit var btnSave: Button
    private lateinit var pbLoader: ProgressBar

    private val randomSuffix = (1000..9999).random()
    private var generatedSenderId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Start breathing background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.setup_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1500)
            it.setExitFadeDuration(1500)
            it.start()
        }

        etBotToken = findViewById(R.id.et_bot_token)
        etGroupId = findViewById(R.id.et_group_id)
        etPartnerName = findViewById(R.id.et_partner_name)
        rgGender = findViewById(R.id.rg_gender)
        tvSenderId = findViewById(R.id.tv_generated_sender_id)
        btnSave = findViewById(R.id.btn_save)
        pbLoader = findViewById(R.id.pb_loader)

        // Prepopulate if settings already exist
        val repository = AppRepository.getInstance(this)
        etBotToken.setText(repository.getBotToken() ?: "")
        etGroupId.setText(repository.getGroupId() ?: "")
        etPartnerName.setText(repository.getPartnerName())
        
        val gender = repository.getGender() ?: "Male"
        if (gender == "Female") {
            rgGender.check(R.id.rb_female)
        } else {
            rgGender.check(R.id.rb_male)
        }

        // Initialize and listen for gender changes
        updateSenderId()
        rgGender.setOnCheckedChangeListener { _, _ ->
            updateSenderId()
        }

        findViewById<android.view.View>(R.id.card_watch_tutorial)?.setOnClickListener {
            openTutorialVideo()
        }

        findViewById<TextView>(R.id.tv_setup_help).setOnClickListener {
            startActivity(Intent(this, com.moajjem.myduuo.ui.instructions.InstructionsActivity::class.java))
        }

        findViewById<TextView>(R.id.tv_setup_footer_text).apply {
            text = android.text.Html.fromHtml("Made with ❤️ by <font color='#FF4081'><u>Moajjem</u></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }
        findViewById<android.view.View>(R.id.tv_setup_footer).setOnClickListener {
            startActivity(Intent(this, com.moajjem.myduuo.ui.profile.ProfileActivity::class.java))
        }

        btnSave.setOnClickListener {
            saveSettings()
        }
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
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this@SetupActivity, "Unable to open video tutorial link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSenderId() {
        val selectedId = rgGender.checkedRadioButtonId
        generatedSenderId = if (selectedId == R.id.rb_female) {
            "Mun$randomSuffix"
        } else {
            "Moajjem$randomSuffix"
        }
        tvSenderId.text = "Sender ID: $generatedSenderId"
    }

    private fun saveSettings() {
        val botToken = etBotToken.text.toString().trim()
        val groupId = etGroupId.text.toString().trim()
        val partnerName = etPartnerName.text.toString().trim()
        val isFemale = rgGender.checkedRadioButtonId == R.id.rb_female
        val genderStr = if (isFemale) "Female" else "Male"

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

        // Show loading state
        pbLoader.visibility = View.VISIBLE
        btnSave.visibility = View.INVISIBLE

        lifecycleScope.launch {
            // Validate bot token in the background using OkHttp
            val isValid = withContext(Dispatchers.IO) {
                TelegramBotManager.validateToken(botToken)
            }

            if (isValid) {
                val repository = AppRepository.getInstance(this@SetupActivity)
                repository.saveBotToken(botToken)
                repository.saveGroupId(groupId)
                repository.savePartnerName(partnerName)
                repository.saveGender(genderStr)
                repository.saveSenderId(generatedSenderId)

                Toast.makeText(this@SetupActivity, "Setup Completed! ❤️", Toast.LENGTH_SHORT).show()
                
                // Go to Main Screen
                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                finish()
            } else {
                pbLoader.visibility = View.GONE
                btnSave.visibility = View.VISIBLE
                etBotToken.error = "Invalid Telegram Bot Token!"
                Toast.makeText(this@SetupActivity, "Validation failed. Please verify Bot Token.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
