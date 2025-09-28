package com.example.ed.ui.student

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ed.CourseDetailsActivity
import com.example.ed.R
import com.example.ed.adapters.CourseAdapter
import com.example.ed.databinding.FragmentStudentCoursesBinding
import com.example.ed.models.Course
import com.example.ed.models.CourseSection
import com.example.ed.services.DatabaseService
import com.example.ed.services.PointsRewardsService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StudentCoursesFragment : Fragment() {

    private var _binding: FragmentStudentCoursesBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var databaseService: DatabaseService
    private lateinit var pointsRewardsService: PointsRewardsService

    private lateinit var enrolledCoursesAdapter: CourseAdapter
    private lateinit var availableCoursesAdapter: CourseAdapter
    private val enrolledCourses = mutableListOf<Course>()
    private val availableCourses = mutableListOf<Course>()

    private var enrolledCoursesJob: Job? = null
    private var availableCoursesJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        databaseService = DatabaseService.getInstance(requireContext())
        pointsRewardsService = PointsRewardsService.getInstance(requireContext())

        setupRecyclerViews()
        setupClickListeners()
        initializeTabLayout()
        loadEnrolledCourses()
        loadAvailableCourses()
    }

    private fun setupRecyclerViews() {
        enrolledCoursesAdapter = CourseAdapter(
            courses = enrolledCourses,
            enrolledCourses = emptyList(), // No need to pass enrolled courses for filtering in enrolled view
            onCourseClick = { course ->
                // Navigate to weekly content fragment
                val fragment = StudentWeeklyContentFragment.newInstance(course.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onEditClick = null,
            onMenuClick = { course, view ->
                showEnrolledCourseMenu(course, view)
            },
            showAsEnrolled = true, // Show as enrolled courses with View button
            isTeacherView = false // Student view
        )
        binding.rvEnrolledCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEnrolledCourses.adapter = enrolledCoursesAdapter

        availableCoursesAdapter = CourseAdapter(
            courses = availableCourses,
            enrolledCourses = enrolledCourses, // Pass enrolled courses to filter them out
            onCourseClick = { course ->
                showCourseEnrollmentDialog(course)
            },
            onEditClick = null,
            onMenuClick = { course, view ->
                showAvailableCourseMenu(course, view)
            },
            isTeacherView = false // Student view
        )
        binding.rvAvailableCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAvailableCourses.adapter = availableCoursesAdapter
    }

    private fun initializeTabLayout() {
        binding.layoutEnrolledCourses.visibility = View.VISIBLE
        binding.layoutAvailableCourses.visibility = View.GONE
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
    }

    private fun setupClickListeners() {
        binding.btnEnrollInCourse.setOnClickListener {
            binding.tabLayout.getTabAt(1)?.select()
        }

        binding.btnBrowseCourses.setOnClickListener {
            binding.tabLayout.getTabAt(1)?.select()
        }

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.layoutEnrolledCourses.visibility = View.VISIBLE
                        binding.layoutAvailableCourses.visibility = View.GONE
                    }
                    1 -> {
                        binding.layoutEnrolledCourses.visibility = View.GONE
                        binding.layoutAvailableCourses.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun loadEnrolledCourses() {
        enrolledCoursesJob?.cancel()

        binding.progressBarEnrolled.visibility = View.VISIBLE
        binding.layoutEmptyEnrolled.visibility = View.GONE

        lifecycleScope.launch {
            kotlinx.coroutines.delay(5000) // Timeout after 5 seconds
            if (isAdded && _binding != null && binding.progressBarEnrolled.visibility == View.VISIBLE) {
                binding.progressBarEnrolled.visibility = View.GONE
                binding.layoutEmptyEnrolled.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Loading timeout. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        enrolledCoursesJob = lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch

                firestore.collection("enrollments")
                    .whereEqualTo("studentId", currentUser.uid)
                    .whereEqualTo("isActive", true)
                    .get()
                    .addOnSuccessListener { enrollmentDocs ->
                        if (enrollmentDocs.isEmpty) {
                            binding.progressBarEnrolled.visibility = View.GONE
                            binding.layoutEmptyEnrolled.visibility = View.VISIBLE
                            enrolledCourses.clear()
                            enrolledCoursesAdapter.notifyDataSetChanged()
                            return@addOnSuccessListener
                        }

                        val courseIds = enrollmentDocs.documents.mapNotNull { it.getString("courseId") }
                        
                        if (courseIds.isEmpty()) {
                            binding.progressBarEnrolled.visibility = View.GONE
                            binding.layoutEmptyEnrolled.visibility = View.VISIBLE
                            enrolledCourses.clear()
                            enrolledCoursesAdapter.notifyDataSetChanged()
                            return@addOnSuccessListener
                        }

                        // Fetch course details using document references
                        val courseTasks = courseIds.map { courseId ->
                            firestore.collection("courses").document(courseId).get()
                        }

                        com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.DocumentSnapshot>(courseTasks)
                            .addOnSuccessListener { courseDocs ->
                                val courses = courseDocs.mapNotNull { doc ->
                                    try {
                                        if (doc.exists() && (doc.getBoolean("isPublished") == true)) {
                                            Course(
                                                id = doc.id,
                                                title = doc.getString("title") ?: "",
                                                instructor = doc.getString("instructor") ?: "Unknown Instructor",
                                                description = doc.getString("description") ?: "",
                                                progress = doc.getLong("progress")?.toInt() ?: 0,
                                                totalLessons = doc.getLong("totalLessons")?.toInt() ?: 0,
                                                completedLessons = doc.getLong("completedLessons")?.toInt() ?: 0,
                                                duration = doc.getString("duration") ?: "",
                                                difficulty = doc.getString("difficulty") ?: "",
                                                category = doc.getString("category") ?: "",
                                                thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                                                isBookmarked = doc.getBoolean("isBookmarked") ?: false,
                                                rating = doc.getDouble("rating")?.toFloat() ?: 0.0f,
                                                enrolledStudents = doc.getLong("enrolledStudents")?.toInt() ?: 0,
                                                createdAt = doc.getLong("createdAt") ?: 0,
                                                updatedAt = doc.getLong("updatedAt") ?: 0,
                                                courseContent = doc.get("courseContent") as? List<CourseSection> ?: emptyList(),
                                                isPublished = doc.getBoolean("isPublished") ?: false,
                                                teacherId = doc.getString("teacherId") ?: "",
                                                price = doc.getDouble("price") ?: 0.0,
                                                originalPrice = doc.getDouble("originalPrice") ?: 0.0,
                                                isFree = doc.getBoolean("isFree") ?: false,
                                                deadline = doc.getLong("deadline"),
                                                hasDeadline = doc.getBoolean("hasDeadline") ?: false
                                            )
                                        } else {
                                            null
                                        }
                                    } catch (e: Exception) {
                                        Log.e("StudentCourses", "Error parsing enrolled course", e)
                                        null
                                    }
                                }
                                enrolledCourses.clear()
                                enrolledCourses.addAll(courses)
                                enrolledCoursesAdapter.notifyDataSetChanged()

                                binding.progressBarEnrolled.visibility = View.GONE
                                if (courses.isEmpty()) {
                                    binding.layoutEmptyEnrolled.visibility = View.VISIBLE
                                } else {
                                    binding.layoutEmptyEnrolled.visibility = View.GONE
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("StudentCourses", "Error loading courses", e)
                                binding.progressBarEnrolled.visibility = View.GONE
                                binding.layoutEmptyEnrolled.visibility = View.VISIBLE
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("StudentCourses", "Error loading enrollments", e)
                        binding.progressBarEnrolled.visibility = View.GONE
                        binding.layoutEmptyEnrolled.visibility = View.VISIBLE
                    }
            } catch (e: Exception) {
                Log.e("StudentCourses", "Error loading enrolled courses", e)
                binding.progressBarEnrolled.visibility = View.GONE
                binding.layoutEmptyEnrolled.visibility = View.VISIBLE
            }
        }
    }

    private fun loadAvailableCourses() {
        availableCoursesJob?.cancel()

        binding.progressBarAvailable.visibility = View.VISIBLE
        binding.layoutEmptyAvailable.visibility = View.GONE

        lifecycleScope.launch {
            kotlinx.coroutines.delay(5000) // Timeout after 5 seconds
            if (isAdded && _binding != null && binding.progressBarAvailable.visibility == View.VISIBLE) {
                binding.progressBarAvailable.visibility = View.GONE
                binding.layoutEmptyAvailable.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Loading timeout. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        availableCoursesJob = lifecycleScope.launch {
            try {
                val courses = firestore.collection("courses")
                    .whereEqualTo("isPublished", true)
                    .get()
                    .await()

                Log.d("StudentCourses", "Found ${courses.documents.size} published courses")

                val courseList = courses.documents.mapNotNull { document ->
                    try {
                        Course(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            instructor = document.getString("instructor") ?: "",
                            description = document.getString("description") ?: "",
                            progress = document.getLong("progress")?.toInt() ?: 0,
                            totalLessons = document.getLong("totalLessons")?.toInt() ?: 0,
                            completedLessons = document.getLong("completedLessons")?.toInt() ?: 0,
                            duration = document.getString("duration") ?: document.getString("estimatedDuration") ?: "",
                            difficulty = document.getString("difficulty") ?: "",
                            category = document.getString("category") ?: "",
                            thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                            isBookmarked = document.getBoolean("isBookmarked") ?: false,
                            rating = document.getDouble("rating")?.toFloat() ?: 0.0f,
                            enrolledStudents = document.getLong("enrolledStudents")?.toInt() ?: 0,
                            createdAt = document.getLong("createdAt") ?: 0,
                            updatedAt = document.getLong("updatedAt") ?: 0,
                            courseContent = document.get("courseContent") as? List<CourseSection> ?: emptyList(),
                            isPublished = document.getBoolean("isPublished") ?: false,
                            teacherId = document.getString("teacherId") ?: "",
                            price = document.getDouble("price") ?: 0.0,
                            originalPrice = document.getDouble("originalPrice") ?: 0.0,
                            isFree = document.getBoolean("isFree") ?: false,
                            deadline = document.getLong("deadline"),
                            hasDeadline = document.getBoolean("hasDeadline") ?: false
                        )
                    } catch (e: Exception) {
                        Log.e("StudentCourses", "Error parsing available course: ${document.id}", e)
                        null
                    }
                }

                Log.d("StudentCourses", "Successfully parsed ${courseList.size} courses")

                availableCourses.clear()
                availableCourses.addAll(courseList)
                availableCoursesAdapter.notifyDataSetChanged()

                binding.progressBarAvailable.visibility = View.GONE
                if (courseList.isEmpty()) {
                    binding.layoutEmptyAvailable.visibility = View.VISIBLE
                } else {
                    binding.layoutEmptyAvailable.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e("StudentCourses", "Error loading available courses", e)
                binding.progressBarAvailable.visibility = View.GONE
                binding.layoutEmptyAvailable.visibility = View.VISIBLE
            }
        }
    }

    private fun showEnrolledCourseMenu(course: Course, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.enrolled_course_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_continue -> {
                    // Navigate to weekly content fragment
                    val fragment = StudentWeeklyContentFragment.newInstance(course.id)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.action_unenroll -> {
                    showUnenrollConfirmation(course)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showAvailableCourseMenu(course: Course, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.available_course_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_enroll -> {
                    showCourseEnrollmentDialog(course)
                    true
                }
                R.id.action_preview -> {
                    val intent = Intent(requireContext(), CourseDetailsActivity::class.java)
                    intent.putExtra("COURSE_ID", course.id)
                    intent.putExtra("PREVIEW_MODE", true)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showCourseEnrollmentDialog(course: Course) {
        if (course.isFree) {
            // Free course - simple enrollment
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Enroll in Free Course")
                .setMessage("Do you want to enroll in '${course.title}'?\n\nThis course is completely free!")
                .setPositiveButton("Enroll") { _, _ -> enrollInCourse(course) }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Paid course - show payment options
            showPaymentDialog(course)
        }
    }
    
    private fun showPaymentDialog(course: Course) {
        lifecycleScope.launch {
            try {
                val userPoints = pointsRewardsService.getCurrentPoints()
                val discountPercentage = minOf(50, (userPoints / 100).toInt()) // 100 points = 1%, max 50%
                val pointsUsed = discountPercentage * 100 // Points actually used for discount
                val originalPrice = course.price
                val discountAmount = originalPrice * (discountPercentage / 100.0)
                val finalPrice = originalPrice - discountAmount
                
                val paymentMessage = buildString {
                    appendLine("💰 Course: ${course.title}")
                    appendLine("📚 Instructor: ${course.instructor}")
                    appendLine("⭐ Rating: ${course.rating}/5")
                    appendLine()
                    appendLine("💵 Original Price: $${String.format("%.2f", originalPrice)}")
                    if (discountPercentage > 0) {
                        appendLine("🎯 Your Points: ${userPoints}")
                        appendLine("🎁 Discount: ${discountPercentage}% (-$${String.format("%.2f", discountAmount)})")
                        appendLine("💳 Final Price: $${String.format("%.2f", finalPrice)}")
                    }
                    appendLine()
                    appendLine("Choose your payment option:")
                }
                
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("💳 Purchase Course")
                    .setMessage(paymentMessage)
                    .setPositiveButton("💰 Pay Now ($${String.format("%.2f", finalPrice)})") { _, _ -> 
                        processDummyPayment(course, finalPrice, pointsUsed, discountPercentage)
                    }
                    .setNeutralButton("🎮 Earn More Points") { _, _ ->
                        Toast.makeText(requireContext(), "Play games to earn more points for bigger discounts!", Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                    
            } catch (e: Exception) {
                // Fallback if points service fails
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("💳 Purchase Course")
                    .setMessage("Course: ${course.title}\nPrice: $${String.format("%.2f", course.price)}")
                    .setPositiveButton("💰 Pay Now") { _, _ -> 
                        processDummyPayment(course, course.price, 0, 0)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    private fun processDummyPayment(course: Course, finalPrice: Double, pointsUsed: Int, discountPercentage: Int) {
        // Show dummy payment processing dialog
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("💳 Processing Payment...")
            .setMessage("Please wait while we process your payment...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()
        
        // Simulate payment processing
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // 2 second delay to simulate payment
            
            progressDialog.dismiss()
            
            // Randomly simulate payment success/failure (90% success rate for demo)
            val paymentSuccessful = (1..10).random() <= 9
            
            if (paymentSuccessful) {
                showPaymentSuccessDialog(course, finalPrice, discountPercentage) {
                    // Use points for discount if applicable
                    if (discountPercentage > 0) {
                        lifecycleScope.launch {
                            pointsRewardsService.usePointsForDiscount(pointsUsed)
                        }
                    }
                    // Enroll student after successful payment
                    enrollInCourse(course, finalPrice, discountPercentage)
                }
            } else {
                showPaymentFailureDialog(course)
            }
        }
    }
    
    private fun showPaymentSuccessDialog(course: Course, amountPaid: Double, discountUsed: Int, onConfirm: () -> Unit) {
        val successMessage = buildString {
            appendLine("🎉 Payment Successful!")
            appendLine()
            appendLine("Course: ${course.title}")
            appendLine("Amount Paid: $${String.format("%.2f", amountPaid)}")
            if (discountUsed > 0) {
                appendLine("Discount Applied: ${discountUsed}%")
            }
            appendLine()
            appendLine("You are now enrolled in this course!")
            appendLine("Start learning immediately!")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("✅ Payment Complete")
            .setMessage(successMessage)
            .setPositiveButton("🚀 Start Learning") { _, _ -> onConfirm() }
            .setCancelable(false)
            .show()
    }
    
    private fun showPaymentFailureDialog(course: Course) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("❌ Payment Failed")
            .setMessage("We couldn't process your payment for '${course.title}'.\n\nPlease check your payment method and try again.")
            .setPositiveButton("🔄 Try Again") { _, _ -> 
                showPaymentDialog(course) // Retry payment
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Free course enrollment
    private fun enrollInCourse(course: Course) {
        enrollInCourse(course, 0.0, 0)
    }
    
    // Paid course enrollment with payment details
    private fun enrollInCourse(course: Course, amountPaid: Double, discountUsed: Int) {
        val currentUser = auth.currentUser ?: return
        
        val enrollmentData = hashMapOf(
            "studentId" to currentUser.uid,
            "courseId" to course.id,
            "enrolledAt" to System.currentTimeMillis(),
            "isActive" to true,
            "paymentInfo" to hashMapOf(
                "originalPrice" to course.price,
                "amountPaid" to amountPaid,
                "discountPercentage" to discountUsed,
                "discountAmount" to (course.price - amountPaid),
                "paymentDate" to System.currentTimeMillis(),
                "paymentMethod" to "dummy_payment",
                "transactionId" to "TXN_${System.currentTimeMillis()}_${(1000..9999).random()}"
            )
        )
        
        firestore.collection("enrollments")
            .add(enrollmentData)
            .addOnSuccessListener { documentReference ->
                // Also record payment transaction for tracking
                recordPaymentTransaction(course, amountPaid, discountUsed, documentReference.id)
                
                Toast.makeText(requireContext(), 
                    if (amountPaid > 0) "Successfully purchased and enrolled in ${course.title}!" 
                    else "Successfully enrolled in ${course.title}!", 
                    Toast.LENGTH_SHORT).show()
                    
                loadEnrolledCourses() // Refresh enrolled courses
                loadAvailableCourses() // Refresh available courses to remove this one
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to complete enrollment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun recordPaymentTransaction(course: Course, amountPaid: Double, discountUsed: Int, enrollmentId: String) {
        val currentUser = auth.currentUser ?: return
        
        val transactionData = hashMapOf(
            "userId" to currentUser.uid,
            "courseId" to course.id,
            "courseTitle" to course.title,
            "enrollmentId" to enrollmentId,
            "originalPrice" to course.price,
            "amountPaid" to amountPaid,
            "discountPercentage" to discountUsed,
            "discountAmount" to (course.price - amountPaid),
            "transactionDate" to System.currentTimeMillis(),
            "paymentMethod" to "dummy_payment",
            "transactionId" to "TXN_${System.currentTimeMillis()}_${(1000..9999).random()}",
            "status" to "completed"
        )
        
        firestore.collection("payment_transactions")
            .add(transactionData)
            .addOnSuccessListener {
                Log.d("StudentCourses", "Payment transaction recorded successfully")
            }
            .addOnFailureListener { e ->
                Log.e("StudentCourses", "Failed to record payment transaction", e)
            }
    }

    private fun showUnenrollConfirmation(course: Course) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Unenroll from Course")
            .setMessage("Are you sure you want to unenroll from '${course.title}'? You will lose your progress.")
            .setPositiveButton("Unenroll") { _, _ ->
                unenrollFromCourse(course)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun unenrollFromCourse(course: Course) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("enrollments")
            .whereEqualTo("studentId", currentUser.uid)
            .whereEqualTo("courseId", course.id)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
                Toast.makeText(requireContext(), "Unenrolled from ${course.title}", Toast.LENGTH_SHORT).show()
                loadEnrolledCourses() // Refresh enrolled courses
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to unenroll: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        enrolledCoursesJob?.cancel()
        availableCoursesJob?.cancel()
        _binding = null
    }
}
