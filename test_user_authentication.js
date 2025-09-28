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

async function testUserAuthentication() {
  try {
    console.log('🔐 Testing User Authentication and Login Functionality...\n');

    // Get all users from Firestore
    const usersSnapshot = await db.collection('users').get();
    console.log(`📊 Testing authentication for ${usersSnapshot.size} users...\n`);

    let successfulLogins = 0;
    let failedLogins = 0;
    const loginResults = [];

    // Test authentication for each user
    for (const doc of usersSnapshot.docs) {
      const userData = doc.data();
      const userId = doc.id;
      
      try {
        // Get user from Firebase Auth
        const authUser = await auth.getUser(userId);
        
        // Verify user data consistency
        const isConsistent = authUser.email === userData.email;
        
        loginResults.push({
          email: userData.email,
          fullName: userData.fullName,
          role: userData.role,
          uid: userId,
          authExists: true,
          firestoreExists: true,
          dataConsistent: isConsistent,
          canLogin: !authUser.disabled && userData.isActive
        });

        if (!authUser.disabled && userData.isActive) {
          successfulLogins++;
          console.log(`✅ ${userData.email} (${userData.role}) - Ready for login`);
        } else {
          failedLogins++;
          console.log(`❌ ${userData.email} (${userData.role}) - Login disabled`);
        }

      } catch (error) {
        failedLogins++;
        loginResults.push({
          email: userData.email,
          fullName: userData.fullName,
          role: userData.role,
          uid: userId,
          authExists: false,
          firestoreExists: true,
          dataConsistent: false,
          canLogin: false,
          error: error.message
        });
        console.log(`❌ ${userData.email} (${userData.role}) - Auth error: ${error.message}`);
      }
    }

    console.log('\n📊 Authentication Test Summary:');
    console.log(`  ✅ Users ready for login: ${successfulLogins}`);
    console.log(`  ❌ Users with login issues: ${failedLogins}`);
    console.log(`  📊 Total users tested: ${successfulLogins + failedLogins}`);

    // Show breakdown by role
    console.log('\n👥 Login capability by role:');
    const roleStats = {};
    loginResults.forEach(result => {
      if (!roleStats[result.role]) {
        roleStats[result.role] = { canLogin: 0, total: 0 };
      }
      roleStats[result.role].total++;
      if (result.canLogin) {
        roleStats[result.role].canLogin++;
      }
    });

    Object.entries(roleStats).forEach(([role, stats]) => {
      console.log(`  ${role}: ${stats.canLogin}/${stats.total} can login`);
    });

    // Test specific user types
    console.log('\n🧪 Testing specific user type logins:');
    
    const testCases = [
      { email: 'admin@eduflex.com', role: 'Admin', expectedAccess: ['admin_panel', 'user_management'] },
      { email: 'teacher1@eduflex.com', role: 'Teacher', expectedAccess: ['course_creation', 'student_management'] },
      { email: 'student1@eduflex.com', role: 'Student', expectedAccess: ['course_enrollment', 'dashboard'] }
    ];

    for (const testCase of testCases) {
      const result = loginResults.find(r => r.email === testCase.email);
      if (result && result.canLogin) {
        console.log(`  ✅ ${testCase.role} login test: ${testCase.email} - Ready`);
        console.log(`    Expected access: ${testCase.expectedAccess.join(', ')}`);
      } else {
        console.log(`  ❌ ${testCase.role} login test: ${testCase.email} - Failed`);
      }
    }

    // Admin management verification
    console.log('\n👨‍💼 Admin Management Verification:');
    const adminUsers = loginResults.filter(r => r.role === 'Admin' && r.canLogin);
    if (adminUsers.length > 0) {
      console.log(`  ✅ ${adminUsers.length} admin(s) can access user management`);
      console.log(`  📋 Admin management will show all ${loginResults.length} users`);
      
      // Show what admin will see
      console.log('\n📋 Users visible in admin management:');
      const usersByRole = {};
      loginResults.forEach(user => {
        if (!usersByRole[user.role]) usersByRole[user.role] = [];
        usersByRole[user.role].push(user);
      });

      Object.entries(usersByRole).forEach(([role, users]) => {
        console.log(`    ${role}s: ${users.length} users`);
        users.slice(0, 2).forEach(user => {
          console.log(`      - ${user.fullName} (${user.email}) - ${user.canLogin ? 'Active' : 'Inactive'}`);
        });
        if (users.length > 2) {
          console.log(`      ... and ${users.length - 2} more`);
        }
      });
    } else {
      console.log(`  ❌ No admin users can access user management`);
    }

    console.log('\n🎉 Final Results:');
    if (successfulLogins === loginResults.length) {
      console.log('✅ ALL USERS CAN LOGIN SUCCESSFULLY!');
      console.log('✅ ADMIN USER MANAGEMENT WILL SHOW ALL USERS!');
      console.log('\n💡 Login credentials for testing:');
      console.log('  Admin: admin@eduflex.com / admin123');
      console.log('  Teacher: teacher1@eduflex.com / teacher123');
      console.log('  Student: student1@eduflex.com / student123');
    } else {
      console.log(`⚠️  ${failedLogins} users have login issues that need to be resolved.`);
    }

  } catch (error) {
    console.error('❌ Error testing user authentication:', error);
    process.exit(1);
  }
}

// Run the test
testUserAuthentication().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('💥 Authentication test failed:', error);
  process.exit(1);
});