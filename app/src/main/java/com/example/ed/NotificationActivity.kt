package com.example.ed

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.NotificationAdapter
import com.example.ed.models.*
import com.example.ed.services.NotificationService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var settingsCard: MaterialCardView
    private lateinit var assignmentNotificationsSwitch: SwitchMaterial
    private lateinit var gradeNotificationsSwitch: SwitchMaterial
    private lateinit var discussionNotificationsSwitch: SwitchMaterial
    private lateinit var emailNotificationsSwitch: SwitchMaterial
    private lateinit var pushNotificationsSwitch: SwitchMaterial
    private lateinit var quietHoursSwitch: SwitchMaterial
    private lateinit var quietStartSpinner: Spinner
    private lateinit var quietEndSpinner: Spinner
    private lateinit var markAllReadButton: MaterialButton
    private lateinit var clearAllButton: MaterialButton
    private lateinit var emptyStateLayout: LinearLayout
    
    private lateinit var notificationAdapter: NotificationAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var notifications = mutableListOf<AppNotification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupNotificationSettings()
        setupButtons()
        loadNotifications()
        loadNotificationSettings()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        settingsCard = findViewById(R.id.settingsCard)
        assignmentNotificationsSwitch = findViewById(R.id.assignmentNotificationsSwitch)
        gradeNotificationsSwitch = findViewById(R.id.gradeNotificationsSwitch)
        discussionNotificationsSwitch = findViewById(R.id.discussionNotificationsSwitch)
        emailNotificationsSwitch = findViewById(R.id.emailNotificationsSwitch)
        pushNotificationsSwitch = findViewById(R.id.pushNotificationsSwitch)
        quietHoursSwitch = findViewById(R.id.quietHoursSwitch)
        quietStartSpinner = findViewById(R.id.quietStartSpinner)
        quietEndSpinner = findViewById(R.id.quietEndSpinner)
        markAllReadButton = findViewById(R.id.markAllReadButton)
        clearAllButton = findViewById(R.id.clearAllButton)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onNotificationClick = { notification -> markAsRead(notification) },
            onNotificationAction = { notification, action -> handleNotificationAction(notification, action) }
        )
        
        notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = notificationAdapter
        }
    }

    private fun setupNotificationSettings() {
        // Setup quiet hours spinners
        val hours = (0..23).map { String.format("%02d:00", it) }.toTypedArray()
        
        val startAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hours)
        startAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        quietStartSpinner.adapter = startAdapter
        quietStartSpinner.setSelection(22) // 22:00 default
        
        val endAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hours)
        endAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        quietEndSpinner.adapter = endAdapter
        quietEndSpinner.setSelection(7) // 07:00 default
        
        // Setup switch listeners
        assignmentNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("assignments", isChecked)
        }
        
        gradeNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("grades", isChecked)
        }
        
        discussionNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("discussions", isChecked)
        }
        
        emailNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("email", isChecked)
        }
        
        pushNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("push", isChecked)
            if (isChecked) {
                // Request notification permission if needed
                requestNotificationPermission()
            }
        }
        
        quietHoursSwitch.setOnCheckedChangeListener { _, isChecked ->
            quietStartSpinner.isEnabled = isChecked
            quietEndSpinner.isEnabled = isChecked
            saveNotificationSetting("quietHours", isChecked)
        }
        
        quietStartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveQuietHoursSetting("start", position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        quietEndSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveQuietHoursSetting("end", position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        markAllReadButton.setOnClickListener {
            markAllNotificationsAsRead()
        }
        
        clearAllButton.setOnClickListener {
            clearAllNotifications()
        }
    }

    private fun loadNotifications() {
        val userId = getCurrentUserId() ?: return
        
        firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                notifications.clear()
                documents.forEach { document ->
                    val notification = document.toObject(AppNotification::class.java)
                    notifications.add(notification)
                }
                
                if (notifications.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    notificationAdapter.updateNotifications(notifications)
                }
            }
            .addOnFailureListener { exception ->
                // Load mock data for demo
                loadMockNotifications()
            }
    }

    private fun loadMockNotifications() {
        notifications.clear()
        
        val mockNotifications = listOf(
            AppNotification(
                id = "notif1",
                userId = "current_user",
                title = "Assignment Due Tomorrow",
                message = "Android Development Project is due tomorrow at 11:59 PM",
                type = NotificationType.ASSIGNMENT_DUE,
                data = mapOf("assignmentId" to "assignment1"),
                isRead = false,
                createdAt = Date(System.currentTimeMillis() - 3600000) // 1 hour ago
            ),
            AppNotification(
                id = "notif2",
                userId = "current_user",
                title = "New Grade Posted",
                message = "You received 85% on Quiz 2: RecyclerView Implementation",
                type = NotificationType.GRADE_POSTED,
                data = mapOf("assignmentId" to "quiz2", "grade" to "85"),
                isRead = false,
                createdAt = Date(System.currentTimeMillis() - 7200000) // 2 hours ago
            ),
            AppNotification(
                id = "notif3",
                userId = "current_user",
                title = "Discussion Reply",
                message = "Alice Teacher replied to your question in \"Android Basics\"",
                type = NotificationType.DISCUSSION_REPLY,
                data = mapOf("discussionId" to "discussion1"),
                isRead = true,
                createdAt = Date(System.currentTimeMillis() - 86400000) // 1 day ago
            ),
            AppNotification(
                id = "notif4",
                userId = "current_user",
                title = "Course Update",
                message = "New lesson added to Mobile App Development course",
                type = NotificationType.COURSE_UPDATE,
                data = mapOf("courseId" to "course1"),
                isRead = true,
                createdAt = Date(System.currentTimeMillis() - 172800000) // 2 days ago
            ),
            AppNotification(
                id = "notif5",
                userId = "current_user",
                title = "System Maintenance",
                message = "Scheduled maintenance will occur tonight from 2:00 AM to 4:00 AM",
                type = NotificationType.SYSTEM,
                data = emptyMap(),
                isRead = false,
                createdAt = Date(System.currentTimeMillis() - 259200000) // 3 days ago
            )
        )
        
        notifications.addAll(mockNotifications)
        notificationAdapter.updateNotifications(notifications)
        hideEmptyState()
    }

    private fun loadNotificationSettings() {
        val userId = getCurrentUserId() ?: return
        
        firestore.collection("userSettings")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val settings = document.data ?: return@addOnSuccessListener
                    
                    assignmentNotificationsSwitch.isChecked = settings["assignments"] as? Boolean ?: true
                    gradeNotificationsSwitch.isChecked = settings["grades"] as? Boolean ?: true
                    discussionNotificationsSwitch.isChecked = settings["discussions"] as? Boolean ?: true
                    emailNotificationsSwitch.isChecked = settings["email"] as? Boolean ?: false
                    pushNotificationsSwitch.isChecked = settings["push"] as? Boolean ?: true
                    quietHoursSwitch.isChecked = settings["quietHours"] as? Boolean ?: false
                    
                    val quietStart = settings["quietStart"] as? Long ?: 22
                    val quietEnd = settings["quietEnd"] as? Long ?: 7
                    
                    quietStartSpinner.setSelection(quietStart.toInt())
                    quietEndSpinner.setSelection(quietEnd.toInt())
                    
                    quietStartSpinner.isEnabled = quietHoursSwitch.isChecked
                    quietEndSpinner.isEnabled = quietHoursSwitch.isChecked
                } else {
                    // Set default values
                    setDefaultNotificationSettings()
                }
            }
            .addOnFailureListener {
                setDefaultNotificationSettings()
            }
    }

    private fun setDefaultNotificationSettings() {
        assignmentNotificationsSwitch.isChecked = true
        gradeNotificationsSwitch.isChecked = true
        discussionNotificationsSwitch.isChecked = true
        emailNotificationsSwitch.isChecked = false
        pushNotificationsSwitch.isChecked = true
        quietHoursSwitch.isChecked = false
        quietStartSpinner.isEnabled = false
        quietEndSpinner.isEnabled = false
    }

    private fun saveNotificationSetting(key: String, value: Boolean) {
        val userId = getCurrentUserId() ?: return
        
        firestore.collection("userSettings")
            .document(userId)
            .update(key, value)
            .addOnFailureListener {
                // Create document if it doesn't exist
                firestore.collection("userSettings")
                    .document(userId)
                    .set(mapOf(key to value))
            }
    }

    private fun saveQuietHoursSetting(type: String, hour: Int) {
        val userId = getCurrentUserId() ?: return
        val key = if (type == "start") "quietStart" else "quietEnd"
        
        firestore.collection("userSettings")
            .document(userId)
            .update(key, hour)
            .addOnFailureListener {
                firestore.collection("userSettings")
                    .document(userId)
                    .set(mapOf(key to hour))
            }
    }

    private fun markAsRead(notification: AppNotification) {
        if (!notification.isRead) {
            notification.isRead = true
            
            firestore.collection("notifications")
                .document(notification.id)
                .update("isRead", true)
            
            notificationAdapter.notifyDataSetChanged()
        }
        
        // Handle notification action based on type
        handleNotificationClick(notification)
    }

    private fun handleNotificationClick(notification: AppNotification) {
        when (notification.type) {
            NotificationType.ASSIGNMENT_DUE -> {
                val assignmentId = notification.data["assignmentId"]
                // Navigate to assignment details
                Toast.makeText(this, "Opening assignment...", Toast.LENGTH_SHORT).show()
            }
            NotificationType.GRADE_POSTED -> {
                val assignmentId = notification.data["assignmentId"]
                // Navigate to grades
                Toast.makeText(this, "Opening grades...", Toast.LENGTH_SHORT).show()
            }
            NotificationType.DISCUSSION_REPLY -> {
                val discussionId = notification.data["discussionId"]
                // Navigate to discussion
                Toast.makeText(this, "Opening discussion...", Toast.LENGTH_SHORT).show()
            }
            NotificationType.COURSE_UPDATE -> {
                val courseId = notification.data["courseId"]
                // Navigate to course
                Toast.makeText(this, "Opening course...", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Handle other notification types
            }
        }
    }

    private fun handleNotificationAction(notification: AppNotification, action: String) {
        when (action) {
            "delete" -> {
                deleteNotification(notification)
            }
            "snooze" -> {
                snoozeNotification(notification)
            }
        }
    }

    private fun deleteNotification(notification: AppNotification) {
        notifications.remove(notification)
        notificationAdapter.updateNotifications(notifications)
        
        firestore.collection("notifications")
            .document(notification.id)
            .delete()
        
        if (notifications.isEmpty()) {
            showEmptyState()
        }
        
        Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show()
    }

    private fun snoozeNotification(notification: AppNotification) {
        // Snooze for 1 hour
        val snoozeTime = Date(System.currentTimeMillis() + 3600000)
        
        firestore.collection("notifications")
            .document(notification.id)
            .update("snoozeUntil", snoozeTime)
        
        Toast.makeText(this, "Notification snoozed for 1 hour", Toast.LENGTH_SHORT).show()
    }

    private fun markAllNotificationsAsRead() {
        notifications.forEach { notification ->
            if (!notification.isRead) {
                notification.isRead = true
                firestore.collection("notifications")
                    .document(notification.id)
                    .update("isRead", true)
            }
        }
        
        notificationAdapter.notifyDataSetChanged()
        Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllNotifications() {
        notifications.clear()
        notificationAdapter.updateNotifications(notifications)
        showEmptyState()
        
        val userId = getCurrentUserId() ?: return
        firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { document ->
                    document.reference.delete()
                }
            }
        
        Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showEmptyState() {
        emptyStateLayout.visibility = View.VISIBLE
        notificationsRecyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateLayout.visibility = View.GONE
        notificationsRecyclerView.visibility = View.VISIBLE
    }

    private fun requestNotificationPermission() {
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun getCurrentUserId(): String? {
        return "current_user_id" // Mock implementation
    }
}