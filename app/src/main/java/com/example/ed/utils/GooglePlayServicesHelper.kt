package com.example.ed.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Helper class for managing Google Play Services availability and error handling
 */
object GooglePlayServicesHelper {
    
    private const val TAG = "GooglePlayServicesHelper"
    
    /**
     * Check if Google Play Services is available on the device
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }
    
    /**
     * Get a user-friendly error message for Google Play Services issues
     */
    fun getGooglePlayServicesErrorMessage(context: Context): String {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        
        return when (resultCode) {
            ConnectionResult.SUCCESS -> "Google Play Services is available"
            ConnectionResult.SERVICE_MISSING -> "Google Play Services is missing. Please install it from the Play Store."
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Google Play Services needs to be updated. Please update from the Play Store."
            ConnectionResult.SERVICE_DISABLED -> "Google Play Services is disabled. Please enable it in your device settings."
            ConnectionResult.SERVICE_INVALID -> "Google Play Services is invalid. Please reinstall from the Play Store."
            else -> "Google Play Services is not available. Some features may not work properly."
        }
    }
    
    /**
     * Check if Google Play Services error is recoverable and show resolution dialog
     */
    fun handleGooglePlayServicesError(activity: Activity, requestCode: Int = 9000): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.w(TAG, "Google Play Services error: $resultCode")
            
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                // Show dialog to resolve the error
                googleApiAvailability.getErrorDialog(activity, resultCode, requestCode)?.show()
                return true
            } else {
                Log.e(TAG, "Google Play Services error is not recoverable: $resultCode")
                return false
            }
        }
        
        return false
    }
    
    /**
     * Log Google Play Services status for debugging
     */
    fun logGooglePlayServicesStatus(context: Context) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        
        Log.d(TAG, "Google Play Services status: $resultCode")
        Log.d(TAG, "Error message: ${getGooglePlayServicesErrorMessage(context)}")
        Log.d(TAG, "Is user resolvable: ${googleApiAvailability.isUserResolvableError(resultCode)}")
    }
}