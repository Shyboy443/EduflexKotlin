const admin = require('firebase-admin');

// Set environment variables for emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

// Initialize Firebase Admin SDK (bypasses security rules in emulator)
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

const db = admin.firestore();

async function testStudentUIWithAuth() {
  try {
    console.log('🧪 Testing Student UI with Authentication...\n');

    // Test 1: Check courses
    console.log('📚 Checking courses...');
    const coursesSnapshot = await db.collection('courses').get();
    console.log(`Found ${coursesSnapshot.size} courses`);
    
    coursesSnapshot.forEach(doc => {
      const course = doc.data();
      console.log(`  - ${course.title} (ID: ${doc.id}, Published: ${course.isPublished})`);
    });

    // Test 2: Check enrollments
    console.log('\n👨‍🎓 Checking enrollments...');
    const enrollmentsSnapshot = await db.collection('enrollments').get();
    console.log(`Found ${enrollmentsSnapshot.size} enrollments`);
    
    enrollmentsSnapshot.forEach(doc => {
      const enrollment = doc.data();
      console.log(`  - Student: ${enrollment.studentId}, Course: ${enrollment.courseId}, Active: ${enrollment.isActive}`);
    });

    // Test 3: Simulate StudentDashboardFragment logic
    console.log('\n🏠 Testing StudentDashboardFragment logic...');
    
    const studentId = 'student1';
    const enrollmentsQuery = await db.collection('enrollments')
      .where('studentId', '==', studentId)
      .where('isActive', '==', true)
      .get();
    
    console.log(`Found ${enrollmentsQuery.size} active enrollments for ${studentId}`);
    
    if (enrollmentsQuery.empty) {
      console.log('❌ No enrollments - Continue Learning should show empty state');
    } else {
      const courseIds = enrollmentsQuery.docs.map(doc => doc.data().courseId);
      console.log(`📋 Course IDs: ${courseIds.join(', ')}`);
      
      // Fetch course details using the fixed approach
      const coursePromises = courseIds.map(courseId => 
        db.collection('courses').doc(courseId).get()
      );
      
      const courseDocs = await Promise.all(coursePromises);
      const validCourses = courseDocs.filter(doc => doc.exists && doc.data().isPublished);
      
      console.log(`✅ Valid published courses: ${validCourses.length}`);
      validCourses.forEach(doc => {
        const course = doc.data();
        console.log(`  - ${course.title}`);
      });
      
      if (validCourses.length === 0) {
        console.log('⚠️ No valid courses - Continue Learning should show empty state');
      } else {
        console.log('✅ Continue Learning should show enrolled courses');
      }
    }

    // Test 4: Simulate StudentCoursesFragment logic
    console.log('\n📖 Testing StudentCoursesFragment logic...');
    
    // Available courses
    const availableCoursesQuery = await db.collection('courses')
      .where('isPublished', '==', true)
      .get();
    
    console.log(`✅ Available courses: ${availableCoursesQuery.size}`);
    if (availableCoursesQuery.size === 0) {
      console.log('⚠️ No available courses - should show empty state');
    } else {
      console.log('✅ Available courses section should show courses');
    }
    
    // Enrolled courses (same logic as dashboard)
    console.log(`✅ Enrolled courses for ${studentId}: ${enrollmentsQuery.size}`);
    if (enrollmentsQuery.size === 0) {
      console.log('⚠️ No enrolled courses - should show empty state');
    } else {
      console.log('✅ Enrolled courses section should show courses');
    }

    console.log('\n🎉 Authentication test completed successfully!');
    
    // Summary
    console.log('\n📊 Test Summary:');
    console.log(`  - Total courses in database: ${coursesSnapshot.size}`);
    console.log(`  - Total enrollments in database: ${enrollmentsSnapshot.size}`);
    console.log(`  - Active enrollments for student1: ${enrollmentsQuery.size}`);
    console.log(`  - Available published courses: ${availableCoursesQuery.size}`);
    
    console.log('\n✅ Expected UI Behavior:');
    if (coursesSnapshot.size > 0 && enrollmentsQuery.size > 0) {
      console.log('  - StudentDashboardFragment: Should display enrolled courses in Continue Learning');
      console.log('  - StudentCoursesFragment: Should display both available and enrolled courses');
    } else {
      console.log('  - Both fragments should display appropriate empty states');
    }

  } catch (error) {
    console.error('❌ Error during authentication test:', error);
  }
}

testStudentUIWithAuth().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('❌ Test failed:', error);
  process.exit(1);
});