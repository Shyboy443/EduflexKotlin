const admin = require('firebase-admin');

// Initialize Firebase Admin SDK for emulator
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

// Connect to Firestore emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
const db = admin.firestore();

async function debugCourses() {
  try {
    console.log('🔍 Debugging Courses Collection...\n');

    // Get all courses
    const coursesSnapshot = await db.collection('courses').get();
    
    console.log(`📚 Total courses found: ${coursesSnapshot.size}\n`);
    
    if (coursesSnapshot.empty) {
      console.log('❌ No courses found in the database!');
      return;
    }

    coursesSnapshot.docs.forEach((doc, index) => {
      const data = doc.data();
      console.log(`📖 Course ${index + 1}:`);
      console.log(`   ID: ${doc.id}`);
      console.log(`   Title: ${data.title || 'N/A'}`);
      console.log(`   Instructor: ${data.instructor || 'N/A'}`);
      console.log(`   Category: ${data.category || 'N/A'}`);
      console.log(`   Difficulty: ${data.difficulty || 'N/A'}`);
      console.log(`   Is Published: ${data.isPublished}`);
      console.log(`   Created At: ${data.createdAt || 'N/A'}`);
      console.log(`   Teacher ID: ${data.teacherId || 'N/A'}`);
      console.log(`   Description: ${data.description ? data.description.substring(0, 50) + '...' : 'N/A'}`);
      console.log(`   Enrolled Students: ${data.enrolledStudents || 0}`);
      console.log(`   Rating: ${data.rating || 0}`);
      console.log(`   Price: ${data.price || 0}`);
      console.log(`   Is Free: ${data.isFree}`);
      console.log(`   Thumbnail URL: ${data.thumbnailUrl || 'N/A'}`);
      console.log('   ---');
    });

    // Check for published courses specifically
    const publishedCoursesSnapshot = await db.collection('courses')
      .where('isPublished', '==', true)
      .get();
    
    console.log(`\n✅ Published courses: ${publishedCoursesSnapshot.size}`);
    
    // Check enrollments
    const enrollmentsSnapshot = await db.collection('enrollments').get();
    console.log(`🎓 Total enrollments: ${enrollmentsSnapshot.size}`);
    
    if (!enrollmentsSnapshot.empty) {
      console.log('\n📋 Enrollment details:');
      enrollmentsSnapshot.docs.forEach((doc, index) => {
        const data = doc.data();
        console.log(`   ${index + 1}. Student: ${data.studentId}, Course: ${data.courseId}, Active: ${data.isActive}`);
      });
    }

    // Test the exact query used in StudentDashboardFragment for popular courses
    console.log('\n🔍 Testing popular courses query (limit 5):');
    const popularCoursesSnapshot = await db.collection('courses')
      .where('isPublished', '==', true)
      .limit(5)
      .get();
    
    console.log(`   Found ${popularCoursesSnapshot.size} popular courses`);
    
    // Test the exact query used in StudentCoursesFragment for available courses
    console.log('\n🔍 Testing available courses query:');
    const availableCoursesSnapshot = await db.collection('courses')
      .where('isPublished', '==', true)
      .get();
    
    console.log(`   Found ${availableCoursesSnapshot.size} available courses`);

  } catch (error) {
    console.error('❌ Error debugging courses:', error);
  }
}

debugCourses();