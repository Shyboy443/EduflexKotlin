package com.example.ed.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

/**
 * Utility class for network connectivity checks and operations
 */
object NetworkUtils {
    
    private const val TAG = "NetworkUtils"
    
    /**
     * Check if the device has an active internet connection
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnectedOrConnecting == true
        }
    }
    
    /**
     * Check if the device is connected to WiFi
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }
    
    /**
     * Check if the device is connected to cellular data
     */
    fun isCellularConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected
        }
    }
    
    /**
     * Get a user-friendly network status message
     */
    fun getNetworkStatusMessage(context: Context): String {
        return when {
            !isNetworkAvailable(context) -> "No internet connection available"
            isWifiConnected(context) -> "Connected via WiFi"
            isCellularConnected(context) -> "Connected via cellular data"
            else -> "Connected to internet"
        }
    }
    
    /**
     * Log network status for debugging
     */
    fun logNetworkStatus(context: Context) {
        val status = getNetworkStatusMessage(context)
        Log.d(TAG, "Network status: $status")
    }
}