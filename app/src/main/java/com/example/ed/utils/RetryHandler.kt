package com.example.ed.utils

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlin.math.pow
import kotlin.random.Random

class RetryHandler {
    
    companion object {
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_BASE_DELAY = 1000L // 1 second
        private const val DEFAULT_MAX_DELAY = 30000L // 30 seconds
        private const val JITTER_FACTOR = 0.1
    }
    
    /**
     * Retry a suspend function with exponential backoff
     */
    suspend fun <T> retryWithBackoff(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        baseDelay: Long = DEFAULT_BASE_DELAY,
        maxDelay: Long = DEFAULT_MAX_DELAY,
        shouldRetry: (Throwable) -> Boolean = { true },
        onRetry: ((Int, Throwable) -> Unit)? = null,
        operation: suspend () -> T
    ): T {
        var lastException: Throwable? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Throwable) {
                lastException = e
                
                if (attempt == maxRetries || !shouldRetry(e)) {
                    throw e
                }
                
                onRetry?.invoke(attempt + 1, e)
                
                val delay = calculateDelay(attempt, baseDelay, maxDelay)
                delay(delay)
            }
        }
        
        throw lastException ?: RuntimeException("Retry failed")
    }
    
    /**
     * Retry a regular function with exponential backoff (non-suspend)
     */
    fun <T> retrySync(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        baseDelay: Long = DEFAULT_BASE_DELAY,
        maxDelay: Long = DEFAULT_MAX_DELAY,
        shouldRetry: (Throwable) -> Boolean = { true },
        onRetry: ((Int, Throwable) -> Unit)? = null,
        operation: () -> T
    ): T {
        var lastException: Throwable? = null
        val handler = Handler(Looper.getMainLooper())
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Throwable) {
                lastException = e
                
                if (attempt == maxRetries || !shouldRetry(e)) {
                    throw e
                }
                
                onRetry?.invoke(attempt + 1, e)
                
                val delay = calculateDelay(attempt, baseDelay, maxDelay)
                Thread.sleep(delay)
            }
        }
        
        throw lastException ?: RuntimeException("Retry failed")
    }
    
    /**
     * Retry with callback-based operations (for Firebase operations)
     */
    fun <T> retryCallback(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        baseDelay: Long = DEFAULT_BASE_DELAY,
        maxDelay: Long = DEFAULT_MAX_DELAY,
        shouldRetry: (Throwable) -> Boolean = { true },
        onRetry: ((Int, Throwable) -> Unit)? = null,
        onSuccess: (T) -> Unit,
        onFailure: (Throwable) -> Unit,
        operation: (onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) -> Unit
    ) {
        retryCallbackInternal(
            attempt = 0,
            maxRetries = maxRetries,
            baseDelay = baseDelay,
            maxDelay = maxDelay,
            shouldRetry = shouldRetry,
            onRetry = onRetry,
            onSuccess = onSuccess,
            onFailure = onFailure,
            operation = operation
        )
    }
    
    private fun <T> retryCallbackInternal(
        attempt: Int,
        maxRetries: Int,
        baseDelay: Long,
        maxDelay: Long,
        shouldRetry: (Throwable) -> Boolean,
        onRetry: ((Int, Throwable) -> Unit)?,
        onSuccess: (T) -> Unit,
        onFailure: (Throwable) -> Unit,
        operation: (onSuccess: (T) -> Unit, onFailure: (Throwable) -> Unit) -> Unit
    ) {
        operation(
            onSuccess,
            { error ->
                if (attempt >= maxRetries || !shouldRetry(error)) {
                    onFailure(error)
                    return@operation
                }
                
                onRetry?.invoke(attempt + 1, error)
                
                val delay = calculateDelay(attempt, baseDelay, maxDelay)
                Handler(Looper.getMainLooper()).postDelayed({
                    retryCallbackInternal(
                        attempt = attempt + 1,
                        maxRetries = maxRetries,
                        baseDelay = baseDelay,
                        maxDelay = maxDelay,
                        shouldRetry = shouldRetry,
                        onRetry = onRetry,
                        onSuccess = onSuccess,
                        onFailure = onFailure,
                        operation = operation
                    )
                }, delay)
            }
        )
    }
    
    /**
     * Calculate delay with exponential backoff and jitter
     */
    private fun calculateDelay(attempt: Int, baseDelay: Long, maxDelay: Long): Long {
        val exponentialDelay = baseDelay * (2.0.pow(attempt.toDouble())).toLong()
        val delayWithCap = minOf(exponentialDelay, maxDelay)
        
        // Add jitter to prevent thundering herd
        val jitter = delayWithCap * JITTER_FACTOR * Random.nextDouble()
        return (delayWithCap + jitter).toLong()
    }
    
    /**
     * Common retry conditions
     */
    object RetryConditions {
        
        val networkErrors: (Throwable) -> Boolean = { error ->
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("timeout", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true
        }
        
        val firebaseErrors: (Throwable) -> Boolean = { error ->
            val message = error.message?.lowercase() ?: ""
            message.contains("unavailable") ||
            message.contains("deadline exceeded") ||
            message.contains("internal") ||
            message.contains("resource exhausted")
        }
        
        val temporaryErrors: (Throwable) -> Boolean = { error ->
            networkErrors(error) || firebaseErrors(error)
        }
        
        fun customCondition(vararg errorMessages: String): (Throwable) -> Boolean = { error ->
            val message = error.message?.lowercase() ?: ""
            errorMessages.any { message.contains(it.lowercase()) }
        }
    }
    
    /**
     * Retry strategies
     */
    object RetryStrategies {
        
        val quick = RetryConfig(
            maxRetries = 2,
            baseDelay = 500L,
            maxDelay = 5000L
        )
        
        val standard = RetryConfig(
            maxRetries = 3,
            baseDelay = 1000L,
            maxDelay = 30000L
        )
        
        val persistent = RetryConfig(
            maxRetries = 5,
            baseDelay = 2000L,
            maxDelay = 60000L
        )
        
        val aggressive = RetryConfig(
            maxRetries = 10,
            baseDelay = 100L,
            maxDelay = 10000L
        )
    }
    
    data class RetryConfig(
        val maxRetries: Int,
        val baseDelay: Long,
        val maxDelay: Long
    )
}