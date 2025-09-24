package com.example.ed.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import android.view.View
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

sealed class AppError(
    val message: String,
    val code: String? = null,
    val cause: Throwable? = null
) {
    class NetworkError(message: String = "Network connection error", cause: Throwable? = null) : 
        AppError(message, "NETWORK_ERROR", cause)
    
    class AuthenticationError(message: String = "Authentication failed", cause: Throwable? = null) : 
        AppError(message, "AUTH_ERROR", cause)
    
    class ValidationError(message: String, cause: Throwable? = null) : 
        AppError(message, "VALIDATION_ERROR", cause)
    
    class DatabaseError(message: String = "Database operation failed", cause: Throwable? = null) : 
        AppError(message, "DATABASE_ERROR", cause)
    
    class StorageError(message: String = "File operation failed", cause: Throwable? = null) : 
        AppError(message, "STORAGE_ERROR", cause)
    
    class UnknownError(message: String = "An unexpected error occurred", cause: Throwable? = null) : 
        AppError(message, "UNKNOWN_ERROR", cause)
    
    class TimeoutError(message: String = "Operation timed out", cause: Throwable? = null) : 
        AppError(message, "TIMEOUT_ERROR", cause)
    
    class PermissionError(message: String = "Permission denied", cause: Throwable? = null) : 
        AppError(message, "PERMISSION_ERROR", cause)
}

