package com.example.ed.utils

import android.animation.*
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

object AnimationUtils {

    private const val DEFAULT_DURATION = 300L
    private const val FAST_DURATION = 150L
    private const val SLOW_DURATION = 500L

    /**
     * Fade in animation
     */
    fun fadeIn(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.alpha = 0f
        view.isVisible = true
        
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onComplete?.invoke() }
            .start()
    }

    /**
     * Fade out animation
     */
    fun fadeOut(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { 
                view.isVisible = false
                onComplete?.invoke() 
            }
            .start()
    }

    /**
     * Slide in from bottom animation
     */
    fun slideInFromBottom(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        val originalY = view.translationY
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.isVisible = true
        
        view.animate()
            .translationY(originalY)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction { onComplete?.invoke() }
            .start()
    }

    /**
     * Slide out to bottom animation
     */
    fun slideOutToBottom(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction { 
                view.isVisible = false
                view.translationY = 0f
                onComplete?.invoke() 
            }
            .start()
    }

    /**
     * Scale animation for button press feedback
     */
    fun scalePress(view: View, onComplete: (() -> Unit)? = null) {
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0.95f),
            PropertyValuesHolder.ofFloat("scaleY", 0.95f)
        ).apply {
            duration = FAST_DURATION / 2
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f)
        ).apply {
            duration = FAST_DURATION / 2
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Expand animation for collapsible views
     */
    fun expand(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec((view.parent as View).width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        val targetHeight = view.measuredHeight
        view.layoutParams.height = 0
        view.isVisible = true
        
        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener { animation ->
            view.layoutParams.height = animation.animatedValue as Int
            view.requestLayout()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                onComplete?.invoke()
            }
        })
        animator.duration = duration
        animator.interpolator = FastOutSlowInInterpolator()
        animator.start()
    }

    /**
     * Collapse animation for collapsible views
     */
    fun collapse(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        val initialHeight = view.measuredHeight
        
        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener { animation ->
            view.layoutParams.height = animation.animatedValue as Int
            view.requestLayout()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.isVisible = false
                view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                onComplete?.invoke()
            }
        })
        animator.duration = duration
        animator.interpolator = FastOutSlowInInterpolator()
        animator.start()
    }

    /**
     * Staggered animation for list items
     */
    fun staggeredFadeIn(views: List<View>, delayBetween: Long = 50L) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * delayBetween)
                .setDuration(DEFAULT_DURATION)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
    }

    /**
     * Ripple effect animation
     */
    fun rippleEffect(view: View, centerX: Float, centerY: Float) {
        val maxRadius = kotlin.math.max(view.width, view.height).toFloat()
        
        val animator = ValueAnimator.ofFloat(0f, maxRadius)
        animator.addUpdateListener { animation ->
            val radius = animation.animatedValue as Float
            // This would typically be used with a custom drawable or canvas drawing
            view.invalidate()
        }
        animator.duration = DEFAULT_DURATION
        animator.interpolator = FastOutSlowInInterpolator()
        animator.start()
    }

    /**
     * Shake animation for error feedback
     */
    fun shake(view: View, intensity: Float = 10f) {
        val shake = ObjectAnimator.ofFloat(
            view, "translationX",
            0f, intensity, -intensity, intensity, -intensity, intensity/2, -intensity/2, 0f
        )
        shake.duration = SLOW_DURATION
        shake.interpolator = AccelerateDecelerateInterpolator()
        shake.start()
    }

    /**
     * Pulse animation for attention
     */
    fun pulse(view: View, scale: Float = 1.1f, duration: Long = DEFAULT_DURATION) {
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", scale),
            PropertyValuesHolder.ofFloat("scaleY", scale)
        ).apply {
            this.duration = duration / 2
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f)
        ).apply {
            this.duration = duration / 2
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        AnimatorSet().apply {
            playSequentially(scaleUp, scaleDown)
            start()
        }
    }
}