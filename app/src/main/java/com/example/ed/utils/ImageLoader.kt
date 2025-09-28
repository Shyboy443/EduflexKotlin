package com.example.ed.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.example.ed.R
import com.example.ed.services.LocalImageStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object ImageLoader {
    
    private const val TAG = "ImageLoader"
    
    /**
     * Load image into ImageView from local storage path or URI
     */
    fun loadImage(
        context: Context,
        imageView: ImageView,
        imagePath: String?,
        placeholder: Int = R.drawable.ic_course_placeholder,
        scope: CoroutineScope
    ) {
        // Set placeholder immediately
        imageView.setImageResource(placeholder)
        
        if (imagePath.isNullOrEmpty()) {
            Log.d(TAG, "Image path is null or empty, using placeholder")
            return
        }
        
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromPath(imagePath)
                }
                
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        Log.d(TAG, "Successfully loaded image: $imagePath")
                    } else {
                        Log.w(TAG, "Failed to load bitmap from path: $imagePath")
                        imageView.setImageResource(placeholder)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image: $imagePath", e)
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(placeholder)
                }
            }
        }
    }
    
    /**
     * Load thumbnail image with smaller size for better performance
     */
    fun loadThumbnail(
        context: Context,
        imageView: ImageView,
        imagePath: String?,
        placeholder: Int = R.drawable.ic_course_placeholder,
        scope: CoroutineScope
    ) {
        // Set placeholder immediately
        imageView.setImageResource(placeholder)
        
        if (imagePath.isNullOrEmpty()) {
            Log.d(TAG, "Thumbnail path is null or empty, using placeholder")
            return
        }
        
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromPath(imagePath, maxSize = 300)
                }
                
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        Log.d(TAG, "Successfully loaded thumbnail: $imagePath")
                    } else {
                        Log.w(TAG, "Failed to load thumbnail from path: $imagePath")
                        imageView.setImageResource(placeholder)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading thumbnail: $imagePath", e)
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(placeholder)
                }
            }
        }
    }
    
    /**
     * Load bitmap from local file path with optional size limit
     */
    private fun loadBitmapFromPath(imagePath: String, maxSize: Int? = null): Bitmap? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                Log.w(TAG, "Image file does not exist: $imagePath")
                return null
            }
            
            if (maxSize != null) {
                // Load with size constraints for memory efficiency
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(imagePath, options)
                
                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
                options.inJustDecodeBounds = false
                
                BitmapFactory.decodeFile(imagePath, options)
            } else {
                BitmapFactory.decodeFile(imagePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from file: $imagePath", e)
            null
        }
    }
    
    /**
     * Calculate sample size for efficient bitmap loading
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Check if image path is valid and file exists
     */
    fun isValidImagePath(imagePath: String?): Boolean {
        return try {
            if (imagePath.isNullOrEmpty()) return false
            val file = File(imagePath)
            file.exists() && file.isFile && file.canRead()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking image path validity: $imagePath", e)
            false
        }
    }
    
    /**
     * Get image file size in bytes
     */
    fun getImageFileSize(imagePath: String?): Long {
        return try {
            if (imagePath.isNullOrEmpty()) return 0L
            val file = File(imagePath)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image file size: $imagePath", e)
            0L
        }
    }
    
    /**
     * Convert local file path to content URI for sharing
     */
    fun getShareableUri(context: Context, imagePath: String?): Uri? {
        return try {
            if (imagePath.isNullOrEmpty()) return null
            val file = File(imagePath)
            if (file.exists()) {
                // For sharing, we might need to copy to external cache
                Uri.fromFile(file)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating shareable URI: $imagePath", e)
            null
        }
    }
}
