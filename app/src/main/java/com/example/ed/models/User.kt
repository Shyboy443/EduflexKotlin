package com.example.ed.models

import java.util.Date

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "Student",
    val isActive: Boolean = true,
    val createdAt: Date = Date()
) {
    fun getInitials(): String {
        return fullName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "U" }
    }
    
    fun getRoleColor(): String {
        return when (role.lowercase()) {
            "admin" -> "#FF5722"
            "teacher" -> "#2196F3"
            "student" -> "#4CAF50"
            else -> "#9E9E9E"
        }
    }
    
    fun getStatusText(): String {
        return if (isActive) "Active" else "Inactive"
    }
}