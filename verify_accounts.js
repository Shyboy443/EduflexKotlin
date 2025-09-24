// Script to verify that sample accounts are properly saved in Firebase
const { initializeApp } = require('firebase/app');
const { getAuth, signInWithEmailAndPassword, signOut } = require('firebase/auth');
const { getFirestore, doc, getDoc, collection, query, where, getDocs } = require('firebase/firestore');

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

// Accounts to verify
const accountsToVerify = [
  { email: 'admin@eduflex.com', password: 'admin123', role: 'Admin' },
  { email: 'student@eduflex.com', password: 'student123', role: 'Student' },
  { email: 'teacher@eduflex.com', password: 'teacher123', role: 'Teacher' }
];

async function verifyAccount(accountData) {
  try {
    console.log(`\n🔍 Verifying ${accountData.role} account: ${accountData.email}`);
    
    // Test authentication
    const userCredential = await signInWithEmailAndPassword(auth, accountData.email, accountData.password);
    const user = userCredential.user;
    console.log(`✅ Authentication successful - UID: ${user.uid}`);
    
    // Check Firestore profile
    const userDoc = await getDoc(doc(db, 'users', user.uid));
    if (userDoc.exists()) {
      const userData = userDoc.data();
      console.log(`✅ Firestore profile found`);
      console.log(`   📧 Email: ${userData.email}`);
      console.log(`   👤 Name: ${userData.fullName}`);
      console.log(`   🎭 Role: ${userData.role}`);
      console.log(`   📅 Created: ${new Date(userData.createdAt).toLocaleString()}`);
      console.log(`   🟢 Active: ${userData.isActive}`);
    } else {
      console.log(`❌ Firestore profile NOT found`);
    }
    
    // Sign out
    await signOut(auth);
    
    return {
      success: true,
      email: accountData.email,
      role: accountData.role,
      uid: user.uid,
      firestoreExists: userDoc.exists()
    };
    
  } catch (error) {
    console.error(`❌ Verification failed for ${accountData.email}:`, error.message);
    return {
      success: false,
      email: accountData.email,
      role: accountData.role,
      error: error.message
    };
  }
}

async function verifyAllAccounts() {
  console.log('🔍 Starting account verification process...');
  
  const results = [];
  
  for (const accountData of accountsToVerify) {
    const result = await verifyAccount(accountData);
    results.push(result);
    
    // Small delay between verifications
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  
  // Get total user count from Firestore
  console.log('\n📊 Checking total users in database...');
  try {
    const usersSnapshot = await getDocs(collection(db, 'users'));
    console.log(`📈 Total users in Firestore: ${usersSnapshot.size}`);
    
    // Count by role
    const roleCounts = {};
    usersSnapshot.forEach(doc => {
      const userData = doc.data();
      const role = userData.role || 'Unknown';
      roleCounts[role] = (roleCounts[role] || 0) + 1;
    });
    
    console.log('📊 Users by role:');
    Object.entries(roleCounts).forEach(([role, count]) => {
      console.log(`   ${role}: ${count}`);
    });
    
  } catch (error) {
    console.error('❌ Error checking user statistics:', error.message);
  }
  
  console.log('\n' + '='.repeat(60));
  console.log('📋 ACCOUNT VERIFICATION SUMMARY');
  console.log('='.repeat(60));
  
  results.forEach((result, index) => {
    console.log(`\n${index + 1}. ${result.role.toUpperCase()} ACCOUNT:`);
    console.log(`   📧 Email: ${result.email}`);
    console.log(`   🔐 Authentication: ${result.success ? '✅ Working' : '❌ Failed'}`);
    console.log(`   💾 Firestore Profile: ${result.firestoreExists ? '✅ Exists' : '❌ Missing'}`);
    if (result.uid) {
      console.log(`   🆔 UID: ${result.uid}`);
    }
    if (result.error) {
      console.log(`   ⚠️  Error: ${result.error}`);
    }
  });
  
  const successCount = results.filter(r => r.success && r.firestoreExists).length;
  console.log(`\n📊 Verification Results: ${successCount}/${results.length} accounts fully functional`);
  
  if (successCount === results.length) {
    console.log('🎉 All accounts are properly created and saved in the database!');
  } else {
    console.log('⚠️  Some accounts may need attention.');
  }
  
  return results;
}

// Run the verification process
verifyAllAccounts()
  .then((results) => {
    console.log('\n✅ Verification completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n💥 Verification failed:', error);
    process.exit(1);
  });