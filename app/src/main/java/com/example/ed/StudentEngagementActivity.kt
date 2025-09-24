package com.example.ed

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.ed.adapters.DiscussionAdapter
import com.example.ed.adapters.StudentProgressAdapter
import com.example.ed.adapters.BadgeAdapter
import com.example.ed.models.*

import com.example.ed.models.LessonProgressUI
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

class StudentEngagementActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var engagementPagerAdapter: EngagementPagerAdapter

    // Data
    private lateinit var firestore: FirebaseFirestore
    private var courseId: String = ""
    private var studentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_engagement)

        courseId = intent.getStringExtra("courseId") ?: ""
        studentId = intent.getStringExtra("studentId") ?: ""

        initializeViews()
        setupFirestore()
        setupViewPager()
        loadEngagementData()
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        
        supportActionBar?.apply {
            title = "Student Engagement"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupFirestore() {
        firestore = FirebaseFirestore.getInstance()
    }

    private fun setupViewPager() {
        engagementPagerAdapter = EngagementPagerAdapter(this)
        viewPager.adapter = engagementPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Progress"
                1 -> "Discussions"
                2 -> "Achievements"
                3 -> "Analytics"
                else -> "Tab ${position + 1}"
            }
            tab.setIcon(when (position) {
                0 -> android.R.drawable.ic_menu_agenda
                1 -> android.R.drawable.ic_menu_info_details
                2 -> android.R.drawable.star_on
                3 -> android.R.drawable.ic_menu_sort_by_size
                else -> android.R.drawable.ic_menu_help
            })
        }.attach()
    }

    private fun loadEngagementData() {
        lifecycleScope.launch {
            // Load student progress, discussions, and achievements
            loadStudentProgress()
            loadDiscussions()
            loadAchievements()
            loadAnalytics()
        }
    }

    private suspend fun loadStudentProgress() {
        // Implementation for loading student progress
    }

    private suspend fun loadDiscussions() {
        // Implementation for loading discussions
    }

    private suspend fun loadAchievements() {
        // Implementation for loading achievements
    }

    private suspend fun loadAnalytics() {
        // Implementation for loading analytics
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

// Progress Fragment
class ProgressFragment : androidx.fragment.app.Fragment() {
    
    private lateinit var progressRecyclerView: RecyclerView
    private lateinit var overallProgressIndicator: CircularProgressIndicator
    private lateinit var overallProgressText: TextView
    private lateinit var progressAdapter: StudentProgressAdapter
    private lateinit var streakCard: MaterialCardView
    private lateinit var streakText: TextView
    private lateinit var pointsText: TextView

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        loadProgressData()
    }

    private fun initializeViews(view: View) {
        progressRecyclerView = view.findViewById(R.id.progressRecyclerView)
        overallProgressIndicator = view.findViewById(R.id.overallProgressIndicator)
        overallProgressText = view.findViewById(R.id.overallProgressText)
        streakCard = view.findViewById(R.id.streakCard)
        streakText = view.findViewById(R.id.streakText)
        pointsText = view.findViewById(R.id.pointsText)
    }

    private fun setupRecyclerView() {
        progressAdapter = StudentProgressAdapter { lessonProgress ->
            // Handle lesson click
            openLessonDetails(lessonProgress)
        }
        progressRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = progressAdapter
        }
    }

    private fun loadProgressData() {
        // Load student progress data
        val sampleProgress = generateSampleProgress()
        progressAdapter.updateProgress(sampleProgress)
        
        // Update overall progress
        val overallProgress = calculateOverallProgress(sampleProgress)
        overallProgressIndicator.progress = overallProgress
        overallProgressText.text = "$overallProgress%"
        
        // Update streak and points
        streakText.text = "7 day streak!"
        pointsText.text = "1,250 points"
    }

    private fun generateSampleProgress(): List<LessonProgressUI> {
        return listOf(
            LessonProgressUI("1", "lesson1", "Introduction to Programming", 100, System.currentTimeMillis(), true),
            LessonProgressUI("2", "lesson2", "Variables and Data Types", 85, System.currentTimeMillis(), true),
            LessonProgressUI("3", "lesson3", "Control Structures", 60, null, false),
            LessonProgressUI("4", "lesson4", "Functions and Methods", 0, null, false)
        )
    }

    private fun calculateOverallProgress(progressList: List<LessonProgressUI>): Int {
        if (progressList.isEmpty()) return 0
        return progressList.map { it.completionPercentage }.average().toInt()
    }

    private fun openLessonDetails(lessonProgress: LessonProgressUI) {
        // Navigate to lesson details
    }
}

// Discussions Fragment
class DiscussionsFragment : androidx.fragment.app.Fragment() {
    
