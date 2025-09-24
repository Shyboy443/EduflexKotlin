package com.example.ed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ed.models.PaymentRecord
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PaymentDetailsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var cardPaymentInfo: MaterialCardView
    private lateinit var cardCourseInfo: MaterialCardView
    private lateinit var cardTransactionInfo: MaterialCardView
    private lateinit var btnDownloadReceipt: MaterialButton
    private lateinit var btnContactSupport: MaterialButton
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private var paymentId: String = ""
    private var currentPayment: PaymentRecord? = null
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_details)
        
        paymentId = intent.getStringExtra("PAYMENT_ID") ?: ""
        
        initializeComponents()
        setupClickListeners()
        loadPaymentDetails()
    }

    private fun initializeComponents() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        toolbar = findViewById(R.id.toolbar)
        cardPaymentInfo = findViewById(R.id.card_payment_info)
        cardCourseInfo = findViewById(R.id.card_course_info)
        cardTransactionInfo = findViewById(R.id.card_transaction_info)
        btnDownloadReceipt = findViewById(R.id.btn_download_receipt)
        btnContactSupport = findViewById(R.id.btn_contact_support)
        
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Payment Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupClickListeners() {
        btnDownloadReceipt.setOnClickListener {
            // TODO: Implement receipt download functionality
            android.widget.Toast.makeText(this, "Receipt download coming soon", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        btnContactSupport.setOnClickListener {
            // TODO: Implement support contact functionality
            android.widget.Toast.makeText(this, "Support contact coming soon", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPaymentDetails() {
        if (paymentId.isEmpty()) {
            loadSamplePaymentDetails()
            return
        }
        
        firestore.collection("payments")
            .document(paymentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentPayment = document.toObject(PaymentRecord::class.java)
                    currentPayment?.let { displayPaymentDetails(it) }
                } else {
                    loadSamplePaymentDetails()
                }
            }
            .addOnFailureListener {
                loadSamplePaymentDetails()
            }
    }

    private fun loadSamplePaymentDetails() {
        val samplePayment = PaymentRecord(
            id = "payment1",
            userId = auth.currentUser?.uid ?: "",
            courseId = "course1",
            courseName = "Android Development Masterclass",
            amount = 49.99,
            currency = "USD",
            paymentMethod = "Credit Card",
            status = "Completed",
            timestamp = System.currentTimeMillis(),
            transactionId = "txn_1234567890"
        )
        
        currentPayment = samplePayment
        displayPaymentDetails(samplePayment)
    }

    private fun displayPaymentDetails(payment: PaymentRecord) {
        // Update payment info card
        findViewById<android.widget.TextView>(R.id.tv_amount).text = currencyFormat.format(payment.amount)
        findViewById<android.widget.TextView>(R.id.tv_currency).text = payment.currency
        findViewById<android.widget.TextView>(R.id.tv_payment_method).text = payment.paymentMethod
        
        val statusTextView = findViewById<android.widget.TextView>(R.id.tv_status)
        statusTextView.text = payment.status
        
        // Set status color
        val statusColor = when (payment.status.lowercase()) {
            "completed" -> ContextCompat.getColor(this, R.color.success_color)
            "pending" -> ContextCompat.getColor(this, R.color.warning_color)
            "failed" -> ContextCompat.getColor(this, R.color.error_color)
            "refunded" -> ContextCompat.getColor(this, R.color.info_color)
            else -> ContextCompat.getColor(this, R.color.text_secondary)
        }
        statusTextView.setTextColor(statusColor)
        
        // Update course info card
        findViewById<android.widget.TextView>(R.id.tv_course_name).text = payment.courseName
        findViewById<android.widget.TextView>(R.id.tv_course_id).text = "Course ID: ${payment.courseId}"
        
        // Update transaction info card
        findViewById<android.widget.TextView>(R.id.tv_transaction_id).text = payment.transactionId
        
        val date = Date(payment.timestamp)
        findViewById<android.widget.TextView>(R.id.tv_date).text = dateFormat.format(date)
        findViewById<android.widget.TextView>(R.id.tv_time).text = timeFormat.format(date)
        
        // Show/hide buttons based on payment status
        btnDownloadReceipt.visibility = if (payment.status == "Completed") View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        // Navigate back to payment history
        startActivity(Intent(this, PaymentHistoryActivity::class.java))
        finish()
    }
}