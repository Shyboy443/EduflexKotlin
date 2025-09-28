const admin = require('firebase-admin');

// Initialize Firebase Admin SDK for emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

admin.initializeApp({
  projectId: 'demo-project'
});

// Connect to Firestore emulator
const db = admin.firestore();

async function testStudentUI() {
  try {
    console.log('🧪 Testing Student UI Data...\n');

    // Test 1: Check if courses exist
    console.log('📚 Checking available courses...');
    const coursesSnapshot = await db.collection('courses').get();
    console.log(`Found ${coursesSnapshot.size} courses in database`);
    
    if (coursesSnapshot.size > 0) {
      coursesSnapshot.forEach(doc => {
        const course = doc.data();
        console.log(`  - ${course.title} (ID: ${doc.id})`);
        console.log(`    Published: ${course.isPublished}`);
        console.log(`    Instructor: ${course.instructor}`);
      });
    }

    // Test 2: Check enrollments for student1
    console.log('\n👨‍🎓 Checking enrollments for student1...');
    const enrollmentsSnapshot = await db.collection('enrollments')
      .where('studentId', '==', 'student1')
      .where('isActive', '==', true)
      .get();
    
    console.log(`Found ${enrollmentsSnapshot.size} active enrollments for student1`);
    
    if (enrollmentsSnapshot.size > 0) {
      for (const doc of enrollmentsSnapshot.docs) {
        const enrollment = doc.data();
        console.log(`  - Enrolled in course: ${enrollment.courseId}`);
        console.log(`    Enrollment date: ${new Date(enrollment.enrollmentDate).toLocaleDateString()}`);
        
        // Get course details
        const courseDoc = await db.collection('courses').doc(enrollment.courseId).get();
        if (courseDoc.exists) {
          const course = courseDoc.data();
          console.log(`    Course title: ${course.title}`);
        }
      }
    }

    // Test 3: Simulate what StudentDashboardFragment would see
    console.log('\n🏠 Simulating StudentDashboardFragment data loading...');
    
    // Get enrollments
    const studentEnrollments = await db.collection('enrollments')
      .where('studentId', '==', 'student1')
      .where('isActive', '==', true)
      .get();
    
    if (studentEnrollments.empty) {
      console.log('❌ No enrollments found - Continue Learning section should show empty state');
    } else {
      const courseIds = studentEnrollments.docs.map(doc => doc.data().courseId);
      console.log(`📋 Course IDs from enrollments: ${courseIds.join(', ')}`);
      
      // Fetch course details (simulating the fixed query)
      const coursePromises = courseIds.map(courseId => 
        db.collection('courses').doc(courseId).get()
      );
      
      const courseDocs = await Promise.all(coursePromises);
      const validCourses = courseDocs.filter(doc => doc.exists && doc.data().isPublished);
      
      console.log(`✅ Found ${validCourses.length} valid published courses for Continue Learning`);
      validCourses.forEach(doc => {
        const course = doc.data();
        console.log(`  - ${course.title}`);
      });
    }

    // Test 4: Simulate what StudentCoursesFragment would see
    console.log('\n📖 Simulating StudentCoursesFragment data loading...');
    
    // Available courses
    const availableCoursesSnapshot = await db.collection('courses')
      .where('isPublished', '==', true)
      .get();
    
    console.log(`✅ Found ${availableCoursesSnapshot.size} available courses`);
    
    // Enrolled courses (same as dashboard test)
    console.log(`✅ Found ${studentEnrollments.size} enrolled courses for student1`);

    console.log('\n🎉 Student UI test completed successfully!');
    console.log('\n📝 Expected behavior:');
    console.log('  - StudentDashboardFragment: Should show enrolled courses in Continue Learning section');
    console.log('  - StudentCoursesFragment: Should show both available and enrolled courses');
    console.log('  - Empty states: Should appear when no courses/enrollments exist');

  } catch (error) {
    console.error('❌ Error testing student UI:', error);
  }
}

testStudentUI().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('❌ Test failed:', error);
  process.exit(1);
});