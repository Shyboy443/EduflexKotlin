const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin SDK for emulator
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

// Connect to Firestore emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
const db = admin.firestore();

async function verifyBackupFunctionality() {
  try {
    console.log('🔍 Verifying Database Backup Functionality...\n');

    // Check if backup files exist in the expected location
    const backupDir = path.join(__dirname, 'backups');
    console.log('📁 Checking for backup files...');
    
    if (fs.existsSync(backupDir)) {
      const files = fs.readdirSync(backupDir);
      const backupFiles = files.filter(file => file.endsWith('.json'));
      
      console.log(`   ✅ Found ${backupFiles.length} backup files:`);
      backupFiles.forEach(file => {
        const filePath = path.join(backupDir, file);
        const stats = fs.statSync(filePath);
        console.log(`      📄 ${file} (${(stats.size / 1024).toFixed(2)} KB)`);
      });

      // Analyze the most recent backup file
      if (backupFiles.length > 0) {
        const latestBackup = backupFiles.sort().pop();
        const backupPath = path.join(backupDir, latestBackup);
        
        console.log(`\n📊 Analyzing latest backup: ${latestBackup}`);
        
        try {
          const backupContent = JSON.parse(fs.readFileSync(backupPath, 'utf8'));
          
          console.log('   📋 Backup structure:');
          console.log(`      🕒 Timestamp: ${backupContent.timestamp}`);
          console.log(`      📱 App Version: ${backupContent.appVersion || 'N/A'}`);
          console.log(`      🔢 Total Collections: ${Object.keys(backupContent.collections || {}).length}`);
          
          if (backupContent.collections) {
            let totalDocs = 0;
            Object.entries(backupContent.collections).forEach(([collection, data]) => {
              const docCount = Array.isArray(data) ? data.length : 0;
              totalDocs += docCount;
              console.log(`         📚 ${collection}: ${docCount} documents`);
            });
            console.log(`      📈 Total Documents: ${totalDocs}`);
          }
          
          // Verify data integrity
          console.log('\n🔍 Verifying data integrity...');
          
          if (backupContent.collections?.users) {
            const users = backupContent.collections.users;
            const adminUsers = users.filter(u => u.role === 'Admin');
            const teacherUsers = users.filter(u => u.role === 'Teacher');
            const studentUsers = users.filter(u => u.role === 'Student');
            
            console.log(`   👥 User data integrity:`);
            console.log(`      🔑 Admin users: ${adminUsers.length}`);
            console.log(`      👨‍🏫 Teacher users: ${teacherUsers.length}`);
            console.log(`      👨‍🎓 Student users: ${studentUsers.length}`);
            
            // Check for required fields
            const usersWithEmail = users.filter(u => u.email);
            const usersWithUID = users.filter(u => u.uid);
            console.log(`      📧 Users with email: ${usersWithEmail.length}/${users.length}`);
            console.log(`      🆔 Users with UID: ${usersWithUID.length}/${users.length}`);
          }
          
          if (backupContent.collections?.courses) {
            const courses = backupContent.collections.courses;
            console.log(`   📚 Course data integrity:`);
            console.log(`      📖 Total courses: ${courses.length}`);
            
            const coursesWithInstructor = courses.filter(c => c.instructor);
            console.log(`      👨‍🏫 Courses with instructor: ${coursesWithInstructor.length}/${courses.length}`);
          }
          
          if (backupContent.collections?.enrollments) {
            const enrollments = backupContent.collections.enrollments;
            console.log(`   🎓 Enrollment data integrity:`);
            console.log(`      📝 Total enrollments: ${enrollments.length}`);
            
            const enrollmentsWithStudentId = enrollments.filter(e => e.studentId);
            const enrollmentsWithCourseId = enrollments.filter(e => e.courseId);
            console.log(`      👨‍🎓 Enrollments with student ID: ${enrollmentsWithStudentId.length}/${enrollments.length}`);
            console.log(`      📚 Enrollments with course ID: ${enrollmentsWithCourseId.length}/${enrollments.length}`);
          }
          
        } catch (parseError) {
          console.log(`   ❌ Error parsing backup file: ${parseError.message}`);
        }
      }
    } else {
      console.log('   ⚠️  No backup directory found. Backup may not have been created yet.');
    }

    // Compare with current database state
    console.log('\n🔄 Comparing with current database state...');
    
    const collections = ['users', 'courses', 'enrollments', 'students', 'weekly_content', 
                        'student_analytics', 'student_progress', 'image_metadata', 'securityLogs'];
    
    let currentTotal = 0;
    for (const collectionName of collections) {
      const snapshot = await db.collection(collectionName).get();
      currentTotal += snapshot.size;
      console.log(`   📊 ${collectionName}: ${snapshot.size} documents`);
    }
    
    console.log(`   📈 Current total documents: ${currentTotal}`);

    console.log('\n✅ Backup functionality verification completed!');
    
    // Provide testing instructions
    console.log('\n📱 Manual Testing Instructions:');
    console.log('=====================================');
    console.log('1. Open the Android app');
    console.log('2. Login as Admin: admin@eduflex.com / admin123');
    console.log('3. Navigate to Admin Dashboard');
    console.log('4. Tap the Settings icon (⚙️) in the top right');
    console.log('5. Select "Backup Database"');
    console.log('6. Tap "Create New Backup"');
    console.log('7. Wait for the backup process to complete');
    console.log('8. Verify the backup file is created and can be shared');
    console.log('9. Run this script again to verify the backup file contents');

  } catch (error) {
    console.error('❌ Error verifying backup functionality:', error);
  }
}

verifyBackupFunctionality();