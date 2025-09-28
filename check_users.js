const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'eduflex-f62b5'
    });
}

// Connect to Firestore emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
const db = admin.firestore();

async function checkUsers() {
    try {
        console.log('🔍 Checking users collection in Firestore...\n');
        
        const usersSnapshot = await db.collection('users').get();
        
        if (usersSnapshot.empty) {
            console.log('❌ No users found in the collection');
            return;
        }
        
        console.log(`📊 Found ${usersSnapshot.size} user documents:\n`);
        
        usersSnapshot.forEach((doc, index) => {
            const data = doc.data();
            console.log(`${index + 1}. Document ID: ${doc.id}`);
            console.log(`   📧 Email: ${data.email || 'N/A'}`);
            console.log(`   👤 Name: ${data.fullName || 'N/A'}`);
            console.log(`   🎭 Role: ${data.role || 'N/A'}`);
            console.log(`   ✅ Active: ${data.isActive || 'N/A'}`);
            console.log(`   📅 Created: ${data.createdAt ? new Date(data.createdAt.seconds * 1000).toISOString() : 'N/A'}`);
            console.log('   📋 Full data:', JSON.stringify(data, null, 2));
            console.log('   ' + '─'.repeat(50));
        });
        
    } catch (error) {
        console.error('❌ Error checking users:', error);
    }
}

checkUsers().then(() => {
    console.log('\n✅ User check completed');
    process.exit(0);
}).catch(error => {
    console.error('❌ Script failed:', error);
    process.exit(1);
});