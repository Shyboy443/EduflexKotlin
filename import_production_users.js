const admin = require('firebase-admin');
const { initializeApp } = require('firebase/app');
const { getFirestore, collection, getDocs } = require('firebase/firestore');

// Production Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyChfxtlW1XrXI5gLH7tjsxDNOTfgWyoQ0Y",
  authDomain: "eduflex-f62b5.firebaseapp.com",
  databaseURL: "https://eduflex-f62b5-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "eduflex-f62b5",
  storageBucket: "eduflex-f62b5.firebasestorage.app",
  messagingSenderId: "706298535818",
  appId: "1:706298535818:android:68de04b0808358b03c7273"
};

// Initialize production Firebase (client SDK)
const prodApp = initializeApp(firebaseConfig, 'production');
const prodDb = getFirestore(prodApp);

// Initialize emulator Firebase (admin SDK)
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

const emulatorAdmin = admin.initializeApp({
  projectId: 'eduflex-f62b5'
}, 'emulator');

const emulatorDb = emulatorAdmin.firestore();
const emulatorAuth = emulatorAdmin.auth();

async function importProductionUsers() {
  try {
    console.log('🔄 Starting production user import to emulator...\n');

    // Step 1: Get users from production Firestore
    console.log('📥 Fetching users from production database...');
    const usersSnapshot = await getDocs(collection(prodDb, 'users'));
    
    if (usersSnapshot.empty) {
      console.log('❌ No users found in production database');
      return;
    }

    console.log(`✅ Found ${usersSnapshot.size} users in production database\n`);

    // Step 2: Import each user to emulator
    let successCount = 0;
    let errorCount = 0;

    for (const doc of usersSnapshot.docs) {
      const userData = doc.data();
      const userId = doc.id;
      
      try {
        console.log(`👤 Processing user: ${userData.email || 'No email'} (${userData.role || 'No role'})`);

        // Create user in Firebase Auth emulator
        let authUser;
        try {
          authUser = await emulatorAuth.createUser({
            uid: userId,
            email: userData.email,
            displayName: userData.fullName || userData.name,
            emailVerified: true,
            disabled: !userData.isActive
          });
          console.log(`  ✅ Created in Auth with UID: ${authUser.uid}`);
        } catch (authError) {
          if (authError.code === 'auth/uid-already-exists') {
            console.log(`  ⚠️  User already exists in Auth: ${userId}`);
            authUser = await emulatorAuth.getUser(userId);
          } else {
            throw authError;
          }
        }

        // Create user document in Firestore emulator
        await emulatorDb.collection('users').doc(userId).set({
          uid: userId,
          email: userData.email,
          fullName: userData.fullName || userData.name,
          role: userData.role,
          isActive: userData.isActive !== false,
          createdAt: userData.createdAt || admin.firestore.Timestamp.now(),
          provider: userData.provider || 'password',
          // Preserve any additional fields
          ...userData
        });
        console.log(`  ✅ Created in Firestore`);

        successCount++;
        console.log('');

      } catch (error) {
        console.error(`  ❌ Error importing user ${userData.email}: ${error.message}`);
        errorCount++;
      }
    }

    console.log('\n📊 Import Summary:');
    console.log(`  ✅ Successfully imported: ${successCount} users`);
    console.log(`  ❌ Failed to import: ${errorCount} users`);
    console.log(`  📊 Total processed: ${successCount + errorCount} users`);

    // Step 3: Verify the import
    console.log('\n🔍 Verifying import...');
    const emulatorUsersSnapshot = await emulatorDb.collection('users').get();
    console.log(`✅ Emulator now has ${emulatorUsersSnapshot.size} users in Firestore`);

    const authUsers = await emulatorAuth.listUsers();
    console.log(`✅ Emulator now has ${authUsers.users.length} users in Auth`);

    console.log('\n🎉 Production user import completed successfully!');
    console.log('💡 You can now test user authentication and admin management with all production users.');

  } catch (error) {
    console.error('❌ Error importing production users:', error);
    process.exit(1);
  }
}

// Run the import
importProductionUsers().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('💥 Import failed:', error);
  process.exit(1);
});