const admin = require('firebase-admin');

// Initialize Firebase Admin SDK with project ID
admin.initializeApp({
  projectId: "eduflex-f62b5",
  databaseURL: "https://eduflex-f62b5-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const auth = admin.auth();
const db = admin.firestore();

async function createTeacher() {
  try {
    console.log('🧑‍🏫 Creating teacher account...');
    
    // Create user in Firebase Authentication
    const userRecord = await auth.createUser({
      email: 'teacher@gmail.com',
      password: 'teacher123',
      displayName: 'Sample Teacher',
      emailVerified: true
    });
    
    console.log('✅ Teacher authentication account created:', userRecord.uid);
    
    // Create teacher profile in Firestore
    const teacherData = {
      id: userRecord.uid,
      fullName: 'Sample Teacher',
      email: 'teacher@gmail.com',
      role: 'Teacher',
      isActive: true,
      createdAt: Date.now(),
      lastLoginAt: Date.now(),
      profilePicture: '',
      bio: 'Sample teacher account for testing and development purposes.',
      specialization: 'Computer Science',
      yearsOfExperience: 5,
      department: 'Technology',
      phoneNumber: '+1234567890',
      address: 'Sample Address, City, Country'
    };
    
    await db.collection('users').doc(userRecord.uid).set(teacherData);
    console.log('✅ Teacher profile created in Firestore');
    
    // Create a sample course for this teacher
    const courseId = `course_${Date.now()}`;
    const courseData = {
      courseId: courseId,
      title: 'Introduction to Programming',
      description: 'Learn the fundamentals of programming with hands-on exercises and projects.',
      category: 'Computer Science',
      teacherId: userRecord.uid,
      teacherName: 'Sample Teacher',
      duration: '12 weeks',
      level: 'Beginner',
      maxStudents: 30,
      currentEnrollments: 0,
      isActive: true,
      createdAt: Date.now(),
      startDate: Date.now(),
      endDate: Date.now() + (12 * 7 * 24 * 60 * 60 * 1000), // 12 weeks from now
      schedule: 'Monday, Wednesday, Friday - 10:00 AM',
      prerequisites: 'Basic computer literacy',
      objectives: [
        'Understand programming fundamentals',
        'Write basic programs',
        'Debug and troubleshoot code',
        'Apply problem-solving techniques'
      ],
      materials: [
        'Programming textbook',
        'Online coding platform access',
        'Development environment setup guide'
      ]
    };
    
    await db.collection('courses').doc(courseId).set(courseData);
    console.log('✅ Sample course created for teacher');
    
    console.log('\n🎉 Teacher account setup complete!');
    console.log('📧 Email: teacher@gmail.com');
    console.log('🔑 Password: teacher123');
    console.log('👤 Role: Teacher');
    console.log('🆔 UID:', userRecord.uid);
    console.log('\nThe teacher can now sign in to the app and access teacher features.');
    
  } catch (error) {
    console.error('❌ Error creating teacher account:', error.message);
    
    // If user already exists, just update the Firestore profile
    if (error.code === 'auth/email-already-exists') {
      console.log('📝 User already exists, updating profile...');
      try {
        const existingUser = await auth.getUserByEmail('teacher@gmail.com');
        const teacherData = {
          id: existingUser.uid,
          fullName: 'Sample Teacher',
          email: 'teacher@gmail.com',
          role: 'Teacher',
          isActive: true,
          updatedAt: Date.now(),
          profilePicture: '',
          bio: 'Sample teacher account for testing and development purposes.',
          specialization: 'Computer Science',
          yearsOfExperience: 5,
          department: 'Technology'
        };
        
        await db.collection('users').doc(existingUser.uid).set(teacherData, { merge: true });
        console.log('✅ Teacher profile updated in Firestore');
        console.log('🆔 UID:', existingUser.uid);
      } catch (updateError) {
        console.error('❌ Error updating profile:', updateError.message);
      }
    }
  }
}

// Run the function
createTeacher().then(() => {
  console.log('✅ Script completed');
  process.exit(0);
}).catch((error) => {
  console.error('💥 Script failed:', error);
  process.exit(1);
});