package com.example.ed.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ed.R
import com.example.ed.SimpleListAdapter
import com.example.ed.adapters.CourseModulesViewAdapter
import com.example.ed.databinding.FragmentCourseDetailsBinding
import com.example.ed.models.Course
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class CourseDetailsFragment : Fragment() {

    private var _binding: FragmentCourseDetailsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore
    private var courseId: String? = null
    private var course: Course? = null
    
    private lateinit var objectivesAdapter: SimpleListAdapter
    private lateinit var prerequisitesAdapter: SimpleListAdapter
    private lateinit var modulesAdapter: CourseModulesViewAdapter

    companion object {
        private const val ARG_COURSE_ID = "course_id"
        
        fun newInstance(courseId: String): CourseDetailsFragment {
            val fragment = CourseDetailsFragment()
            val args = Bundle()
            args.putString(ARG_COURSE_ID, courseId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseId = it.getString(ARG_COURSE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourseDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        
        setupClickListeners()
        setupRecyclerViews()
        loadCourseDetails()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.btnEditCourse.setOnClickListener {
            course?.let { course ->
                // Navigate back to courses fragment and trigger edit
                parentFragmentManager.popBackStack()
                // You can use a callback or interface to trigger edit in parent fragment
                (parentFragment as? CoursesFragment)?.editCourseFromDetails(course)
            }
        }
    }

    private fun setupRecyclerViews() {
        // Learning Objectives
        objectivesAdapter = SimpleListAdapter(mutableListOf()) { }
        binding.rvLearningObjectives.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLearningObjectives.adapter = objectivesAdapter

        // Prerequisites
        prerequisitesAdapter = SimpleListAdapter(mutableListOf()) { }
        binding.rvPrerequisites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPrerequisites.adapter = prerequisitesAdapter

        // Course Modules
        modulesAdapter = CourseModulesViewAdapter(mutableListOf())
        binding.rvCourseModules.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourseModules.adapter = modulesAdapter
    }

    private fun loadCourseDetails() {
        val id = courseId ?: return
        
        db.collection("courses").document(id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Create Course object
                    course = Course(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        description = document.getString("longDescription") ?: document.getString("description") ?: "",
                        category = document.getString("category") ?: "",
                        difficulty = document.getString("difficulty") ?: "",
                        instructor = document.getString("teacherName") ?: document.getString("instructor") ?: "",
                        teacherId = document.getString("teacherId") ?: "",
                        thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                        isPublished = document.getBoolean("isPublished") ?: false,
                        createdAt = document.getLong("createdAt") ?: 0L,
                        updatedAt = document.getLong("updatedAt") ?: 0L,
                        enrolledStudents = document.getLong("enrolledStudents")?.toInt() ?: document.getLong("totalEnrolled")?.toInt() ?: 0,
                        rating = document.getDouble("rating")?.toFloat() ?: document.getDouble("averageRating")?.toFloat() ?: 0.0f,
                        isFree = document.getBoolean("isFree") ?: true,
                        price = document.getDouble("price") ?: 0.0,
                        duration = document.getString("estimatedDuration") ?: document.getString("duration") ?: ""
                    )
                    
                    populateCourseDetails(document.data ?: emptyMap())
                } else {
                    Toast.makeText(requireContext(), "Course not found", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load course: ${e.message}", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
    }

    private fun populateCourseDetails(data: Map<String, Any>) {
        // Basic Information
        binding.tvCourseTitle.text = data["title"] as? String ?: "Untitled Course"
        binding.tvCourseDescription.text = data["longDescription"] as? String ?: data["description"] as? String ?: "No description available"
        
        // Status and Category
        val isPublished = data["isPublished"] as? Boolean ?: false
        binding.tvCourseStatus.text = if (isPublished) "Published" else "Draft"
        binding.tvCourseStatus.setBackgroundResource(
            if (isPublished) R.drawable.bg_tag else R.drawable.bg_tag_outline
        )
        
        binding.tvCourseCategory.text = data["category"] as? String ?: "General"
        binding.tvCourseDifficulty.text = data["difficulty"] as? String ?: "Beginner"
        
        // Stats
        binding.tvEnrolledCount.text = (data["enrolledStudents"] as? Number ?: data["totalEnrolled"] as? Number ?: 0).toString()
        binding.tvRating.text = String.format("%.1f", (data["rating"] as? Number ?: data["averageRating"] as? Number ?: 0.0).toDouble())
        
        // Price
        val isFree = data["isFree"] as? Boolean ?: true
        if (isFree) {
            binding.tvPrice.text = "Free"
            binding.tvPrice.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            val price = (data["price"] as? Number)?.toDouble() ?: 0.0
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            binding.tvPrice.text = formatter.format(price)
            binding.tvPrice.setTextColor(resources.getColor(android.R.color.black, null))
        }
        
        // Thumbnail
        val thumbnailUrl = data["thumbnailUrl"] as? String
        if (!thumbnailUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.ivCourseThumbnail)
        }
        
        // Learning Objectives
        val objectives = data["learningObjectives"] as? List<String> ?: emptyList()
        objectivesAdapter.updateItems(objectives)
        
        // Prerequisites
        val prerequisites = data["prerequisites"] as? List<String> ?: emptyList()
        if (prerequisites.isNotEmpty()) {
            prerequisitesAdapter.updateItems(prerequisites)
            binding.cardPrerequisites.visibility = View.VISIBLE
        } else {
            binding.cardPrerequisites.visibility = View.GONE
        }
        
        // Modules
        val modules = data["modules"] as? List<Map<String, Any>> ?: emptyList()
        val modulesList = modules.map { moduleData ->
            CourseModulesViewAdapter.ModuleItem(
                id = moduleData["id"] as? String ?: "",
                title = moduleData["title"] as? String ?: "",
                description = moduleData["description"] as? String ?: "",
                lessonsCount = (moduleData["lessons"] as? List<*>)?.size ?: 0
            )
        }
        modulesAdapter.updateItems(modulesList)
        
        // Tags
        val tags = data["tags"] as? List<String> ?: emptyList()
        if (tags.isNotEmpty()) {
            binding.chipGroupTags.removeAllViews()
            tags.forEach { tag ->
                val chip = Chip(requireContext())
                chip.text = tag
                chip.isClickable = false
                binding.chipGroupTags.addView(chip)
            }
            binding.cardTags.visibility = View.VISIBLE
        } else {
            binding.cardTags.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}