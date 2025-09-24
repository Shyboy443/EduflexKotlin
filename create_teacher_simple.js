// Simple teacher creation script using Firebase Web SDK
const { initializeApp } = require('firebase/app');
const { getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword } = require('firebase/auth');
const { getFirestore, doc, setDoc } = require('firebase/firestore');

// Firebase configuration (from your google-services.json)
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
const auth = getAuth(app);
const db = getFirestore(app);

async function createTeacher() {
  try {
    console.log('🧑‍🏫 Creating teacher account...');
    
    // Create user with email and password
    const userCredential = await createUserWithEmailAndPassword(auth, 'teacher@gmail.com', 'teacher123');
    const user = userCredential.user;
    
    console.log('✅ Teacher authentication account created:', user.uid);
    
    // Create teacher profile in Firestore
    const teacherData = {
      id: user.uid,
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
    
    await setDoc(doc(db, 'users', user.uid), teacherData);
    console.log('✅ Teacher profile created in Firestore');
    
    console.log('\n🎉 Teacher account setup complete!');
    console.log('📧 Email: teacher@gmail.com');
    console.log('🔑 Password: teacher123');
    console.log('👤 Role: Teacher');
    console.log('🆔 UID:', user.uid);
    
  } catch (error) {
    console.error('❌ Error creating teacher account:', error.message);
    
    if (error.code === 'auth/email-already-exists') {
      console.log('📝 User already exists. You can sign in with the existing account.');
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