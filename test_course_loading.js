const admin = require('firebase-admin');

// Initialize Firebase Admin SDK for emulator
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
admin.initializeApp({
    projectId: 'eduflex-f62b5'
});

const db = admin.firestore();

async function testCourseLoading() {
    console.log('=== Testing Course Loading Functionality ===\n');
    
    try {
        // Test 1: Check published courses (what the app should see)
        console.log('1. Testing published courses query (what StudentCoursesFragment uses):');
        const publishedCoursesSnapshot = await db.collection('courses')
            .where('isPublished', '==', true)
            .get();
        
        console.log(`   Found ${publishedCoursesSnapshot.docs.length} published courses`);
        
        publishedCoursesSnapshot.docs.forEach((doc, index) => {
            const data = doc.data();
            console.log(`   ${index + 1}. ${data.title} by ${data.instructor}`);
            console.log(`      - Category: ${data.category}`);
            console.log(`      - Difficulty: ${data.difficulty}`);
            console.log(`      - Duration: ${data.duration || data.estimatedDuration || 'N/A'}`);
            console.log(`      - Enrolled Students: ${data.enrolledStudents || 0}`);
            console.log(`      - Published: ${data.isPublished}`);
            console.log('');
        });
        
        // Test 2: Check enrollments for filtering
        console.log('2. Testing enrollments (for filtering in StudentDashboardFragment):');
        const enrollmentsSnapshot = await db.collection('enrollments').get();
        console.log(`   Found ${enrollmentsSnapshot.docs.length} total enrollments`);
        
        const enrollmentsByStudent = {};
        enrollmentsSnapshot.docs.forEach(doc => {
            const data = doc.data();
            if (!enrollmentsByStudent[data.studentId]) {
                enrollmentsByStudent[data.studentId] = [];
            }
            enrollmentsByStudent[data.studentId].push(data.courseId);
        });
        
        Object.keys(enrollmentsByStudent).forEach(studentId => {
            console.log(`   Student ${studentId}: enrolled in ${enrollmentsByStudent[studentId].length} courses`);
        });
        
        // Test 3: Simulate what StudentDashboardFragment should show (popular courses)
        console.log('\n3. Testing popular courses logic (StudentDashboardFragment):');
        const allPublishedCourses = publishedCoursesSnapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
        }));
        
        // Sort by enrolled students (popularity)
        const popularCourses = allPublishedCourses
            .sort((a, b) => (b.enrolledStudents || 0) - (a.enrolledStudents || 0))
            .slice(0, 6);
        
        console.log(`   Top ${popularCourses.length} popular courses:`);
        popularCourses.forEach((course, index) => {
            console.log(`   ${index + 1}. ${course.title} (${course.enrolledStudents || 0} students)`);
        });
        
        // Test 4: Check user authentication data
        console.log('\n4. Testing user data (for authentication):');
        const usersSnapshot = await db.collection('users').limit(3).get();
        console.log(`   Found ${usersSnapshot.docs.length} users (showing first 3):`);
        
        usersSnapshot.docs.forEach((doc, index) => {
            const data = doc.data();
            console.log(`   ${index + 1}. ${data.email} (${data.role})`);
        });
        
        console.log('\n=== Test Summary ===');
        console.log(`✓ Published courses: ${publishedCoursesSnapshot.docs.length}`);
        console.log(`✓ Total enrollments: ${enrollmentsSnapshot.docs.length}`);
        console.log(`✓ Popular courses available: ${popularCourses.length}`);
        console.log(`✓ Test users available: ${usersSnapshot.docs.length}`);
        
        if (publishedCoursesSnapshot.docs.length > 0) {
            console.log('\n✅ Course data is available and should load in the app!');
            console.log('\nTo test in the app:');
            console.log('1. Log in as a student (e.g., student1@example.com / password123)');
            console.log('2. Check the Dashboard for popular courses');
            console.log('3. Go to Courses tab to see all available courses');
        } else {
            console.log('\n❌ No published courses found. The app will show empty states.');
        }
        
    } catch (error) {
        console.error('Error testing course loading:', error);
    }
}

testCourseLoading();