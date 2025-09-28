package com.example.ed.utils

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.ed.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

object ImageDebugHelper {
    
    private const val TAG = "ImageDebugHelper"
    
    fun debugImageLoading(
        context: Context,
        imageView: ImageView,
        imagePath: String?,
        scope: CoroutineScope
    ) {
        Log.d(TAG, "=== IMAGE DEBUG ===")
        Log.d(TAG, "Image path: $imagePath")
        
        if (imagePath.isNullOrEmpty()) {
            Log.d(TAG, "Image path is null or empty - using placeholder")
            imageView.setImageResource(R.drawable.ic_course_placeholder)
            return
        }
        
        // Check if it's a local file path
        if (imagePath.startsWith("/")) {
            val file = File(imagePath)
            Log.d(TAG, "Local file path detected")
            Log.d(TAG, "File exists: ${file.exists()}")
            Log.d(TAG, "File size: ${if (file.exists()) file.length() else "N/A"} bytes")
            Log.d(TAG, "File readable: ${file.canRead()}")
            
            if (file.exists() && file.canRead()) {
                // Use ImageLoader for local files
                ImageLoader.loadImage(
                    context = context,
                    imageView = imageView,
                    imagePath = imagePath,
                    placeholder = R.drawable.ic_course_placeholder,
                    scope = scope
                )
            } else {
                Log.w(TAG, "Local file not accessible, using placeholder")
                imageView.setImageResource(R.drawable.ic_course_placeholder)
            }
        } else {
            Log.d(TAG, "Non-local path (URL or other), using PerformanceOptimizer")
            // Use PerformanceOptimizer for URLs or other paths
            PerformanceOptimizer.loadImageOptimized(
                imageView = imageView,
                imagePath = imagePath,
                placeholder = R.drawable.ic_course_placeholder,
                targetWidth = 200,
                targetHeight = 150
            )
        }
    }
    
    fun showImagePathInfo(context: Context, imagePath: String?) {
        val message = buildString {
            appendLine("📷 IMAGE PATH DEBUG")
            appendLine("==================")
            appendLine("Path: ${imagePath ?: "NULL"}")
            
            if (!imagePath.isNullOrEmpty()) {
                if (imagePath.startsWith("/")) {
                    val file = File(imagePath)
                    appendLine("Type: Local file")
                    appendLine("Exists: ${file.exists()}")
                    appendLine("Size: ${if (file.exists()) file.length() else "N/A"} bytes")
                    appendLine("Readable: ${file.canRead()}")
                } else {
                    appendLine("Type: URL or other")
                }
            } else {
                appendLine("Type: Empty/null")
            }
        }
        
        Log.i(TAG, message)
        Toast.makeText(context, "Image debug info logged - check console", Toast.LENGTH_SHORT).show()
    }
}
