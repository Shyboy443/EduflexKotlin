package com.example.ed.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

data class ImageMetadata(
    val originalWidth: Int,
    val originalHeight: Int,
    val compressedWidth: Int,
    val compressedHeight: Int,
    val fileSize: Long,
    val mimeType: String,
    val orientation: Int,
    val uploadTimestamp: Long,
    val uploaderId: String,
    val fileName: String,
    val storageUrl: String,
    val thumbnailUrl: String? = null
)

data class ImageUploadResult(
    val success: Boolean,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
    val metadata: ImageMetadata? = null,
    val error: String? = null
)

class ImageUploadService private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: ImageUploadService? = null
        
        fun getInstance(): ImageUploadService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageUploadService().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "ImageUploadService"
        private const val MAX_IMAGE_WIDTH = 1920
        private const val MAX_IMAGE_HEIGHT = 1080
        private const val THUMBNAIL_SIZE = 300
        private const val COMPRESSION_QUALITY = 85
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    }
    
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        uploaderId: String,
        folder: String = "course_content"
    ): ImageUploadResult {
        return try {
            // Validate file size
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val fileSize = inputStream?.available()?.toLong() ?: 0
            inputStream?.close()
            
            if (fileSize > MAX_FILE_SIZE) {
                return ImageUploadResult(
                    success = false,
                    error = "File size exceeds 5MB limit"
                )
            }
            
            // Process image
            val processedImage = processImage(context, imageUri)
            val thumbnail = createThumbnail(processedImage.bitmap)
            
            // Generate unique filenames
            val timestamp = System.currentTimeMillis()
            val imageFileName = "${UUID.randomUUID()}_${timestamp}.jpg"
            val thumbnailFileName = "thumb_$imageFileName"
            
            // Upload main image
            val imageRef = storage.reference.child("$folder/$imageFileName")
            val imageBytes = bitmapToByteArray(processedImage.bitmap, COMPRESSION_QUALITY)
            val imageUploadTask = imageRef.putBytes(imageBytes).await()
            val imageUrl = imageRef.downloadUrl.await().toString()
            
            // Upload thumbnail
            val thumbnailRef = storage.reference.child("$folder/thumbnails/$thumbnailFileName")
            val thumbnailBytes = bitmapToByteArray(thumbnail, COMPRESSION_QUALITY)
            val thumbnailUploadTask = thumbnailRef.putBytes(thumbnailBytes).await()
            val thumbnailUrl = thumbnailRef.downloadUrl.await().toString()
            
            // Create metadata
            val metadata = ImageMetadata(
                originalWidth = processedImage.originalWidth,
                originalHeight = processedImage.originalHeight,
                compressedWidth = processedImage.bitmap.width,
                compressedHeight = processedImage.bitmap.height,
                fileSize = imageBytes.size.toLong(),
                mimeType = "image/jpeg",
                orientation = processedImage.orientation,
                uploadTimestamp = timestamp,
                uploaderId = uploaderId,
                fileName = imageFileName,
                storageUrl = imageUrl,
                thumbnailUrl = thumbnailUrl
            )
            
            // Save metadata to Firestore
            saveImageMetadata(metadata)
            
            ImageUploadResult(
                success = true,
                imageUrl = imageUrl,
                thumbnailUrl = thumbnailUrl,
                metadata = metadata
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            ImageUploadResult(
                success = false,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }
    
    private data class ProcessedImage(
        val bitmap: Bitmap,
        val originalWidth: Int,
        val originalHeight: Int,
        val orientation: Int
    )
    
    private fun processImage(context: Context, imageUri: Uri): ProcessedImage {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        
        // Get orientation from EXIF data
        val orientation = getImageOrientation(context, imageUri)
        
        // Rotate bitmap if needed
        val rotatedBitmap = rotateImageIfRequired(originalBitmap, orientation)
        
        // Resize if necessary
        val resizedBitmap = resizeImageIfNeeded(rotatedBitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
        
        return ProcessedImage(
            bitmap = resizedBitmap,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            orientation = orientation
        )
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
    
    private fun bitmapToByteArray(bitmap: Bitmap, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
    
    private suspend fun saveImageMetadata(metadata: ImageMetadata) {
        try {
            val metadataMap = mapOf(
                "originalWidth" to metadata.originalWidth,
                "originalHeight" to metadata.originalHeight,
                "compressedWidth" to metadata.compressedWidth,
                "compressedHeight" to metadata.compressedHeight,
                "fileSize" to metadata.fileSize,
                "mimeType" to metadata.mimeType,
                "orientation" to metadata.orientation,
                "uploadTimestamp" to metadata.uploadTimestamp,
                "uploaderId" to metadata.uploaderId,
                "fileName" to metadata.fileName,
                "storageUrl" to metadata.storageUrl,
                "thumbnailUrl" to metadata.thumbnailUrl
            )
            
            firestore.collection("image_metadata")
                .document(metadata.fileName.substringBefore('.'))
                .set(metadataMap)
                .await()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image metadata", e)
        }
    }
    
    suspend fun deleteImage(imageUrl: String): Boolean {
        return try {
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            
            // Also delete thumbnail if exists
            val fileName = imageRef.name
            val thumbnailRef = storage.reference.child("thumbnails/thumb_$fileName")
            try {
                thumbnailRef.delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete thumbnail", e)
            }
            
            // Delete metadata
            val documentId = fileName.substringBefore('.')
            firestore.collection("image_metadata")
                .document(documentId)
                .delete()
                .await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image", e)
            false
        }
    }
    
    suspend fun getImageMetadata(fileName: String): ImageMetadata? {
        return try {
            val documentId = fileName.substringBefore('.')
            val document = firestore.collection("image_metadata")
                .document(documentId)
                .get()
                .await()
            
            if (document.exists()) {
                ImageMetadata(
                    originalWidth = document.getLong("originalWidth")?.toInt() ?: 0,
                    originalHeight = document.getLong("originalHeight")?.toInt() ?: 0,
                    compressedWidth = document.getLong("compressedWidth")?.toInt() ?: 0,
                    compressedHeight = document.getLong("compressedHeight")?.toInt() ?: 0,
                    fileSize = document.getLong("fileSize") ?: 0,
                    mimeType = document.getString("mimeType") ?: "image/jpeg",
                    orientation = document.getLong("orientation")?.toInt() ?: 0,
                    uploadTimestamp = document.getLong("uploadTimestamp") ?: 0,
                    uploaderId = document.getString("uploaderId") ?: "",
                    fileName = document.getString("fileName") ?: "",
                    storageUrl = document.getString("storageUrl") ?: "",
                    thumbnailUrl = document.getString("thumbnailUrl")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image metadata", e)
            null
        }
    }
}