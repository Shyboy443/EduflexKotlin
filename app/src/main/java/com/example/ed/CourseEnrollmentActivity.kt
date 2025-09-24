package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.ed.models.Course
import com.example.ed.models.PaymentRecord
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class CourseEnrollmentActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var courseImageView: android.widget.ImageView
    private lateinit var courseTitleTextView: android.widget.TextView
    private lateinit var instructorTextView: android.widget.TextView
    private lateinit var descriptionTextView: android.widget.TextView
    private lateinit var priceTextView: android.widget.TextView
    private lateinit var originalPriceTextView: android.widget.TextView
    private lateinit var discountBadge: android.widget.TextView
    private lateinit var durationTextView: android.widget.TextView
    private lateinit var studentsCountTextView: android.widget.TextView
    private lateinit var ratingTextView: android.widget.TextView
    private lateinit var enrollButton: MaterialButton
    private lateinit var paymentMethodCard: MaterialCardView
    private lateinit var selectedPaymentMethodText: android.widget.TextView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentCourse: Course? = null
    private var selectedPaymentMethod = "Credit Card"
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_enrollment)
        
        initializeComponents()
        setupClickListeners()
        loadCourseData()
    }

    private fun initializeComponents() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        toolbar = findViewById(R.id.toolbar)
        courseImageView = findViewById(R.id.iv_course_image)
        courseTitleTextView = findViewById(R.id.tv_course_title)
        instructorTextView = findViewById(R.id.tv_instructor)
        descriptionTextView = findViewById(R.id.tv_description)
        priceTextView = findViewById(R.id.tv_price)
        originalPriceTextView = findViewById(R.id.tv_original_price)
        discountBadge = findViewById(R.id.tv_discount_badge)
        durationTextView = findViewById(R.id.tv_duration)
        studentsCountTextView = findViewById(R.id.tv_students_count)
        ratingTextView = findViewById(R.id.tv_rating)
        enrollButton = findViewById(R.id.btn_enroll)
        paymentMethodCard = findViewById(R.id.card_payment_method)
        selectedPaymentMethodText = findViewById(R.id.tv_selected_payment_method)
        
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Course Enrollment"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        selectedPaymentMethodText.text = selectedPaymentMethod
    }

    private fun setupClickListeners() {
        enrollButton.setOnClickListener {
            if (currentCourse != null) {
                processEnrollment()
            }
        }
        
        paymentMethodCard.setOnClickListener {
            showPaymentMethodDialog()
        }
    }

    private fun loadCourseData() {
        val courseId = intent.getStringExtra("COURSE_ID")
        if (courseId == null) {
            Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load course from Firestore
        firestore.collection("courses").document(courseId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentCourse = document.toObject(Course::class.java)
                    updateUI()
                } else {
                    // Load sample course for demo
                    loadSampleCourse(courseId)
                }
            }
            .addOnFailureListener {
                // Load sample course for demo
                loadSampleCourse(courseId)
            }
    }

    private fun loadSampleCourse(courseId: String) {
        currentCourse = Course(
            id = courseId,
            title = "Android Development Masterclass",
            instructor = "John Smith",
            description = "Learn Android development from scratch with Kotlin and modern Android practices. This comprehensive course covers everything from basic concepts to advanced topics including MVVM architecture, Room database, Retrofit networking, and more.",
            category = "Programming",
            difficulty = "Beginner",
            duration = "40 hours",
            rating = 4.8f,
            enrolledStudents = 1250,
            thumbnailUrl = "",
            isPublished = true,
            price = 49.99,
            originalPrice = 79.99
        )
        updateUI()
    }

    private fun updateUI() {
        currentCourse?.let { course ->
            courseTitleTextView.text = course.title
            instructorTextView.text = "By ${course.instructor}"
            descriptionTextView.text = course.description
            durationTextView.text = course.duration
            studentsCountTextView.text = "${course.enrolledStudents} students enrolled"
            ratingTextView.text = "${course.rating} ⭐"
            
            // Set price
            priceTextView.text = currencyFormat.format(course.price)
            
            // Show discount if applicable
            if (course.originalPrice > course.price) {
                originalPriceTextView.visibility = View.VISIBLE
                discountBadge.visibility = View.VISIBLE
                originalPriceTextView.text = currencyFormat.format(course.originalPrice)
                val discount = ((course.originalPrice - course.price) / course.originalPrice * 100).toInt()
                discountBadge.text = "${discount}% OFF"
            } else {
                originalPriceTextView.visibility = View.GONE
                discountBadge.visibility = View.GONE
            }
            
            // Load course image
            if (course.thumbnailUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(course.thumbnailUrl)
                    .placeholder(R.drawable.ic_book)
                    .error(R.drawable.ic_book)
                    .into(courseImageView)
            } else {
                courseImageView.setImageResource(R.drawable.ic_book)
            }
            
            // Update enroll button
            enrollButton.text = "Enroll for ${currencyFormat.format(course.price)}"
        }
    }

    private fun showPaymentMethodDialog() {
        val paymentMethods = arrayOf("Credit Card", "PayPal", "Google Pay", "Apple Pay")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Payment Method")
            .setSingleChoiceItems(paymentMethods, paymentMethods.indexOf(selectedPaymentMethod)) { dialog, which ->
                selectedPaymentMethod = paymentMethods[which]
                selectedPaymentMethodText.text = selectedPaymentMethod
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processEnrollment() {
        val course = currentCourse ?: return
        val user = auth.currentUser
        
        if (user == null) {
            Toast.makeText(this, "Please log in to enroll", Toast.LENGTH_SHORT).show()
            return
        }

        // Show payment processing dialog
        showPaymentDialog(course)
    }

    private fun showPaymentDialog(course: Course) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_processing, null)
        val etCardNumber = dialogView.findViewById<TextInputEditText>(R.id.et_card_number)
        val etExpiryDate = dialogView.findViewById<TextInputEditText>(R.id.et_expiry_date)
        val etCvv = dialogView.findViewById<TextInputEditText>(R.id.et_cvv)
        val etCardholderName = dialogView.findViewById<TextInputEditText>(R.id.et_cardholder_name)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Payment Details")
            .setView(dialogView as View)
            .setPositiveButton("Pay ${currencyFormat.format(course.price)}") { dialog: android.content.DialogInterface, which: Int ->
                // Validate payment details
                if (validatePaymentDetails(etCardNumber, etExpiryDate, etCvv, etCardholderName)) {
                    processPayment(course)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validatePaymentDetails(
        cardNumber: TextInputEditText,
        expiryDate: TextInputEditText,
        cvv: TextInputEditText,
        cardholderName: TextInputEditText
    ): Boolean {
        val cardNumberText = cardNumber.text.toString().trim()
        val expiryText = expiryDate.text.toString().trim()
        val cvvText = cvv.text.toString().trim()
        val nameText = cardholderName.text.toString().trim()
        
        when {
            cardNumberText.length < 16 -> {
                Toast.makeText(this, "Please enter a valid card number", Toast.LENGTH_SHORT).show()
                return false
            }
            expiryText.length < 5 -> {
                Toast.makeText(this, "Please enter a valid expiry date", Toast.LENGTH_SHORT).show()
                return false
            }
            cvvText.length < 3 -> {
                Toast.makeText(this, "Please enter a valid CVV", Toast.LENGTH_SHORT).show()
                return false
            }
            nameText.isEmpty() -> {
                Toast.makeText(this, "Please enter cardholder name", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        
        return true
    }

    private fun processPayment(course: Course) {
        // Show loading state
        enrollButton.isEnabled = false
        enrollButton.text = "Processing..."
        
        // Simulate payment processing
        enrollButton.postDelayed({
            // Create payment record
            val paymentRecord = PaymentRecord(
                id = UUID.randomUUID().toString(),
                userId = auth.currentUser?.uid ?: "",
                courseId = course.id,
                courseName = course.title,
                amount = course.price,
                currency = "USD",
                paymentMethod = selectedPaymentMethod,
                status = "Completed",
                timestamp = System.currentTimeMillis(),
                transactionId = "txn_${System.currentTimeMillis()}"
            )
            
            // Save payment record to Firestore
            firestore.collection("payments")
                .document(paymentRecord.id)
                .set(paymentRecord)
                .addOnSuccessListener {
                    // Enroll user in course
                    enrollUserInCourse(course, paymentRecord)
                }
                .addOnFailureListener {
                    // For demo, still proceed with enrollment
                    enrollUserInCourse(course, paymentRecord)
                }
        }, 2000)
    }

    private fun enrollUserInCourse(course: Course, paymentRecord: PaymentRecord) {
        val userId = auth.currentUser?.uid ?: return
        
        // Add course to user's enrolled courses
        val enrollmentData = mapOf(
            "courseId" to course.id,
            "courseName" to course.title,
            "instructor" to course.instructor,
            "enrolledAt" to System.currentTimeMillis(),
            "progress" to 0,
            "paymentId" to paymentRecord.id
        )
        
        // Add to user's enrolled courses
        firestore.collection("users")
            .document(userId)
            .collection("enrolledCourses")
            .document(course.id)
            .set(enrollmentData)
            .addOnSuccessListener {
                // Also update the course's enrolled students count
                updateCourseEnrollmentCount(course.id)
                showEnrollmentSuccess()
            }
            .addOnFailureListener {
                // For demo, still show success
                showEnrollmentSuccess()
            }
    }
    
    private fun updateCourseEnrollmentCount(courseId: String) {
        firestore.collection("courses")
            .document(courseId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentCount = document.getLong("enrolledStudents") ?: 0
                    firestore.collection("courses")
                        .document(courseId)
                        .update("enrolledStudents", currentCount + 1)
                }
            }
    }

    private fun showEnrollmentSuccess() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Enrollment Successful!")
            .setMessage("You have successfully enrolled in ${currentCourse?.title}. You can now access the course content.")
            .setPositiveButton("Go to My Courses") { _, _ ->
                val intent = Intent(this, StudentCoursesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Continue Browsing") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}