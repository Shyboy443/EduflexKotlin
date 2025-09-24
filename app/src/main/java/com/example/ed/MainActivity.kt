package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.request.CachePolicy

class MainActivity : ComponentActivity() {

    private lateinit var pager: ViewPager2
    private var pagerIdle = true
    private val clickGuard = ClickGuard(250L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if user has already selected a theme
        if (ThemeManager.isThemeSelected(this)) {
            // Skip onboarding and go directly to login
            goToLogin()
            return
        }

        pager = ViewPager2(this).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            // setPageTransformer(SimpleSlideTransformer()) // Temporarily commented out for performance testing
            offscreenPageLimit = 1

            // Disable state restore on pager and its inner RecyclerView
            isSaveEnabled = false
            isSaveFromParentEnabled = false

            (getChildAt(0) as RecyclerView).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                itemAnimator = null
                setHasFixedSize(true)
                setItemViewCacheSize(4)
                isSaveEnabled = false
                isSaveFromParentEnabled = false
            }

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    pagerIdle = state == ViewPager2.SCROLL_STATE_IDLE
                }
            })
        }

        val pageLayouts = listOf(
            R.layout.fragment_onboarding,   // 0 Welcome
            R.layout.fragment_onboarding1,  // 1 Explore
            R.layout.fragment_onboarding2,  // 2 Live
            R.layout.fragment_onboarding3   // 3 Rewards
        )

        val pageImages = intArrayOf(
            R.drawable.img1, R.drawable.img2, R.drawable.img3, R.drawable.img4
        )

        val adapter = OnboardingAdapter(
            layouts = pageLayouts,
            bgImages = pageImages,
            // onSkip = { goToHome() }, // Removed as it's unused
            onNext = {
                if (!pagerIdle || !clickGuard.allowed()) return@OnboardingAdapter
                val max = pageLayouts.size - 1
                if (pager.currentItem < max) pager.setCurrentItem(pager.currentItem + 1, true)
            },
            onBack = {
                if (!pagerIdle || !clickGuard.allowed()) return@OnboardingAdapter
                pager.setCurrentItem((pager.currentItem - 1).coerceAtLeast(0), true)
            },
            onFinish = {
                if (!pagerIdle || !clickGuard.allowed()) return@OnboardingAdapter
                goToThemeSelection()
            }
        ).also {
            it.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT
        }

        pager.adapter = adapter

        // Force page 0 immediately…
        pager.setCurrentItem(0, false)

        setContentView(pager)

        // …and once more AFTER layout to beat any late restores
        pager.post { pager.setCurrentItem(0, false) }
    }

    // Prevent Activity state restore for this screen
    @Suppress("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) { /* no-op */ }

    @Suppress("MissingSuperCall")
    override fun onRestoreInstanceState(savedInstanceState: Bundle) { /* no-op */ }

    private fun goToThemeSelection() {
        startActivity(Intent(this, ThemeSelectionActivity::class.java))
        finish()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}

/** Slide-only transformer — no fade/scale artifacts */
class SimpleSlideTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        when {
            position < -1f -> page.alpha = 0f
            position <= 1f -> {
                page.alpha = 1f
                page.translationX = -page.width * position
                page.scaleX = 1f
                page.scaleY = 1f
            }
            else -> page.alpha = 0f
        }
    }
}

private class ClickGuard(private val intervalMs: Long) {
    private var lastClick = 0L
    fun allowed(): Boolean {
        val now = System.currentTimeMillis()
        val ok = now - lastClick >= intervalMs
        if (ok) lastClick = now
        return ok
    }
}

/** Adapter: load bg with Coil; DON'T bring scrim to front (elevations handle it) */
private class OnboardingAdapter(
    private val layouts: List<Int>,
    private val bgImages: IntArray,
    // private val onSkip: () -> Unit, // Removed as it's unused
    private val onNext: () -> Unit,
    private val onBack: () -> Unit,
    private val onFinish: () -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.VH>() {

    class VH(val root: View) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val v = holder.root

        v.findViewById<ImageView?>(R.id.bg)?.apply {
            setImageDrawable(null)
            setBackgroundColor("#B3000000".toColorInt())
            load(bgImages[position]) {
                crossfade(false)
                memoryCachePolicy(CachePolicy.ENABLED)
                diskCachePolicy(CachePolicy.ENABLED)
                allowHardware(true)
            }
        }

        // Buttons

        v.findViewById<View?>(R.id.button_next_onboarding0)?.setOnClickListener { onNext() }

        v.findViewById<View?>(R.id.button_back_onboarding1)?.setOnClickListener { onBack() }
        v.findViewById<View?>(R.id.next_button)?.setOnClickListener { onNext() }

        v.findViewById<View?>(R.id.button_back_onboarding2)?.setOnClickListener { onBack() }
        v.findViewById<View?>(R.id.button_next_onboarding2)?.setOnClickListener { onNext() }

        v.findViewById<View?>(R.id.button_back_onboarding3)?.setOnClickListener { onBack() }
        v.findViewById<View?>(R.id.button_finish_onboarding3)?.setOnClickListener { onFinish() }
    }

    override fun onViewRecycled(holder: VH) {
        holder.root.findViewById<ImageView?>(R.id.bg)?.apply {
            setImageDrawable(null)
            background = null
        }
        super.onViewRecycled(holder)
    }

    override fun getItemViewType(position: Int): Int = layouts[position]
    override fun getItemCount(): Int = layouts.size
}
