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

async function checkSpecificCourse() {
  try {
    console.log('🔍 Searching for course "ashen"...\n');
    
    // Get all courses
    const coursesSnapshot = await db.collection('COURSES').get();
    
    let foundCourse = null;
    let totalCourses = 0;
    
    coursesSnapshot.forEach(doc => {
      totalCourses++;
      const courseData = doc.data();
      
      // Check if this is the course we're looking for
      if (courseData.title === 'ashen') {
        foundCourse = {
          id: doc.id,
          data: courseData
        };
      }
    });
    
    console.log(`📊 Total courses in database: ${totalCourses}`);
    
    if (foundCourse) {
      console.log('\n✅ Found course "ashen"!');
      console.log('📋 Course ID:', foundCourse.id);
      console.log('📋 Course Data:');
      console.log(JSON.stringify(foundCourse.data, null, 2));
      
      // Check specific fields that might affect visibility
      console.log('\n🔍 Key fields for visibility:');
      console.log('- isPublished:', foundCourse.data.isPublished, '(type:', typeof foundCourse.data.isPublished, ')');
      console.log('- title:', foundCourse.data.title);
      console.log('- category:', foundCourse.data.category);
      console.log('- instructor:', foundCourse.data.instructor);
      console.log('- teacherId:', foundCourse.data.teacherId);
      
    } else {
      console.log('\n❌ Course "ashen" not found in database!');
      
      // Show some sample course titles for comparison
      console.log('\n📝 Sample course titles in database:');
      let count = 0;
      coursesSnapshot.forEach(doc => {
        if (count < 10) {
          const courseData = doc.data();
          console.log(`- "${courseData.title}" (isPublished: ${courseData.isPublished})`);
          count++;
        }
      });
    }
    
  } catch (error) {
    console.error('❌ Error checking course:', error);
  }
}

checkSpecificCourse().then(() => {
  console.log('\n✅ Course check completed');
  process.exit(0);
}).catch(error => {
  console.error('❌ Script failed:', error);
  process.exit(1);
});