package com.example.ed.utils

import android.util.Patterns
import java.util.regex.Pattern

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

object DataValidator {
    
    // Email validation
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, "Email cannot be empty")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                ValidationResult(false, "Please enter a valid email address")
            else -> ValidationResult(true)
        }
    }
    
    // Password validation
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.length < 8 -> ValidationResult(false, "Password must be at least 8 characters long")
            !password.any { it.isUpperCase() } -> 
                ValidationResult(false, "Password must contain at least one uppercase letter")
            !password.any { it.isLowerCase() } -> 
                ValidationResult(false, "Password must contain at least one lowercase letter")
            !password.any { it.isDigit() } -> 
                ValidationResult(false, "Password must contain at least one number")
            else -> ValidationResult(true)
        }
    }
    
    // Course validation
    fun validateCourse(
        title: String,
        description: String,
        price: Double,
        instructorId: String
    ): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult(false, "Course title cannot be empty")
            title.length < 3 -> ValidationResult(false, "Course title must be at least 3 characters long")
            title.length > 100 -> ValidationResult(false, "Course title cannot exceed 100 characters")
            description.isBlank() -> ValidationResult(false, "Course description cannot be empty")
            description.length < 10 -> ValidationResult(false, "Course description must be at least 10 characters long")
            description.length > 1000 -> ValidationResult(false, "Course description cannot exceed 1000 characters")
            price < 0 -> ValidationResult(false, "Course price cannot be negative")
            price > 10000 -> ValidationResult(false, "Course price cannot exceed $10,000")
            instructorId.isBlank() -> ValidationResult(false, "Instructor ID cannot be empty")
            else -> ValidationResult(true)
        }
    }
    
    // Assignment validation
    fun validateAssignment(
        title: String,
        description: String,
        dueDate: Long,
        maxPoints: Int,
        courseId: String
    ): ValidationResult {
        val currentTime = System.currentTimeMillis()
        return when {
            title.isBlank() -> ValidationResult(false, "Assignment title cannot be empty")
            title.length < 3 -> ValidationResult(false, "Assignment title must be at least 3 characters long")
            title.length > 100 -> ValidationResult(false, "Assignment title cannot exceed 100 characters")
            description.isBlank() -> ValidationResult(false, "Assignment description cannot be empty")
            description.length < 10 -> ValidationResult(false, "Assignment description must be at least 10 characters long")
            dueDate <= currentTime -> ValidationResult(false, "Due date must be in the future")
            maxPoints <= 0 -> ValidationResult(false, "Maximum points must be greater than 0")
            maxPoints > 1000 -> ValidationResult(false, "Maximum points cannot exceed 1000")
            courseId.isBlank() -> ValidationResult(false, "Course ID cannot be empty")
            else -> ValidationResult(true)
        }
    }
    
    // User profile validation
    fun validateUserProfile(
        firstName: String,
        lastName: String,
        phoneNumber: String? = null
    ): ValidationResult {
        return when {
            firstName.isBlank() -> ValidationResult(false, "First name cannot be empty")
            firstName.length < 2 -> ValidationResult(false, "First name must be at least 2 characters long")
            firstName.length > 50 -> ValidationResult(false, "First name cannot exceed 50 characters")
            !firstName.all { it.isLetter() || it.isWhitespace() } -> 
                ValidationResult(false, "First name can only contain letters and spaces")
            lastName.isBlank() -> ValidationResult(false, "Last name cannot be empty")
            lastName.length < 2 -> ValidationResult(false, "Last name must be at least 2 characters long")
            lastName.length > 50 -> ValidationResult(false, "Last name cannot exceed 50 characters")
            !lastName.all { it.isLetter() || it.isWhitespace() } -> 
                ValidationResult(false, "Last name can only contain letters and spaces")
            phoneNumber != null && !validatePhoneNumber(phoneNumber).isValid -> 
                validatePhoneNumber(phoneNumber)
            else -> ValidationResult(true)
        }
    }
    
    // Phone number validation
    fun validatePhoneNumber(phoneNumber: String): ValidationResult {
        val phonePattern = Pattern.compile("^[+]?[1-9]\\d{1,14}$")
        return when {
            phoneNumber.isBlank() -> ValidationResult(false, "Phone number cannot be empty")
            !phonePattern.matcher(phoneNumber.replace("\\s".toRegex(), "")).matches() -> 
                ValidationResult(false, "Please enter a valid phone number")
            else -> ValidationResult(true)
        }
    }
    
    // Grade validation
    fun validateGrade(score: Double, maxPoints: Double): ValidationResult {
        return when {
            score < 0 -> ValidationResult(false, "Score cannot be negative")
            score > maxPoints -> ValidationResult(false, "Score cannot exceed maximum points")
            maxPoints <= 0 -> ValidationResult(false, "Maximum points must be greater than 0")
            else -> ValidationResult(true)
        }
    }
    
    // Announcement validation
    fun validateAnnouncement(
        title: String,
        content: String,
        courseId: String
    ): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult(false, "Announcement title cannot be empty")
            title.length < 3 -> ValidationResult(false, "Announcement title must be at least 3 characters long")
            title.length > 100 -> ValidationResult(false, "Announcement title cannot exceed 100 characters")
            content.isBlank() -> ValidationResult(false, "Announcement content cannot be empty")
            content.length < 10 -> ValidationResult(false, "Announcement content must be at least 10 characters long")
            content.length > 2000 -> ValidationResult(false, "Announcement content cannot exceed 2000 characters")
            courseId.isBlank() -> ValidationResult(false, "Course ID cannot be empty")
            else -> ValidationResult(true)
        }
    }
    
    // Payment validation
    fun validatePayment(
        amount: Double,
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        cardholderName: String
    ): ValidationResult {
        return when {
            amount <= 0 -> ValidationResult(false, "Payment amount must be greater than 0")
            amount > 10000 -> ValidationResult(false, "Payment amount cannot exceed $10,000")
            !validateCardNumber(cardNumber).isValid -> validateCardNumber(cardNumber)
            !validateExpiryDate(expiryDate).isValid -> validateExpiryDate(expiryDate)
            !validateCVV(cvv).isValid -> validateCVV(cvv)
            !validateCardholderName(cardholderName).isValid -> validateCardholderName(cardholderName)
            else -> ValidationResult(true)
        }
    }
    
    private fun validateCardNumber(cardNumber: String): ValidationResult {
        val cleanCardNumber = cardNumber.replace("\\s".toRegex(), "")
        return when {
            cleanCardNumber.isBlank() -> ValidationResult(false, "Card number cannot be empty")
            !cleanCardNumber.all { it.isDigit() } -> ValidationResult(false, "Card number can only contain digits")
            cleanCardNumber.length < 13 || cleanCardNumber.length > 19 -> 
                ValidationResult(false, "Card number must be between 13 and 19 digits")
            !isValidLuhn(cleanCardNumber) -> ValidationResult(false, "Invalid card number")
            else -> ValidationResult(true)
        }
    }
    
    private fun validateExpiryDate(expiryDate: String): ValidationResult {
        val expiryPattern = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$")
        return when {
            expiryDate.isBlank() -> ValidationResult(false, "Expiry date cannot be empty")
            !expiryPattern.matcher(expiryDate).matches() -> 
                ValidationResult(false, "Expiry date must be in MM/YY format")
            else -> {
                val parts = expiryDate.split("/")
                val month = parts[0].toInt()
                val year = 2000 + parts[1].toInt()
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
                
                when {
                    year < currentYear -> ValidationResult(false, "Card has expired")
                    year == currentYear && month < currentMonth -> ValidationResult(false, "Card has expired")
                    else -> ValidationResult(true)
                }
            }
        }
    }
    
    private fun validateCVV(cvv: String): ValidationResult {
        return when {
            cvv.isBlank() -> ValidationResult(false, "CVV cannot be empty")
            !cvv.all { it.isDigit() } -> ValidationResult(false, "CVV can only contain digits")
            cvv.length < 3 || cvv.length > 4 -> ValidationResult(false, "CVV must be 3 or 4 digits")
            else -> ValidationResult(true)
        }
    }
    
    private fun validateCardholderName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Cardholder name cannot be empty")
            name.length < 2 -> ValidationResult(false, "Cardholder name must be at least 2 characters long")
            name.length > 50 -> ValidationResult(false, "Cardholder name cannot exceed 50 characters")
            !name.all { it.isLetter() || it.isWhitespace() } -> 
                ValidationResult(false, "Cardholder name can only contain letters and spaces")
            else -> ValidationResult(true)
        }
    }
    
    // Luhn algorithm for credit card validation
    private fun isValidLuhn(cardNumber: String): Boolean {
        var sum = 0
        var alternate = false
        
        for (i in cardNumber.length - 1 downTo 0) {
            var n = cardNumber[i].toString().toInt()
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
    
    // File validation
    fun validateImageFile(fileName: String, fileSize: Long): ValidationResult {
        val allowedExtensions = listOf("jpg", "jpeg", "png", "gif", "webp")
        val maxFileSize = 5 * 1024 * 1024 // 5MB
        
        return when {
            fileName.isBlank() -> ValidationResult(false, "File name cannot be empty")
            !allowedExtensions.any { fileName.lowercase().endsWith(".$it") } -> 
                ValidationResult(false, "Only JPG, PNG, GIF, and WebP files are allowed")
            fileSize > maxFileSize -> ValidationResult(false, "File size cannot exceed 5MB")
            fileSize <= 0 -> ValidationResult(false, "Invalid file size")
            else -> ValidationResult(true)
        }
    }
    
    // URL validation
    fun validateUrl(url: String): ValidationResult {
        return when {
            url.isBlank() -> ValidationResult(false, "URL cannot be empty")
            !Patterns.WEB_URL.matcher(url).matches() -> 
                ValidationResult(false, "Please enter a valid URL")
            else -> ValidationResult(true)
        }
    }
    
    // Generic text validation
    fun validateText(
        text: String,
        fieldName: String,
        minLength: Int = 1,
        maxLength: Int = 255,
        allowEmpty: Boolean = false
    ): ValidationResult {
        return when {
            !allowEmpty && text.isBlank() -> ValidationResult(false, "$fieldName cannot be empty")
            text.length < minLength -> ValidationResult(false, "$fieldName must be at least $minLength characters long")
            text.length > maxLength -> ValidationResult(false, "$fieldName cannot exceed $maxLength characters")
            else -> ValidationResult(true)
        }
    }
}