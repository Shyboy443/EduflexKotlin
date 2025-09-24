package com.example.ed.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object PerformanceOptimizer : LifecycleObserver {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newFixedThreadPool(4)
    private val imageCache = LruCache<String, Bitmap>(getCacheSize())
    private val dataCache = ConcurrentHashMap<String, CacheItem<*>>()
    private val viewHolderPool = RecyclerView.RecycledViewPool()
    
    // Performance monitoring
    private var frameDropCount = 0
    private var lastFrameTime = 0L
    internal val performanceMetrics = mutableMapOf<String, Long>()

    /**
     * Initialize performance optimizer
     */
    fun initialize(context: Context) {
        setupViewHolderPool()
        setupMemoryManagement(context)
        startPerformanceMonitoring()
    }

    /**
     * Calculate optimal cache size based on available memory
     */
    private fun getCacheSize(): Int {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        return maxMemory / 8 // Use 1/8th of available memory for cache
    }

    /**
     * Setup view holder pool for RecyclerViews
     */
    private fun setupViewHolderPool() {
        viewHolderPool.setMaxRecycledViews(0, 20) // Generic items
        viewHolderPool.setMaxRecycledViews(1, 15) // Course items
        viewHolderPool.setMaxRecycledViews(2, 10) // Assignment items
        viewHolderPool.setMaxRecycledViews(3, 8)  // Discussion items
    }

    /**
     * Setup memory management
     */
    private fun setupMemoryManagement(context: Context) {
        // Register for memory pressure callbacks
        context.registerComponentCallbacks(object : android.content.ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
            
            override fun onLowMemory() {
                clearCaches()
            }
            
            override fun onTrimMemory(level: Int) {
                when (level) {
                    android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                        // App is in background, trim some memory
                        trimCache(0.5f)
                    }
                    android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                        // System is running low on memory
                        trimCache(0.8f)
                    }
                    android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                        // Clear all caches
                        clearCaches()
                    }
                }
            }
        })
    }

    /**
     * Start performance monitoring
     */
    private fun startPerformanceMonitoring() {
        val choreographer = android.view.Choreographer.getInstance()
        choreographer.postFrameCallback(object : android.view.Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameTime != 0L) {
                    val frameDuration = (frameTimeNanos - lastFrameTime) / 1_000_000 // Convert to ms
                    if (frameDuration > 16.67) { // 60 FPS threshold
                        frameDropCount++
                    }
                }
                lastFrameTime = frameTimeNanos
                choreographer.postFrameCallback(this)
            }
        })
    }

    /**
     * Optimize RecyclerView performance
     */
    fun optimizeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            // Use shared view holder pool
            setRecycledViewPool(viewHolderPool)
            
            // Enable item animator optimizations
            itemAnimator?.changeDuration = 0
            itemAnimator?.moveDuration = 0
            
            // Set optimal cache sizes
            setItemViewCacheSize(20)
            
            // Enable nested scrolling optimization
            isNestedScrollingEnabled = false
            
            // Add scroll listener for performance monitoring
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            // Preload next items when scrolling stops
                            preloadNextItems(recyclerView)
                        }
                        RecyclerView.SCROLL_STATE_DRAGGING -> {
                            // Pause non-essential operations during scrolling
                            pauseNonEssentialOperations()
                        }
                    }
                }
            })
        }
    }

    /**
     * Preload next items in RecyclerView
     */
    private fun preloadNextItems(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        layoutManager?.let { lm ->
            val lastVisiblePosition = lm.findLastVisibleItemPosition()
            val totalItemCount = lm.itemCount
            
            // Preload next 5 items
            if (lastVisiblePosition + 5 < totalItemCount) {
                for (i in lastVisiblePosition + 1..minOf(lastVisiblePosition + 5, totalItemCount - 1)) {
                    recyclerView.adapter?.createViewHolder(recyclerView, recyclerView.adapter!!.getItemViewType(i))
                }
            }
        }
    }

    /**
     * Pause non-essential operations during scrolling
     */
    private fun pauseNonEssentialOperations() {
        // Pause image loading, animations, etc.
        // This would be implemented based on specific needs
    }

    /**
     * Optimize image loading and caching
     */
    fun loadImageOptimized(
        imageView: ImageView,
        imagePath: String,
        placeholder: Int? = null,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ) {
        val imageViewRef = WeakReference(imageView)
        
        // Set placeholder
        placeholder?.let { imageView.setImageResource(it) }
        
        // Check cache first
        imageCache.get(imagePath)?.let { cachedBitmap ->
            imageViewRef.get()?.setImageBitmap(cachedBitmap)
            return
        }
        
        // Load image in background
        backgroundExecutor.execute {
            try {
                val bitmap = loadBitmapOptimized(imagePath, targetWidth, targetHeight)
                bitmap?.let {
                    // Cache the bitmap
                    imageCache.put(imagePath, it)
                    
                    // Set image on main thread
                    mainHandler.post {
                        imageViewRef.get()?.setImageBitmap(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Load bitmap with optimal memory usage
     */
    private fun loadBitmapOptimized(imagePath: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            
            // First decode to get dimensions
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, options)
            
            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            
            BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate optimal sample size for bitmap loading
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (reqWidth > 0 && reqHeight > 0) {
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
        }
        
        return inSampleSize
    }

    /**
     * Generic data caching with expiration
     */
    fun <T> cacheData(key: String, data: T, expirationMs: Long = 300_000) { // 5 minutes default
        val expirationTime = System.currentTimeMillis() + expirationMs
        dataCache[key] = CacheItem(data, expirationTime)
    }

    /**
     * Retrieve cached data
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCachedData(key: String): T? {
        val cacheItem = dataCache[key] ?: return null
        
        return if (cacheItem.isExpired()) {
            dataCache.remove(key)
            null
        } else {
            cacheItem.data as? T
        }
    }

    /**
     * Debounce function calls to improve performance
     */
    fun debounce(delayMs: Long, action: () -> Unit): () -> Unit {
        var debounceJob: Job? = null
        
        return {
            debounceJob?.cancel()
            debounceJob = CoroutineScope(Dispatchers.Main).launch {
                delay(delayMs)
                action()
            }
        }
    }

    /**
     * Throttle function calls
     */
    fun throttle(intervalMs: Long, action: () -> Unit): () -> Unit {
        var lastExecutionTime = 0L
        
        return {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastExecutionTime >= intervalMs) {
                lastExecutionTime = currentTime
                action()
            }
        }
    }

    /**
     * Optimize view hierarchy
     */
    fun optimizeViewHierarchy(rootView: ViewGroup) {
        fun processView(view: View) {
            // Remove unnecessary background drawables
            if (view.background != null && view.parent is ViewGroup) {
                val parent = view.parent as ViewGroup
                if (parent.background != null) {
                    // Parent has background, child might not need one
                    view.background = null
                }
            }
            
            // Optimize text views
            if (view is android.widget.TextView) {
                // Disable hardware acceleration for complex text if needed
                if (view.text.length > 1000) {
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
            }
            
            // Process child views
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    processView(view.getChildAt(i))
                }
            }
        }
        
        processView(rootView)
    }

    /**
     * Batch UI updates for better performance
     */
    fun batchUIUpdates(updates: List<() -> Unit>) {
        mainHandler.post {
            updates.forEach { it() }
        }
    }

    /**
     * Measure execution time
     */
    internal inline fun <T> measureExecutionTime(tag: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val executionTime = System.currentTimeMillis() - startTime
        
        performanceMetrics[tag] = executionTime
        
        if (executionTime > 100) { // Log slow operations
            android.util.Log.w("Performance", "$tag took ${executionTime}ms")
        }
        
        return result
    }

    /**
     * Clear all caches
     */
    fun clearCaches() {
        imageCache.evictAll()
        dataCache.clear()
        System.gc() // Suggest garbage collection
    }

    /**
     * Trim cache by percentage
     */
    private fun trimCache(percentage: Float) {
        val imageCacheSize = imageCache.size()
        val dataCacheSize = dataCache.size
        
        // Trim image cache
        val imagesToRemove = (imageCacheSize * percentage).toInt()
        repeat(imagesToRemove) {
            imageCache.trimToSize(imageCache.size() - 1)
        }
        
        // Trim data cache (remove oldest entries)
        val dataToRemove = (dataCacheSize * percentage).toInt()
        val keysToRemove = dataCache.keys.take(dataToRemove)
        keysToRemove.forEach { dataCache.remove(it) }
    }

    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        return mapOf(
            "frameDropCount" to frameDropCount,
            "imageCacheSize" to imageCache.size(),
            "dataCacheSize" to dataCache.size,
            "executionTimes" to performanceMetrics.toMap()
        )
    }

    /**
     * Reset performance metrics
     */
    fun resetPerformanceMetrics() {
        frameDropCount = 0
        performanceMetrics.clear()
    }

    /**
     * Lifecycle callbacks
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        // Trim cache when app goes to background
        trimCache(0.3f)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        clearCaches()
        backgroundExecutor.shutdown()
    }

    /**
     * Cache item with expiration
     */
    private data class CacheItem<T>(
        val data: T,
        val expirationTime: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expirationTime
    }

    /**
     * Memory usage utilities
     */
    object MemoryUtils {
        fun getAvailableMemory(): Long {
            val runtime = Runtime.getRuntime()
            return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        }
        
        fun getMemoryUsagePercentage(): Float {
            val runtime = Runtime.getRuntime()
            val used = runtime.totalMemory() - runtime.freeMemory()
            val max = runtime.maxMemory()
            return (used.toFloat() / max.toFloat()) * 100f
        }
        
        fun isLowMemory(): Boolean {
            return getMemoryUsagePercentage() > 80f
        }
    }

    /**
     * Network optimization utilities
     */
    object NetworkOptimizer {
        private val requestCache = ConcurrentHashMap<String, Long>()
        
        fun shouldMakeRequest(url: String, cacheTimeMs: Long = 60_000): Boolean {
            val lastRequestTime = requestCache[url] ?: 0
            val currentTime = System.currentTimeMillis()
            
            return if (currentTime - lastRequestTime > cacheTimeMs) {
                requestCache[url] = currentTime
                true
            } else {
                false
            }
        }
        
        fun clearRequestCache() {
            requestCache.clear()
        }
    }
}