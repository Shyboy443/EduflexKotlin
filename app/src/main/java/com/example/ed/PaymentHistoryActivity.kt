package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.PaymentHistoryAdapter
import com.example.ed.models.PaymentRecord
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class PaymentHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvPaymentHistory: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var emptyStateView: View
    
    private lateinit var paymentAdapter: PaymentHistoryAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private var paymentHistory = mutableListOf<PaymentRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_history)
        
        initializeComponents()
        setupRecyclerView()
        setupClickListeners()
        loadPaymentHistory()
    }

    private fun initializeComponents() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        toolbar = findViewById(R.id.toolbar)
        rvPaymentHistory = findViewById(R.id.rv_payment_history)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        emptyStateView = findViewById(R.id.empty_state_view)
        
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Payment History"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        paymentAdapter = PaymentHistoryAdapter(paymentHistory) { payment ->
            // Show payment details
            showPaymentDetails(payment)
        }
        
        rvPaymentHistory.layoutManager = LinearLayoutManager(this)
        rvPaymentHistory.adapter = paymentAdapter
    }

    private fun setupClickListeners() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_catalog -> {
                    startActivity(Intent(this, CourseCatalogActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_my_courses -> {
                    startActivity(Intent(this, StudentCoursesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, StudentProfileActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_payments -> {
                    // Already on payments
                    true
                }
                else -> false
            }
        }
        
        // Set current item
        bottomNavigation.selectedItemId = R.id.nav_payments
    }

    private fun loadPaymentHistory() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState()
            return
        }

        firestore.collection("payments")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                paymentHistory.clear()
                for (document in documents) {
                    val payment = document.toObject(PaymentRecord::class.java)
                    paymentHistory.add(payment)
                }
                
                // Sort payments by timestamp in descending order (newest first)
                paymentHistory.sortByDescending { it.timestamp }
                
                if (paymentHistory.isEmpty()) {
                    showEmptyState()
                } else {
                    showPaymentHistory()
                }
                
                paymentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Load sample data for demo
                loadSamplePaymentHistory()
            }
    }

    private fun loadSamplePaymentHistory() {
        paymentHistory.clear()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        paymentHistory.addAll(listOf(
            PaymentRecord(
                id = "payment1",
                userId = auth.currentUser?.uid ?: "",
                courseId = "course1",
                courseName = "Android Development Masterclass",
                amount = 49.99,
                currency = "USD",
                paymentMethod = "Credit Card",
                status = "Completed",
                timestamp = dateFormat.parse("2024-01-15 14:30:00")?.time ?: 0L,
                transactionId = "txn_1234567890"
            ),
            PaymentRecord(
                id = "payment2",
                userId = auth.currentUser?.uid ?: "",
                courseId = "course2",
                courseName = "Web Development Bootcamp",
                amount = 79.99,
                currency = "USD",
                paymentMethod = "PayPal",
                status = "Completed",
                timestamp = dateFormat.parse("2024-01-10 09:15:00")?.time ?: 0L,
                transactionId = "txn_0987654321"
            ),
            PaymentRecord(
                id = "payment3",
                userId = auth.currentUser?.uid ?: "",
                courseId = "course3",
                courseName = "Business Strategy Fundamentals",
                amount = 29.99,
                currency = "USD",
                paymentMethod = "Credit Card",
                status = "Refunded",
                timestamp = dateFormat.parse("2024-01-05 16:45:00")?.time ?: 0L,
                transactionId = "txn_1122334455"
            )
        ))
        
        showPaymentHistory()
        paymentAdapter.notifyDataSetChanged()
    }

    private fun showPaymentHistory() {
        rvPaymentHistory.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun showEmptyState() {
        rvPaymentHistory.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
    }

    private fun showPaymentDetails(payment: PaymentRecord) {
        // TODO: Show detailed payment information in a dialog or new activity
        val intent = Intent(this, PaymentDetailsActivity::class.java)
        intent.putExtra("PAYMENT_ID", payment.id)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        // Navigate to student dashboard
        startActivity(Intent(this, StudentDashboardActivity::class.java))
        finish()
    }
}