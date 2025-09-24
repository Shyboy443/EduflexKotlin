package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.StudentSubmission
import com.example.ed.models.SubmissionStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class SubmissionAdapter(
    private val onSubmissionClick: (StudentSubmission) -> Unit,
    private val onQuickGrade: (StudentSubmission, Double) -> Unit
) : RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder>() {

    private var submissions = mutableListOf<StudentSubmission>()

    fun updateSubmissions(newSubmissions: List<StudentSubmission>) {
        submissions.clear()
        submissions.addAll(newSubmissions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submission, parent, false)
        return SubmissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        holder.bind(submissions[position])
    }

    override fun getItemCount(): Int = submissions.size

    inner class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.submissionCard)
        private val studentNameText: TextView = itemView.findViewById(R.id.studentNameText)
        private val submissionTimeText: TextView = itemView.findViewById(R.id.submissionTimeText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val gradeText: TextView = itemView.findViewById(R.id.gradeText)
        private val quickGradeButton: MaterialButton = itemView.findViewById(R.id.quickGradeButton)

        fun bind(submission: StudentSubmission) {
            studentNameText.text = submission.studentName
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            submissionTimeText.text = "Submitted: ${dateFormat.format(submission.submittedAt)}"
            
            statusText.text = when (submission.status) {
                SubmissionStatus.SUBMITTED -> "Pending Review"
                SubmissionStatus.GRADED -> "Graded"
                SubmissionStatus.NEEDS_REVIEW -> "Needs Review"
                SubmissionStatus.LATE -> "Late Submission"
                SubmissionStatus.PENDING -> "Pending"
            }
            
            if (submission.grade != null) {
                gradeText.text = "${submission.grade}%"
                gradeText.visibility = View.VISIBLE
            } else {
                gradeText.visibility = View.GONE
            }
            
            quickGradeButton.visibility = if (submission.status == SubmissionStatus.SUBMITTED) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            cardView.setOnClickListener {
                onSubmissionClick(submission)
            }
            
            quickGradeButton.setOnClickListener {
                // Quick grade with 80% as default
                onQuickGrade(submission, 80.0)
            }
        }
    }
}