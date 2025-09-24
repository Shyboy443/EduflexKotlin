package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.PaymentRecord
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PaymentHistoryAdapter(
    private val payments: List<PaymentRecord>,
    private val onPaymentClick: (PaymentRecord) -> Unit
) : RecyclerView.Adapter<PaymentHistoryAdapter.PaymentViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    inner class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCourseName: TextView = itemView.findViewById(R.id.tv_course_name)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvPaymentMethod: TextView = itemView.findViewById(R.id.tv_payment_method)
        private val tvTransactionId: TextView = itemView.findViewById(R.id.tv_transaction_id)

        fun bind(payment: PaymentRecord) {
            tvCourseName.text = payment.courseName
            tvAmount.text = currencyFormat.format(payment.amount)
            
            val date = Date(payment.timestamp)
            tvDate.text = dateFormat.format(date)
            tvTime.text = timeFormat.format(date)
            
            tvStatus.text = payment.status
            tvPaymentMethod.text = payment.paymentMethod
            tvTransactionId.text = "ID: ${payment.transactionId}"
            
            // Set status color
            val statusColor = when (payment.status.lowercase()) {
                "completed" -> ContextCompat.getColor(itemView.context, R.color.success_color)
                "pending" -> ContextCompat.getColor(itemView.context, R.color.warning_color)
                "failed" -> ContextCompat.getColor(itemView.context, R.color.error_color)
                "refunded" -> ContextCompat.getColor(itemView.context, R.color.info_color)
                else -> ContextCompat.getColor(itemView.context, R.color.text_secondary)
            }
            tvStatus.setTextColor(statusColor)
            
            itemView.setOnClickListener {
                onPaymentClick(payment)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(payments[position])
    }

    override fun getItemCount(): Int = payments.size
}