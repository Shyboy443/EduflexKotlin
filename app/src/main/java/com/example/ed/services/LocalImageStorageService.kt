package com.example.ed.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

data class LocalImageResult(
    val success: Boolean,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
    val error: String? = null
)

class LocalImageStorageService private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: LocalImageStorageService? = null
        
        fun getInstance(): LocalImageStorageService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalImageStorageService().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "LocalImageStorage"
        private const val MAX_IMAGE_WIDTH = 1920
        private const val MAX_IMAGE_HEIGHT = 1080
        private const val THUMBNAIL_SIZE = 300
        private const val COMPRESSION_QUALITY = 85
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        
        // Directory names
        private const val IMAGES_DIR = "course_images"
        private const val THUMBNAILS_DIR = "course_thumbnails"
    }
    
    fun saveImage(
        context: Context,
        imageUri: Uri,
        courseId: String? = null
    ): LocalImageResult {
        return try {
            // Validate file size
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val fileSize = inputStream?.available()?.toLong() ?: 0
            inputStream?.close()
            
            if (fileSize > MAX_FILE_SIZE) {
                return LocalImageResult(
                    success = false,
                    error = "File size exceeds 10MB limit"
                )
            }
            
            // Process image
            val processedImage = processImage(context, imageUri)
            val thumbnail = createThumbnail(processedImage)
            
            // Generate unique filename
            val timestamp = System.currentTimeMillis()
            val imageFileName = "${courseId ?: UUID.randomUUID()}_${timestamp}.jpg"
            val thumbnailFileName = "thumb_$imageFileName"
            
            // Save main image
            val imageFile = saveImageToInternalStorage(
                context, 
                processedImage, 
                IMAGES_DIR, 
                imageFileName
            )
            
            // Save thumbnail
            val thumbnailFile = saveImageToInternalStorage(
                context, 
                thumbnail, 
                THUMBNAILS_DIR, 
                thumbnailFileName
            )
            
            if (imageFile != null && thumbnailFile != null) {
                LocalImageResult(
                    success = true,
                    imageUrl = imageFile.absolutePath,
                    thumbnailUrl = thumbnailFile.absolutePath
                )
            } else {
                LocalImageResult(
                    success = false,
                    error = "Failed to save image files"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image locally", e)
            LocalImageResult(
                success = false,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }
    
    private fun processImage(context: Context, imageUri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        // Get orientation from EXIF data
        val orientation = getImageOrientation(context, imageUri)
        
        // Rotate bitmap if needed
        val rotatedBitmap = rotateImageIfRequired(originalBitmap, orientation)
        
        // Resize if necessary
        return resizeImageIfNeeded(rotatedBitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
    }
    
    private fun getImageOrientation(context: Context, imageUri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            inputStream.close()
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
            Log.w(TAG, "Could not get image orientation", e)
            ExifInterface.ORIENTATION_NORMAL
        }
    }
    
    private fun rotateImageIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun resizeImageIfNeeded(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (aspectRatio > 1) {
            // Landscape
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            // Portrait or square
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val aspectRatio = width.toFloat() / height.toFloat()
        
        val thumbnailWidth: Int
        val thumbnailHeight: Int
        
        if (aspectRatio > 1) {
            thumbnailWidth = THUMBNAIL_SIZE
            thumbnailHeight = (THUMBNAIL_SIZE / aspectRatio).toInt()
        } else {
            thumbnailHeight = THUMBNAIL_SIZE
            thumbnailWidth = (THUMBNAIL_SIZE * aspectRatio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, thumbnailWidth, thumbnailHeight, true)
    }
    
    private fun saveImageToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        directory: String,
        filename: String
    ): File? {
        return try {
            // Create directory if it doesn't exist
            val dir = File(context.filesDir, directory)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            // Create file
            val file = File(dir, filename)
            
            // Save bitmap to file
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "Image saved successfully: ${file.absolutePath}")
            file
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to internal storage", e)
            null
        }
    }
    
    fun getImageBitmap(imagePath: String): Bitmap? {
        return try {
            if (File(imagePath).exists()) {
                BitmapFactory.decodeFile(imagePath)
            } else {
                Log.w(TAG, "Image file not found: $imagePath")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from path: $imagePath", e)
            null
        }
    }
    
    fun deleteImage(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Image deletion ${if (deleted) "successful" else "failed"}: $imagePath")
                deleted
            } else {
                Log.w(TAG, "Image file not found for deletion: $imagePath")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image: $imagePath", e)
            false
        }
    }
    
    fun getImageFile(imagePath: String): File? {
        return try {
            val file = File(imagePath)
            if (file.exists()) file else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image file: $imagePath", e)
            null
        }
    }
    
    fun getAllCourseImages(context: Context): List<String> {
        return try {
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (imagesDir.exists() && imagesDir.isDirectory) {
                imagesDir.listFiles()?.map { it.absolutePath } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all course images", e)
            emptyList()
        }
    }
    
    fun clearAllImages(context: Context): Boolean {
        return try {
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR)
            
            var success = true
            
            // Clear main images
            if (imagesDir.exists()) {
                imagesDir.listFiles()?.forEach { file ->
                    if (!file.delete()) {
                        success = false
                        Log.w(TAG, "Failed to delete image: ${file.absolutePath}")
                    }
                }
            }
            
            // Clear thumbnails
            if (thumbnailsDir.exists()) {
                thumbnailsDir.listFiles()?.forEach { file ->
                    if (!file.delete()) {
                        success = false
                        Log.w(TAG, "Failed to delete thumbnail: ${file.absolutePath}")
                    }
                }
            }
            
            Log.d(TAG, "Image cleanup ${if (success) "successful" else "partial"}")
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all images", e)
            false
        }
    }
}
