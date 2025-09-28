package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.databinding.ItemStudentEnrollmentBinding
import com.example.ed.models.StudentEnrollment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentEnrollmentAdapter(
    private val items: MutableList<StudentEnrollment>
) : RecyclerView.Adapter<StudentEnrollmentAdapter.VH>() {

    inner class VH(val binding: ItemStudentEnrollmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemStudentEnrollmentBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvStudentName.text = item.studentName
            tvStudentEmail.text = item.studentEmail
            tvCourseName.text = item.courseName
            tvProgress.text = "${item.progress}%"
            tvEnrolledAt.text = "Enrolled: ${formatDate(item.enrolledAt)}"
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<StudentEnrollment>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun formatDate(ts: Long): String {
        if (ts <= 0L) return "-"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}
