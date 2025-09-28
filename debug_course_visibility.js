const admin = require('firebase-admin');

// Initialize Firebase Admin SDK for emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
admin.initializeApp({
    projectId: 'eduflex-f62b5'
});

const db = admin.firestore();

async function debugCourseVisibility() {
    console.log('=== ADVANCED COURSE VISIBILITY DEBUGGING ===\n');
    
    try {
        // Step 1: Get ALL courses (no filters)
        console.log('1. CHECKING ALL COURSES IN DATABASE:');
        console.log('=====================================');
        const allCoursesSnapshot = await db.collection('courses').get();
        console.log(`Total courses in database: ${allCoursesSnapshot.docs.length}\n`);
        
        const allCourses = [];
        allCoursesSnapshot.docs.forEach((doc, index) => {
            const data = doc.data();
            allCourses.push({
                id: doc.id,
                ...data
            });
            
            console.log(`Course ${index + 1}: ${data.title || 'NO TITLE'}`);
            console.log(`  - ID: ${doc.id}`);
            console.log(`  - Instructor: ${data.instructor || 'NO INSTRUCTOR'}`);
            console.log(`  - Category: ${data.category || 'NO CATEGORY'}`);
            console.log(`  - isPublished: ${data.isPublished}`);
            console.log(`  - Published field type: ${typeof data.isPublished}`);
            console.log(`  - Created At: ${data.createdAt ? new Date(data.createdAt).toISOString() : 'NO DATE'}`);
            console.log(`  - All fields: ${Object.keys(data).join(', ')}`);
            console.log('');
        });
        
        // Step 2: Test the exact query used by StudentCoursesFragment
        console.log('2. TESTING STUDENT COURSES FRAGMENT QUERY:');
        console.log('==========================================');
        const publishedCoursesSnapshot = await db.collection('courses')
            .where('isPublished', '==', true)
            .get();
        
        console.log(`Courses returned by isPublished == true query: ${publishedCoursesSnapshot.docs.length}\n`);
        
        publishedCoursesSnapshot.docs.forEach((doc, index) => {
            const data = doc.data();
            console.log(`Published Course ${index + 1}: ${data.title}`);
            console.log(`  - ID: ${doc.id}`);
            console.log(`  - isPublished: ${data.isPublished}`);
        });
        
        // Step 3: Analyze the differences
        console.log('\n3. ANALYZING MISSING COURSES:');
        console.log('=============================');
        const publishedCourseIds = publishedCoursesSnapshot.docs.map(doc => doc.id);
        const missingCourses = allCourses.filter(course => !publishedCourseIds.includes(course.id));
        
        console.log(`Missing courses (${missingCourses.length}):`);
        missingCourses.forEach((course, index) => {
            console.log(`Missing Course ${index + 1}: ${course.title || 'NO TITLE'}`);
            console.log(`  - ID: ${course.id}`);
            console.log(`  - isPublished: ${course.isPublished} (type: ${typeof course.isPublished})`);
            console.log(`  - Reason: ${course.isPublished === true ? 'UNKNOWN - should be visible!' : 'Not published or invalid isPublished value'}`);
            console.log('');
        });
        
        // Step 4: Test alternative queries
        console.log('4. TESTING ALTERNATIVE QUERIES:');
        console.log('===============================');
        
        // Test with string "true"
        const stringTrueQuery = await db.collection('courses')
            .where('isPublished', '==', 'true')
            .get();
        console.log(`Courses with isPublished == "true" (string): ${stringTrueQuery.docs.length}`);
        
        // Test with no filter (all courses)
        const allCoursesQuery = await db.collection('courses').get();
        console.log(`All courses (no filter): ${allCoursesQuery.docs.length}`);
        
        // Test courses that have isPublished field
        const coursesWithPublishedField = allCourses.filter(course => 
            course.hasOwnProperty('isPublished')
        );
        console.log(`Courses with isPublished field: ${coursesWithPublishedField.length}`);
        
        // Test courses with truthy isPublished
        const coursesWithTruthyPublished = allCourses.filter(course => 
            course.isPublished
        );
        console.log(`Courses with truthy isPublished: ${coursesWithTruthyPublished.length}`);
        
        // Step 5: Check for data type issues
        console.log('\n5. DATA TYPE ANALYSIS:');
        console.log('=====================');
        const publishedFieldAnalysis = {};
        allCourses.forEach(course => {
            const type = typeof course.isPublished;
            const value = course.isPublished;
            const key = `${type}:${value}`;
            
            if (!publishedFieldAnalysis[key]) {
                publishedFieldAnalysis[key] = [];
            }
            publishedFieldAnalysis[key].push(course.title || course.id);
        });
        
        console.log('isPublished field analysis:');
        Object.keys(publishedFieldAnalysis).forEach(key => {
            console.log(`  ${key}: ${publishedFieldAnalysis[key].length} courses`);
            publishedFieldAnalysis[key].forEach(title => {
                console.log(`    - ${title}`);
            });
        });
        
        // Step 6: Recommendations
        console.log('\n6. RECOMMENDATIONS:');
        console.log('==================');
        
        if (missingCourses.length > 0) {
            console.log('❌ ISSUES FOUND:');
            missingCourses.forEach(course => {
                if (course.isPublished !== true) {
                    console.log(`  - Course "${course.title || course.id}" has isPublished = ${course.isPublished} (${typeof course.isPublished})`);
                    console.log(`    Fix: Set isPublished to boolean true`);
                }
            });
            
            console.log('\n🔧 SUGGESTED FIXES:');
            console.log('1. Update courses with incorrect isPublished values');
            console.log('2. Ensure all published courses have isPublished: true (boolean)');
            console.log('3. Re-run the seeding script if needed');
        } else {
            console.log('✅ All courses are properly published and should be visible');
        }
        
    } catch (error) {
        console.error('Error during debugging:', error);
    }
}

debugCourseVisibility();