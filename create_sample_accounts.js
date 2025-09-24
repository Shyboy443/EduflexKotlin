// Comprehensive script to create sample accounts for Admin, Student, and Teacher
const { initializeApp } = require('firebase/app');
const { getAuth, createUserWithEmailAndPassword } = require('firebase/auth');
const { getFirestore, doc, setDoc } = require('firebase/firestore');

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
const auth = getAuth(app);
const db = getFirestore(app);

// Sample account data
const sampleAccounts = [
  {
    email: 'admin@eduflex.com',
    password: 'admin123',
    role: 'Admin',
    fullName: 'System Administrator',
    profileData: {
      bio: 'System administrator with full access to all platform features.',
      department: 'Administration',
      permissions: ['manage_users', 'manage_courses', 'view_analytics', 'system_settings'],
      phoneNumber: '+1-555-0001',
      address: 'EduFlex Headquarters, Tech City'
    }
  },
  {
    email: 'student@eduflex.com',
    password: 'student123',
    role: 'Student',
    fullName: 'John Student',
    profileData: {
      bio: 'Enthusiastic student eager to learn new technologies and skills.',
      grade: '12th Grade',
      studentId: 'STU2024001',
      enrollmentDate: Date.now(),
      phoneNumber: '+1-555-0002',
      address: '123 Student Lane, Learning City',
      guardianName: 'Jane Student',
      guardianPhone: '+1-555-0003'
    }
  },
  {
    email: 'teacher@eduflex.com',
    password: 'teacher123',
    role: 'Teacher',
    fullName: 'Sarah Teacher',
    profileData: {
      bio: 'Experienced educator passionate about technology and student success.',
      specialization: 'Computer Science & Mathematics',
      yearsOfExperience: 8,
      department: 'STEM Education',
      employeeId: 'TCH2024001',
      phoneNumber: '+1-555-0004',
      address: '456 Educator Ave, Knowledge City',
      qualifications: ['M.S. Computer Science', 'B.Ed. Mathematics', 'Google Certified Educator']
    }
  }
];

async function createAccount(accountData) {
  try {
    console.log(`\n🔄 Creating ${accountData.role} account: ${accountData.email}`);
    
    // Create user in Firebase Authentication
    const userCredential = await createUserWithEmailAndPassword(
      auth, 
      accountData.email, 
      accountData.password
    );
    const user = userCredential.user;
    
    console.log(`✅ Authentication account created with UID: ${user.uid}`);
    
    // Prepare Firestore profile data
    const firestoreData = {
      id: user.uid,
      fullName: accountData.fullName,
      email: accountData.email,
      role: accountData.role,
      isActive: true,
      createdAt: Date.now(),
      lastLoginAt: Date.now(),
      profilePicture: '',
      ...accountData.profileData
    };
    
    // Save to Firestore
    await setDoc(doc(db, 'users', user.uid), firestoreData);
    console.log(`✅ Profile saved to Firestore database`);
    
    return {
      success: true,
      uid: user.uid,
      email: accountData.email,
      password: accountData.password,
      role: accountData.role,
      fullName: accountData.fullName
    };
    
  } catch (error) {
    console.error(`❌ Error creating ${accountData.role} account:`, error.message);
    
    if (error.code === 'auth/email-already-in-use') {
      console.log(`📝 Account ${accountData.email} already exists`);
      return {
        success: false,
        error: 'Email already in use',
        email: accountData.email,
        password: accountData.password,
        role: accountData.role,
        fullName: accountData.fullName
      };
    }
    
    return {
      success: false,
      error: error.message,
      email: accountData.email,
      role: accountData.role
    };
  }
}

async function createAllAccounts() {
  console.log('🚀 Starting sample account creation process...\n');
  
  const results = [];
  
  for (const accountData of sampleAccounts) {
    const result = await createAccount(accountData);
    results.push(result);
    
    // Small delay between account creations
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  
  console.log('\n' + '='.repeat(60));
  console.log('📋 SAMPLE ACCOUNT CREATION SUMMARY');
  console.log('='.repeat(60));
  
  results.forEach((result, index) => {
    console.log(`\n${index + 1}. ${result.role.toUpperCase()} ACCOUNT:`);
    console.log(`   📧 Email: ${result.email}`);
    console.log(`   🔑 Password: ${result.password}`);
    console.log(`   👤 Name: ${result.fullName}`);
    console.log(`   📊 Status: ${result.success ? '✅ Created Successfully' : '❌ ' + result.error}`);
    if (result.uid) {
      console.log(`   🆔 UID: ${result.uid}`);
    }
  });
  
  console.log('\n' + '='.repeat(60));
  console.log('🎉 Account creation process completed!');
  console.log('📱 You can now use these credentials to sign in to the EduFlex app.');
  console.log('🔍 Check your Firebase Console to verify the accounts are saved.');
  
  return results;
}

// Run the account creation process
createAllAccounts()
  .then((results) => {
    console.log('\n✅ Script execution completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n💥 Script execution failed:', error);
    process.exit(1);
  });