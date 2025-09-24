package com.example.ed.models

data class SimpleCourse(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val instructorId: String = "",
    val enrollmentCount: Int = 0,
    val isActive: Boolean = true
)