const admin = require('firebase-admin');

// Set up Firebase Admin SDK for emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

const db = admin.firestore();

async function fixCoursePublication() {
  console.log('🔧 Starting course publication fix...');
  
  try {
    // Get all courses
    const coursesSnapshot = await db.collection('courses').get();
    console.log(`📚 Found ${coursesSnapshot.docs.length} total courses`);
    
    let fixedCount = 0;
    const batch = db.batch();
    
    for (const doc of coursesSnapshot.docs) {
      const data = doc.data();
      
      // Check if isPublished field is missing or not boolean true
      if (data.isPublished !== true) {
        console.log(`🔧 Fixing course: ${data.title || doc.id}`);
        
        // Update the course to be published
        batch.update(doc.ref, {
          isPublished: true,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        
        fixedCount++;
      }
    }
    
    if (fixedCount > 0) {
      await batch.commit();
      console.log(`✅ Fixed ${fixedCount} courses - set isPublished to true`);
    } else {
      console.log('✅ All courses already have correct isPublished status');
    }
    
    // Verify the fix
    console.log('\n🔍 Verifying fix...');
    const publishedCoursesSnapshot = await db.collection('courses')
      .where('isPublished', '==', true)
      .get();
    
    console.log(`📊 Published courses after fix: ${publishedCoursesSnapshot.docs.length}`);
    
    console.log('\n📋 Published courses:');
    publishedCoursesSnapshot.docs.forEach((doc, index) => {
      const data = doc.data();
      console.log(`  ${index + 1}. ${data.title} (${data.category})`);
    });
    
    console.log('\n🎉 Course publication fix completed successfully!');
    
  } catch (error) {
    console.error('❌ Error fixing course publication:', error);
  }
}

fixCoursePublication();