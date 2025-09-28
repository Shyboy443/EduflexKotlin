/*
 * DEPRECATED FILE - DO NOT USE
 * This file is deprecated and should be deleted.
 * Use the correct implementation in com.example.ed.ScheduleLiveLectureActivity instead.
 */

package com.example.edtech_deprecated

/*
// Commented out to prevent compilation errors
// The correct implementation is in com.example.ed.ScheduleLiveLectureActivity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ed.databinding.ActivityScheduleLiveLectureBinding
import com.example.ed.models.Course
import com.example.ed.models.LiveLecture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleLiveLectureActivityDeprecated : AppCompatActivity() {
    // This entire class is commented out to prevent compilation errors
    // Use com.example.ed.ScheduleLiveLectureActivity instead
}

/*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleLiveLectureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if editing existing lecture
        isEditMode = intent.getBooleanExtra("edit_mode", false)
        lectureId = intent.getStringExtra("lecture_id")
        
        setupToolbar()
        setupDateTimePickers()
        setupDurationSpinner()
        setupClickListeners()
        loadTeacherCourses()
        
        if (isEditMode && lectureId != null) {
            loadLectureForEdit()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        if (isEditMode) {
            binding.toolbar.title = "Edit Live Lecture"
            binding.btnSchedule.text = "Update Lecture"
        }
    }
    
    private fun setupDateTimePickers() {
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
        
        binding.etTime.setOnClickListener {
            showTimePicker()
        }
        
        // Set default date and time (1 hour from now)
        selectedDate.add(Calendar.HOUR_OF_DAY, 1)
        updateDateTimeFields()
    }
    
    private fun setupDurationSpinner() {
        val durations = arrayOf("30 minutes", "45 minutes", "60 minutes", "90 minutes", "120 minutes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, durations)
        binding.etDuration.setAdapter(adapter)
        binding.etDuration.setText("60 minutes", false)
    }
    
    private fun setupClickListeners() {
        binding.btnSchedule.setOnClickListener {
            if (validateInputs()) {
                if (isEditMode) {
                    updateLecture()
                } else {
                    scheduleLecture()
                }
            }
        }
        
        binding.btnSaveDraft.setOnClickListener {
            saveDraft()
        }
        
        binding.switchAutoGenerateLink.setOnCheckedChangeListener { _, isChecked ->
            binding.tilMeetingLink.isEnabled = !isChecked
            if (isChecked) {
                binding.etMeetingLink.setText("")
            }
        }
    }
    
    private fun loadTeacherCourses() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("courses")
            .whereEqualTo("instructorId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                teacherCourses.clear()
                documents.forEach { doc ->
                    val course = doc.toObject(Course::class.java)
                    teacherCourses.add(course)
                }
                
                setupCourseDropdown()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load courses", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun setupCourseDropdown() {
        val courseNames = teacherCourses.map { "${it.title} (${it.category})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, courseNames)
        binding.etCourse.setAdapter(adapter)
        
        binding.etCourse.setOnItemClickListener { _, _, position, _ ->
            selectedCourse = teacherCourses[position]
        }
    }
    
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeFields()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        
        // Don't allow past dates
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
    
    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                updateDateTimeFields()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }
    
    private fun updateDateTimeFields() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        binding.etDate.setText(dateFormat.format(selectedDate.time))
        binding.etTime.setText(timeFormat.format(selectedDate.time))
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        if (selectedCourse == null) {
            binding.tilCourse.error = "Please select a course"
            isValid = false
        } else {
            binding.tilCourse.error = null
        }
        
        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.tilTitle.error = "Please enter lecture title"
            isValid = false
        } else {
            binding.tilTitle.error = null
        }
        
        if (binding.etDescription.text.toString().trim().isEmpty()) {
            binding.tilDescription.error = "Please enter description"
            isValid = false
        } else {
            binding.tilDescription.error = null
        }
        
        if (selectedDate.timeInMillis <= System.currentTimeMillis()) {
            binding.tilDate.error = "Please select a future date and time"
            binding.tilTime.error = "Please select a future date and time"
            isValid = false
        } else {
            binding.tilDate.error = null
            binding.tilTime.error = null
        }
        
        val maxParticipants = binding.etMaxParticipants.text.toString().toIntOrNull()
        if (maxParticipants == null || maxParticipants <= 0) {
            binding.tilMaxParticipants.error = "Please enter valid number of participants"
            isValid = false
        } else {
            binding.tilMaxParticipants.error = null
        }
        
        return isValid
    }
    
    private fun scheduleLecture() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSchedule.isEnabled = false
        
        val currentUser = auth.currentUser ?: return
        val course = selectedCourse ?: return
        
        val duration = extractDurationMinutes(binding.etDuration.text.toString())
        val meetingLink = if (binding.switchAutoGenerateLink.isChecked) {
            generateMeetingLink()
        } else {
            binding.etMeetingLink.text.toString().trim()
        }
        
        val lecture = LiveLecture(
            id = UUID.randomUUID().toString(),
            courseId = course.id,
            courseName = course.title,
            instructorId = currentUser.uid,
            instructorName = currentUser.displayName ?: "Unknown",
            title = binding.etTitle.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            scheduledTime = selectedDate.time,
            duration = duration,
            meetingLink = meetingLink,
            isActive = false,
            isRecorded = binding.switchRecord.isChecked,
            maxParticipants = binding.etMaxParticipants.text.toString().toInt(),
            currentParticipants = 0,
            tags = listOf(course.category),
            createdAt = Date(),
            updatedAt = Date()
        )
        
        db.collection("live_lectures")
            .document(lecture.id)
            .set(lecture)
            .addOnSuccessListener {
                if (binding.switchNotifyStudents.isChecked) {
                    sendNotificationToStudents(lecture)
                }
                
                Toast.makeText(this, "Lecture scheduled successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSchedule.isEnabled = true
                Toast.makeText(this, "Failed to schedule lecture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun updateLecture() {
        // Implementation for updating existing lecture
        Toast.makeText(this, "Update functionality coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun loadLectureForEdit() {
        // Implementation for loading existing lecture data
        Toast.makeText(this, "Edit functionality coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveDraft() {
        Toast.makeText(this, "Draft saved locally", Toast.LENGTH_SHORT).show()
    }
    
    private fun extractDurationMinutes(durationText: String): Int {
        return when {
            durationText.contains("30") -> 30
            durationText.contains("45") -> 45
            durationText.contains("60") -> 60
            durationText.contains("90") -> 90
            durationText.contains("120") -> 120
            else -> 60
        }
    }
    
    private fun generateMeetingLink(): String {
        // Generate a unique meeting link
        val meetingId = UUID.randomUUID().toString().substring(0, 8)
        return "https://meet.edtech.com/room/$meetingId"
    }
    
    private fun sendNotificationToStudents(lecture: LiveLecture) {
        // Get enrolled students for this course
        db.collection("enrollments")
            .whereEqualTo("courseId", lecture.courseId)
            .get()
            .addOnSuccessListener { enrollments ->
                val studentIds = enrollments.documents.mapNotNull { it.getString("studentId") }
                
                // Send notification to each student
                studentIds.forEach { studentId ->
                    sendPushNotification(studentId, lecture)
                }
            }
    }
    
    private fun sendPushNotification(studentId: String, lecture: LiveLecture) {
        // Implementation for sending push notifications
        // This would typically use Firebase Cloud Messaging
        val notification = mapOf(
            "studentId" to studentId,
            "lectureId" to lecture.id,
            "title" to "New Live Lecture Scheduled",
            "message" to "${lecture.instructorName} has scheduled a live lecture for ${lecture.courseName}",
            "type" to "live_lecture_scheduled",
            "timestamp" to Date(),
            "isRead" to false
        )
        
        db.collection("notifications")
            .add(notification)
    }
}
*/