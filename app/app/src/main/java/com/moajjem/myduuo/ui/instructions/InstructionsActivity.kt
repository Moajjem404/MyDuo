package com.moajjem.myduuo.ui.instructions

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.moajjem.myduuo.R

class InstructionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        // Start background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.instructions_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1500)
            it.setExitFadeDuration(1500)
            it.start()
        }

        // Back button action
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Set custom footer
        findViewById<android.widget.TextView>(R.id.tv_instructions_footer_text).apply {
            text = android.text.Html.fromHtml("Made with ❤️ by <font color='#FF4081'><u>Moajjem</u></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }
        findViewById<android.view.View>(R.id.tv_instructions_footer).setOnClickListener {
            startActivity(android.content.Intent(this, com.moajjem.myduuo.ui.profile.ProfileActivity::class.java))
        }
    }
}
