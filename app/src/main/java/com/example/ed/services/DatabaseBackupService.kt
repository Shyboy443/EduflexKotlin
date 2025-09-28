package com.example.ed.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DatabaseBackupService(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    companion object {
        private const val TAG = "DatabaseBackupService"
        private const val BACKUP_FOLDER = "EduFlex_Backups"
    }
    
    data class BackupData(
        val timestamp: String,
        val version: String,
        val collections: Map<String, List<Map<String, Any>>>
    )
    
    suspend fun createFullDatabaseBackup(): Result<File> {
        return try {
            Log.d(TAG, "Starting database backup...")
            
            // Get all collection names
            val collectionNames = listOf(
                "users",
                "courses", 
                "enrollments",
                "students",
                "teachers",
                "admins",
                "weekly_content",
                "student_analytics",
                "student_progress",
                "image_metadata",
                "securityLogs"
            )
            
            val collectionsData = mutableMapOf<String, List<Map<String, Any>>>()
            
            // Export each collection
            for (collectionName in collectionNames) {
                try {
                    Log.d(TAG, "Backing up collection: $collectionName")
                    val documents = exportCollection(collectionName)
                    collectionsData[collectionName] = documents
                    Log.d(TAG, "Collection $collectionName: ${documents.size} documents")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to backup collection $collectionName: ${e.message}")
                    collectionsData[collectionName] = emptyList()
                }
            }
            
            // Create backup data structure
            val backupData = BackupData(
                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                version = "1.0",
                collections = collectionsData
            )
            
            // Save to file
            val backupFile = saveBackupToFile(backupData)
            Log.d(TAG, "Backup completed successfully: ${backupFile.absolutePath}")
            
            Result.success(backupFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Database backup failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun exportCollection(collectionName: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(collectionName).get().await()
            val documents = mutableListOf<Map<String, Any>>()
            
            for (document in snapshot.documents) {
                val data = document.data?.toMutableMap() ?: mutableMapOf()
                data["_documentId"] = document.id
                documents.add(data)
            }
            
            documents
        } catch (e: Exception) {
            Log.w(TAG, "Error exporting collection $collectionName: ${e.message}")
            emptyList()
        }
    }
    
    private fun saveBackupToFile(backupData: BackupData): File {
        // Create backup directory
        val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        // Create filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "eduflex_backup_$timestamp.json"
        val backupFile = File(backupDir, filename)
        
        // Write JSON data to file
        FileWriter(backupFile).use { writer ->
            gson.toJson(backupData, writer)
        }
        
        return backupFile
    }
    
    fun shareBackupFile(backupFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "EduFlex Database Backup")
                putExtra(Intent.EXTRA_TEXT, "EduFlex database backup created on ${Date()}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Share Backup File")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing backup file", e)
            Toast.makeText(context, "Error sharing backup file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    fun getBackupFileInfo(backupFile: File): String {
        val sizeInMB = backupFile.length() / (1024 * 1024).toDouble()
        val lastModified = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(backupFile.lastModified()))
        
        return """
            Backup File: ${backupFile.name}
            Size: ${"%.2f".format(sizeInMB)} MB
            Created: $lastModified
            Location: ${backupFile.absolutePath}
        """.trimIndent()
    }
    
    fun getAllBackupFiles(): List<File> {
        val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER)
        return if (backupDir.exists()) {
            backupDir.listFiles { file -> file.name.endsWith(".json") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    fun deleteBackupFile(backupFile: File): Boolean {
        return try {
            backupFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup file", e)
            false
        }
    }
}