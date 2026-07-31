package com.moajjem.myduuo.ui.history

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.moajjem.myduuo.R
import com.moajjem.myduuo.data.AppRepository
import com.moajjem.myduuo.data.DatabaseHelper
import com.moajjem.myduuo.ui.home.MainActivity
import com.moajjem.myduuo.ui.profile.ProfileActivity
import com.moajjem.myduuo.ui.settings.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var databaseHelper: DatabaseHelper
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        appRepository = AppRepository.getInstance(this)
        databaseHelper = DatabaseHelper.getInstance(this)

        // Start background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.history_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1500)
            it.setExitFadeDuration(1500)
            it.start()
        }

        // Back button on toolbar
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Live partner status observing for header toolbar
        val viewOnlineIndicator = findViewById<View>(R.id.view_online_indicator)
        val tvPartnerOnlineStatus = findViewById<TextView>(R.id.tv_partner_online_status)
        
        CoroutineScope(Dispatchers.Main).launch {
            appRepository.partnerState.collect { state ->
                if (state == null) {
                    tvPartnerOnlineStatus.text = "Offline"
                    viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
                } else {
                    val isLocked = state.app.contains("Off") || state.app.contains("Locked")
                    val isOnline = (System.currentTimeMillis() / 1000 - state.time < 60) && !isLocked
                    if (isOnline) {
                        tvPartnerOnlineStatus.text = "Online"
                        viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GREEN)
                    } else {
                        tvPartnerOnlineStatus.text = if (isLocked) "Away" else "Offline"
                        viewOnlineIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isLocked) Color.YELLOW else Color.RED)
                    }
                }
            }
        }

        // Footer Credit
        findViewById<TextView>(R.id.tv_history_footer_text).apply {
            text = android.text.Html.fromHtml("Made with <font color='#FF4081'>❤️</font> by <font color='#FF80AB'><b><u>Moajjem</u></b></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }
        findViewById<View>(R.id.tv_history_footer).setOnClickListener { view ->
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (ignored: Exception) {}
            view.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                startActivity(Intent(this@HistoryActivity, ProfileActivity::class.java))
            }.start()
        }

        // Bottom Navigation Tab listeners
        findViewById<View>(R.id.tab_home).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.tab_history).setOnClickListener {
            // Already here
        }
        findViewById<View>(R.id.tab_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.tab_profile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Load History Items
        loadHistoryList()
    }

    private fun loadHistoryList() {
        val container = findViewById<LinearLayout>(R.id.history_list_container)
        val layoutEmpty = findViewById<View>(R.id.layout_empty_history)
        container.removeAllViews()

        val historyList = databaseHelper.getPartnerHistory()

        if (historyList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            container.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            container.visibility = View.VISIBLE

            val cardDrawables = listOf(
                R.drawable.glass_card_pink_border,
                R.drawable.glass_card_purple_border,
                R.drawable.glass_card_cyan_border,
                R.drawable.glass_card_gold_border
            )

            for ((index, item) in historyList.withIndex()) {
                val drawableRes = cardDrawables[index % cardDrawables.size]
                val rowView = createHistoryRow(item.app, item.time, drawableRes)
                container.addView(rowView)
            }
        }
    }

    private fun createHistoryRow(app: String, time: Long, drawableRes: Int): View {
        // Root container for row item
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            background = ContextCompat.getDrawable(this@HistoryActivity, drawableRes)
            isClickable = true
            isFocusable = true
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            layoutParams = params
            
            setOnClickListener { view ->
                try {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (ignored: Exception) {}
                view.animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).withEndAction {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()
            }
        }

        // Circular Image View
        val imageView = com.google.android.material.imageview.ShapeableImageView(this).apply {
            val size = dpToPx(38)
            layoutParams = LinearLayout.LayoutParams(size, size)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(com.google.android.material.shape.RelativeCornerSize(0.5f))
                .build()
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "App Icon"
        }
        mapAppIcon(app, imageView)
        row.addView(imageView)

        // Text details container
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(dpToPx(14), 0, 0, 0)
            }
            layoutParams = params
        }

        // App title text
        val tvTitle = TextView(this).apply {
            text = app
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }
        textContainer.addView(tvTitle)

        // Relative duration or absolute time details
        val tvDetail = TextView(this).apply {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = sdf.format(Date(time * 1000))
            
            val diffSec = (System.currentTimeMillis() / 1000) - time
            val timeText = if (diffSec < 60) {
                "Just now ⚡"
            } else if (diffSec < 3600) {
                "${diffSec / 60}m ago"
            } else {
                "${diffSec / 3600}h ago"
            }
            
            text = "$formattedTime • $timeText"
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 12f
            setPadding(0, dpToPx(3), 0, 0)
        }
        textContainer.addView(tvDetail)

        row.addView(textContainer)

        return row
    }

    private fun mapAppIcon(appName: String, imageView: ImageView) {
        val cleanName = appName.trim().lowercase(Locale.getDefault())

        val resId = when {
            cleanName.contains("chrome") -> R.drawable.ic_app_chrome
            cleanName.contains("facebook") && !cleanName.contains("messenger") && !cleanName.contains("orca") -> R.drawable.ic_app_facebook
            cleanName.contains("telegram") || cleanName.contains("org.telegram") -> R.drawable.ic_app_telegram
            cleanName.contains("messenger") || cleanName.contains("orca") || cleanName.contains("mlite") -> R.drawable.ic_app_messenger
            cleanName.contains("instagram") -> R.drawable.ic_app_instagram
            cleanName.contains("youtube") || cleanName.contains("yt") -> R.drawable.ic_app_youtube
            cleanName.contains("whatsapp") -> R.drawable.ic_app_whatsapp
            cleanName.contains("tiktok") || cleanName.contains("tik tok") || cleanName.contains("musically") -> R.drawable.ic_app_tiktok
            cleanName.contains("twitter") || cleanName == "x" -> R.drawable.ic_app_twitter
            cleanName.contains("snapchat") -> R.drawable.ic_app_snapchat
            cleanName.contains("spotify") -> R.drawable.ic_app_spotify
            cleanName.contains("netflix") -> R.drawable.ic_app_netflix
            cleanName.contains("gmail") || cleanName.contains("mail") || cleanName.contains("android.gm") -> R.drawable.ic_app_gmail
            cleanName.contains("discord") -> R.drawable.ic_app_discord
            cleanName.contains("linkedin") -> R.drawable.ic_app_linkedin
            cleanName.contains("reddit") -> R.drawable.ic_app_reddit
            cleanName.contains("camera") -> R.drawable.ic_app_camera
            cleanName.contains("phone") || cleanName.contains("dialer") || cleanName.contains("call") -> R.drawable.ic_app_phone
            cleanName.contains("setting") -> R.drawable.ic_app_settings
            cleanName.contains("launcher") || cleanName.contains("home") || cleanName.contains("poco") || cleanName.contains("pixel") || cleanName.contains("oneui") || cleanName.contains("miui") -> R.drawable.ic_app_launcher
            cleanName.contains("locked") || cleanName.contains("lock") || cleanName.contains("screen off") || cleanName.contains("off") -> R.drawable.ic_app_locked
            else -> R.drawable.ic_app_default
        }
        imageView.setImageResource(resId)
        imageView.imageTintList = null
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
