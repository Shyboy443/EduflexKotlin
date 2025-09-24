package com.example.ed.models

import java.io.Serializable
import java.util.*

/**
 * Represents a widget on the dashboard
 */
data class DashboardWidget(
    val id: String,
    val type: WidgetType,
    val title: String,
    val size: WidgetSize,
    val position: Int,
    val isVisible: Boolean,
    val data: Map<String, Any> = emptyMap()
) : Serializable

/**
 * Types of widgets available on the dashboard
 */
enum class WidgetType : Serializable {
    RECENT_COURSES,
    UPCOMING_ASSIGNMENTS,
    GRADE_DISTRIBUTION,
    STUDENT_ACTIVITY,
    RECENT_SUBMISSIONS,
    ANNOUNCEMENTS,
    CALENDAR,
    QUICK_ACTIONS
}

/**
 * Size options for dashboard widgets
 */
enum class WidgetSize : Serializable {
    SMALL,   // 1 grid column
    MEDIUM,  // 2 grid columns
    LARGE    // 2 grid columns, taller
}