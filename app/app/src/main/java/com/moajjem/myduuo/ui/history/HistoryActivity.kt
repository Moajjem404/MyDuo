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
            text = android.text.Html.fromHtml("Made with ❤️ by <font color='#FF4081'><u>Moajjem</u></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }
        findViewById<View>(R.id.tv_history_footer).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
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

            for (item in historyList) {
                val rowView = createHistoryRow(item.app, item.time)
                container.addView(rowView)
            }
        }
    }

    private fun createHistoryRow(app: String, time: Long): View {
        // Root container for row item
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            background = ContextCompat.getDrawable(this@HistoryActivity, R.drawable.glass_card)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            layoutParams = params
        }

        // Circular Image View
        val imageView = com.google.android.material.imageview.ShapeableImageView(this).apply {
            val size = dpToPx(36)
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
                setMargins(dpToPx(16), 0, 0, 0)
            }
            layoutParams = params
        }

        // App title text
        val tvTitle = TextView(this).apply {
            text = app
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }
        textContainer.addView(tvTitle)

        // Relative duration or absolute time details
        val tvDetail = TextView(this).apply {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = sdf.format(Date(time * 1000))
            
            val diffSec = (System.currentTimeMillis() / 1000) - time
            val timeText = if (diffSec < 60) {
                "Just now"
            } else if (diffSec < 3600) {
                "${diffSec / 60} min ago"
            } else {
                "${diffSec / 3600} hours ago"
            }
            
            text = "$formattedTime ($timeText)"
            setTextColor(Color.parseColor("#B0BEC5"))
            textSize = 12f
            setPadding(0, dpToPx(2), 0, 0)
        }
        textContainer.addView(tvDetail)

        row.addView(textContainer)

        return row
    }

    private fun mapAppIcon(appName: String, imageView: ImageView) {
        val cleanName = appName.trim().lowercase(Locale.getDefault())
        val iconFile = File(filesDir, "${cleanName}_icon.png")

        if (iconFile.exists()) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(iconFile.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback drawables
        val resId = when {
            cleanName.contains("facebook") -> android.R.drawable.ic_menu_slideshow
            cleanName.contains("youtube") -> android.R.drawable.ic_media_play
            cleanName.contains("chrome") -> android.R.drawable.ic_menu_compass
            cleanName.contains("telegram") -> android.R.drawable.ic_menu_send
            cleanName.contains("messenger") -> android.R.drawable.ic_menu_gallery
            cleanName.contains("instagram") -> android.R.drawable.ic_menu_camera
            cleanName.contains("whatsapp") -> android.R.drawable.ic_menu_call
            cleanName.contains("locked") -> android.R.drawable.ic_lock_idle_lock
            else -> android.R.drawable.ic_menu_info_details
        }
        imageView.setImageResource(resId)
        imageView.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4081"))
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
