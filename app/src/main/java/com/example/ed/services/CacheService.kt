package com.example.ed.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.ed.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class CacheService private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: CacheService? = null
        
        fun getInstance(context: Context): CacheService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CacheService(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val TAG = "CacheService"
        private const val PREFS_NAME = "ed_cache"
        private const val CACHE_EXPIRY_TIME = 5 * 60 * 1000L // 5 minutes
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // In-memory cache for real-time data
    private val coursesCache = ConcurrentHashMap<String, CacheEntry<List<SimpleCourse>>>()
    private val assignmentsCache = ConcurrentHashMap<String, CacheEntry<List<Assignment>>>()
    private val announcementsCache = ConcurrentHashMap<String, CacheEntry<List<Announcement>>>()
    private val paymentsCache = ConcurrentHashMap<String, CacheEntry<List<PaymentRecord>>>()
    private val courseDetailsCache = ConcurrentHashMap<String, CacheEntry<EnhancedCourse>>()
    private val statsCache = ConcurrentHashMap<String, CacheEntry<*>>()
    
    // StateFlows for reactive caching
    private val _coursesFlow = MutableStateFlow<List<SimpleCourse>>(emptyList())
    val coursesFlow: Flow<List<SimpleCourse>> = _coursesFlow.asStateFlow()
    
    private val _assignmentsFlow = MutableStateFlow<List<Assignment>>(emptyList())
    val assignmentsFlow: Flow<List<Assignment>> = _assignmentsFlow.asStateFlow()
    
    private val _announcementsFlow = MutableStateFlow<List<Announcement>>(emptyList())
    val announcementsFlow: Flow<List<Announcement>> = _announcementsFlow.asStateFlow()
    
    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val expiryTime: Long = CACHE_EXPIRY_TIME
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > expiryTime
    }
    
    // Course caching
    fun cacheCourses(instructorId: String, courses: List<SimpleCourse>) {
        try {
            val cacheEntry = CacheEntry(courses, System.currentTimeMillis())
            coursesCache[instructorId] = cacheEntry
            _coursesFlow.value = courses
            
            // Persist to SharedPreferences for offline access
            val json = gson.toJson(courses)
            sharedPreferences.edit()
                .putString("courses_$instructorId", json)
                .putLong("courses_${instructorId}_timestamp", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Cached ${courses.size} courses for instructor $instructorId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching courses", e)
        }
    }
    
    fun getCachedCourses(instructorId: String): List<SimpleCourse>? {
        return try {
            val cacheEntry = coursesCache[instructorId]
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                Log.d(TAG, "Retrieved ${cacheEntry.data.size} courses from memory cache")
                return cacheEntry.data
            }
            
            // Try SharedPreferences
            val json = sharedPreferences.getString("courses_$instructorId", null)
            val timestamp = sharedPreferences.getLong("courses_${instructorId}_timestamp", 0)
            
            if (json != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                val type = object : TypeToken<List<SimpleCourse>>() {}.type
                val courses = gson.fromJson<List<SimpleCourse>>(json, type)
                Log.d(TAG, "Retrieved ${courses.size} courses from persistent cache")
                return courses
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached courses", e)
            null
        }
    }
    
    // Assignment caching
    fun cacheAssignments(instructorId: String, assignments: List<Assignment>) {
        try {
            val cacheEntry = CacheEntry(assignments, System.currentTimeMillis())
            assignmentsCache[instructorId] = cacheEntry
            _assignmentsFlow.value = assignments
            
            val json = gson.toJson(assignments)
            sharedPreferences.edit()
                .putString("assignments_$instructorId", json)
                .putLong("assignments_${instructorId}_timestamp", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Cached ${assignments.size} assignments for instructor $instructorId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching assignments", e)
        }
    }
    
    fun getCachedAssignments(instructorId: String): List<Assignment>? {
        return try {
            val cacheEntry = assignmentsCache[instructorId]
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                return cacheEntry.data
            }
            
            val json = sharedPreferences.getString("assignments_$instructorId", null)
            val timestamp = sharedPreferences.getLong("assignments_${instructorId}_timestamp", 0)
            
            if (json != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                val type = object : TypeToken<List<Assignment>>() {}.type
                return gson.fromJson<List<Assignment>>(json, type)
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached assignments", e)
            null
        }
    }
    
    // Announcement caching
    fun cacheAnnouncements(courseId: String, announcements: List<Announcement>) {
        try {
            val cacheEntry = CacheEntry(announcements, System.currentTimeMillis())
            announcementsCache[courseId] = cacheEntry
            _announcementsFlow.value = announcements
            
            val json = gson.toJson(announcements)
            sharedPreferences.edit()
                .putString("announcements_$courseId", json)
                .putLong("announcements_${courseId}_timestamp", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Cached ${announcements.size} announcements for course $courseId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching announcements", e)
        }
    }
    
    fun getCachedAnnouncements(courseId: String): List<Announcement>? {
        return try {
            val cacheEntry = announcementsCache[courseId]
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                return cacheEntry.data
            }
            
            val json = sharedPreferences.getString("announcements_$courseId", null)
            val timestamp = sharedPreferences.getLong("announcements_${courseId}_timestamp", 0)
            
            if (json != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                val type = object : TypeToken<List<Announcement>>() {}.type
                return gson.fromJson<List<Announcement>>(json, type)
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached announcements", e)
            null
        }
    }
    

    
    // Payment history caching
    fun cachePaymentHistory(userId: String, payments: List<PaymentRecord>) {
        try {
            val cacheEntry = CacheEntry(payments, System.currentTimeMillis())
            paymentsCache[userId] = cacheEntry
            
            val json = gson.toJson(payments)
            sharedPreferences.edit()
                .putString("payments_$userId", json)
                .putLong("payments_${userId}_timestamp", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Cached ${payments.size} payments for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching payments", e)
        }
    }
    
    fun getCachedPaymentHistory(userId: String): List<PaymentRecord>? {
        return try {
            val cacheEntry = paymentsCache[userId]
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                return cacheEntry.data
            }
            
            val json = sharedPreferences.getString("payments_$userId", null)
            val timestamp = sharedPreferences.getLong("payments_${userId}_timestamp", 0)
            
            if (json != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                val type = object : TypeToken<List<PaymentRecord>>() {}.type
                return gson.fromJson<List<PaymentRecord>>(json, type)
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached payments", e)
            null
        }
    }
    
    // Course details caching
    fun cacheCourseDetails(courseId: String, course: EnhancedCourse) {
        try {
            val cacheEntry = CacheEntry(course, System.currentTimeMillis())
            courseDetailsCache[courseId] = cacheEntry
            
            val json = gson.toJson(course)
            sharedPreferences.edit()
                .putString("course_details_$courseId", json)
                .putLong("course_details_${courseId}_timestamp", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Cached course details for $courseId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching course details", e)
        }
    }
    
    fun getCachedCourseDetails(courseId: String): EnhancedCourse? {
        return try {
            val cacheEntry = courseDetailsCache[courseId]
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                return cacheEntry.data
            }
            
            val json = sharedPreferences.getString("course_details_$courseId", null)
            val timestamp = sharedPreferences.getLong("course_details_${courseId}_timestamp", 0)
            
            if (json != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                return gson.fromJson(json, EnhancedCourse::class.java)
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached course details", e)
            null
        }
    }
    
    // Stats caching
    fun cacheInstructorStats(instructorId: String, stats: InstructorStats) {
        try {
            val cacheEntry = CacheEntry(stats, System.currentTimeMillis())
            statsCache["instructor_$instructorId"] = cacheEntry
            
            val json = gson.toJson(stats)
            sharedPreferences.edit()
                .putString("instructor_stats_$instructorId", json)
                .putLong("instructor_stats_${instructorId}_timestamp", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Cached instructor stats for $instructorId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching instructor stats", e)
        }
    }
    
    fun getCachedInstructorStats(instructorId: String): InstructorStats? {
        return try {
            val cacheEntry = statsCache["instructor_$instructorId"]
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                return cacheEntry.data as InstructorStats
            }
            
            val json = sharedPreferences.getString("instructor_stats_$instructorId", null)
            val timestamp = sharedPreferences.getLong("instructor_stats_${instructorId}_timestamp", 0)
            
            if (json != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                return gson.fromJson(json, InstructorStats::class.java)
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached instructor stats", e)
            null
        }
    }
    
    fun cacheStudentStats(studentId: String, stats: StudentStats) {
        try {
            val cacheEntry = CacheEntry(stats, System.currentTimeMillis())
            statsCache["student_$studentId"] = cacheEntry
            
            val json = gson.toJson(stats)
            sharedPreferences.edit()
                .putString("student_stats_$studentId", json)
                .putLong("student_stats_${studentId}_timestamp", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Cached student stats for $studentId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching student stats", e)
        }
    }
    
    fun getCachedStudentStats(studentId: String): StudentStats? {
        return try {
            val cacheEntry = statsCache["student_$studentId"]
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                return cacheEntry.data as StudentStats
            }
            
            val json = sharedPreferences.getString("student_stats_$studentId", null)
            val timestamp = sharedPreferences.getLong("student_stats_${studentId}_timestamp", 0)
            
            if (json != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                return gson.fromJson(json, StudentStats::class.java)
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached student stats", e)
            null
        }
    }
    
    // Cache management
    fun clearCache() {
        try {
            coursesCache.clear()
            assignmentsCache.clear()
            announcementsCache.clear()
            paymentsCache.clear()
            courseDetailsCache.clear()
            statsCache.clear()
            
            sharedPreferences.edit().clear().apply()
            
            Log.d(TAG, "Cache cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }
    
    fun clearExpiredCache() {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Clear expired memory cache
            coursesCache.entries.removeAll { it.value.isExpired() }
            assignmentsCache.entries.removeAll { it.value.isExpired() }
            announcementsCache.entries.removeAll { it.value.isExpired() }
            paymentsCache.entries.removeAll { it.value.isExpired() }
            courseDetailsCache.entries.removeAll { it.value.isExpired() }
            statsCache.entries.removeAll { it.value.isExpired() }
            
            // Clear expired SharedPreferences entries
            val editor = sharedPreferences.edit()
            val allEntries = sharedPreferences.all
            
            for ((key, _) in allEntries) {
                if (key.endsWith("_timestamp")) {
                    val timestamp = sharedPreferences.getLong(key, 0)
                    if (currentTime - timestamp > CACHE_EXPIRY_TIME) {
                        val dataKey = key.replace("_timestamp", "")
                        editor.remove(key).remove(dataKey)
                    }
                }
            }
            
            editor.apply()
            Log.d(TAG, "Expired cache entries cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing expired cache", e)
        }
    }
    
    fun getCacheSize(): Long {
        return try {
            var size = 0L
            size += coursesCache.size
            size += assignmentsCache.size
            size += announcementsCache.size
            size += paymentsCache.size
            size += courseDetailsCache.size
            size += statsCache.size
            size
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
            0L
        }
    }
}