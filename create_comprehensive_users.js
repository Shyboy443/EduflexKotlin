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

// Sample user data that matches the production pattern
const sampleUsers = [
  {
    email: 'admin@eduflex.com',
    fullName: 'System Administrator',
    role: 'Admin',
    password: 'admin123'
  },
  {
    email: 'teacher1@eduflex.com',
    fullName: 'Dr. Sarah Johnson',
    role: 'Teacher',
    password: 'teacher123'
  },
  {
    email: 'teacher2@eduflex.com',
    fullName: 'Prof. Michael Chen',
    role: 'Teacher',
    password: 'teacher123'
  },
  {
    email: 'teacher3@eduflex.com',
    fullName: 'Dr. Emily Rodriguez',
    role: 'Teacher',
    password: 'teacher123'
  },
  {
    email: 'student1@eduflex.com',
    fullName: 'John Smith',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student2@eduflex.com',
    fullName: 'Emma Johnson',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student3@eduflex.com',
    fullName: 'Michael Brown',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student4@eduflex.com',
    fullName: 'Sophia Davis',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student5@eduflex.com',
    fullName: 'William Wilson',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student6@eduflex.com',
    fullName: 'Olivia Miller',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student7@eduflex.com',
    fullName: 'James Garcia',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student8@eduflex.com',
    fullName: 'Isabella Martinez',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student9@eduflex.com',
    fullName: 'Benjamin Anderson',
    role: 'Student',
    password: 'student123'
  },
  {
    email: 'student10@eduflex.com',
    fullName: 'Charlotte Taylor',
    role: 'Student',
    password: 'student123'
  },
  // Keep the existing users
  {
    email: 'kaveen@gmail.com',
    fullName: 'Kaveen Perera',
    role: 'Student',
    password: 'kaveen123'
  },
  {
    email: 'ashen12@gmil.com',
    fullName: 'Ashen Senanayake',
    role: 'Student',
    password: 'ashen123'
  }
];

async function createComprehensiveUsers() {
  try {
    console.log('🔄 Creating comprehensive user accounts in emulator...\n');

    let successCount = 0;
    let errorCount = 0;

    for (const userData of sampleUsers) {
      try {
        console.log(`👤 Creating user: ${userData.email} (${userData.role})`);

        // Create user in Firebase Auth
        let authUser;
        try {
          authUser = await auth.createUser({
            email: userData.email,
            password: userData.password,
            displayName: userData.fullName,
            emailVerified: true,
            disabled: false
          });
          console.log(`  ✅ Created in Auth with UID: ${authUser.uid}`);
        } catch (authError) {
          if (authError.code === 'auth/email-already-exists') {
            console.log(`  ⚠️  User already exists in Auth: ${userData.email}`);
            // Get existing user
            authUser = await auth.getUserByEmail(userData.email);
          } else {
            throw authError;
          }
        }

        // Create user document in Firestore
        await db.collection('users').doc(authUser.uid).set({
          uid: authUser.uid,
          email: userData.email,
          fullName: userData.fullName,
          role: userData.role,
          isActive: true,
          createdAt: admin.firestore.Timestamp.now(),
          provider: 'password',
          profileData: {
            bio: `${userData.role} at EduFlex platform`,
            department: userData.role === 'Teacher' ? 'Academic' : userData.role === 'Admin' ? 'Administration' : 'Student Affairs',
            phoneNumber: `+1-555-${String(Math.floor(Math.random() * 9000) + 1000)}`,
            address: `${Math.floor(Math.random() * 999) + 1} Education St, Learning City`
          }
        });
        console.log(`  ✅ Created in Firestore`);

        successCount++;
        console.log('');

      } catch (error) {
        console.error(`  ❌ Error creating user ${userData.email}: ${error.message}`);
        errorCount++;
      }
    }

    console.log('\n📊 User Creation Summary:');
    console.log(`  ✅ Successfully created: ${successCount} users`);
    console.log(`  ❌ Failed to create: ${errorCount} users`);
    console.log(`  📊 Total processed: ${successCount + errorCount} users`);

    // Verify the creation
    console.log('\n🔍 Verifying user creation...');
    const firestoreUsersSnapshot = await db.collection('users').get();
    console.log(`✅ Firestore now has ${firestoreUsersSnapshot.size} users`);

    const authUsers = await auth.listUsers();
    console.log(`✅ Auth now has ${authUsers.users.length} users`);

    // Show user breakdown by role
    console.log('\n👥 User breakdown by role:');
    const roleCount = {};
    firestoreUsersSnapshot.forEach(doc => {
      const data = doc.data();
      roleCount[data.role] = (roleCount[data.role] || 0) + 1;
    });

    Object.entries(roleCount).forEach(([role, count]) => {
      console.log(`  ${role}: ${count} users`);
    });

    console.log('\n🎉 Comprehensive user creation completed successfully!');
    console.log('💡 You can now test user authentication and admin management.');
    console.log('\n📋 Login credentials:');
    console.log('  Admin: admin@eduflex.com / admin123');
    console.log('  Teacher: teacher1@eduflex.com / teacher123');
    console.log('  Student: student1@eduflex.com / student123');
    console.log('  (All users follow the same pattern: email / role123)');

  } catch (error) {
    console.error('❌ Error creating comprehensive users:', error);
    process.exit(1);
  }
}

// Run the creation
createComprehensiveUsers().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('💥 User creation failed:', error);
  process.exit(1);
});