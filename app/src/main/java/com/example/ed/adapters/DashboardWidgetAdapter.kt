package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.*
import com.example.ed.models.SimpleCourse as Course
import com.example.ed.services.DatabaseService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardWidgetAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val onWidgetClick: (DashboardWidget) -> Unit,
    private val onWidgetEdit: (DashboardWidget) -> Unit,
    private val onWidgetRemove: (DashboardWidget) -> Unit
) : RecyclerView.Adapter<DashboardWidgetAdapter.WidgetViewHolder>() {

    private var widgets = listOf<DashboardWidget>()
    private var isCustomizeMode = false

    fun updateWidgets(newWidgets: List<DashboardWidget>) {
        widgets = newWidgets
        notifyDataSetChanged()
    }

    fun setCustomizeMode(customizeMode: Boolean) {
        isCustomizeMode = customizeMode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetViewHolder {
        val layoutRes = when (viewType) {
            WidgetSize.SMALL.ordinal -> R.layout.item_widget_small
            WidgetSize.MEDIUM.ordinal -> R.layout.item_widget_medium
            WidgetSize.LARGE.ordinal -> R.layout.item_widget_large
            else -> R.layout.item_widget_medium
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return WidgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
        holder.bind(widgets[position])
    }

    override fun getItemCount(): Int = widgets.size

    override fun getItemViewType(position: Int): Int {
        return widgets[position].size.ordinal
    }

    inner class WidgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val contentRecyclerView: RecyclerView? = itemView.findViewById(R.id.contentRecyclerView)
        private val editButton: MaterialButton? = itemView.findViewById(R.id.editButton)
        private val removeButton: MaterialButton? = itemView.findViewById(R.id.removeButton)
        private val dragHandle: ImageView? = itemView.findViewById(R.id.dragHandle)

        fun bind(widget: DashboardWidget) {
            titleTextView.text = widget.title

            // Show/hide customize controls
            editButton?.visibility = if (isCustomizeMode) View.VISIBLE else View.GONE
            removeButton?.visibility = if (isCustomizeMode) View.VISIBLE else View.GONE
            dragHandle?.visibility = if (isCustomizeMode) View.VISIBLE else View.GONE

            // Setup widget content based on type
            setupWidgetContent(widget)

            // Set click listeners
            if (!isCustomizeMode) {
                cardView.setOnClickListener { onWidgetClick(widget) }
            } else {
                cardView.setOnClickListener(null)
            }

            editButton?.setOnClickListener { onWidgetEdit(widget) }
            removeButton?.setOnClickListener { onWidgetRemove(widget) }

            // Set card appearance
            cardView.strokeColor = if (isCustomizeMode) {
                ContextCompat.getColor(itemView.context, R.color.primary)
            } else {
                ContextCompat.getColor(itemView.context, R.color.divider)
            }
        }

        private fun setupWidgetContent(widget: DashboardWidget) {
            when (widget.type) {
                WidgetType.RECENT_COURSES -> setupRecentCoursesWidget()
                WidgetType.UPCOMING_ASSIGNMENTS -> setupUpcomingAssignmentsWidget()
                WidgetType.GRADE_DISTRIBUTION -> setupGradeDistributionWidget()
                WidgetType.STUDENT_ACTIVITY -> setupStudentActivityWidget()
                WidgetType.RECENT_SUBMISSIONS -> setupRecentSubmissionsWidget()
                WidgetType.ANNOUNCEMENTS -> setupAnnouncementsWidget()
                WidgetType.CALENDAR -> setupCalendarWidget()
                WidgetType.QUICK_ACTIONS -> setupQuickActionsWidget()
            }
        }

        private fun setupRecentCoursesWidget() {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val databaseService = DatabaseService.getInstance(itemView.context)
                lifecycleOwner.lifecycleScope.launch {
                    databaseService.getCoursesByInstructorRealTime(currentUser.uid)
                        .collect { courses ->
                            // Convert Course to SimpleCourse (aliased as Course in this file)
                            val simpleCourses = courses.map { course ->
                                Course(
                                    id = course.id,
                                    name = course.title,
                                    code = course.id,
                                    description = course.description,
                                    instructorId = course.teacherId,
                                    enrollmentCount = course.enrolledStudents,
                                    isActive = course.isPublished
                                )
                            }
                            val recentCourses = simpleCourses.take(5) // Show only 5 most recent
                            val adapter = RecentCoursesAdapter(recentCourses)
                            contentRecyclerView?.apply {
                                layoutManager = LinearLayoutManager(itemView.context)
                                this.adapter = adapter
                            }
                        }
                }
            }
        }

        private fun setupUpcomingAssignmentsWidget() {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val databaseService = DatabaseService.getInstance(itemView.context)
                lifecycleOwner.lifecycleScope.launch {
                    databaseService.getAssignmentsRealTime(currentUser.uid)
                        .collect { assignments ->
                            val upcomingAssignments = assignments
                                .filter { it.dueDate > System.currentTimeMillis() }
                                .take(5) // Show only 5 upcoming assignments
                            val adapter = UpcomingAssignmentsAdapter(upcomingAssignments)
                            contentRecyclerView?.apply {
                                layoutManager = LinearLayoutManager(itemView.context)
                                this.adapter = adapter
                            }
                        }
                }
            }
        }

        private fun setupGradeDistributionWidget() {
            // Setup grade distribution chart or summary
            val gradeStatsTextView: TextView? = itemView.findViewById(R.id.gradeStatsTextView)
            gradeStatsTextView?.text = "A: 25%\nB: 35%\nC: 30%\nD: 8%\nF: 2%"
        }

        private fun setupStudentActivityWidget() {
            val activityTextView: TextView? = itemView.findViewById(R.id.activityTextView)
            activityTextView?.text = "Active Students: 89\nOnline Now: 23\nAvg. Session: 45min"
        }

        private fun setupRecentSubmissionsWidget() {
            contentRecyclerView?.let { recyclerView ->
                val submissions = getMockRecentSubmissions()
                val adapter = RecentSubmissionsAdapter(submissions)
                recyclerView.layoutManager = LinearLayoutManager(itemView.context)
                recyclerView.adapter = adapter
            }
        }

        private fun setupAnnouncementsWidget() {
            contentRecyclerView?.let { recyclerView ->
                val announcements = getMockAnnouncements()
                val adapter = AnnouncementsAdapter(announcements)
                recyclerView.layoutManager = LinearLayoutManager(itemView.context)
                recyclerView.adapter = adapter
            }
        }

        private fun setupCalendarWidget() {
            val calendarTextView: TextView? = itemView.findViewById(R.id.calendarTextView)
            val today = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
            calendarTextView?.text = "Today: $today\n\n• 10:00 AM - Lecture\n• 2:00 PM - Office Hours\n• 4:00 PM - Meeting"
        }

        private fun setupQuickActionsWidget() {
            val createAssignmentButton: MaterialButton? = itemView.findViewById(R.id.createAssignmentButton)
            val gradeSubmissionsButton: MaterialButton? = itemView.findViewById(R.id.gradeSubmissionsButton)
            val sendAnnouncementButton: MaterialButton? = itemView.findViewById(R.id.sendAnnouncementButton)

            createAssignmentButton?.setOnClickListener {
                // Handle create assignment
            }

            gradeSubmissionsButton?.setOnClickListener {
                // Handle grade submissions
            }

            sendAnnouncementButton?.setOnClickListener {
                // Handle send announcement
            }
        }

        private fun getMockRecentCourses(): List<Course> {
            return listOf(
                Course(
                    id = "course1",
                    name = "Android Development",
                    code = "CS 4330",
                    description = "Mobile app development with Android",
                    instructorId = "instructor1",
                    enrollmentCount = 45,
                    isActive = true
                ),
                Course(
                    id = "course2",
                    name = "Web Development",
                    code = "CS 3320",
                    description = "Full-stack web development",
                    instructorId = "instructor1",
                    enrollmentCount = 38,
                    isActive = true
                ),
                Course(
                    id = "course3",
                    name = "Database Systems",
                    code = "CS 3380",
                    description = "Database design and implementation",
                    instructorId = "instructor1",
                    enrollmentCount = 52,
                    isActive = true
                )
            )
        }

        private fun getMockUpcomingAssignments(): List<Assignment> {
            return listOf(
                Assignment(
                    id = "assignment1",
                    title = "Final Project",
                    description = "Complete Android app",
                    dueDate = System.currentTimeMillis() + 86400000, // Tomorrow
                    maxPoints = 100
                ),
                Assignment(
                    id = "assignment2",
                    title = "Quiz 3",
                    description = "Database normalization",
                    dueDate = System.currentTimeMillis() + 172800000, // 2 days
                    maxPoints = 50
                )
            )
        }

        private fun getMockRecentSubmissions(): List<Submission> {
            return listOf(
                Submission(
                    id = "sub1",
                    assignmentId = "assignment1",
                    studentId = "student1",
                    studentName = "John Doe",
                    submittedAt = Date(System.currentTimeMillis() - 3600000),
                    status = SubmissionStatus.SUBMITTED,
                    grade = null,
                    feedback = ""
                ),
                Submission(
                    id = "sub2",
                    assignmentId = "assignment2",
                    studentId = "student2",
                    studentName = "Jane Smith",
                    submittedAt = Date(System.currentTimeMillis() - 7200000),
                    status = SubmissionStatus.GRADED,
                    grade = 85.0,
                    feedback = "Good work!"
                )
            )
        }

        private fun getMockAnnouncements(): List<Announcement> {
            return listOf(
                Announcement(
                    id = "ann1",
                    title = "Midterm Results",
                    content = "Midterm grades have been posted",
                    courseId = "course1",
                    createdAt = Date(System.currentTimeMillis() - 86400000),
                    isImportant = true
                ),
                Announcement(
                    id = "ann2",
                    title = "Office Hours Change",
                    content = "Office hours moved to 3-5 PM",
                    courseId = "course2",
                    createdAt = Date(System.currentTimeMillis() - 172800000),
                    isImportant = false
                )
            )
        }
    }

    // Simple adapters for widget content
    private class RecentCoursesAdapter(private val courses: List<Course>) : 
        RecyclerView.Adapter<RecentCoursesAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_widget_course, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(courses[position])
        }
        
        override fun getItemCount(): Int = minOf(courses.size, 3)
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
            private val codeTextView: TextView = itemView.findViewById(R.id.codeTextView)
            private val enrollmentTextView: TextView = itemView.findViewById(R.id.enrollmentTextView)
            
            fun bind(course: Course) {
                nameTextView.text = course.name
                codeTextView.text = course.code
                enrollmentTextView.text = "${course.enrollmentCount} students"
            }
        }
    }

    private class UpcomingAssignmentsAdapter(private val assignments: List<Assignment>) : 
        RecyclerView.Adapter<UpcomingAssignmentsAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_widget_assignment, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(assignments[position])
        }
        
        override fun getItemCount(): Int = minOf(assignments.size, 3)
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
            private val dueDateTextView: TextView = itemView.findViewById(R.id.dueDateTextView)
            
            fun bind(assignment: Assignment) {
                titleTextView.text = assignment.title
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                dueDateTextView.text = "Due ${dateFormat.format(Date(assignment.dueDate))}"
            }
        }
    }

    private class RecentSubmissionsAdapter(private val submissions: List<Submission>) : 
        RecyclerView.Adapter<RecentSubmissionsAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_widget_submission, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(submissions[position])
        }
        
        override fun getItemCount(): Int = minOf(submissions.size, 3)
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val studentNameTextView: TextView = itemView.findViewById(R.id.studentNameTextView)
            private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
            
            fun bind(submission: Submission) {
                studentNameTextView.text = submission.studentName
                statusTextView.text = submission.status.name
            }
        }
    }

    private class AnnouncementsAdapter(private val announcements: List<Announcement>) : 
        RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_widget_announcement, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(announcements[position])
        }
        
        override fun getItemCount(): Int = minOf(announcements.size, 3)
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
            private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
            
            fun bind(announcement: Announcement) {
                titleTextView.text = announcement.title
                contentTextView.text = announcement.content
            }
        }
    }
}