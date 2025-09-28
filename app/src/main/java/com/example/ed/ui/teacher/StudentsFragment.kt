package com.example.ed.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.adapters.StudentsAdapter
import com.example.ed.models.StudentEnrollment
import com.example.ed.models.StudentInfo
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StudentsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    // UI Components
    private lateinit var tvTotalStudents: TextView
    private lateinit var etSearch: TextInputEditText
    private lateinit var layoutCourseFilters: LinearLayout
    private lateinit var rvStudents: RecyclerView
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: MaterialButton
    
    private lateinit var studentsAdapter: StudentsAdapter
    private val students = mutableListOf<StudentInfo>()
    private val teacherCourses = mutableListOf<String>()
    private var selectedCourseFilter = "All Courses"
    
    companion object {
        private const val TAG = "StudentsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_teacher_students, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Initialize UI components
        initializeViews(view)
        
        // Setup UI
        setupRecyclerView()
        setupSearch()
        setupClickListeners()
        
        // Load data
        loadStudentsData()
    }

    private fun initializeViews(view: View) {
        tvTotalStudents = view.findViewById(R.id.tv_total_students)
        etSearch = view.findViewById(R.id.et_search)
        layoutCourseFilters = view.findViewById(R.id.layout_course_filters)
        rvStudents = view.findViewById(R.id.rv_students)
        layoutLoading = view.findViewById(R.id.layout_loading)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        layoutErrorState = view.findViewById(R.id.layout_error_state)
        tvErrorMessage = view.findViewById(R.id.tv_error_message)
        btnRetry = view.findViewById(R.id.btn_retry)
    }

    private fun setupRecyclerView() {
        studentsAdapter = StudentsAdapter(
            context = requireContext(),
            students = students,
            onStudentClick = { student -> showStudentDetails(student) },
            onViewProgressClick = { student -> showStudentProgress(student) },
            onSendMessageClick = { student -> sendMessageToStudent(student) }
        )
        
        rvStudents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studentsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                studentsAdapter.filterStudents(query, selectedCourseFilter)
            }
        })
    }

    private fun setupClickListeners() {
        btnRetry.setOnClickListener {
            loadStudentsData()
        }
    }

    private fun loadStudentsData() {
        showLoadingState()
        
        lifecycleScope.launch {
            try {
                val teacherId = auth.currentUser?.uid
                if (teacherId == null) {
                    showErrorState("Authentication error")
                    return@launch
                }

                // Load teacher's courses first
                loadTeacherCourses(teacherId)
                
                // Load students enrolled in teacher's courses
                loadEnrolledStudents(teacherId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading students data", e)
                showErrorState("Failed to load students: ${e.message}")
            }
        }
    }

    private suspend fun loadTeacherCourses(teacherId: String) {
        try {
            val coursesSnapshot = firestore.collection("courses")
                .whereEqualTo("instructorId", teacherId)
                .get()
                .await()
            
            teacherCourses.clear()
            teacherCourses.add("All Courses")
            
            for (document in coursesSnapshot.documents) {
                val courseName = document.getString("title") ?: "Unknown Course"
                teacherCourses.add(courseName)
            }
            
            setupCourseFilters()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading teacher courses", e)
        }
    }

    private suspend fun loadEnrolledStudents(teacherId: String) {
        try {
            // Get all enrollments for teacher's courses
            val enrollmentsSnapshot = firestore.collection("enrollments")
                .whereEqualTo("teacherId", teacherId)
                .orderBy("enrollmentDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val studentMap = mutableMapOf<String, MutableList<StudentEnrollment>>()
            
            // Group enrollments by student ID
            for (document in enrollmentsSnapshot.documents) {
                val studentId = document.getString("studentId") ?: continue
                val courseId = document.getString("courseId") ?: continue
                val courseName = document.getString("courseName") ?: "Unknown Course"
                val enrollmentDate = document.getLong("enrollmentDate") ?: 0L
                val progress = document.getDouble("progress") ?: 0.0
                val isCompleted = document.getBoolean("isCompleted") ?: false
                val lastAccessDate = document.getLong("lastAccessedDate") ?: 0L
                
                val enrollmentInfo = StudentEnrollment(
                    courseId = courseId,
                    courseName = courseName,
                    enrollmentDate = enrollmentDate,
                    progressPercentage = progress,
                    isCompleted = isCompleted,
                    lastAccessedDate = lastAccessDate
                )
                
                studentMap.getOrPut(studentId) { mutableListOf() }.add(enrollmentInfo)
            }
            
            // Load student details for each enrolled student
            val studentsList = mutableListOf<StudentInfo>()
            
            for ((studentId, enrollments) in studentMap) {
                try {
                    val userDoc = firestore.collection("users")
                        .document(studentId)
                        .get()
                        .await()
                    
                    if (userDoc.exists() && userDoc.getString("role") == "student") {
                        val fullName = userDoc.getString("fullName") ?: "Unknown Student"
                        val email = userDoc.getString("email") ?: ""
                        val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                        val lastActiveTimestamp = userDoc.getLong("lastActiveTimestamp") ?: 0L
                        val isActive = userDoc.getBoolean("isActive") ?: true
                        val joinedTimestamp = userDoc.getLong("createdAt") ?: 0L
                        
                        // Calculate average progress
                        val averageProgress = if (enrollments.isNotEmpty()) {
                            enrollments.map { it.progressPercentage }.average()
                        } else 0.0
                        
                        val studentInfo = StudentInfo(
                            studentId = studentId,
                            fullName = fullName,
                            email = email,
                            profileImageUrl = profileImageUrl,
                            enrolledCourses = enrollments,
                            totalEnrolledCourses = enrollments.size,
                            averageProgress = averageProgress,
                            lastActiveTimestamp = lastActiveTimestamp,
                            isActive = isActive,
                            joinedTimestamp = joinedTimestamp
                        )
                        
                        studentsList.add(studentInfo)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading student details for $studentId", e)
                }
            }
            
            // Update UI
            students.clear()
            students.addAll(studentsList)
            studentsAdapter.updateStudents(students)
            updateTotalStudentsCount()
            
            if (students.isEmpty()) {
                showEmptyState()
            } else {
                showContentState()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading enrolled students", e)
            showErrorState("Failed to load students: ${e.message}")
        }
    }

    private fun setupCourseFilters() {
        layoutCourseFilters.removeAllViews()
        
        teacherCourses.forEach { courseName ->
            val chip = Chip(requireContext()).apply {
                text = courseName
                isCheckable = true
                isChecked = courseName == selectedCourseFilter
                
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        // Uncheck other chips
                        for (i in 0 until layoutCourseFilters.childCount) {
                            val otherChip = layoutCourseFilters.getChildAt(i) as? Chip
                            if (otherChip != this) {
                                otherChip?.isChecked = false
                            }
                        }
                        selectedCourseFilter = courseName
                        studentsAdapter.filterStudents(etSearch.text.toString(), selectedCourseFilter)
                    }
                }
            }
            
            layoutCourseFilters.addView(chip)
        }
    }

    private fun updateTotalStudentsCount() {
        val count = students.size
        tvTotalStudents.text = "$count Student${if (count != 1) "s" else ""}"
    }

    private fun showLoadingState() {
        layoutLoading.visibility = View.VISIBLE
        rvStudents.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE
        layoutErrorState.visibility = View.GONE
    }

    private fun showContentState() {
        layoutLoading.visibility = View.GONE
        rvStudents.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        layoutErrorState.visibility = View.GONE
    }

    private fun showEmptyState() {
        layoutLoading.visibility = View.GONE
        rvStudents.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        layoutErrorState.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        layoutLoading.visibility = View.GONE
        rvStudents.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE
        layoutErrorState.visibility = View.VISIBLE
        tvErrorMessage.text = message
    }

    private fun showStudentDetails(student: StudentInfo) {
        // TODO: Implement student details activity/dialog
        Toast.makeText(requireContext(), "Student details for ${student.fullName}", Toast.LENGTH_SHORT).show()
    }

    private fun showStudentProgress(student: StudentInfo) {
        // TODO: Implement student progress activity/dialog
        Toast.makeText(requireContext(), "Progress for ${student.fullName}", Toast.LENGTH_SHORT).show()
    }

    private fun sendMessageToStudent(student: StudentInfo) {
        // TODO: Implement messaging functionality
        Toast.makeText(requireContext(), "Message to ${student.fullName}", Toast.LENGTH_SHORT).show()
    }
}