    private lateinit var discussionsRecyclerView: RecyclerView
    private lateinit var newDiscussionButton: MaterialButton
    private lateinit var discussionAdapter: DiscussionAdapter

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_discussions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        setupListeners()
        loadDiscussions()
    }

    private fun initializeViews(view: View) {
        discussionsRecyclerView = view.findViewById(R.id.discussionsRecyclerView)
        newDiscussionButton = view.findViewById(R.id.newDiscussionButton)
    }

    private fun setupRecyclerView() {
        discussionAdapter = DiscussionAdapter { discussion ->
            openDiscussionDetails(discussion)
        }
        discussionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = discussionAdapter
        }
    }

    private fun setupListeners() {
        newDiscussionButton.setOnClickListener {
            showNewDiscussionDialog()
        }
    }

    private fun loadDiscussions() {
        val sampleDiscussions = generateSampleDiscussions()
        discussionAdapter.updateDiscussions(sampleDiscussions)
    }

    private fun generateSampleDiscussions(): List<Discussion> {
        return listOf(
            Discussion(
                id = "1",
                courseId = "course1",
                title = "Question about loops",
                content = "Can someone explain the difference between for and while loops?",
                authorId = "student1",
                authorName = "John Doe",
                createdAt = System.currentTimeMillis(),
                replies = listOf(
                    DiscussionReply("r1", "1", "Great question! Let me explain...", "teacher1", "Teacher Smith", UserRole.TEACHER, 0, 0, false, System.currentTimeMillis())
                ),
                isResolved = false,
                tags = listOf("loops", "programming"),
                upvotes = 5
            )
        )
    }

    private fun openDiscussionDetails(discussion: Discussion) {
        // Navigate to discussion details
    }

    private fun showNewDiscussionDialog() {
        // Show dialog to create new discussion
    }
}

// Achievements Fragment
class AchievementsFragment : androidx.fragment.app.Fragment() {
    
    private lateinit var badgesRecyclerView: RecyclerView
    private lateinit var badgeAdapter: BadgeAdapter
    private lateinit var totalBadgesText: TextView
    private lateinit var recentAchievementCard: MaterialCardView

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_achievements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        loadAchievements()
    }

    private fun initializeViews(view: View) {
        badgesRecyclerView = view.findViewById(R.id.badgesRecyclerView)
        totalBadgesText = view.findViewById(R.id.totalBadgesText)
        recentAchievementCard = view.findViewById(R.id.recentAchievementCard)
    }

    private fun setupRecyclerView() {
        badgeAdapter = BadgeAdapter { badge ->
            showBadgeDetails(badge)
        }
        badgesRecyclerView.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            adapter = badgeAdapter
        }
    }

    private fun loadAchievements() {
        val sampleBadges = generateSampleBadges()
        badgeAdapter.updateBadges(sampleBadges)
        totalBadgesText.text = "${sampleBadges.size} badges earned"
    }

    private fun generateSampleBadges(): List<Badge> {
        return listOf(
            Badge("1", "First Steps", "Complete your first lesson", "🎯", System.currentTimeMillis(), BadgeCategory.ACHIEVEMENT),
            Badge("2", "Quiz Master", "Score 100% on 5 quizzes", "🏆", System.currentTimeMillis(), BadgeCategory.ACHIEVEMENT),
            Badge("3", "Discussion Leader", "Start 10 discussions", "💬", System.currentTimeMillis(), BadgeCategory.PARTICIPATION)
        )
    }

    private fun showBadgeDetails(badge: Badge) {
        // Show badge details dialog
    }
}

// Analytics Fragment
class AnalyticsFragment : androidx.fragment.app.Fragment() {
    
    private lateinit var weeklyProgressChart: View
    private lateinit var studyTimeText: TextView
    private lateinit var averageScoreText: TextView
    private lateinit var completionRateText: TextView

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        loadAnalyticsData()
    }

    private fun initializeViews(view: View) {
        weeklyProgressChart = view.findViewById(R.id.weeklyProgressChart)
        studyTimeText = view.findViewById(R.id.studyTimeText)
        averageScoreText = view.findViewById(R.id.averageScoreText)
        completionRateText = view.findViewById(R.id.completionRateText)
    }

    private fun loadAnalyticsData() {
        // Load and display analytics data
        studyTimeText.text = "12.5 hours this week"
        averageScoreText.text = "87% average score"
        completionRateText.text = "75% completion rate"
    }
}

// ViewPager Adapter
class EngagementPagerAdapter(activity: AppCompatActivity) : androidx.viewpager2.adapter.FragmentStateAdapter(activity) {
    
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): androidx.fragment.app.Fragment {
        return when (position) {
            0 -> ProgressFragment()
            1 -> DiscussionsFragment()
            2 -> AchievementsFragment()
            3 -> AnalyticsFragment()
            else -> ProgressFragment()
        }
    }
}