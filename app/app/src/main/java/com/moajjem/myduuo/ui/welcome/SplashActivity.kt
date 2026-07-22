package com.moajjem.myduuo.ui.welcome

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.moajjem.myduuo.R
import com.moajjem.myduuo.data.AppRepository
import com.moajjem.myduuo.ui.home.MainActivity
import com.moajjem.myduuo.ui.setup.SetupActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val subtitleText = "Stay Connected Forever ❤️"
    private var charIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Start breathing background gradient animation
        val rootLayout = findViewById<RelativeLayout>(R.id.splash_root)
        val animDrawable = rootLayout.background as? AnimationDrawable
        animDrawable?.let {
            it.setEnterFadeDuration(1000)
            it.setExitFadeDuration(1000)
            it.start()
        }

        // Footer credit HTML
        findViewById<TextView>(R.id.tv_splash_footer).apply {
            text = android.text.Html.fromHtml(
                "Made with ❤️ by <font color='#FF4081'>Moajjem</font>",
                android.text.Html.FROM_HTML_MODE_LEGACY
            )
        }

        // Start the orchestrated animation sequence
        startAnimationSequence()
    }

    private fun startAnimationSequence() {
        val logo = findViewById<View>(R.id.iv_splash_logo)
        val glowRing = findViewById<View>(R.id.glow_ring)
        val orbitOuter = findViewById<View>(R.id.orbit_ring_outer)
        val orbitInner = findViewById<View>(R.id.orbit_ring_inner)
        val titleContainer = findViewById<View>(R.id.title_container)
        val subtitle = findViewById<TextView>(R.id.tv_splash_subtitle)
        val shimmerLine = findViewById<View>(R.id.shimmer_line)
        val versionText = findViewById<View>(R.id.tv_splash_version)
        val footerContainer = findViewById<View>(R.id.footer_container)

        val particle1 = findViewById<View>(R.id.particle_1)
        val particle2 = findViewById<View>(R.id.particle_2)
        val particle3 = findViewById<View>(R.id.particle_3)
        val particle4 = findViewById<View>(R.id.particle_4)

        // Phase 1: Logo entrance (scale up + fade in) with glow
        handler.postDelayed({
            // Glow ring fade in
            ObjectAnimator.ofFloat(glowRing, "alpha", 0f, 0.6f).apply {
                duration = 800
                interpolator = DecelerateInterpolator()
                start()
            }

            // Logo scale up with overshoot
            logo.alpha = 0f
            logo.scaleX = 0.3f
            logo.scaleY = 0.3f
            val logoFade = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f)
            val logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.3f, 1f)
            val logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.3f, 1f)
            AnimatorSet().apply {
                playTogether(logoFade, logoScaleX, logoScaleY)
                duration = 700
                interpolator = OvershootInterpolator(1.5f)
                start()
            }

            // Glow pulse animation (continuous)
            val glowPulse = ObjectAnimator.ofFloat(glowRing, "alpha", 0.3f, 0.7f).apply {
                duration = 1500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            handler.postDelayed({ glowPulse.start() }, 800)
        }, 300)

        // Phase 2: Orbit rings appear + start rotating
        handler.postDelayed({
            // Outer ring
            ObjectAnimator.ofFloat(orbitOuter, "alpha", 0f, 0.8f).apply {
                duration = 500
                start()
            }
            ObjectAnimator.ofFloat(orbitOuter, "rotation", 0f, 360f).apply {
                duration = 8000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }

            // Inner ring (opposite direction)
            ObjectAnimator.ofFloat(orbitInner, "alpha", 0f, 0.5f).apply {
                duration = 500
                start()
            }
            ObjectAnimator.ofFloat(orbitInner, "rotation", 0f, -360f).apply {
                duration = 6000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }, 600)

        // Phase 3: Title slide up + fade in
        handler.postDelayed({
            titleContainer.translationY = 30f
            val titleFade = ObjectAnimator.ofFloat(titleContainer, "alpha", 0f, 1f)
            val titleSlide = ObjectAnimator.ofFloat(titleContainer, "translationY", 30f, 0f)
            AnimatorSet().apply {
                playTogether(titleFade, titleSlide)
                duration = 600
                interpolator = DecelerateInterpolator()
                start()
            }
        }, 900)

        // Phase 4: Typewriter subtitle effect
        handler.postDelayed({
            subtitle.alpha = 1f
            startTypewriterEffect(subtitle)
        }, 1300)

        // Phase 5: Shimmer line + version
        handler.postDelayed({
            ObjectAnimator.ofFloat(shimmerLine, "alpha", 0f, 1f).apply {
                duration = 400
                start()
            }

            // Shimmer translation animation
            ObjectAnimator.ofFloat(shimmerLine, "translationX", -100f, 100f).apply {
                duration = 1500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            ObjectAnimator.ofFloat(versionText, "alpha", 0f, 1f).apply {
                duration = 400
                startDelay = 200
                start()
            }
        }, 1600)

        // Phase 6: Floating particles
        handler.postDelayed({
            animateParticle(particle1, -20f, 200)
            animateParticle(particle2, 15f, 400)
            animateParticle(particle3, -10f, 100)
            animateParticle(particle4, 20f, 300)
        }, 800)

        // Phase 7: Footer fade in
        handler.postDelayed({
            ObjectAnimator.ofFloat(footerContainer, "alpha", 0f, 1f).apply {
                duration = 500
                start()
            }
        }, 2000)

        // Phase 8: Navigate after delay
        handler.postDelayed({
            checkPermissionsAndRoute()
        }, 3200)
    }

    private fun startTypewriterEffect(tv: TextView) {
        if (charIndex <= subtitleText.length) {
            tv.text = subtitleText.substring(0, charIndex)
            charIndex++
            handler.postDelayed({ startTypewriterEffect(tv) }, 50)
        }
    }

    private fun animateParticle(view: View, drift: Float, delay: Long) {
        handler.postDelayed({
            view.alpha = 0f
            val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 0.8f)
            val moveUp = ObjectAnimator.ofFloat(view, "translationY", 0f, -40f)
            val moveX = ObjectAnimator.ofFloat(view, "translationX", 0f, drift)

            AnimatorSet().apply {
                playTogether(fadeIn, moveUp, moveX)
                duration = 2000
                interpolator = DecelerateInterpolator()
                start()
            }

            // Continuous gentle floating
            ObjectAnimator.ofFloat(view, "translationY", -40f, -60f).apply {
                duration = 2500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = 2000
                start()
            }
        }, delay)
    }

    private fun checkPermissionsAndRoute() {
        val allGranted = hasUsageStatsPermission() &&
                hasNotificationPermission() &&
                isIgnoringBatteryOptimizations()

        if (!allGranted) {
            startActivity(Intent(this, WelcomeActivity::class.java))
        } else {
            val repository = AppRepository.getInstance(this)
            if (!repository.isSetupComplete()) {
                startActivity(Intent(this, SetupActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
