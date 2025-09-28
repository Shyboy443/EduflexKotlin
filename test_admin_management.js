const admin = require('firebase-admin');

// Set environment variables for emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

// Initialize Firebase Admin SDK
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

const db = admin.firestore();
const auth = admin.auth();

async function testAdminManagement() {
  try {
    console.log('🔍 Testing Admin User Management Functionality...\n');

    // Test 1: Check Firestore users collection
    console.log('📊 Testing Firestore users collection access...');
    const usersSnapshot = await db.collection('users').get();
    console.log(`✅ Found ${usersSnapshot.size} users in Firestore`);

    // Show user breakdown
    const roleCount = {};
    const usersList = [];
    
    usersSnapshot.forEach(doc => {
      const data = doc.data();
      roleCount[data.role] = (roleCount[data.role] || 0) + 1;
      usersList.push({
        id: doc.id,
        email: data.email,
        fullName: data.fullName,
        role: data.role,
        isActive: data.isActive
      });
    });

    console.log('\n👥 User breakdown by role:');
    Object.entries(roleCount).forEach(([role, count]) => {
      console.log(`  ${role}: ${count} users`);
    });

    // Test 2: Check Firebase Auth users
    console.log('\n🔐 Testing Firebase Auth users...');
    const authUsers = await auth.listUsers();
    console.log(`✅ Found ${authUsers.users.length} users in Firebase Auth`);

    // Test 3: Verify user data structure matches Android app expectations
    console.log('\n📋 Verifying user data structure...');
    let validUsers = 0;
    let invalidUsers = 0;

    usersList.forEach(user => {
      const hasRequiredFields = user.email && user.fullName && user.role;
      if (hasRequiredFields) {
        validUsers++;
      } else {
        invalidUsers++;
        console.log(`  ❌ Invalid user: ${user.id} - Missing required fields`);
      }
    });

    console.log(`✅ Valid users: ${validUsers}`);
    console.log(`❌ Invalid users: ${invalidUsers}`);

    // Test 4: Test user authentication
    console.log('\n🔑 Testing user authentication...');
    const testUsers = [
      { email: 'admin@eduflex.com', role: 'Admin' },
      { email: 'teacher1@eduflex.com', role: 'Teacher' },
      { email: 'student1@eduflex.com', role: 'Student' }
    ];

    for (const testUser of testUsers) {
      try {
        const authUser = await auth.getUserByEmail(testUser.email);
        console.log(`  ✅ ${testUser.role} (${testUser.email}) - Auth UID: ${authUser.uid}`);
        
        // Check if Firestore document exists
        const firestoreDoc = await db.collection('users').doc(authUser.uid).get();
        if (firestoreDoc.exists) {
          console.log(`    ✅ Firestore document exists`);
        } else {
          console.log(`    ❌ Firestore document missing`);
        }
      } catch (error) {
        console.log(`  ❌ ${testUser.role} (${testUser.email}) - Error: ${error.message}`);
      }
    }

    // Test 5: Simulate Android app query
    console.log('\n📱 Simulating Android app user management query...');
    try {
      const androidQuery = await db.collection('users').get();
      const androidUsers = [];
      
      androidQuery.forEach(doc => {
        try {
          const user = {
            id: doc.id,
            fullName: doc.data().fullName || "Unknown",
            email: doc.data().email || "No email",
            role: doc.data().role || "Student",
            isActive: doc.data().isActive !== false,
            createdAt: doc.data().createdAt || new Date()
          };
          androidUsers.push(user);
        } catch (e) {
          console.log(`    ❌ Error parsing user document: ${doc.id}`);
        }
      });

      console.log(`✅ Android app would see ${androidUsers.length} users`);
      
      // Show sample users
      console.log('\n📋 Sample users that would appear in admin management:');
      androidUsers.slice(0, 5).forEach(user => {
        console.log(`  - ${user.fullName} (${user.email}) - ${user.role} - ${user.isActive ? 'Active' : 'Inactive'}`);
      });
      
      if (androidUsers.length > 5) {
        console.log(`  ... and ${androidUsers.length - 5} more users`);
      }

    } catch (error) {
      console.log(`❌ Android simulation failed: ${error.message}`);
    }

    console.log('\n🎉 Admin Management Test Summary:');
    console.log(`  📊 Total users in database: ${usersSnapshot.size}`);
    console.log(`  🔐 Total users in auth: ${authUsers.users.length}`);
    console.log(`  ✅ Valid user records: ${validUsers}`);
    console.log(`  ❌ Invalid user records: ${invalidUsers}`);
    
    if (usersSnapshot.size > 0 && validUsers === usersSnapshot.size) {
      console.log('\n✅ Admin user management should work correctly!');
      console.log('💡 All users should be visible in the Android app admin panel.');
    } else {
      console.log('\n⚠️  There may be issues with admin user management.');
    }

  } catch (error) {
    console.error('❌ Error testing admin management:', error);
    process.exit(1);
  }
}

// Run the test
testAdminManagement().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('💥 Test failed:', error);
  process.exit(1);
});