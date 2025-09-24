package com.example.ed

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.AssignmentAdapter
import com.example.ed.models.Assignment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AssignmentListActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var assignmentsRecyclerView: RecyclerView
    private lateinit var addAssignmentFab: FloatingActionButton
    
    private lateinit var assignmentAdapter: AssignmentAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var assignments = mutableListOf<Assignment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment_list)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadAssignments()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        assignmentsRecyclerView = findViewById(R.id.assignmentsRecyclerView)
        addAssignmentFab = findViewById(R.id.addAssignmentFab)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Assignments"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_assignment_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        assignmentAdapter = AssignmentAdapter(assignments) { assignment ->
            // Handle assignment click
            openAssignmentDetails(assignment)
        }
        
        assignmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        assignmentsRecyclerView.adapter = assignmentAdapter
    }

    private fun setupFab() {
        addAssignmentFab.setOnClickListener {
            // Navigate to create assignment activity
            Toast.makeText(this, "Create Assignment", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAssignments() {
        // Mock assignment data - in real app, load from Firebase
        assignments.clear()
        
        val mockAssignments = listOf(
            Assignment(
                id = "1",
                title = "Quiz 1: Android Basics",
                description = "Basic concepts of Android development",
                dueDate = System.currentTimeMillis() + 86400000 * 7, // 1 week from now
                maxPoints = 100,
                createdAt = System.currentTimeMillis()
            ),
            Assignment(
                id = "2", 
                title = "Assignment 2: RecyclerView Implementation",
                description = "Implement a RecyclerView with custom adapter",
                dueDate = System.currentTimeMillis() + 86400000 * 14, // 2 weeks from now
                maxPoints = 150,
                createdAt = System.currentTimeMillis()
            ),
            Assignment(
                id = "3",
                title = "Project: Weather App",
                description = "Build a complete weather application",
                dueDate = System.currentTimeMillis() + 86400000 * 21, // 3 weeks from now
                maxPoints = 200,
                createdAt = System.currentTimeMillis()
            )
        )
        
        assignments.addAll(mockAssignments)
        assignmentAdapter.notifyDataSetChanged()
    }

    private fun openAssignmentDetails(assignment: Assignment) {
        // Navigate to assignment details
        Toast.makeText(this, "Opening ${assignment.title}", Toast.LENGTH_SHORT).show()
    }

    private fun showFilterDialog() {
        // Show filter options
        Toast.makeText(this, "Filter assignments", Toast.LENGTH_SHORT).show()
    }

    private fun showSortDialog() {
        // Show sort options
        Toast.makeText(this, "Sort assignments", Toast.LENGTH_SHORT).show()
    }
}