package com.example.ed.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.ed.R
import com.example.ed.models.ContentItem
import com.example.ed.models.ContentType
import com.example.ed.models.ContentAttachment
import com.example.ed.services.AIContentService
import java.util.*

class AddContentItemActivity : AppCompatActivity() {

    // UI Components
    private lateinit var spinnerContentType: Spinner
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etDuration: TextInputEditText
    private lateinit var switchRequired: Switch
    private lateinit var chipGroupTags: ChipGroup
    
    // Content Type Specific Views
    private lateinit var cardTextContent: CardView
    private lateinit var etTextContent: EditText
    private lateinit var btnGenerateWithAI: MaterialButton
    
    private lateinit var cardMediaContent: CardView
    private lateinit var tvSelectedFile: TextView
    private lateinit var btnSelectFile: MaterialButton
    private lateinit var btnUploadFile: MaterialButton
    private lateinit var progressUpload: ProgressBar
    
    private lateinit var cardVideoContent: CardView
    private lateinit var etVideoUrl: TextInputEditText
    private lateinit var btnSelectVideo: MaterialButton
    
    private lateinit var cardInteractiveContent: CardView
    private lateinit var etInteractiveUrl: TextInputEditText
    private lateinit var etEmbedCode: EditText
    
    private lateinit var btnSaveContent: MaterialButton
    private lateinit var btnCancel: MaterialButton
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    
    // AI Service
    private lateinit var aiService: AIContentService
    
    // Data
    private var selectedFileUri: Uri? = null
    private var uploadedFileUrl: String = ""
    private var contentAttachments = mutableListOf<ContentAttachment>()
    private var isEditMode = false
    private var editingContentItem: ContentItem? = null
    
    companion object {
        private const val REQUEST_FILE_SELECT = 1001
        private const val REQUEST_VIDEO_SELECT = 1002
        private const val REQUEST_IMAGE_SELECT = 1003
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_content_item)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        
        // Initialize AI Service
        aiService = AIContentService()
        
        // Check if editing
        editingContentItem = intent.getParcelableExtra<ContentItem>("content_item")
        isEditMode = editingContentItem != null
        
        initializeViews()
        setupSpinner()
        setupClickListeners()
        