object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    fun handleError(
        context: Context,
        error: Throwable,
        view: View? = null,
        showUserMessage: Boolean = true
    ): AppError {
        val appError = mapThrowableToAppError(error)
        
        // Log the error
        Log.e(TAG, "Error occurred: ${appError.code}", appError.cause ?: error)
        
        // Show user-friendly message if requested
        if (showUserMessage) {
            showErrorMessage(context, appError, view)
        }
        
        return appError
    }
    
    private fun mapThrowableToAppError(error: Throwable): AppError {
        return when (error) {
            is FirebaseAuthException -> {
                when (error.errorCode) {
                    "ERROR_INVALID_EMAIL" -> AppError.ValidationError("Invalid email address")
                    "ERROR_WRONG_PASSWORD" -> AppError.AuthenticationError("Incorrect password")
                    "ERROR_USER_NOT_FOUND" -> AppError.AuthenticationError("User not found")
                    "ERROR_USER_DISABLED" -> AppError.AuthenticationError("User account has been disabled")
                    "ERROR_TOO_MANY_REQUESTS" -> AppError.AuthenticationError("Too many failed attempts. Please try again later")
                    "ERROR_OPERATION_NOT_ALLOWED" -> AppError.AuthenticationError("This sign-in method is not allowed")
                    "ERROR_EMAIL_ALREADY_IN_USE" -> AppError.ValidationError("Email address is already in use")
                    "ERROR_WEAK_PASSWORD" -> AppError.ValidationError("Password is too weak")
                    else -> AppError.AuthenticationError("Authentication failed: ${error.message}")
                }
            }
            
            is FirebaseFirestoreException -> {
                when (error.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                        AppError.PermissionError("You don't have permission to perform this action")
                    FirebaseFirestoreException.Code.NOT_FOUND -> 
                        AppError.DatabaseError("Requested data not found")
                    FirebaseFirestoreException.Code.ALREADY_EXISTS -> 
                        AppError.ValidationError("Data already exists")
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> 
                        AppError.DatabaseError("Database quota exceeded. Please try again later")
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION -> 
                        AppError.ValidationError("Operation failed due to invalid conditions")
                    FirebaseFirestoreException.Code.ABORTED -> 
                        AppError.DatabaseError("Operation was aborted due to a conflict")
                    FirebaseFirestoreException.Code.OUT_OF_RANGE -> 
                        AppError.ValidationError("Invalid data range")
                    FirebaseFirestoreException.Code.UNIMPLEMENTED -> 
                        AppError.DatabaseError("Feature not implemented")
                    FirebaseFirestoreException.Code.INTERNAL -> 
                        AppError.DatabaseError("Internal server error")
                    FirebaseFirestoreException.Code.UNAVAILABLE -> 
                        AppError.NetworkError("Service temporarily unavailable")
                    FirebaseFirestoreException.Code.DATA_LOSS -> 
                        AppError.DatabaseError("Data corruption detected")
                    FirebaseFirestoreException.Code.UNAUTHENTICATED -> 
                        AppError.AuthenticationError("Authentication required")
                    FirebaseFirestoreException.Code.INVALID_ARGUMENT -> 
                        AppError.ValidationError("Invalid data provided")
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> 
                        AppError.TimeoutError("Operation timed out")
                    FirebaseFirestoreException.Code.CANCELLED -> 
                        AppError.DatabaseError("Operation was cancelled")
                    else -> AppError.DatabaseError("Database error: ${error.message}")
                }
            }
            
            is StorageException -> {
                when (error.errorCode) {
                    StorageException.ERROR_OBJECT_NOT_FOUND -> 
                        AppError.StorageError("File not found")
                    StorageException.ERROR_BUCKET_NOT_FOUND -> 
                        AppError.StorageError("Storage bucket not found")
                    StorageException.ERROR_PROJECT_NOT_FOUND -> 
                        AppError.StorageError("Project not found")
                    StorageException.ERROR_QUOTA_EXCEEDED -> 
                        AppError.StorageError("Storage quota exceeded")
                    StorageException.ERROR_NOT_AUTHENTICATED -> 
                        AppError.AuthenticationError("Authentication required for file access")
                    StorageException.ERROR_NOT_AUTHORIZED -> 
                        AppError.PermissionError("Not authorized to access file")
                    StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> 
                        AppError.StorageError("Upload failed after multiple attempts")
                    StorageException.ERROR_INVALID_CHECKSUM -> 
                        AppError.StorageError("File corruption detected during upload")
                    StorageException.ERROR_CANCELED -> 
                        AppError.StorageError("File operation was cancelled")
                    else -> AppError.StorageError("File operation failed: ${error.message}")
                }
            }
            
            is UnknownHostException -> 
                AppError.NetworkError("No internet connection available")
            
            is TimeoutException -> 
                AppError.TimeoutError("Operation timed out. Please check your connection")
            
            is SecurityException -> 
                AppError.PermissionError("Permission denied: ${error.message}")
            
            is IllegalArgumentException -> 
                AppError.ValidationError("Invalid input: ${error.message}")
            
            is IllegalStateException -> 
                AppError.ValidationError("Invalid operation: ${error.message}")
            
            else -> AppError.UnknownError("An unexpected error occurred: ${error.message}")
        }
    }
    
    private fun showErrorMessage(context: Context, error: AppError, view: View?) {
        val message = getUserFriendlyMessage(error)
        
        if (view != null) {
            // Show Snackbar if view is available
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction("Dismiss") { }
                .show()
        } else {
            // Fallback to Toast
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun getUserFriendlyMessage(error: AppError): String {
        return when (error) {
            is AppError.NetworkError -> "Please check your internet connection and try again"
            is AppError.AuthenticationError -> error.message
            is AppError.ValidationError -> error.message
            is AppError.DatabaseError -> "Unable to save data. Please try again"
            is AppError.StorageError -> "File operation failed. Please try again"
            is AppError.TimeoutError -> "Operation is taking longer than expected. Please try again"
            is AppError.PermissionError -> "You don't have permission to perform this action"
            is AppError.UnknownError -> "Something went wrong. Please try again"
        }
    }
    
    fun showSuccessMessage(context: Context, message: String, view: View? = null) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    fun showInfoMessage(context: Context, message: String, view: View? = null) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    fun showWarningMessage(context: Context, message: String, view: View? = null) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(android.graphics.Color.parseColor("#FF9800"))
                .show()
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    // Retry mechanism for operations
    suspend fun <T> retryOperation(
        maxRetries: Int = 3,
        delayMs: Long = 1000,
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Operation failed on attempt ${attempt + 1}", e)
                
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(delayMs * (attempt + 1))
                }
            }
        }
        
        throw lastException ?: Exception("Operation failed after $maxRetries attempts")
    }
    
    // Check if error is recoverable
    fun isRecoverableError(error: AppError): Boolean {
        return when (error) {
            is AppError.NetworkError -> true
            is AppError.TimeoutError -> true
            is AppError.DatabaseError -> error.code != "PERMISSION_DENIED"
            is AppError.StorageError -> error.code != "NOT_AUTHORIZED"
            is AppError.AuthenticationError -> false
            is AppError.ValidationError -> false
            is AppError.PermissionError -> false
            is AppError.UnknownError -> true
        }
    }
}