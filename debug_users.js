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

async function debugUsers() {
    try {
        console.log('🔍 Debugging users collection in Firestore...\n');
        
        const usersSnapshot = await db.collection('users').get();
        
        if (usersSnapshot.empty) {
            console.log('❌ No users found in the collection');
            return;
        }
        
        console.log(`📊 Found ${usersSnapshot.size} user documents:\n`);
        
        const roleCount = { Student: 0, Teacher: 0, Admin: 0, Other: 0 };
        
        usersSnapshot.forEach((doc, index) => {
            const data = doc.data();
            const role = data.role || 'Unknown';
            
            console.log(`${index + 1}. Document ID: ${doc.id}`);
            console.log(`   📧 Email: ${data.email || 'N/A'}`);
            console.log(`   👤 Name: ${data.fullName || 'N/A'}`);
            console.log(`   🎭 Role: ${role}`);
            console.log(`   ✅ Active: ${data.isActive !== undefined ? data.isActive : 'N/A'}`);
            console.log(`   📅 Created: ${data.createdAt ? new Date(data.createdAt.seconds * 1000).toISOString() : 'N/A'}`);
            console.log('');
            
            // Count roles
            if (role === 'Student') roleCount.Student++;
            else if (role === 'Teacher') roleCount.Teacher++;
            else if (role === 'Admin') roleCount.Admin++;
            else roleCount.Other++;
        });
        
        console.log('📈 Role Summary:');
        console.log(`   👨‍🎓 Students: ${roleCount.Student}`);
        console.log(`   👨‍🏫 Teachers: ${roleCount.Teacher}`);
        console.log(`   👨‍💼 Admins: ${roleCount.Admin}`);
        console.log(`   ❓ Other: ${roleCount.Other}`);
        
        // Check if there are any other collections that might contain users
        console.log('\n🔍 Checking for other collections...');
        const collections = await db.listCollections();
        console.log('📁 Available collections:');
        collections.forEach(collection => {
            console.log(`   - ${collection.id}`);
        });
        
    } catch (error) {
        console.error('❌ Error debugging users:', error);
    }
}

debugUsers().then(() => {
    console.log('\n✅ Debug completed');
    process.exit(0);
}).catch(error => {
    console.error('❌ Script failed:', error);
    process.exit(1);
});