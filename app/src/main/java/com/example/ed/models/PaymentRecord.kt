package com.example.ed.models

data class PaymentRecord(
    val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val paymentMethod: String = "",
    val status: String = "", // "Completed", "Pending", "Failed", "Refunded"
    val timestamp: Long = 0L,
    val transactionId: String = "",
    val receiptUrl: String = "",
    val notes: String = ""
)