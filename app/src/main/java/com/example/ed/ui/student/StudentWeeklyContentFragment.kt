package com.example.ed.ui.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ed.R
import com.example.ed.StudentQuizActivity
import com.example.ed.adapters.StudentContentAdapter
import com.example.ed.databinding.FragmentStudentWeeklyContentBinding
import com.example.ed.models.*
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StudentWeeklyContentFragment : Fragment() {
    
    private var _binding: FragmentStudentWeeklyContentBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private var courseId: String = ""
    private var currentWeek: Int = 1
    private var maxUnlockedWeek: Int = 1
    private var weeklyContents = mutableListOf<WeeklyContent>()
    private var studentProgress = mutableMapOf<Int, WeekProgress>()
    private lateinit var contentAdapter: StudentContentAdapter
    
    companion object {
        private const val PASSING_SCORE = 75 // 75% to pass
        private const val TAG = "StudentWeeklyContent"
        private const val REQUEST_QUIZ = 1001
        
        fun newInstance(courseId: String): StudentWeeklyContentFragment {
            val fragment = StudentWeeklyContentFragment()
            val args = Bundle()
            args.putString("COURSE_ID", courseId)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentWeeklyContentBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        courseId = arguments?.getString("COURSE_ID") ?: ""
        if (courseId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid course", Toast.LENGTH_SHORT).show()
            return
        }
        
        setupToolbar()
        setupRecyclerView()
        setupWeekTabs()
        loadCourseDetails()
        loadStudentProgress()
    }
    
    private fun setupToolbar() {
        // Fragment doesn't need toolbar setup as it's handled by parent activity
    }
    
    private fun setupRecyclerView() {
        contentAdapter = StudentContentAdapter(
            onContentClick = { contentItem ->
                openContent(contentItem)
            },
            onQuizClick = { quiz ->
                if (quiz != null) {
                    startQuiz(quiz)
                }
            }
        )
        
        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contentAdapter
        }
    }
    
    private fun setupWeekTabs() {
        // Add 16 week tabs
        for (i in 1..16) {
            val tab = binding.tabLayoutWeeks.newTab()
            tab.text = "Week $i"
            tab.tag = i
            binding.tabLayoutWeeks.addTab(tab)
        }
        
        binding.tabLayoutWeeks.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val week = it.tag as Int
                    if (week <= maxUnlockedWeek) {
                        currentWeek = week
                        loadWeekContent(week)
                    } else {
                        // Show locked message
                        showLockedWeekDialog(week)
                        // Reselect current week tab
                        binding.tabLayoutWeeks.getTabAt(currentWeek - 1)?.select()
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun loadCourseDetails() {
        db.collection("courses")
            .document(courseId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.tvCourseTitle.text = document.getString("title") ?: "Course"
                    binding.tvCourseDescription.text = document.getString("description") ?: ""
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading course details", e)
            }
    }
    
    private fun loadStudentProgress() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("student_progress")
            .whereEqualTo("studentId", userId)
            .whereEqualTo("courseId", courseId)
            .get()
            .addOnSuccessListener { documents ->
                studentProgress.clear()
                maxUnlockedWeek = 1 // Start with week 1 unlocked
                
                for (document in documents) {
                    val weekNumber = document.getLong("weekNumber")?.toInt() ?: continue
                    val progress = WeekProgress(
                        weekNumber = weekNumber,
                        isCompleted = document.getBoolean("isCompleted") ?: false,
                        quizScore = document.getDouble("quizScore")?.toFloat() ?: 0f,
                        contentProgress = document.getDouble("contentProgress")?.toFloat() ?: 0f,
                        lastAccessedAt = document.getLong("lastAccessedAt") ?: 0
                    )
                    studentProgress[weekNumber] = progress
                    
                    // Unlock next week if current week is completed with passing score
                    if (progress.isCompleted && progress.quizScore >= PASSING_SCORE) {
                        maxUnlockedWeek = maxOf(maxUnlockedWeek, weekNumber + 1)
                    }
                }
                
                // Ensure max unlocked week doesn't exceed 16
                maxUnlockedWeek = minOf(maxUnlockedWeek, 16)
                
                updateWeekTabsUI()
                loadWeekContent(currentWeek)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading student progress", e)
                loadWeekContent(currentWeek)
            }
    }
    
    private fun updateWeekTabsUI() {
        for (i in 0 until binding.tabLayoutWeeks.tabCount) {
            val tab = binding.tabLayoutWeeks.getTabAt(i)
            val weekNumber = i + 1
            
            when {
                weekNumber < maxUnlockedWeek -> {
                    // Completed week
                    val progress = studentProgress[weekNumber]
                    if (progress?.isCompleted == true) {
                        tab?.text = "Week $weekNumber ✓"
                    }
                }
                weekNumber == maxUnlockedWeek -> {
                    // Current week
                    tab?.text = "Week $weekNumber"
                }
                else -> {
                    // Locked week
                    tab?.text = "Week $weekNumber 🔒"
                }
            }
        }
    }
    
    private fun loadWeekContent(weekNumber: Int) {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        
        // Update current week info
        binding.tvCurrentWeek.text = "Week $weekNumber"
        val progress = studentProgress[weekNumber]
        updateProgressUI(progress)
        
        // Load weekly content from Firestore
        db.collection("weekly_content")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("weekNumber", weekNumber)
            .whereEqualTo("isPublished", true)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val weeklyContent = WeeklyContent(
                        id = document.id,
                        courseId = courseId,
                        weekNumber = weekNumber,
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        learningObjectives = document.get("learningObjectives") as? List<String> ?: emptyList(),
                        contentItems = parseContentItems(document.get("contentItems")),
                        quiz = parseQuiz(document.get("quizId") as? String),
                        isPublished = true
                    )
                    
                    displayWeekContent(weeklyContent)
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "No content available for Week $weekNumber"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading week content", e)
                binding.progressBar.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed to load content", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun parseContentItems(data: Any?): List<ContentItem> {
        val items = mutableListOf<ContentItem>()
        if (data is List<*>) {
            for (item in data) {
                if (item is Map<*, *>) {
                    items.add(ContentItem(
                        id = item["id"] as? String ?: "",
                        title = item["title"] as? String ?: "",
                        type = ContentType.valueOf(item["type"] as? String ?: "TEXT"),
                        content = item["content"] as? String ?: "",
                        mediaUrl = item["mediaUrl"] as? String ?: "",
                        duration = (item["duration"] as? Long)?.toInt() ?: 0,
                        order = (item["order"] as? Long)?.toInt() ?: 0,
                        isRequired = item["isRequired"] as? Boolean ?: true
                    ))
                }
            }
        }
        return items.sortedBy { it.order }
    }
    
    private fun parseQuiz(quizId: String?): Quiz? {
        if (quizId.isNullOrEmpty()) return null
        
        // This will be loaded asynchronously
        var quiz: Quiz? = null
        db.collection("quizzes")
            .document(quizId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    quiz = document.toObject(Quiz::class.java)
                    // Update the adapter with the quiz
                    contentAdapter.updateQuiz(quiz)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading quiz", e)
            }
        
        return quiz
    }
    
    private fun displayWeekContent(weeklyContent: WeeklyContent) {
        binding.progressBar.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
        
        // Update week details
        binding.tvWeekTitle.text = weeklyContent.title
        binding.tvWeekDescription.text = weeklyContent.description
        
        // Display learning objectives
        if (weeklyContent.learningObjectives.isNotEmpty()) {
            binding.tvLearningObjectives.visibility = View.VISIBLE
            binding.tvLearningObjectives.text = "Learning Objectives:\n" + 
                weeklyContent.learningObjectives.joinToString("\n• ", "• ")
        } else {
            binding.tvLearningObjectives.visibility = View.GONE
        }
        
        // Update content adapter
        contentAdapter.updateContent(weeklyContent.contentItems, weeklyContent.quiz)
        
        // Check if week is locked
        if (currentWeek > maxUnlockedWeek) {
            binding.btnCompleteWeek.isEnabled = false
            binding.btnCompleteWeek.text = "Week Locked"
        } else {
            binding.btnCompleteWeek.isEnabled = true
            binding.btnCompleteWeek.text = "Take Quiz"
            binding.btnCompleteWeek.setOnClickListener {
                if (weeklyContent.quiz != null) {
                    startQuiz(weeklyContent.quiz!!)
                } else {
                    Toast.makeText(requireContext(), "No quiz available for this week", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateProgressUI(progress: WeekProgress?) {
        if (progress != null) {
            binding.progressBarWeek.progress = progress.contentProgress.toInt()
            binding.tvProgressPercentage.text = "${progress.contentProgress.toInt()}%"
            
            if (progress.quizScore > 0) {
                binding.tvQuizScore.visibility = View.VISIBLE
                binding.tvQuizScore.text = "Quiz Score: ${progress.quizScore}%"
                
                if (progress.quizScore >= PASSING_SCORE) {
                    binding.tvQuizScore.setTextColor(requireContext().getColor(R.color.success_green))
                } else {
                    binding.tvQuizScore.setTextColor(requireContext().getColor(R.color.error_red))
                }
            } else {
                binding.tvQuizScore.visibility = View.GONE
            }
        } else {
            binding.progressBarWeek.progress = 0
            binding.tvProgressPercentage.text = "0%"
            binding.tvQuizScore.visibility = View.GONE
        }
    }
    
    private fun openContent(contentItem: ContentItem) {
        when (contentItem.type) {
            ContentType.TEXT -> {
                showTextContent(contentItem)
            }
            ContentType.VIDEO -> {
                openVideoContent(contentItem)
            }
            ContentType.DOCUMENT -> {
                openDocumentContent(contentItem)
            }
            ContentType.PRESENTATION -> {
                openPresentationContent(contentItem)
            }
            else -> {
                Toast.makeText(requireContext(), "Opening ${contentItem.title}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Mark content as viewed
        markContentAsViewed(contentItem.id)
    }
    
    private fun showTextContent(contentItem: ContentItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(contentItem.title)
            .setMessage(contentItem.content)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun openVideoContent(contentItem: ContentItem) {
        if (contentItem.mediaUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contentItem.mediaUrl))
            startActivity(intent)
        }
    }
    
    private fun openDocumentContent(contentItem: ContentItem) {
        if (contentItem.mediaUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contentItem.mediaUrl))
            startActivity(intent)
        }
    }
    
    private fun openPresentationContent(contentItem: ContentItem) {
        if (contentItem.mediaUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contentItem.mediaUrl))
            startActivity(intent)
        }
    }
    
    private fun markContentAsViewed(contentId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        val viewData = hashMapOf(
            "studentId" to userId,
            "courseId" to courseId,
            "contentId" to contentId,
            "weekNumber" to currentWeek,
            "viewedAt" to System.currentTimeMillis()
        )
        
        db.collection("content_views")
            .add(viewData)
            .addOnSuccessListener {
                updateContentProgress()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error marking content as viewed", e)
            }
    }
    
    private fun updateContentProgress() {
        // Calculate and update progress for current week
        // This would involve counting viewed content items vs total
    }
    
    private fun startQuiz(quiz: Quiz) {
        val intent = Intent(requireContext(), StudentQuizActivity::class.java)
        intent.putExtra("QUIZ_ID", quiz.id)
        intent.putExtra("COURSE_ID", courseId)
        intent.putExtra("WEEK_NUMBER", currentWeek)
        intent.putExtra("PASSING_SCORE", PASSING_SCORE)
        startActivityForResult(intent, REQUEST_QUIZ)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_QUIZ && resultCode == android.app.Activity.RESULT_OK) {
            val score = data?.getFloatExtra("QUIZ_SCORE", 0f) ?: 0f
            val passed = score >= PASSING_SCORE
            
            // Update progress
            saveQuizResult(score, passed)
            
            if (passed) {
                showPassDialog(score)
                // Unlock next week
                if (currentWeek < 16) {
                    maxUnlockedWeek = maxOf(maxUnlockedWeek, currentWeek + 1)
                    updateWeekTabsUI()
                }
            } else {
                showFailDialog(score)
            }
        }
    }
    
    private fun saveQuizResult(score: Float, passed: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        
        val progressData = hashMapOf(
            "studentId" to userId,
            "courseId" to courseId,
            "weekNumber" to currentWeek,
            "quizScore" to score,
            "isCompleted" to passed,
            "completedAt" to System.currentTimeMillis(),
            "lastAccessedAt" to System.currentTimeMillis()
        )
        
        db.collection("student_progress")
            .document("${userId}_${courseId}_week${currentWeek}")
            .set(progressData)
            .addOnSuccessListener {
                Log.d(TAG, "Progress saved successfully")
                loadStudentProgress() // Reload progress
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving progress", e)
            }
    }
    
    private fun showPassDialog(score: Float) {
        AlertDialog.Builder(requireContext())
            .setTitle("Congratulations! 🎉")
            .setMessage("You passed with ${score}%!\n\nYou have unlocked Week ${currentWeek + 1}")
            .setPositiveButton("Continue to Next Week") { _, _ ->
                if (currentWeek < 16) {
                    currentWeek++
                    binding.tabLayoutWeeks.getTabAt(currentWeek - 1)?.select()
                }
            }
            .setNegativeButton("Stay Here", null)
            .show()
    }
    
    private fun showFailDialog(score: Float) {
        AlertDialog.Builder(requireContext())
            .setTitle("Try Again")
            .setMessage("You scored ${score}%. You need at least $PASSING_SCORE% to pass.\n\nReview the content and try again.")
            .setPositiveButton("Review Content", null)
            .setNegativeButton("Retake Quiz") { _, _ ->
                // Allow retake
                val quiz = contentAdapter.getQuiz()
                if (quiz != null) {
                    startQuiz(quiz)
                }
            }
            .show()
    }
    
    private fun showLockedWeekDialog(weekNumber: Int) {
        val previousWeek = weekNumber - 1
        AlertDialog.Builder(requireContext())
            .setTitle("Week $weekNumber Locked")
            .setMessage("Complete Week $previousWeek with at least $PASSING_SCORE% on the quiz to unlock this week.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    data class WeekProgress(
        val weekNumber: Int,
        val isCompleted: Boolean,
        val quizScore: Float,
        val contentProgress: Float,
        val lastAccessedAt: Long
    )
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
