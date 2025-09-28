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

async function investigateUsers() {
  try {
    console.log('🔍 Investigating User Management Issues...\n');

    // Check Firestore users collection
    console.log('📊 Checking Firestore users collection...');
    const usersSnapshot = await db.collection('users').get();
    console.log(`Found ${usersSnapshot.size} users in Firestore`);
    
    const firestoreUsers = [];
    usersSnapshot.forEach(doc => {
      const userData = doc.data();
      firestoreUsers.push({
        id: doc.id,
        email: userData.email,
        role: userData.role,
        fullName: userData.fullName,
        provider: userData.provider,
        uid: userData.uid
      });
      console.log(`  - ${userData.email} (${userData.role}) - UID: ${userData.uid || 'No UID'}`);
    });

    // Check Firebase Auth users
    console.log('\n🔐 Checking Firebase Auth users...');
    let authUsers = null;
    let authEmails = new Set();
    
    try {
      authUsers = await auth.listUsers();
      console.log(`Found ${authUsers.users.length} users in Firebase Auth`);
      
      authUsers.users.forEach(user => {
        console.log(`  - ${user.email} - UID: ${user.uid} - Provider: ${user.providerData.map(p => p.providerId).join(', ')}`);
      });

      // Compare Firestore vs Auth users
      console.log('\n🔄 Comparing Firestore vs Auth users...');
      authEmails = new Set(authUsers.users.map(u => u.email));
      const firestoreEmails = new Set(firestoreUsers.map(u => u.email));
      
      console.log('\n📋 Users in Firestore but NOT in Auth:');
      firestoreUsers.forEach(user => {
        if (!authEmails.has(user.email)) {
          console.log(`  ❌ ${user.email} (${user.role}) - Missing from Auth`);
        }
      });

      console.log('\n📋 Users in Auth but NOT in Firestore:');
      authUsers.users.forEach(user => {
        if (!firestoreEmails.has(user.email)) {
          console.log(`  ❌ ${user.email} - Missing from Firestore`);
        }
      });

      console.log('\n✅ Users in both Auth and Firestore:');
      firestoreUsers.forEach(user => {
        if (authEmails.has(user.email)) {
          console.log(`  ✅ ${user.email} (${user.role})`);
        }
      });

    } catch (authError) {
      console.error('❌ Error checking Firebase Auth:', authError.message);
      authUsers = { users: [] }; // Set default to avoid undefined errors
    }

    // Check for UID mismatches
    console.log('\n🔍 Checking for UID mismatches...');
    for (const firestoreUser of firestoreUsers) {
      if (firestoreUser.uid) {
        try {
          const authUser = await auth.getUser(firestoreUser.uid);
          if (authUser.email !== firestoreUser.email) {
            console.log(`  ⚠️ UID mismatch: Firestore ${firestoreUser.email} has UID ${firestoreUser.uid} but Auth UID belongs to ${authUser.email}`);
          }
        } catch (error) {
          console.log(`  ❌ UID ${firestoreUser.uid} for ${firestoreUser.email} not found in Auth`);
        }
      } else {
        console.log(`  ⚠️ ${firestoreUser.email} has no UID in Firestore`);
      }
    }

    console.log('\n📊 Summary:');
    console.log(`  - Firestore users: ${firestoreUsers.length}`);
    console.log(`  - Auth users: ${authUsers?.users?.length || 0}`);
    console.log(`  - Users missing from Auth: ${firestoreUsers.filter(u => !authEmails.has(u.email)).length}`);
    console.log(`  - Users missing from Firestore: ${authUsers?.users?.filter(u => !firestoreEmails.has(u.email)).length || 0}`);

  } catch (error) {
    console.error('❌ Error investigating users:', error);
  }
}

investigateUsers().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('❌ Investigation failed:', error);
  process.exit(1);
});