package com.example.ed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.R
import com.example.ed.models.ContentItem
import com.example.ed.models.ContentType
import com.example.ed.models.Quiz
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class StudentContentAdapter(
    private val onContentClick: (ContentItem) -> Unit,
    private val onQuizClick: (Quiz?) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var contentItems = listOf<ContentItem>()
    private var quiz: Quiz? = null
    
    companion object {
        private const val VIEW_TYPE_CONTENT = 0
        private const val VIEW_TYPE_QUIZ = 1
    }
    
    fun updateContent(items: List<ContentItem>, weekQuiz: Quiz?) {
        contentItems = items
        quiz = weekQuiz
        notifyDataSetChanged()
    }
    
    fun updateQuiz(weekQuiz: Quiz?) {
        quiz = weekQuiz
        notifyItemChanged(contentItems.size) // Quiz is at the end
    }
    
    fun getQuiz(): Quiz? = quiz
    
    override fun getItemViewType(position: Int): Int {
        return if (position < contentItems.size) {
            VIEW_TYPE_CONTENT
        } else {
            VIEW_TYPE_QUIZ
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CONTENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_student_content, parent, false)
                ContentViewHolder(view)
            }
            VIEW_TYPE_QUIZ -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_student_quiz, parent, false)
                QuizViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ContentViewHolder -> {
                val content = contentItems[position]
                holder.bind(content, onContentClick)
            }
            is QuizViewHolder -> {
                holder.bind(quiz, onQuizClick)
            }
        }
    }
    
    override fun getItemCount(): Int {
        // Content items + 1 for quiz (if exists)
        return contentItems.size + if (quiz != null) 1 else 0
    }
    
    class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContent: MaterialCardView = itemView.findViewById(R.id.card_content)
        private val ivContentIcon: ImageView = itemView.findViewById(R.id.iv_content_icon)
        private val tvContentTitle: TextView = itemView.findViewById(R.id.tv_content_title)
        private val tvContentType: TextView = itemView.findViewById(R.id.tv_content_type)
        private val tvContentDuration: TextView = itemView.findViewById(R.id.tv_content_duration)
        private val tvRequired: TextView = itemView.findViewById(R.id.tv_required)
        private val btnViewContent: MaterialButton = itemView.findViewById(R.id.btn_view_content)
        
        fun bind(content: ContentItem, onClick: (ContentItem) -> Unit) {
            tvContentTitle.text = content.title
            tvContentType.text = content.type.name.replace("_", " ")
            
            // Set duration if available
            if (content.duration > 0) {
                tvContentDuration.visibility = View.VISIBLE
                tvContentDuration.text = "${content.duration} min"
            } else {
                tvContentDuration.visibility = View.GONE
            }
            
            // Show required badge
            tvRequired.visibility = if (content.isRequired) View.VISIBLE else View.GONE
            
            // Set icon based on content type
            val iconRes = when (content.type) {
                ContentType.TEXT -> R.drawable.ic_text_content
                ContentType.VIDEO -> R.drawable.ic_video_content
                ContentType.AUDIO -> R.drawable.ic_audio_content
                ContentType.DOCUMENT -> R.drawable.ic_document_content
                ContentType.PRESENTATION -> R.drawable.ic_presentation_content
                ContentType.INTERACTIVE -> R.drawable.ic_interactive_content
                ContentType.DISCUSSION -> R.drawable.ic_discussion_content
                else -> R.drawable.ic_content_default
            }
            ivContentIcon.setImageResource(iconRes)
            
            // Set click listeners
            btnViewContent.setOnClickListener { onClick(content) }
            cardContent.setOnClickListener { onClick(content) }
        }
    }
    
    class QuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardQuiz: MaterialCardView = itemView.findViewById(R.id.card_quiz)
        private val tvQuizTitle: TextView = itemView.findViewById(R.id.tv_quiz_title)
        private val tvQuizDescription: TextView = itemView.findViewById(R.id.tv_quiz_description)
        private val tvQuizQuestions: TextView = itemView.findViewById(R.id.tv_quiz_questions)
        private val tvQuizDuration: TextView = itemView.findViewById(R.id.tv_quiz_duration)
        private val tvQuizPoints: TextView = itemView.findViewById(R.id.tv_quiz_points)
        private val btnStartQuiz: MaterialButton = itemView.findViewById(R.id.btn_start_quiz)
        
        fun bind(quiz: Quiz?, onClick: (Quiz?) -> Unit) {
            if (quiz != null) {
                cardQuiz.visibility = View.VISIBLE
                tvQuizTitle.text = quiz.title
                tvQuizDescription.text = quiz.description
                tvQuizQuestions.text = "${quiz.questions.size} Questions"
                tvQuizPoints.text = "${quiz.totalPoints} Points"
                
                if (quiz.timeLimit > 0) {
                    tvQuizDuration.visibility = View.VISIBLE
                    tvQuizDuration.text = "${quiz.timeLimit} minutes"
                } else {
                    tvQuizDuration.visibility = View.GONE
                }
                
                btnStartQuiz.setOnClickListener { onClick(quiz) }
                cardQuiz.setOnClickListener { onClick(quiz) }
            } else {
                cardQuiz.visibility = View.GONE
            }
        }
    }
}
