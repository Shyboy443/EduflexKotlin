const { initializeApp } = require('firebase/app');
const { getFirestore, connectFirestoreEmulator, collection, getDocs, query, where } = require('firebase/firestore');

// Initialize Firebase
const firebaseConfig = {
  projectId: 'demo-project'
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// Connect to emulator
try {
  connectFirestoreEmulator(db, 'localhost', 8080);
} catch (error) {
  // Already connected
}

async function testData() {
  try {
    console.log('🔍 Testing Firestore data...\n');

    // Test courses
    console.log('📚 Checking courses...');
    const coursesSnapshot = await getDocs(collection(db, 'courses'));
    console.log(`Found ${coursesSnapshot.size} courses`);
    
    coursesSnapshot.forEach(doc => {
      const course = doc.data();
      console.log(`  - ${course.title} (Published: ${course.isPublished})`);
    });

    // Test enrollments
    console.log('\n👨‍🎓 Checking enrollments...');
    const enrollmentsSnapshot = await getDocs(collection(db, 'enrollments'));
    console.log(`Found ${enrollmentsSnapshot.size} enrollments`);
    
    enrollmentsSnapshot.forEach(doc => {
      const enrollment = doc.data();
      console.log(`  - Student: ${enrollment.studentId}, Course: ${enrollment.courseId}, Active: ${enrollment.isActive}`);
    });

    // Test specific query for student1
    console.log('\n🎯 Checking enrollments for student1...');
    const student1Query = query(
      collection(db, 'enrollments'),
      where('studentId', '==', 'student1'),
      where('isActive', '==', true)
    );
    const student1Snapshot = await getDocs(student1Query);
    console.log(`Found ${student1Snapshot.size} active enrollments for student1`);

    console.log('\n✅ Data test completed!');
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

testData().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('❌ Test failed:', error);
  process.exit(1);
});