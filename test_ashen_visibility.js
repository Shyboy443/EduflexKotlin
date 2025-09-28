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

async function testAshenVisibility() {
  try {
    console.log('🔍 Testing if "ashen" course is visible in StudentCoursesFragment query...\n');
    
    // This is the exact query used by StudentCoursesFragment
    const coursesSnapshot = await db.collection('COURSES')
      .where('isPublished', '==', true)
      .get();
    
    console.log(`📊 Total published courses found: ${coursesSnapshot.size}`);
    
    let ashenFound = false;
    let ashenData = null;
    let allCourses = [];
    
    coursesSnapshot.forEach(doc => {
      const courseData = doc.data();
      allCourses.push({
        id: doc.id,
        title: courseData.title,
        category: courseData.category,
        instructor: courseData.instructor,
        isPublished: courseData.isPublished
      });
      
      if (courseData.title === 'ashen') {
        ashenFound = true;
        ashenData = {
          id: doc.id,
          data: courseData
        };
      }
    });
    
    if (ashenFound) {
      console.log('\n✅ SUCCESS: Course "ashen" is visible in the query!');
      console.log('📋 Course details:');
      console.log('- ID:', ashenData.id);
      console.log('- Title:', ashenData.data.title);
      console.log('- Category:', ashenData.data.category);
      console.log('- Instructor:', ashenData.data.instructor);
      console.log('- isPublished:', ashenData.data.isPublished, '(type:', typeof ashenData.data.isPublished, ')');
      console.log('- Description:', ashenData.data.description);
      console.log('- Difficulty:', ashenData.data.difficulty);
      console.log('- Duration:', ashenData.data.duration);
    } else {
      console.log('\n❌ ISSUE: Course "ashen" is NOT visible in the query!');
      console.log('\n📋 Available courses:');
      allCourses.slice(0, 10).forEach((course, index) => {
        console.log(`  ${index + 1}. "${course.title}" (${course.category}) - Published: ${course.isPublished}`);
      });
      if (allCourses.length > 10) {
        console.log(`  ... and ${allCourses.length - 10} more courses`);
      }
    }
    
    // Also check if there are any courses with title "ashen" regardless of publication status
    console.log('\n🔍 Checking all courses for "ashen" (regardless of publication status)...');
    const allCoursesSnapshot = await db.collection('COURSES').get();
    let ashenFoundInAll = false;
    
    allCoursesSnapshot.forEach(doc => {
      const courseData = doc.data();
      if (courseData.title === 'ashen') {
        ashenFoundInAll = true;
        console.log('✅ Found "ashen" in all courses:');
        console.log('- ID:', doc.id);
        console.log('- isPublished:', courseData.isPublished, '(type:', typeof courseData.isPublished, ')');
      }
    });
    
    if (!ashenFoundInAll) {
      console.log('❌ Course "ashen" not found in database at all');
    }
    
  } catch (error) {
    console.error('❌ Error testing course visibility:', error);
  }
}

testAshenVisibility().then(() => {
  console.log('\n✅ Visibility test completed');
  process.exit(0);
}).catch(error => {
  console.error('❌ Script failed:', error);
  process.exit(1);
});