        if (isEditMode) {
            populateFields()
        }
    }
    
    private fun initializeViews() {
        spinnerContentType = findViewById(R.id.spinner_content_type)
        etTitle = findViewById(R.id.et_title)
        etDescription = findViewById(R.id.et_description)
        etDuration = findViewById(R.id.et_duration)
        switchRequired = findViewById(R.id.switch_required)
        chipGroupTags = findViewById(R.id.chip_group_tags)
        
        // Text Content
        cardTextContent = findViewById(R.id.card_text_content)
        etTextContent = findViewById(R.id.et_text_content)
        btnGenerateWithAI = findViewById(R.id.btn_generate_with_ai)
        
        // Media Content
        cardMediaContent = findViewById(R.id.card_media_content)
        tvSelectedFile = findViewById(R.id.tv_selected_file)
        btnSelectFile = findViewById(R.id.btn_select_file)
        btnUploadFile = findViewById(R.id.btn_upload_file)
        progressUpload = findViewById(R.id.progress_upload)
        
        // Video Content
        cardVideoContent = findViewById(R.id.card_video_content)
        etVideoUrl = findViewById(R.id.et_video_url)
        btnSelectVideo = findViewById(R.id.btn_select_video)
        
        // Interactive Content
        cardInteractiveContent = findViewById(R.id.card_interactive_content)
        etInteractiveUrl = findViewById(R.id.et_interactive_url)
        etEmbedCode = findViewById(R.id.et_embed_code)
        
        btnSaveContent = findViewById(R.id.btn_save_content)
        btnCancel = findViewById(R.id.btn_cancel)
        
        // Set title
        title = if (isEditMode) "Edit Content Item" else "Add Content Item"
    }
    
    private fun setupSpinner() {
        val contentTypes = ContentType.values().map { it.name.replace("_", " ") }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, contentTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerContentType.adapter = adapter
        
        spinnerContentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateContentTypeViews(ContentType.values()[position])
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun updateContentTypeViews(contentType: ContentType) {
        // Hide all content type specific views
        cardTextContent.visibility = View.GONE
        cardMediaContent.visibility = View.GONE
        cardVideoContent.visibility = View.GONE
        cardInteractiveContent.visibility = View.GONE
        
        // Show relevant views based on content type
        when (contentType) {
            ContentType.TEXT -> {
                cardTextContent.visibility = View.VISIBLE
            }
            ContentType.DOCUMENT, ContentType.PRESENTATION -> {
                cardMediaContent.visibility = View.VISIBLE
                btnSelectFile.text = "Select ${contentType.name.lowercase().capitalize()}"
            }
            ContentType.VIDEO -> {
                cardVideoContent.visibility = View.VISIBLE
            }
            ContentType.AUDIO -> {
                cardMediaContent.visibility = View.VISIBLE
                btnSelectFile.text = "Select Audio File"
            }
            ContentType.INTERACTIVE, ContentType.SIMULATION -> {
                cardInteractiveContent.visibility = View.VISIBLE
            }
            ContentType.LIVE_SESSION -> {
                // Handle live session separately
                Toast.makeText(this, "Live sessions are created separately", Toast.LENGTH_SHORT).show()
            }
            ContentType.DISCUSSION -> {
                cardTextContent.visibility = View.VISIBLE
                etTextContent.hint = "Enter discussion prompt or topic..."
            }
            ContentType.CASE_STUDY -> {
                cardTextContent.visibility = View.VISIBLE
                etTextContent.hint = "Enter case study details..."
            }
        }
    }
    
    private fun setupClickListeners() {
        btnGenerateWithAI.setOnClickListener {
            generateContentWithAI()
        }
        
        btnSelectFile.setOnClickListener {
            selectFile()
        }
        
        btnUploadFile.setOnClickListener {
            uploadFile()
        }
        
        btnSelectVideo.setOnClickListener {
            selectVideo()
        }
        
        btnSaveContent.setOnClickListener {
            saveContentItem()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
        
        // Add sample tags
        addTag("Lecture")
        addTag("Reading")
        addTag("Practice")
        addTag("Assignment")
        addTag("Resource")
    }
    
    private fun addTag(tagName: String) {
        val chip = Chip(this)
        chip.text = tagName
        chip.isCheckable = true
        chipGroupTags.addView(chip)
    }
    
    private fun generateContentWithAI() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        
        if (title.isEmpty()) {
            etTitle.error = "Please enter a title first"
            return
        }
        
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Generating content with AI...")
        progressDialog.show()
        
        // Generate content based on title and description
        val prompt = "Create educational content for: $title\n${if (description.isNotEmpty()) "Description: $description" else ""}"
        
        aiService.generateContent(prompt) { generatedContent ->
            runOnUiThread {
                progressDialog.dismiss()
                if (generatedContent != null) {
                    etTextContent.setText(generatedContent)
                    Toast.makeText(this, "Content generated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to generate content", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun selectFile() {
        val contentType = ContentType.values()[spinnerContentType.selectedItemPosition]
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        
        when (contentType) {
            ContentType.DOCUMENT -> {
                intent.type = "application/pdf"
            }
            ContentType.PRESENTATION -> {
                intent.type = "application/vnd.ms-powerpoint"
                intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                ))
            }
            ContentType.AUDIO -> {
                intent.type = "audio/*"
            }
            else -> {
                intent.type = "*/*"
            }
        }
        
        startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_FILE_SELECT)
    }
    
    private fun selectVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_VIDEO_SELECT)
    }
    
    private fun uploadFile() {
        val fileUri = selectedFileUri ?: return
        
        progressUpload.visibility = View.VISIBLE
        btnUploadFile.isEnabled = false
        
        val fileName = "content_${UUID.randomUUID()}_${System.currentTimeMillis()}"
        val storageRef = storage.reference.child("content_items/$fileName")
        
        storageRef.putFile(fileUri)
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                progressUpload.progress = progress
            }
            .addOnSuccessListener { taskSnapshot ->
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    uploadedFileUrl = uri.toString()
                    progressUpload.visibility = View.GONE
                    btnUploadFile.isEnabled = true
                    tvSelectedFile.text = "File uploaded successfully!"
                    Toast.makeText(this, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Add to attachments
                    val attachment = ContentAttachment(
                        id = UUID.randomUUID().toString(),
                        name = fileName,
                        url = uploadedFileUrl,
                        type = getFileType(fileUri),
                        size = taskSnapshot.totalByteCount
                    )
                    contentAttachments.add(attachment)
                }
            }
            .addOnFailureListener { exception ->
                progressUpload.visibility = View.GONE
                btnUploadFile.isEnabled = true
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun getFileType(uri: Uri): String {
        return contentResolver.getType(uri) ?: "application/octet-stream"
    }
    
    private fun saveContentItem() {
        if (!validateInput()) return
        
        val contentType = ContentType.values()[spinnerContentType.selectedItemPosition]
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val duration = etDuration.text.toString().toIntOrNull() ?: 0
        val isRequired = switchRequired.isChecked
        
        // Get content based on type
        var content = ""
        var mediaUrl = ""
        
        when (contentType) {
            ContentType.TEXT, ContentType.DISCUSSION, ContentType.CASE_STUDY -> {
                content = etTextContent.text.toString().trim()
            }
            ContentType.DOCUMENT, ContentType.PRESENTATION, ContentType.AUDIO -> {
                mediaUrl = uploadedFileUrl
            }
            ContentType.VIDEO -> {
                mediaUrl = etVideoUrl.text.toString().trim()
                if (mediaUrl.isEmpty() && uploadedFileUrl.isNotEmpty()) {
                    mediaUrl = uploadedFileUrl
                }
            }
            ContentType.INTERACTIVE, ContentType.SIMULATION -> {
                mediaUrl = etInteractiveUrl.text.toString().trim()
                content = etEmbedCode.text.toString().trim()
            }
            else -> {}
        }
        
        // Get selected tags
        val selectedTags = mutableListOf<String>()
        for (i in 0 until chipGroupTags.childCount) {
            val chip = chipGroupTags.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedTags.add(chip.text.toString())
            }
        }
        
        val contentItem = ContentItem(
            id = editingContentItem?.id ?: UUID.randomUUID().toString(),
            type = contentType,
            title = title,
            content = content,
            mediaUrl = mediaUrl,
            duration = duration,
            order = intent.getIntExtra("order", 0),
            isRequired = isRequired,
            aiGenerated = false,
            attachments = contentAttachments,
            createdAt = editingContentItem?.createdAt ?: System.currentTimeMillis()
        )
        
        // Return result
        val resultIntent = Intent()
        resultIntent.putExtra("content_item", contentItem as android.os.Parcelable)
        resultIntent.putExtra("tags", selectedTags.toTypedArray())
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
    
    private fun validateInput(): Boolean {
        if (etTitle.text.toString().trim().isEmpty()) {
            etTitle.error = "Title is required"
            return false
        }
        
        val contentType = ContentType.values()[spinnerContentType.selectedItemPosition]
        
        when (contentType) {
            ContentType.TEXT, ContentType.DISCUSSION, ContentType.CASE_STUDY -> {
                if (etTextContent.text.toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please enter content", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            ContentType.DOCUMENT, ContentType.PRESENTATION, ContentType.AUDIO -> {
                if (uploadedFileUrl.isEmpty()) {
                    Toast.makeText(this, "Please upload a file", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            ContentType.VIDEO -> {
                if (etVideoUrl.text.toString().trim().isEmpty() && uploadedFileUrl.isEmpty()) {
                    Toast.makeText(this, "Please provide a video URL or upload a video", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            ContentType.INTERACTIVE, ContentType.SIMULATION -> {
                if (etInteractiveUrl.text.toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please provide an interactive content URL", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            else -> {}
        }
        
        return true
    }
    
    private fun populateFields() {
        editingContentItem?.let { item ->
            // Set content type
            val typeIndex = ContentType.values().indexOf(item.type)
            spinnerContentType.setSelection(typeIndex)
            
            // Set basic fields
            etTitle.setText(item.title)
            etDuration.setText(item.duration.toString())
            switchRequired.isChecked = item.isRequired
            
            // Set content based on type
            when (item.type) {
                ContentType.TEXT, ContentType.DISCUSSION, ContentType.CASE_STUDY -> {
                    etTextContent.setText(item.content)
                }
                ContentType.VIDEO -> {
                    etVideoUrl.setText(item.mediaUrl)
                }
                ContentType.INTERACTIVE, ContentType.SIMULATION -> {
                    etInteractiveUrl.setText(item.mediaUrl)
                    etEmbedCode.setText(item.content)
                }
                else -> {
                    uploadedFileUrl = item.mediaUrl
                    tvSelectedFile.text = "File already uploaded"
                }
            }
            
            // Set attachments
            contentAttachments.clear()
            contentAttachments.addAll(item.attachments)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_FILE_SELECT, REQUEST_VIDEO_SELECT, REQUEST_IMAGE_SELECT -> {
                    selectedFileUri = data.data
                    selectedFileUri?.let { uri ->
                        val fileName = getFileName(uri)
                        tvSelectedFile.text = "Selected: $fileName"
                        btnUploadFile.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
    
    private fun getFileName(uri: Uri): String {
        var fileName = "Unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}
