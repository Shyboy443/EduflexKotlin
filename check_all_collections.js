const admin = require('firebase-admin');

// Set environment variables for emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

// Initialize Firebase Admin SDK
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

const db = admin.firestore();

async function checkAllCollections() {
  try {
    console.log('🔍 Checking all collections for user data...\n');

    // Get all collections
    const collections = await db.listCollections();
    console.log(`Found ${collections.length} collections:`);
    collections.forEach(collection => {
      console.log(`  - ${collection.id}`);
    });

    console.log('\n📊 Detailed collection analysis:\n');

    // Check each collection for user-related data
    for (const collection of collections) {
      console.log(`\n📁 Collection: ${collection.id}`);
      try {
        const snapshot = await collection.get();
        console.log(`  Documents: ${snapshot.size}`);
        
        if (snapshot.size > 0 && snapshot.size <= 10) {
          // Show sample documents for small collections
          snapshot.forEach(doc => {
            const data = doc.data();
            console.log(`    - ${doc.id}:`, JSON.stringify(data, null, 2).substring(0, 200) + '...');
          });
        } else if (snapshot.size > 0) {
          // Show first few documents for large collections
          let count = 0;
          snapshot.forEach(doc => {
            if (count < 3) {
              const data = doc.data();
              console.log(`    - ${doc.id}:`, JSON.stringify(data, null, 2).substring(0, 200) + '...');
              count++;
            }
          });
          if (snapshot.size > 3) {
            console.log(`    ... and ${snapshot.size - 3} more documents`);
          }
        }
      } catch (error) {
        console.log(`  ❌ Error reading collection: ${error.message}`);
      }
    }

    // Specifically check for user-related collections
    console.log('\n🔍 Checking specific user-related collections:\n');

    const userCollections = ['users', 'students', 'teachers', 'admins'];
    
    for (const collectionName of userCollections) {
      console.log(`\n👤 ${collectionName.toUpperCase()} Collection:`);
      try {
        const snapshot = await db.collection(collectionName).get();
        console.log(`  Found ${snapshot.size} documents`);
        
        snapshot.forEach(doc => {
          const data = doc.data();
          console.log(`    - ID: ${doc.id}`);
          if (data.email) console.log(`      Email: ${data.email}`);
          if (data.fullName) console.log(`      Name: ${data.fullName}`);
          if (data.role) console.log(`      Role: ${data.role}`);
          if (data.uid) console.log(`      UID: ${data.uid}`);
          if (data.provider) console.log(`      Provider: ${data.provider}`);
          console.log('');
        });
      } catch (error) {
        console.log(`  ❌ Collection doesn't exist or error: ${error.message}`);
      }
    }

  } catch (error) {
    console.error('❌ Error checking collections:', error);
  }
}

checkAllCollections().then(() => {
  process.exit(0);
}).catch(error => {
  console.error('❌ Check failed:', error);
  process.exit(1);
});