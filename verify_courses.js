const admin = require('firebase-admin');

process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
admin.initializeApp({ projectId: 'eduflex-f62b5' });
const db = admin.firestore();

async function testStudentCoursesQuery() {
  console.log('🔍 Testing StudentCoursesFragment query...');
  
  try {
    // This is the exact query used by StudentCoursesFragment
    const snapshot = await db.collection('courses')
      .where('isPublished', '==', true)
      .get();
    
    console.log(`📊 Query returned ${snapshot.docs.length} published courses`);
    
    if (snapshot.docs.length >= 7) {
      console.log('✅ SUCCESS: More than 7 courses found - the issue is resolved!');
    } else {
      console.log('❌ ISSUE: Still less than 7 courses found');
    }
    
    console.log('\n📋 Sample courses that will be visible:');
    snapshot.docs.slice(0, 10).forEach((doc, index) => {
      const data = doc.data();
      console.log(`  ${index + 1}. ${data.title} (${data.category})`);
    });
    
    if (snapshot.docs.length > 10) {
      console.log(`  ... and ${snapshot.docs.length - 10} more courses`);
    }
    
  } catch (error) {
    console.error('❌ Error testing query:', error);
  }
}

testStudentCoursesQuery();