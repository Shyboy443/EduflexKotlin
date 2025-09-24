package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.DashboardWidgetAdapter
import com.example.ed.models.*
import com.example.ed.models.DashboardWidget
import com.example.ed.models.WidgetType
import com.example.ed.models.WidgetSize
import com.example.ed.services.DatabaseService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var welcomeCard: MaterialCardView
    private lateinit var welcomeTextView: TextView
    private lateinit var quickStatsCard: MaterialCardView
    private lateinit var totalCoursesTextView: TextView
    private lateinit var totalStudentsTextView: TextView
    private lateinit var pendingAssignmentsTextView: TextView
    private lateinit var averageGradeTextView: TextView
    private lateinit var widgetsRecyclerView: RecyclerView
    private lateinit var customizeButton: MaterialButton
    private lateinit var addWidgetFab: FloatingActionButton
    
    private lateinit var widgetAdapter: DashboardWidgetAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var dashboardWidgets = mutableListOf<DashboardWidget>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        loadDashboardData()
        loadUserWidgets()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        welcomeCard = findViewById(R.id.welcomeCard)
        welcomeTextView = findViewById(R.id.welcomeTextView)
        quickStatsCard = findViewById(R.id.quickStatsCard)
        totalCoursesTextView = findViewById(R.id.totalCoursesTextView)
        totalStudentsTextView = findViewById(R.id.totalStudentsTextView)
        pendingAssignmentsTextView = findViewById(R.id.pendingAssignmentsTextView)
        averageGradeTextView = findViewById(R.id.averageGradeTextView)
        widgetsRecyclerView = findViewById(R.id.widgetsRecyclerView)
        customizeButton = findViewById(R.id.customizeButton)
        addWidgetFab = findViewById(R.id.addWidgetFab)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Dashboard"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                startActivity(Intent(this, NotificationActivity::class.java))
                true
            }
            R.id.action_settings -> {
                // Navigate to settings
                true
            }
            R.id.action_profile -> {
                // Navigate to profile
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        widgetAdapter = DashboardWidgetAdapter(
            this,
            onWidgetClick = { widget: DashboardWidget -> handleWidgetClick(widget) },
            onWidgetEdit = { widget: DashboardWidget -> editWidget(widget) },
            onWidgetRemove = { widget: DashboardWidget -> removeWidget(widget) }
        )
        
        val gridLayoutManager = GridLayoutManager(this, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (dashboardWidgets[position].size) {
                    WidgetSize.SMALL -> 1
                    WidgetSize.MEDIUM -> 2
                    WidgetSize.LARGE -> 2
                }
            }
        }
        
        widgetsRecyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = widgetAdapter
        }
        
        // Setup drag and drop for widget reordering
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                
                Collections.swap(dashboardWidgets, fromPosition, toPosition)
                widgetAdapter.notifyItemMoved(fromPosition, toPosition)
                
                saveDashboardLayout()
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        
        itemTouchHelper.attachToRecyclerView(widgetsRecyclerView)
    }

    private fun setupButtons() {
        customizeButton.setOnClickListener {
            toggleCustomizeMode()
        }
        
        addWidgetFab.setOnClickListener {
            showAddWidgetDialog()
        }
    }

    private fun loadDashboardData() {
        // Load welcome message
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            currentHour < 12 -> "Good Morning"
            currentHour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
        welcomeTextView.text = "$greeting, ${getCurrentUserName()}"
        
        // Load quick stats
        loadQuickStats()
    }

    private fun loadQuickStats() {
        val userId = getCurrentUserId() ?: return
        val databaseService = DatabaseService.getInstance(this)
        
        lifecycleScope.launch {
            try {
                val stats = databaseService.getInstructorStats(userId)
                
                totalCoursesTextView.text = stats.totalCourses.toString()
                totalStudentsTextView.text = stats.totalStudents.toString()
                pendingAssignmentsTextView.text = stats.pendingAssignments.toString()
                averageGradeTextView.text = if (stats.averageGrade > 0) {
                    String.format("%.1f%%", stats.averageGrade)
                } else {
                    "N/A"
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading stats", e)
                // Fallback to show loading state or error message
                totalCoursesTextView.text = "0"
                totalStudentsTextView.text = "0"
                pendingAssignmentsTextView.text = "0"
                averageGradeTextView.text = "N/A"
            }
        }
    }

    private fun loadUserWidgets() {
        val userId = getCurrentUserId() ?: return
        
        firestore.collection("dashboardLayouts")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val widgetData = document.get("widgets") as? List<Map<String, Any>>
                    if (widgetData != null) {
                        dashboardWidgets.clear()
                        widgetData.forEach { data ->
                            val widget = DashboardWidget(
                                id = data["id"] as String,
                                type = WidgetType.valueOf(data["type"] as String),
                                title = data["title"] as String,
                                size = WidgetSize.valueOf(data["size"] as String),
                                position = (data["position"] as Long).toInt(),
                                isVisible = data["isVisible"] as Boolean,
                                data = data["data"] as? Map<String, Any> ?: emptyMap()
                            )
                            dashboardWidgets.add(widget)
                        }
                        dashboardWidgets.sortBy { it.position }
                        widgetAdapter.updateWidgets(dashboardWidgets)
                    } else {
                        loadDefaultWidgets()
                    }
                } else {
                    loadDefaultWidgets()
                }
            }
            .addOnFailureListener {
                loadDefaultWidgets()
            }
    }

    private fun loadDefaultWidgets() {
        dashboardWidgets.clear()
        
        val defaultWidgets = listOf(
            DashboardWidget(
                id = "recent_courses",
                type = WidgetType.RECENT_COURSES,
                title = "Recent Courses",
                size = WidgetSize.MEDIUM,
                position = 0,
                isVisible = true
            ),
            DashboardWidget(
                id = "upcoming_assignments",
                type = WidgetType.UPCOMING_ASSIGNMENTS,
                title = "Upcoming Assignments",
                size = WidgetSize.MEDIUM,
                position = 1,
                isVisible = true
            ),
            DashboardWidget(
                id = "grade_distribution",
                type = WidgetType.GRADE_DISTRIBUTION,
                title = "Grade Distribution",
                size = WidgetSize.SMALL,
                position = 2,
                isVisible = true
            ),
            DashboardWidget(
                id = "student_activity",
                type = WidgetType.STUDENT_ACTIVITY,
                title = "Student Activity",
                size = WidgetSize.SMALL,
                position = 3,
                isVisible = true
            ),
            DashboardWidget(
                id = "recent_submissions",
                type = WidgetType.RECENT_SUBMISSIONS,
                title = "Recent Submissions",
                size = WidgetSize.LARGE,
                position = 4,
                isVisible = true
            ),
            DashboardWidget(
                id = "announcements",
                type = WidgetType.ANNOUNCEMENTS,
                title = "Announcements",
                size = WidgetSize.MEDIUM,
                position = 5,
                isVisible = true
            )
        )
        
        dashboardWidgets.addAll(defaultWidgets)
        widgetAdapter.updateWidgets(dashboardWidgets)
        saveDashboardLayout()
    }

    private fun handleWidgetClick(widget: DashboardWidget) {
        when (widget.type) {
            WidgetType.RECENT_COURSES -> {
                startActivity(Intent(this, CourseListActivity::class.java))
            }
            WidgetType.UPCOMING_ASSIGNMENTS -> {
                startActivity(Intent(this, AssignmentListActivity::class.java))
            }
            WidgetType.GRADE_DISTRIBUTION -> {
                startActivity(Intent(this, GradingSystemActivity::class.java))
            }
            WidgetType.STUDENT_ACTIVITY -> {
                startActivity(Intent(this, StudentEngagementActivity::class.java))
            }
            WidgetType.RECENT_SUBMISSIONS -> {
                startActivity(Intent(this, GradingSystemActivity::class.java))
            }
            WidgetType.ANNOUNCEMENTS -> {
                // Navigate to announcements
            }
            WidgetType.CALENDAR -> {
                // Navigate to calendar
            }
            WidgetType.QUICK_ACTIONS -> {
                // Show quick actions menu
            }
        }
    }

    private fun editWidget(widget: DashboardWidget) {
        // Show widget configuration dialog
        val dialog = WidgetConfigDialog.newInstance(widget)
        dialog.setOnWidgetConfiguredListener { updatedWidget: DashboardWidget ->
            val index = dashboardWidgets.indexOfFirst { it.id == updatedWidget.id }
            if (index != -1) {
                dashboardWidgets[index] = updatedWidget
                widgetAdapter.notifyItemChanged(index)
                saveDashboardLayout()
            }
        }
        dialog.show(supportFragmentManager, "widget_config")
    }

    private fun removeWidget(widget: DashboardWidget) {
        val index = dashboardWidgets.indexOfFirst { it.id == widget.id }
        if (index != -1) {
            dashboardWidgets.removeAt(index)
            widgetAdapter.notifyItemRemoved(index)
            saveDashboardLayout()
        }
    }

    private fun toggleCustomizeMode() {
        val isCustomizing = customizeButton.text == "Done"
        
        if (isCustomizing) {
            customizeButton.text = "Customize"
            addWidgetFab.hide()
            widgetAdapter.setCustomizeMode(false)
        } else {
            customizeButton.text = "Done"
            addWidgetFab.show()
            widgetAdapter.setCustomizeMode(true)
        }
    }

    private fun showAddWidgetDialog() {
        val availableWidgets = WidgetType.values().filter { type ->
            dashboardWidgets.none { it.type == type }
        }
        
        if (availableWidgets.isEmpty()) {
            // Show message that all widgets are already added
            return
        }
        
        val dialog = AddWidgetDialog.newInstance(availableWidgets)
        dialog.setOnWidgetSelectedListener { widgetType: WidgetType ->
            val newWidget = DashboardWidget(
                id = UUID.randomUUID().toString(),
                type = widgetType,
                title = getWidgetTitle(widgetType),
                size = getDefaultWidgetSize(widgetType),
                position = dashboardWidgets.size,
                isVisible = true
            )
            
            dashboardWidgets.add(newWidget)
            widgetAdapter.notifyItemInserted(dashboardWidgets.size - 1)
            saveDashboardLayout()
        }
        dialog.show(supportFragmentManager, "add_widget")
    }

    private fun saveDashboardLayout() {
        val userId = getCurrentUserId() ?: return
        
        val widgetData = dashboardWidgets.mapIndexed { index, widget ->
            mapOf(
                "id" to widget.id,
                "type" to widget.type.name,
                "title" to widget.title,
                "size" to widget.size.name,
                "position" to index,
                "isVisible" to widget.isVisible,
                "data" to widget.data
            )
        }
        
        firestore.collection("dashboardLayouts")
            .document(userId)
            .set(mapOf("widgets" to widgetData))
    }

    private fun getWidgetTitle(type: WidgetType): String {
        return when (type) {
            WidgetType.RECENT_COURSES -> "Recent Courses"
            WidgetType.UPCOMING_ASSIGNMENTS -> "Upcoming Assignments"
            WidgetType.GRADE_DISTRIBUTION -> "Grade Distribution"
            WidgetType.STUDENT_ACTIVITY -> "Student Activity"
            WidgetType.RECENT_SUBMISSIONS -> "Recent Submissions"
            WidgetType.ANNOUNCEMENTS -> "Announcements"
            WidgetType.CALENDAR -> "Calendar"
            WidgetType.QUICK_ACTIONS -> "Quick Actions"
        }
    }

    private fun getDefaultWidgetSize(type: WidgetType): WidgetSize {
        return when (type) {
            WidgetType.RECENT_COURSES -> WidgetSize.MEDIUM
            WidgetType.UPCOMING_ASSIGNMENTS -> WidgetSize.MEDIUM
            WidgetType.GRADE_DISTRIBUTION -> WidgetSize.SMALL
            WidgetType.STUDENT_ACTIVITY -> WidgetSize.SMALL
            WidgetType.RECENT_SUBMISSIONS -> WidgetSize.LARGE
            WidgetType.ANNOUNCEMENTS -> WidgetSize.MEDIUM
            WidgetType.CALENDAR -> WidgetSize.LARGE
            WidgetType.QUICK_ACTIONS -> WidgetSize.SMALL
        }
    }

    private fun getCurrentUserId(): String? {
        return "current_user_id" // Mock implementation
    }

    private fun getCurrentUserName(): String {
        return "Dr. Smith" // Mock implementation
    }
}