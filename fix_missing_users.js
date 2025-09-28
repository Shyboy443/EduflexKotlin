// Script to fix missing user documents in Firestore using Admin SDK
const admin = require('firebase-admin');

// For local development with emulator, no authentication needed
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';

admin.initializeApp({
  projectId: "eduflex-f62b5"
});

const db = admin.firestore();

// Sample user data to create
const usersToCreate = [
  {
    fullName: 'System Administrator',
    email: 'admin@eduflex.com',
    role: 'Admin',
    isActive: true,
    createdAt: new Date(),
    profileData: {
      bio: 'System administrator with full access to all platform features.',
      department: 'Administration',
      permissions: ['manage_users', 'manage_courses', 'view_analytics', 'system_settings'],
      phoneNumber: '+1-555-0001',
      address: 'EduFlex Headquarters, Tech City'
    }
  },
  {
    fullName: 'John Student',
    email: 'student@eduflex.com',
    role: 'Student',
    isActive: true,
    createdAt: new Date(),
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
    fullName: 'Sarah Teacher',
    email: 'teacher@eduflex.com',
    role: 'Teacher',
    isActive: true,
    createdAt: new Date(),
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

async function testConnectivity() {
  try {
    console.log('🔍 Testing Firestore connectivity...');
    const testDoc = db.collection('connection_test').doc('test');
    await testDoc.set({ test: true, timestamp: Date.now() });
    console.log('✅ Firestore write test successful');
    
    // Clean up
    await testDoc.delete();
    console.log('✅ Firestore delete test successful');
  } catch (error) {
    throw new Error(`Firestore connectivity test failed: ${error.message}`);
  }
}

async function createUserDocument(userData) {
  try {
    // Generate a unique ID for the user document
    const userId = `user_${userData.role.toLowerCase()}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    // Prepare the document data
    const firestoreData = {
      id: userId,
      fullName: userData.fullName,
      email: userData.email,
      role: userData.role,
      isActive: userData.isActive,
      createdAt: userData.createdAt,
      lastLoginAt: new Date(),
      profilePicture: '',
      ...userData.profileData
    };
    
    // Check if a user with this email already exists
    const existingQuery = await db.collection('users').where('email', '==', userData.email).get();
    
    if (!existingQuery.empty) {
      console.log(`📝 User document already exists for ${userData.email}`);
      return { success: true, existed: true, userId: existingQuery.docs[0].id };
    }
    
    // Create the document
    await db.collection('users').doc(userId).set(firestoreData);
    console.log(`✅ Created user document for ${userData.email} with ID: ${userId}`);
    
    return { success: true, existed: false, userId };
    
  } catch (error) {
    console.error(`❌ Error creating user document for ${userData.email}:`, error);
    return { success: false, error: error.message };
  }
}

async function fixMissingUsers() {
  console.log('🔧 Starting to fix missing user documents...\n');
  
  try {
    // Test connectivity first
    await testConnectivity();
    
    const results = [];
    
    for (const userData of usersToCreate) {
      console.log(`\n🔄 Processing ${userData.role}: ${userData.email}`);
      const result = await createUserDocument(userData);
      results.push({ ...result, email: userData.email, role: userData.role });
      
      // Small delay between operations
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    console.log('\n' + '='.repeat(60));
    console.log('📋 USER DOCUMENT CREATION SUMMARY');
    console.log('='.repeat(60));
    
    results.forEach((result, index) => {
      const userData = usersToCreate[index];
      console.log(`\n${index + 1}. ${result.role.toUpperCase()} ACCOUNT:`);
      console.log(`   📧 Email: ${result.email}`);
      console.log(`   👤 Name: ${userData.fullName}`);
      if (result.success) {
        if (result.existed) {
          console.log(`   📊 Status: ✅ Document already existed`);
        } else {
          console.log(`   📊 Status: ✅ Document created successfully`);
          console.log(`   🆔 Document ID: ${result.userId}`);
        }
      } else {
        console.log(`   📊 Status: ❌ ${result.error}`);
      }
    });
    
    console.log('\n' + '='.repeat(60));
    console.log('🎉 User document creation process completed!');
    console.log('📱 You can now check the User Management interface in the app.');
    console.log('🔍 Check your Firebase Console to verify the documents are saved.');
    
    return results;
    
  } catch (error) {
    console.error('❌ Error during user document creation:', error);
    throw error;
  }
}

// Run the script
fixMissingUsers()
  .then((results) => {
    console.log('\n✅ Script execution completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n💥 Script execution failed:', error);
    process.exit(1);
  });