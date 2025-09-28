const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
admin.initializeApp({
  projectId: 'eduflex-f62b5'
});

const db = admin.firestore();

// Configure to use local emulator
db.settings({
  host: 'localhost:8080',
  ssl: false
});

async function createSampleCourses() {
  try {
    console.log('Creating sample courses...');
    
    // Sample courses data
    const courses = [
      {
        id: 'course1',
        title: 'Introduction to JavaScript',
        instructor: 'John Smith',
        teacherId: 'teacher1',
        description: 'Learn the fundamentals of JavaScript programming',
        category: 'Programming',
        difficulty: 'Beginner',
        duration: '4 weeks',
        estimatedDuration: '4 weeks',
        thumbnailUrl: 'https://via.placeholder.com/300x200?text=JavaScript',
        isPublished: true,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        enrolledStudents: 2,
        rating: 4.5,
        price: 0.0,
        isFree: true,
        totalLessons: 10,
        courseContent: []
      },
      {
        id: 'course2',
        title: 'Advanced React Development',
        instructor: 'Jane Doe',
        teacherId: 'teacher2',
        description: 'Master advanced React concepts and patterns',
        category: 'Web Development',
        difficulty: 'Advanced',
        duration: '6 weeks',
        estimatedDuration: '6 weeks',
        thumbnailUrl: 'https://via.placeholder.com/300x200?text=React',
        isPublished: true,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        enrolledStudents: 1,
        rating: 4.8,
        price: 99.99,
        isFree: false,
        totalLessons: 15,
        courseContent: []
      },
      {
        id: 'course3',
        title: 'Python for Data Science',
        instructor: 'Dr. Smith',
        teacherId: 'teacher3',
        description: 'Learn Python programming for data analysis and machine learning',
        category: 'Data Science',
        difficulty: 'Intermediate',
        duration: '8 weeks',
        estimatedDuration: '8 weeks',
        thumbnailUrl: 'https://via.placeholder.com/300x200?text=Python',
        isPublished: true,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        enrolledStudents: 0,
        rating: 4.3,
        price: 149.99,
        isFree: false,
        totalLessons: 20,
        courseContent: []
      }
    ];

    // Create courses
    for (const course of courses) {
      await db.collection('courses').doc(course.id).set(course);
      console.log(`Created course: ${course.title}`);
    }

    console.log('Creating sample enrollments...');
    
    // Sample enrollments (assuming we have a student user)
    const enrollments = [
      {
        studentId: 'student1', // This should match an actual user ID
        courseId: 'course1',
        enrolledAt: Date.now(),
        isActive: true,
        progress: 30,
        lastAccessedAt: Date.now()
      },
      {
        studentId: 'student1',
        courseId: 'course2',
        enrolledAt: Date.now() - 86400000, // 1 day ago
        isActive: true,
        progress: 60,
        lastAccessedAt: Date.now() - 3600000 // 1 hour ago
      }
    ];

    // Create enrollments
    for (const enrollment of enrollments) {
      await db.collection('enrollments').add(enrollment);
      console.log(`Created enrollment for student ${enrollment.studentId} in course ${enrollment.courseId}`);
    }

    console.log('Sample data created successfully!');
    console.log('Courses created:', courses.length);
    console.log('Enrollments created:', enrollments.length);
    
  } catch (error) {
    console.error('Error creating sample data:', error);
  }
}

createSampleCourses();