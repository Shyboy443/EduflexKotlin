const admin = require('firebase-admin');

// Initialize Firebase Admin SDK for emulator
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

// Connect to Firestore emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
const db = admin.firestore();

async function testBackupFunctionality() {
  console.log('🔍 Testing Database Backup Functionality...\n');

  try {
    // Test 1: Verify all collections exist and have data
    console.log('📊 Checking database collections for backup...');
    
    const collections = [
      'users',
      'courses', 
      'enrollments',
      'students',
      'weekly_content',
      'student_analytics',
      'student_progress',
      'image_metadata',
      'securityLogs'
    ];

    const collectionData = {};
    let totalDocuments = 0;

    for (const collectionName of collections) {
      try {
        const snapshot = await db.collection(collectionName).get();
        const docCount = snapshot.size;
        collectionData[collectionName] = docCount;
        totalDocuments += docCount;
        
        console.log(`   ✅ ${collectionName}: ${docCount} documents`);
        
        // Show sample data structure for verification
        if (docCount > 0) {
          const firstDoc = snapshot.docs[0];
          const sampleData = firstDoc.data();
          const fieldCount = Object.keys(sampleData).length;
          console.log(`      📋 Sample fields: ${fieldCount} (${Object.keys(sampleData).slice(0, 3).join(', ')}${fieldCount > 3 ? '...' : ''})`);
        }
      } catch (error) {
        console.log(`   ❌ ${collectionName}: Error accessing collection - ${error.message}`);
        collectionData[collectionName] = 0;
      }
    }

    console.log(`\n📈 Total documents across all collections: ${totalDocuments}`);

    // Test 2: Verify backup would include all user types
    console.log('\n👥 Verifying user data for backup...');
    const usersSnapshot = await db.collection('users').get();
    const usersByRole = {};
    
    usersSnapshot.forEach(doc => {
      const userData = doc.data();
      const role = userData.role || 'Unknown';
      usersByRole[role] = (usersByRole[role] || 0) + 1;
    });

    console.log('   User distribution:');
    Object.entries(usersByRole).forEach(([role, count]) => {
      console.log(`   - ${role}: ${count} users`);
    });

    // Test 3: Check for sensitive data that should be backed up
    console.log('\n🔐 Checking for sensitive data handling...');
    
    // Check if users have authentication data
    let usersWithAuth = 0;
    let usersWithEmail = 0;
    
    usersSnapshot.forEach(doc => {
      const userData = doc.data();
      if (userData.uid) usersWithAuth++;
      if (userData.email) usersWithEmail++;
    });

    console.log(`   ✅ Users with authentication IDs: ${usersWithAuth}/${usersSnapshot.size}`);
    console.log(`   ✅ Users with email addresses: ${usersWithEmail}/${usersSnapshot.size}`);

    // Test 4: Verify course and enrollment data
    console.log('\n📚 Checking course and enrollment data...');
    
    const coursesSnapshot = await db.collection('courses').get();
    const enrollmentsSnapshot = await db.collection('enrollments').get();
    
    console.log(`   📖 Courses available: ${coursesSnapshot.size}`);
    console.log(`   🎓 Student enrollments: ${enrollmentsSnapshot.size}`);

    // Test 5: Check weekly content and progress data
    console.log('\n📅 Checking learning content and progress...');
    
    const weeklyContentSnapshot = await db.collection('weekly_content').get();
    const progressSnapshot = await db.collection('student_progress').get();
    const analyticsSnapshot = await db.collection('student_analytics').get();
    
    console.log(`   📝 Weekly content items: ${weeklyContentSnapshot.size}`);
    console.log(`   📊 Student progress records: ${progressSnapshot.size}`);
    console.log(`   📈 Analytics records: ${analyticsSnapshot.size}`);

    // Summary
    console.log('\n🎯 Backup Functionality Test Summary:');
    console.log('=====================================');
    console.log(`✅ Total collections to backup: ${collections.length}`);
    console.log(`✅ Total documents to backup: ${totalDocuments}`);
    console.log(`✅ User accounts: ${usersSnapshot.size} (Admin: ${usersByRole.Admin || 0}, Teacher: ${usersByRole.Teacher || 0}, Student: ${usersByRole.Student || 0})`);
    console.log(`✅ Course data: ${coursesSnapshot.size} courses, ${enrollmentsSnapshot.size} enrollments`);
    console.log(`✅ Learning data: ${weeklyContentSnapshot.size} content items, ${progressSnapshot.size} progress records`);
    
    if (totalDocuments > 0) {
      console.log('\n🎉 Database backup functionality is ready to test!');
      console.log('\n📱 To test in the Android app:');
      console.log('1. Login as Admin (admin@eduflex.com / admin123)');
      console.log('2. Go to Admin Dashboard');
      console.log('3. Tap the Settings icon (⚙️) in the top right');
      console.log('4. Select "Backup Database"');
      console.log('5. Choose "Create New Backup"');
      console.log('6. Wait for backup to complete');
      console.log('7. Choose to share or view the backup file');
      
      console.log('\n📋 Expected backup file contents:');
      collections.forEach(collection => {
        if (collectionData[collection] > 0) {
          console.log(`   - ${collection}: ${collectionData[collection]} documents`);
        }
      });
    } else {
      console.log('\n⚠️  Warning: No data found to backup. Please ensure the database is populated.');
    }

  } catch (error) {
    console.error('❌ Error testing backup functionality:', error);
  }
}

// Run the test
testBackupFunctionality()
  .then(() => {
    console.log('\n✅ Backup functionality test completed!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Test failed:', error);
    process.exit(1);
  });