# EduFlex Sample Accounts - Login Details

## 📋 Overview
This document contains the login credentials for all sample accounts created in the EduFlex Firebase database. All accounts have been successfully created and verified in both Firebase Authentication and Firestore database.

**Creation Date:** September 24, 2025  
**Status:** ✅ All accounts verified and functional  
**Database:** Firebase Firestore + Authentication  

---

## 🔐 Login Credentials

### 1. 👑 ADMIN ACCOUNT
- **Email:** `admin@eduflex.com`
- **Password:** `admin123`
- **Role:** Admin
- **Full Name:** System Administrator
- **UID:** `rCweJa5jlhTGVQkHqBF9b0W6dCz2`
- **Permissions:** Full system access, user management, course management, analytics
- **Department:** Administration

**Features Access:**
- ✅ User Management (Create, Edit, Delete users)
- ✅ Course Management (All courses)
- ✅ System Analytics & Reports
- ✅ Platform Settings & Configuration
- ✅ Database Management

---

### 2. 🎓 STUDENT ACCOUNT
- **Email:** `student@eduflex.com`
- **Password:** `student123`
- **Role:** Student
- **Full Name:** John Student
- **UID:** `aY6L9gLYUMcncIQNlRYItUB1UVH3`
- **Student ID:** STU2024001
- **Grade:** 12th Grade
- **Guardian:** Jane Student (+1-555-0003)

**Features Access:**
- ✅ Course Enrollment & Learning
- ✅ Assignment Submission
- ✅ Progress Tracking
- ✅ Student Dashboard
- ✅ Learning Materials Access

---

### 3. 🧑‍🏫 TEACHER ACCOUNT
- **Email:** `teacher@eduflex.com`
- **Password:** `teacher123`
- **Role:** Teacher
- **Full Name:** Sarah Teacher
- **UID:** `VItzt489oEetMwhxGTJ7ID33jw73`
- **Employee ID:** TCH2024001
- **Specialization:** Computer Science & Mathematics
- **Experience:** 8 years
- **Department:** STEM Education

**Features Access:**
- ✅ Course Creation & Management
- ✅ Student Enrollment Management
- ✅ Assignment Creation & Grading
- ✅ Student Progress Monitoring
- ✅ Teaching Materials Upload

---

## 🚀 How to Use These Accounts

### For Testing the EduFlex Mobile App:
1. Open the EduFlex app on your Android device/emulator
2. Choose any of the accounts above
3. Enter the email and password
4. Sign in and explore role-specific features

### For Firebase Console Access:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your EduFlex project (`eduflex-f62b5`)
3. Navigate to Authentication > Users to see all accounts
4. Navigate to Firestore Database > users collection to see profiles

---

## 📊 Database Structure

Each account has been saved with the following structure in Firestore:

```
users/{uid}/
├── id: string (Firebase UID)
├── fullName: string
├── email: string
├── role: string (Admin/Student/Teacher)
├── isActive: boolean
├── createdAt: timestamp
├── lastLoginAt: timestamp
├── profilePicture: string (empty initially)
├── bio: string
└── [role-specific fields...]
```

### Role-Specific Fields:

**Admin:**
- `department`: "Administration"
- `permissions`: Array of permission strings
- `phoneNumber`: Contact information
- `address`: Office address

**Student:**
- `grade`: "12th Grade"
- `studentId`: "STU2024001"
- `enrollmentDate`: Timestamp
- `guardianName`: Parent/Guardian name
- `guardianPhone`: Emergency contact

**Teacher:**
- `specialization`: Subject expertise
- `yearsOfExperience`: Teaching experience
- `employeeId`: "TCH2024001"
- `qualifications`: Array of degrees/certifications

---

## ✅ Verification Status

All accounts have been verified for:
- ✅ Firebase Authentication (Sign-in working)
- ✅ Firestore Profile (Data saved correctly)
- ✅ Role-based permissions
- ✅ Complete profile information

**Last Verified:** September 24, 2025 at 3:17 PM

---

## 🔒 Security Notes

- These are **sample/demo accounts** for development and testing
- Passwords are simple for testing purposes
- In production, implement proper password policies
- Consider enabling two-factor authentication
- Regularly rotate credentials for production systems

---

## 📞 Support

If you encounter any issues with these accounts:
1. Check Firebase Console for account status
2. Verify Firestore security rules
3. Ensure proper Firebase configuration
4. Run the verification script: `node verify_accounts.js`

**Created by:** EduFlex Development Team  
**Last Updated:** September 24, 2025