package com.example.educationalapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ed.R
import com.example.ed.utils.SecurityUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*

class MaterialUploadActivity : AppCompatActivity() {

    private lateinit var rgMaterialType: RadioGroup
    private lateinit var rbLectureVideo: RadioButton
    private lateinit var rbDocument: RadioButton
    private lateinit var rbImage: RadioButton
    private lateinit var rbAudio: RadioButton
    private lateinit var rbLink: RadioButton
    private lateinit var etMaterialTitle: TextInputEditText
    private lateinit var etMaterialDescription: TextInputEditText
    private lateinit var etExternalLink: TextInputEditText
    private lateinit var llFileSelection: LinearLayout
    private lateinit var tvUploadText: TextView
    private lateinit var tvFileInfo: TextView
    private lateinit var llSelectedFile: LinearLayout
    private lateinit var ivFileType: ImageView
    private lateinit var tvFileName: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var btnRemoveFile: ImageButton
    private lateinit var llUploadProgress: LinearLayout
    private lateinit var tvUploadPercentage: TextView
    private lateinit var progressUpload: ProgressBar
    private lateinit var spinnerModule: Spinner
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnUploadMaterial: MaterialButton

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    private var selectedFileSize: Long = 0
    private lateinit var courseId: String
    private var selectedModuleId: String = ""
    
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var storageRef: StorageReference

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_material_upload)

        // Security check - ensure only teachers can access
        lifecycleScope.launch {
            if (!SecurityUtils.canAccessTeacherFeatures()) {
                SecurityUtils.logSecurityEvent("UNAUTHORIZED_MATERIAL_UPLOAD_ACCESS", "User attempted to access material upload without teacher permissions")
                Toast.makeText(this@MaterialUploadActivity, "Access denied. Teacher permissions required.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
        }

        // Get course ID from intent
        courseId = intent.getStringExtra("COURSE_ID") ?: ""
        if (courseId.isEmpty()) {
            Toast.makeText(this, "Invalid course ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        storageRef = storage.reference

        initializeViews()
        setupToolbar()
        setupClickListeners()
        loadModules()
    }

    private fun initializeViews() {
        rgMaterialType = findViewById(R.id.rg_material_type)
        rbLectureVideo = findViewById(R.id.rb_lecture_video)
        rbDocument = findViewById(R.id.rb_document)
        rbImage = findViewById(R.id.rb_image)
        rbAudio = findViewById(R.id.rb_audio)
        rbLink = findViewById(R.id.rb_link)
        etMaterialTitle = findViewById(R.id.et_material_title)
        etMaterialDescription = findViewById(R.id.et_material_description)
        etExternalLink = findViewById(R.id.et_external_link)
        llFileSelection = findViewById(R.id.ll_file_selection)
        tvUploadText = findViewById(R.id.tv_upload_text)
        tvFileInfo = findViewById(R.id.tv_file_info)
        llSelectedFile = findViewById(R.id.ll_selected_file)
        ivFileType = findViewById(R.id.iv_file_type)
        tvFileName = findViewById(R.id.tv_file_name)
        tvFileSize = findViewById(R.id.tv_file_size)
        btnRemoveFile = findViewById(R.id.btn_remove_file)
        llUploadProgress = findViewById(R.id.ll_upload_progress)
        tvUploadPercentage = findViewById(R.id.tv_upload_percentage)
        progressUpload = findViewById(R.id.progress_upload)
        spinnerModule = findViewById(R.id.spinner_module)
        btnCancel = findViewById(R.id.btn_cancel)
        btnUploadMaterial = findViewById(R.id.btn_upload_material)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Upload Course Material"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupClickListeners() {
        // Material type selection
        rgMaterialType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_link -> {
                    findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_external_link).visibility = View.VISIBLE
                    findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_file_upload).visibility = View.GONE
                    updateFileInfoText("External link - no file needed")
                }
                else -> {
                    findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_external_link).visibility = View.GONE
                    findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_file_upload).visibility = View.VISIBLE
                    updateFileInfoForType(checkedId)
                }
            }
        }

        // File selection
        llFileSelection.setOnClickListener {
            if (rbLink.isChecked) return@setOnClickListener
            openFilePicker()
        }

        // Remove selected file
        btnRemoveFile.setOnClickListener {
            clearSelectedFile()
        }

        // Cancel button
        btnCancel.setOnClickListener {
            onBackPressed()
        }

        // Upload button
        btnUploadMaterial.setOnClickListener {
            lifecycleScope.launch {
                if (SecurityUtils.isOperationAllowed("UPLOAD_MATERIAL")) {
                    if (validateMaterialData()) {
                        uploadMaterial()
                    }
                } else {
                    Toast.makeText(this@MaterialUploadActivity, "Rate limit exceeded. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFileInfoForType(checkedId: Int) {
        when (checkedId) {
            R.id.rb_lecture_video -> {
                tvFileInfo.text = "Supported: MP4, AVI, MOV, WMV\nMax size: 500MB"
            }
            R.id.rb_document -> {
                tvFileInfo.text = "Supported: PDF, DOC, DOCX, PPT, PPTX\nMax size: 100MB"
            }
            R.id.rb_image -> {
                tvFileInfo.text = "Supported: JPG, PNG, GIF, SVG\nMax size: 50MB"
            }
            R.id.rb_audio -> {
                tvFileInfo.text = "Supported: MP3, WAV, AAC, M4A\nMax size: 100MB"
            }
        }
    }

    private fun updateFileInfoText(text: String) {
        tvFileInfo.text = text
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = when {
                rbLectureVideo.isChecked -> "video/*"
                rbDocument.isChecked -> "application/*"
                rbImage.isChecked -> "image/*"
                rbAudio.isChecked -> "audio/*"
                else -> "*/*"
            }
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select File"))
    }

    private fun handleSelectedFile(uri: Uri) {
        selectedFileUri = uri
        
        // Get file information
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            
            if (cursor.moveToFirst()) {
                selectedFileName = cursor.getString(nameIndex) ?: "Unknown"
                selectedFileSize = cursor.getLong(sizeIndex)
            }
        }

        // Validate file size
        if (!validateFileSize()) {
            clearSelectedFile()
            return
        }

        // Update UI
        displaySelectedFile()
    }

    private fun validateFileSize(): Boolean {
        val maxSizeBytes = when {
            rbLectureVideo.isChecked -> 500 * 1024 * 1024L // 500MB
            rbDocument.isChecked -> 100 * 1024 * 1024L // 100MB
            rbImage.isChecked -> 50 * 1024 * 1024L // 50MB
            rbAudio.isChecked -> 100 * 1024 * 1024L // 100MB
            else -> 100 * 1024 * 1024L // Default 100MB
        }

        if (selectedFileSize > maxSizeBytes) {
            val maxSizeMB = maxSizeBytes / (1024 * 1024)
            Toast.makeText(this, "File size exceeds maximum limit of ${maxSizeMB}MB", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun displaySelectedFile() {
        llSelectedFile.visibility = View.VISIBLE
        tvFileName.text = selectedFileName
        tvFileSize.text = formatFileSize(selectedFileSize)
        
        // Set appropriate file type icon
        val iconRes = when {
            selectedFileName.endsWith(".pdf", true) -> R.drawable.ic_pdf
            selectedFileName.endsWith(".doc", true) || selectedFileName.endsWith(".docx", true) -> R.drawable.ic_doc
            selectedFileName.endsWith(".ppt", true) || selectedFileName.endsWith(".pptx", true) -> R.drawable.ic_ppt
            selectedFileName.endsWith(".mp4", true) || selectedFileName.endsWith(".avi", true) -> R.drawable.ic_video
            selectedFileName.endsWith(".mp3", true) || selectedFileName.endsWith(".wav", true) -> R.drawable.ic_audio
            selectedFileName.endsWith(".jpg", true) || selectedFileName.endsWith(".png", true) -> R.drawable.ic_image
            else -> R.drawable.ic_file
        }
        ivFileType.setImageResource(iconRes)
    }

    private fun clearSelectedFile() {
        selectedFileUri = null
        selectedFileName = ""
        selectedFileSize = 0
        llSelectedFile.visibility = View.GONE
        llUploadProgress.visibility = View.GONE
    }

    private fun formatFileSize(bytes: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${df.format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
            else -> "${df.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    private fun loadModules() {
        firestore.collection("courses")
            .document(courseId)
            .collection("modules")
            .get()
            .addOnSuccessListener { documents ->
                val moduleList = mutableListOf<String>()
                val moduleIds = mutableListOf<String>()
                
                moduleList.add("General Materials")
                moduleIds.add("general")
                
                for (document in documents) {
                    val moduleName = document.getString("title") ?: "Unnamed Module"
                    moduleList.add(moduleName)
                    moduleIds.add(document.id)
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, moduleList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerModule.adapter = adapter
                
                spinnerModule.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedModuleId = moduleIds[position]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load modules: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateMaterialData(): Boolean {
        val title = etMaterialTitle.text.toString().trim()
        
        if (title.isEmpty()) {
            etMaterialTitle.error = "Material title is required"
            etMaterialTitle.requestFocus()
            return false
        }

        if (rbLink.isChecked) {
            val link = etExternalLink.text.toString().trim()
            if (link.isEmpty()) {
                etExternalLink.error = "External link is required"
                etExternalLink.requestFocus()
                return false
            }
            if (!android.util.Patterns.WEB_URL.matcher(link).matches()) {
                etExternalLink.error = "Please enter a valid URL"
                etExternalLink.requestFocus()
                return false
            }
        } else {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a file to upload", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private suspend fun uploadMaterial() {
        // Re-verify teacher permissions before uploading
        if (!SecurityUtils.canAccessTeacherFeatures()) {
            SecurityUtils.logSecurityEvent("UNAUTHORIZED_MATERIAL_UPLOAD", "User attempted to upload material without teacher permissions")
            Toast.makeText(this, "Access denied. Teacher permissions required.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val materialId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()

            // Sanitize input data
            val title = SecurityUtils.sanitizeInput(etMaterialTitle.text.toString().trim())
            val description = SecurityUtils.sanitizeInput(etMaterialDescription.text.toString().trim())

            val materialType = when {
                rbLectureVideo.isChecked -> "VIDEO"
                rbDocument.isChecked -> "DOCUMENT"
                rbImage.isChecked -> "IMAGE"
                rbAudio.isChecked -> "AUDIO"
                rbLink.isChecked -> "LINK"
                else -> "OTHER"
            }

            if (rbLink.isChecked) {
                // Handle external link
                val link = SecurityUtils.sanitizeInput(etExternalLink.text.toString().trim())
                saveMaterialToFirestore(materialId, title, description, materialType, link, "", currentTime)
            } else {
                // Handle file upload
                selectedFileUri?.let { uri ->
                    uploadFileToStorage(uri, materialId, title, description, materialType, currentTime)
                }
            }

        } catch (e: Exception) {
            SecurityUtils.logSecurityEvent(
                "MATERIAL_UPLOAD_ERROR",
                "Error during material upload - Error: ${e.message}"
            )
            Toast.makeText(this, "Error uploading material: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadFileToStorage(
        uri: Uri,
        materialId: String,
        title: String,
        description: String,
        materialType: String,
        currentTime: Long
    ) {
        llUploadProgress.visibility = View.VISIBLE
        btnUploadMaterial.isEnabled = false

        val fileName = "${materialId}_${selectedFileName}"
        val fileRef = storageRef.child("courses/$courseId/materials/$fileName")

        val uploadTask = fileRef.putFile(uri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progressUpload.progress = progress
            tvUploadPercentage.text = "$progress%"
        }.addOnSuccessListener { taskSnapshot ->
            // Get download URL
            fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                saveMaterialToFirestore(
                    materialId, title, description, materialType,
                    downloadUri.toString(), fileName, currentTime
                )
            }
        }.addOnFailureListener { exception ->
            llUploadProgress.visibility = View.GONE
            btnUploadMaterial.isEnabled = true
            
            SecurityUtils.logSecurityEvent(
                "MATERIAL_UPLOAD_FAILED",
                "Failed to upload material file - Error: ${exception.message}"
            )
            Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveMaterialToFirestore(
        materialId: String,
        title: String,
        description: String,
        materialType: String,
        url: String,
        fileName: String,
        currentTime: Long
    ) {
        val material = hashMapOf(
            "id" to materialId,
            "courseId" to courseId,
            "moduleId" to selectedModuleId,
            "title" to title,
            "description" to description,
            "type" to materialType,
            "url" to url,
            "fileName" to fileName,
            "fileSize" to selectedFileSize,
            "createdAt" to currentTime,
            "updatedAt" to currentTime,
            "createdBy" to auth.currentUser?.uid
        )

        firestore.collection("courses")
            .document(courseId)
            .collection("materials")
            .document(materialId)
            .set(material)
            .addOnSuccessListener {
                SecurityUtils.logSecurityEvent(
                    "MATERIAL_UPLOADED",
                    "Material uploaded successfully - ID: $materialId, Title: $title, Type: $materialType"
                )
                
                Toast.makeText(this, "Material uploaded successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                SecurityUtils.logSecurityEvent(
                    "MATERIAL_SAVE_FAILED",
                    "Failed to save material to Firestore - Error: ${exception.message}"
                )
                Toast.makeText(this, "Failed to save material: ${exception.message}", Toast.LENGTH_LONG).show()
                
                llUploadProgress.visibility = View.GONE
                btnUploadMaterial.isEnabled = true
            }
    }

    override fun onBackPressed() {
        if (llUploadProgress.visibility == View.VISIBLE) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Upload in Progress")
                .setMessage("Upload is in progress. Are you sure you want to cancel?")
                .setPositiveButton("Yes, Cancel") { _, _ -> super.onBackPressed() }
                .setNegativeButton("Continue Upload", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}