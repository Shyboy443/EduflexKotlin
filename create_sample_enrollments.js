// Script to create sample enrollments for testing
const { initializeApp } = require('firebase/app');
const { getFirestore, collection, addDoc, doc, setDoc, getDocs, query, limit } = require('firebase/firestore');
const { getAuth, signInWithEmailAndPassword } = require('firebase/auth');

// Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyChfxtlW1XrXI5gLH7tjsxDNOTfgWyoQ0Y",
  authDomain: "eduflex-f62b5.firebaseapp.com",
  databaseURL: "https://eduflex-f62b5-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "eduflex-f62b5",
  storageBucket: "eduflex-f62b5.firebasestorage.app",
  messagingSenderId: "706298535818",
  appId: "1:706298535818:android:68de04b0808358b03c7273"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// Student account UID from SAMPLE_ACCOUNTS_LOGIN_DETAILS.md
const STUDENT_UID = 'aY6L9gLYUMcncIQNlRYItUB1UVH3';

// Admin credentials for authentication
const ADMIN_EMAIL = 'admin@eduflex.com';
const ADMIN_PASSWORD = 'admin123';

async function createSampleEnrollments() {
  console.log('🎓 Creating sample enrollments for student account...');
  
  try {
    // First, authenticate as admin
    console.log('🔐 Authenticating as admin...');
    await signInWithEmailAndPassword(auth, ADMIN_EMAIL, ADMIN_PASSWORD);
    console.log('✅ Successfully authenticated as admin');
    
    // Get some courses from the database
    console.log('📚 Fetching available courses...');
    const coursesQuery = query(collection(db, 'courses'), limit(5));
    const coursesSnapshot = await getDocs(coursesQuery);
    
    if (coursesSnapshot.empty) {
      console.log('❌ No courses found in database. Please run the seeding script first.');
      return;
    }
    
    const courses = [];
    coursesSnapshot.forEach(doc => {
      courses.push({ id: doc.id, ...doc.data() });
    });
    
    console.log(`📖 Found ${courses.length} courses to enroll in`);
    
    // Create enrollments for the student
    const enrollments = [];
    for (let i = 0; i < courses.length; i++) {
      const course = courses[i];
      const enrollmentData = {
        studentId: STUDENT_UID,
        courseId: course.id,
        enrolledAt: Date.now() - (i * 24 * 60 * 60 * 1000), // Enrolled on different days
        isActive: true,
        progress: Math.floor(Math.random() * 80) + 10, // Random progress between 10-90%
        completedLessons: Math.floor(Math.random() * 10),
        totalLessons: Math.floor(Math.random() * 15) + 10,
        lastAccessedAt: Date.now() - (Math.random() * 7 * 24 * 60 * 60 * 1000), // Last accessed within a week
        status: 'active'
      };
      
      const enrollmentRef = await addDoc(collection(db, 'enrollments'), enrollmentData);
      enrollments.push({ id: enrollmentRef.id, ...enrollmentData });
      
      console.log(`✅ Created enrollment for course: ${course.title}`);
    }
    
    console.log(`\n🎉 Successfully created ${enrollments.length} sample enrollments!`);
    console.log('\n📊 Enrollment summary:');
    enrollments.forEach((enrollment, index) => {
      const course = courses[index];
      console.log(`  ${course.title} - ${enrollment.progress}% complete`);
    });
    
    console.log('\n✅ Sample enrollments created successfully!');
    console.log('🔄 You can now test the app - enrolled courses should appear in the My Courses section.');
    
  } catch (error) {
    console.error('❌ Error creating sample enrollments:', error);
    if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password') {
      console.error('💡 Make sure the admin account exists. Run create_sample_accounts.js first.');
    }
    process.exit(1);
  }
}

// Run the script
createSampleEnrollments().then(() => {
  console.log('🏁 Enrollment creation process finished.');
  process.exit(0);
}).catch((error) => {
  console.error('💥 Enrollment creation process failed:', error);
  process.exit(1);
});