# Firebase Data Seeding Guide

## Overview
This guide explains how to use the Firebase Data Seeding feature in the Ed application to populate your Firestore database with sample data for development and testing purposes.

## Features
- **Dynamic Data Generation**: Creates realistic sample data without hardcoded values
- **Comprehensive Database Structure**: Populates all major collections with related data
- **Batch Operations**: Efficient data insertion using Firestore batch operations
- **Error Handling**: Robust error handling with detailed logging
- **Data Cleanup**: Ability to remove sample data when no longer needed

## Database Structure

The seeder creates data across the following Firestore collections:

### 1. Users Collection (`users`)
- **Teachers**: 6 sample teacher accounts
- **Students**: 10 sample student accounts
- **Fields**: 
  - `userId`: Unique identifier (teacher_X or student_X)
  - `name`: Randomly selected from predefined lists
  - `email`: Generated based on name
  - `role`: "teacher" or "student"
  - `createdAt`: Current timestamp
  - `isActive`: true
  - `profilePicture`: Default placeholder URL

### 2. Courses Collection (`courses`)
- **Count**: 15-20 courses across 10 categories
- **Categories**: Mathematics, Science, English, History, Geography, Computer Science, Physics, Chemistry, Biology, Art
- **Fields**:
  - `courseId`: Unique identifier (course_X)
  - `title`: Category-specific course titles
  - `description`: Generated description
  - `category`: Course category
  - `teacherId`: Reference to a teacher
  - `teacherName`: Teacher's name
  - `price`: Random price between $50-$500
  - `duration`: Random duration (4-16 weeks)
  - `level`: "Beginner", "Intermediate", or "Advanced"
  - `enrollmentCount`: Random count (10-150)
  - `rating`: Random rating (3.5-5.0)
  - `createdAt`: Current timestamp
  - `isActive`: true
  - `thumbnail`: Placeholder image URL

### 3. Enrollments Collection (`enrollments`)
- **Count**: 25-35 enrollments
- **Fields**:
  - `enrollmentId`: Unique identifier (enrollment_X)
  - `studentId`: Reference to a student
  - `courseId`: Reference to a course
  - `enrolledAt`: Random date within last 6 months
  - `status`: "active", "completed", or "paused"
  - `progress`: Random progress percentage
  - `lastAccessedAt`: Recent timestamp

### 4. Assignments Collection (`assignments`)
- **Count**: 30-40 assignments
- **Fields**:
  - `assignmentId`: Unique identifier (assignment_X)
  - `courseId`: Reference to a course
  - `title`: Generated assignment titles
  - `description`: Assignment description
  - `dueDate`: Future date
  - `maxScore`: Random score (50-100)
  - `createdAt`: Current timestamp
  - `isActive`: true

### 5. Analytics Collection (`analytics`)
- **Count**: One record per student
- **Fields**:
  - `analyticsId`: Unique identifier (analytics_X)
  - `userId`: Reference to a student
  - `enrolledCourses`: Count of enrolled courses
  - `completedCourses`: Count of completed courses
  - `averageGrade`: Random grade (60-95)
  - `studyStreak`: Random streak (0-30 days)
  - `totalStudyTime`: Random time in minutes
  - `lastUpdated`: Current timestamp

### 6. Materials Collection (`materials`)
- **Count**: 40-50 materials
- **Fields**:
  - `materialId`: Unique identifier (material_X)
  - `courseId`: Reference to a course
  - `title`: Generated material titles
  - `type`: "video", "document", "quiz", or "assignment"
  - `url`: Placeholder URL
  - `duration`: Random duration for videos
  - `uploadedAt`: Current timestamp
  - `isActive`: true

## How to Use the Data Seeder

### Accessing the Feature
1. Open the **Student Dashboard** in the Ed application
2. **Double-tap** the profile picture in the top-left corner
3. A dialog will appear with seeding options

### Seeding Options
- **Seed Database**: Populates the database with fresh sample data
- **Clear Sample Data**: Removes all previously seeded sample data
- **Cancel**: Closes the dialog without any action

### Seeding Process
1. Select "Seed Database" from the dialog
2. The app will show a progress toast: "Seeding database... This may take a moment."
3. The seeder will create data in batches for optimal performance
4. Upon completion, you'll see a success message with the total number of records created
5. The dashboard will automatically refresh to show the new data

### Clearing Sample Data
1. Select "Clear Sample Data" from the dialog
2. The app will remove all documents with sample data identifiers
3. A confirmation message will show the number of records deleted
4. The dashboard will refresh to reflect the changes

## Verifying Data in Firebase Console

### Accessing Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Sign in with your Google account
3. Select your project from the project list

### Navigating to Firestore Database
1. In the left sidebar, click on **"Firestore Database"**
2. If prompted, select **"Cloud Firestore"** (not Realtime Database)
3. You'll see the main Firestore interface with your collections

### Inspecting the Data

#### Collection View
- You'll see all collections listed on the left side
- Sample data collections will include: `users`, `courses`, `enrollments`, `assignments`, `analytics`, `materials`
- Click on any collection name to view its documents

#### Document Identification
Sample data documents can be identified by their IDs:
- **Users**: `teacher_1`, `teacher_2`, ..., `student_1`, `student_2`, ...
- **Courses**: `course_1`, `course_2`, `course_3`, ...
- **Enrollments**: `enrollment_1`, `enrollment_2`, ...
- **Assignments**: `assignment_1`, `assignment_2`, ...
- **Analytics**: `analytics_1`, `analytics_2`, ...
- **Materials**: `material_1`, `material_2`, ...

#### Document Details
1. Click on any document ID to view its fields and values
2. You can see all the data fields populated with realistic sample values
3. Timestamps will show when the data was created
4. Related fields (like `teacherId` in courses) will reference other sample documents

#### Filtering and Searching
- Use the **Filter** option to search for specific documents
- You can filter by field values or document IDs
- Example: Filter by `role == "teacher"` in the users collection

### Data Relationships
The seeded data maintains proper relationships:
- **Courses** are assigned to **Teachers**
- **Students** are enrolled in **Courses** (via Enrollments)
- **Assignments** belong to **Courses**
- **Analytics** track **Student** progress
- **Materials** are associated with **Courses**

## Best Practices

### Security Rules
Ensure your Firestore security rules allow read/write access for authenticated users:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Data Organization
- Sample data uses consistent naming conventions
- All timestamps are in Firestore Timestamp format
- Numeric values are appropriate for their context
- String fields use realistic, varied content

### Performance Considerations
- The seeder uses batch operations (max 450 operations per batch)
- Data creation is done asynchronously to avoid blocking the UI
- Error handling ensures partial failures don't corrupt the database

### Development vs Production
- This feature is intended for development and testing only
- Consider removing or restricting access in production builds
- Sample data should be cleared before deploying to production

## Troubleshooting

### Common Issues
1. **Permission Denied**: Check Firebase security rules
2. **Network Errors**: Ensure internet connectivity
3. **Quota Exceeded**: Check Firebase usage limits
4. **Build Errors**: Ensure all dependencies are properly configured

### Logging
- All operations are logged with tag "FirebaseDataSeeder"
- Check Android Studio Logcat for detailed information
- Success and error messages are displayed via Toast notifications

### Data Verification
- Always verify data in Firebase Console after seeding
- Check that relationships between collections are maintained
- Ensure all expected collections and documents are created

## Support
For issues or questions regarding the Firebase Data Seeding feature, check:
1. Android Studio Logcat for detailed error messages
2. Firebase Console for data verification
3. Network connectivity and Firebase project configuration