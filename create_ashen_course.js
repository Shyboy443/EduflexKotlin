const admin = require('firebase-admin');

// Initialize Firebase Admin SDK for emulator
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

// Connect to Firestore emulator
const db = admin.firestore();
db.settings({
  host: 'localhost:8080',
  ssl: false
});

async function createAshenCourse() {
  try {
    console.log('🎯 Creating course "ashen" with your exact data...\n');
    
    // Your exact course data
    const courseData = {
      category: "Mathematics",
      completedLessons: 0,
      courseContent: [],
      createdAt: 1758716427094,
      description: "ashe",
      difficulty: "Beginner",
      duration: "2",
      enrolledStudents: 0,
      hasDeadline: false,
      instructor: "Sarah Teacher",
      isBookmarked: false,
      isPublished: true,
      progress: 0,
      rating: 0,
      teacherId: "VItzt489oEetMwhxGTJ7ID33jw73",
      thumbnailUrl: "",
      title: "ashen",
      totalLessons: 0,
      updatedAt: 1758716427094
    };
    
    // Add the course to Firestore
    const docRef = await db.collection('COURSES').add(courseData);
    
    console.log('✅ Course "ashen" created successfully!');
    console.log('📋 Course ID:', docRef.id);
    console.log('📋 Course Data:');
    console.log(JSON.stringify(courseData, null, 2));
    
    // Verify the course was created
    console.log('\n🔍 Verifying course creation...');
    const createdDoc = await docRef.get();
    if (createdDoc.exists) {
      console.log('✅ Course verified in database');
      const verifiedData = createdDoc.data();
      console.log('📊 Key fields:');
      console.log('- title:', verifiedData.title);
      console.log('- isPublished:', verifiedData.isPublished, '(type:', typeof verifiedData.isPublished, ')');
      console.log('- category:', verifiedData.category);
      console.log('- instructor:', verifiedData.instructor);
    } else {
      console.log('❌ Course verification failed');
    }
    
    // Check total courses now
    const allCoursesSnapshot = await db.collection('COURSES').get();
    console.log(`\n📊 Total courses in database: ${allCoursesSnapshot.size}`);
    
    // Check published courses
    const publishedCoursesSnapshot = await db.collection('COURSES')
      .where('isPublished', '==', true)
      .get();
    console.log(`📊 Total published courses: ${publishedCoursesSnapshot.size}`);
    
  } catch (error) {
    console.error('❌ Error creating course:', error);
  }
}

createAshenCourse().then(() => {
  console.log('\n✅ Course creation completed');
  process.exit(0);
}).catch(error => {
  console.error('❌ Script failed:', error);
  process.exit(1);
});