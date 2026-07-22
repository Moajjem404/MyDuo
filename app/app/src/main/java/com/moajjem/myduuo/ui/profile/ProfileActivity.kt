package com.moajjem.myduuo.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.moajjem.myduuo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Start background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.profile_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1500)
            it.setExitFadeDuration(1500)
            it.start()
        }

        // Display current app version dynamically
        val tvVersion = findViewById<TextView>(R.id.tv_profile_version)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Version: ${pInfo.versionName}"
        } catch (e: Exception) {
            tvVersion.text = "Version: 0.3"
        }

        // Setup profile picture download & disk caching
        val ivProfilePic = findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.iv_profile_pic)
        val cacheFile = File(filesDir, "moajjem_profile.jpg")

        if (cacheFile.exists()) {
            Log.d(TAG, "Profile image cache found. Loading from disk...")
            try {
                val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bitmap != null) {
                    ivProfilePic.setImageBitmap(bitmap)
                } else {
                    Log.w(TAG, "Cached bitmap decode failed, downloading...")
                    downloadAndCacheProfileImage(cacheFile, ivProfilePic)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cached image: ${e.message}", e)
                downloadAndCacheProfileImage(cacheFile, ivProfilePic)
            }
        } else {
            Log.d(TAG, "No profile image cache. Downloading...")
            downloadAndCacheProfileImage(cacheFile, ivProfilePic)
        }

        // Back button action
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // GitHub Button action
        findViewById<Button>(R.id.btn_github).setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Moajjem404"))
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Telegram Button action
        findViewById<Button>(R.id.btn_telegram).setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Moajjem404"))
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        findViewById<TextView>(R.id.tv_profile_footer_text).apply {
            text = android.text.Html.fromHtml("Made with ❤️ by <font color='#FF4081'><u>Moajjem</u></font>", android.text.Html.FROM_HTML_MODE_LEGACY)
        }

        // Live partner status observing for header toolbar
        val appRepository = com.moajjem.myduuo.data.AppRepository.getInstance(this)
        val viewOnlineIndicator = findViewById<android.view.View>(R.id.view_online_indicator)
        val tvPartnerOnlineStatus = findViewById<TextView>(R.id.tv_partner_online_status)
        
        // Use CoroutineScope to collect state flow
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

        // Back action for toolbar back button
        findViewById<android.view.View>(R.id.btn_back).setOnClickListener {
            finish()
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
            android.widget.Toast.makeText(this, "History feature coming soon! 💗", android.widget.Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.tab_settings).setOnClickListener {
            val intent = Intent(this, com.moajjem.myduuo.ui.settings.SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        findViewById<android.view.View>(R.id.tab_profile).setOnClickListener {
            // Already here
        }
    }

    private fun downloadAndCacheProfileImage(destFile: File, ivProfilePic: com.google.android.material.imageview.ShapeableImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://raw.githubusercontent.com/Moajjem404/MyDuo/refs/heads/main/img/moajjem.jpg"
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            // Save bytes to file (Cache on disk)
                            destFile.writeBytes(bytes)
                            Log.d(TAG, "Profile image successfully downloaded and cached to: ${destFile.absolutePath}")

                            // Decode bitmap
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                withContext(Dispatchers.Main) {
                                    ivProfilePic.setImageBitmap(bitmap)
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Failed to download image. Server responded with: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading profile image: ${e.message}", e)
            }
        }
    }
}
