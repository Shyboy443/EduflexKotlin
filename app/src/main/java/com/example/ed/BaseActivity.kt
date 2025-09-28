package com.example.ed

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ed.utils.PerformanceOptimizer
import com.example.ed.utils.AccessibilityHelper
import com.example.ed.utils.AnimationUtils
import kotlinx.coroutines.launch

/**
 * Base activity that provides common functionality and optimizations
 * for all activities in the app
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply current theme before any UI setup
        ThemeManager.applyCurrentTheme(this)
        
        // Initialize performance monitoring
        PerformanceOptimizer.initialize(this)
        lifecycle.addObserver(PerformanceOptimizer)
        
        // Setup accessibility
        AccessibilityHelper.applyAccessibilityImprovements(findViewById(android.R.id.content), this)
        
        // Monitor memory usage
        monitorMemoryUsage()
    }

    override fun onResume() {
        super.onResume()
        // Clear any cached data that might be stale
        PerformanceOptimizer.NetworkOptimizer.clearRequestCache()
    }

    override fun onPause() {
        super.onPause()
        // Trim memory when app goes to background
        if (isFinishing) {
            PerformanceOptimizer.clearCaches()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(PerformanceOptimizer)
    }

    /**
     * Helper method to configure RecyclerView with optimizations
     */
    protected fun optimizeRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView, itemDescription: String) {
        PerformanceOptimizer.optimizeRecyclerView(recyclerView)
        AccessibilityHelper.configureRecyclerViewAccessibility(recyclerView, itemDescription)
    }

    /**
     * Helper method to check if accessibility is enabled
     */
    protected fun isAccessibilityEnabled(): Boolean {
        return AccessibilityHelper.isAccessibilityEnabled(this)
    }

    /**
     * Helper method to check memory usage
     */
    protected fun checkMemoryUsage(): Float {
        return PerformanceOptimizer.MemoryUtils.getMemoryUsagePercentage()
    }

    /**
     * Helper method to handle low memory situations
     */
    protected fun handleLowMemory() {
        if (PerformanceOptimizer.MemoryUtils.isLowMemory()) {
            PerformanceOptimizer.clearCaches()
            announceToAccessibility("Memory optimized")
        }
    }

    /**
     * Helper method for accessibility announcements
     */
    protected fun announceToAccessibility(message: String) {
        val rootView = findViewById<android.view.ViewGroup>(android.R.id.content)
        rootView?.let { AccessibilityHelper.announceForAccessibility(it, message) }
    }

    /**
     * Helper method to show loading with animation
     */
    protected fun showLoadingWithAnimation(view: android.view.View, message: String = "Loading...") {
        AnimationUtils.fadeIn(view) {
            if (view is com.example.ed.ui.LoadingStateView) {
                view.showLoading(message)
            }
        }
    }

    /**
     * Helper method to hide loading with animation
     */
    protected fun hideLoadingWithAnimation(view: android.view.View, onComplete: (() -> Unit)? = null) {
        AnimationUtils.fadeOut(view) {
            if (view is com.example.ed.ui.LoadingStateView) {
                view.hide()
            }
            onComplete?.invoke()
        }
    }

    /**
     * Helper method to show error with shake animation
     */
    protected fun showErrorWithAnimation(view: android.view.View, message: String, onRetry: (() -> Unit)? = null) {
        AnimationUtils.shake(view)
        if (view is com.example.ed.ui.LoadingStateView) {
            view.showError(message, onRetry)
        }
    }

    /**
     * Monitor memory usage and handle low memory situations
     */
    private fun monitorMemoryUsage() {
        lifecycleScope.launch {
            try {
                val memoryUsage = checkMemoryUsage()
                if (memoryUsage > 0.8f) { // If using more than 80% of available memory
                    handleLowMemory()
                }
            } catch (e: Exception) {
                // Log error but don't crash the app
                android.util.Log.w("BaseActivity", "Error monitoring memory usage: ${e.message}")
            }
        }
    }

    /**
     * Batch UI updates for better performance
     */
    protected fun batchUIUpdates(updates: List<() -> Unit>) {
        PerformanceOptimizer.batchUIUpdates(updates)
    }

    /**
     * Debounced function execution
     */
    protected fun debounce(delayMs: Long, action: () -> Unit): () -> Unit {
        return PerformanceOptimizer.debounce(delayMs, action)
    }

    /**
     * Throttled function execution
     */
    protected fun throttle(intervalMs: Long, action: () -> Unit): () -> Unit {
        return PerformanceOptimizer.throttle(intervalMs, action)
    }

    /**
     * Measure execution time for performance monitoring
     */
    protected fun <T> measurePerformance(tag: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        android.util.Log.d("Performance", "$tag took ${endTime - startTime}ms")
        return result
    }

    /**
     * Get performance metrics
     */
    protected fun getPerformanceMetrics(): Map<String, Any> {
        return PerformanceOptimizer.getPerformanceMetrics()
    }
}