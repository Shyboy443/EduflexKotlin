package com.example.ed.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.ed.R

class SkeletonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerRadius = 8f
    
    private var shimmerAnimator: ValueAnimator? = null
    private var shimmerOffset = 0f
    
    private val baseColor = ContextCompat.getColor(context, R.color.skeleton_base)
    private val shimmerColor = ContextCompat.getColor(context, R.color.skeleton_shimmer)
    
    init {
        paint.color = baseColor
        setupShimmerAnimation()
    }
    
    private fun setupShimmerAnimation() {
        shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animator ->
                shimmerOffset = animator.animatedValue as Float
                invalidate()
            }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        
        // Draw base skeleton
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        
        // Draw shimmer effect
        if (shimmerAnimator?.isRunning == true) {
            val shimmerWidth = width * 0.3f
            val shimmerStart = (width + shimmerWidth) * shimmerOffset - shimmerWidth
            
            val gradient = LinearGradient(
                shimmerStart, 0f,
                shimmerStart + shimmerWidth, 0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    shimmerColor,
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            
            shimmerPaint.shader = gradient
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shimmerPaint)
        }
    }
    
    fun startShimmer() {
        shimmerAnimator?.start()
    }
    
    fun stopShimmer() {
        shimmerAnimator?.cancel()
        invalidate()
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startShimmer()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }
}