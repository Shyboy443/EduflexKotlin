const admin = require('firebase-admin');

// Initialize Firebase Admin SDK with project ID only (for Firestore emulator or local development)
admin.initializeApp({
  projectId: "eduflex-f62b5",
  databaseURL: "https://eduflex-f62b5-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const db = admin.firestore();

// Sample data
const courseCategories = [
  "Mathematics", "Science", "English", "History", "Geography", 
  "Computer Science", "Physics", "Chemistry", "Biology", "Art"
];

const courseTitles = {
  "Mathematics": ["Algebra Fundamentals", "Calculus I", "Statistics", "Geometry", "Trigonometry"],
  "Science": ["General Science", "Environmental Science", "Earth Science", "Space Science"],
  "English": ["English Literature", "Creative Writing", "Grammar Essentials", "Public Speaking"],
  "History": ["World History", "Ancient Civilizations", "Modern History", "Sri Lankan History"],
  "Geography": ["Physical Geography", "Human Geography", "World Geography", "Climate Studies"],
  "Computer Science": ["Programming Basics", "Web Development", "Data Structures", "Mobile Apps"],
  "Physics": ["Classical Mechanics", "Thermodynamics", "Electromagnetism", "Quantum Physics"],
  "Chemistry": ["Organic Chemistry", "Inorganic Chemistry", "Physical Chemistry", "Biochemistry"],
  "Biology": ["Cell Biology", "Genetics", "Ecology", "Human Anatomy", "Microbiology"],
  "Art": ["Drawing Fundamentals", "Digital Art", "Art History", "Sculpture", "Photography"]
};

const teacherNames = [
  "Dr. Sarah Johnson", "Prof. Michael Chen", "Ms. Emily Rodriguez", "Dr. David Kumar",
  "Prof. Lisa Thompson", "Mr. James Wilson", "Dr. Maria Garcia", "Prof. Robert Lee",
  "Ms. Jennifer Brown", "Dr. Ahmed Hassan", "Prof. Anna Kowalski", "Mr. Daniel Kim"
];

const studentNames = [
  "Alex Thompson", "Priya Patel", "Marcus Johnson", "Sophia Chen", "Ethan Williams",
  "Isabella Garcia", "Noah Brown", "Ava Davis", "Liam Miller", "Emma Wilson",
  "Oliver Martinez", "Charlotte Anderson", "William Taylor", "Amelia Thomas", "James Jackson",
  "Harper White", "Benjamin Harris", "Evelyn Martin", "Lucas Thompson", "Abigail Garcia"
];

function random(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomChoice(array) {
  return array[Math.floor(Math.random() * array.length)];
}

async function testConnectivity() {
  try {
    console.log('🔍 Testing Firestore connectivity...');
    const testDoc = db.collection('connection_test').doc('test');
    await testDoc.set({ test: true, timestamp: Date.now() });
    console.log('✅ Firestore write test successful');
    
    // Clean up
    await testDoc.delete();
    console.log('✅ Firestore delete test successful');
  } catch (error) {
    throw new Error(`Firestore connectivity test failed: ${error.message}`);
  }
}

async function createUsers() {
  console.log('👥 Creating users...');
  const batch = db.batch();
  let count = 0;
  
  // Create teachers
  teacherNames.forEach((name, index) => {
    const teacherId = `teacher_${Date.now()}_${index}`;
    const email = name.toLowerCase().replace(/\s+/g, '.').replace(/dr\.|prof\.|ms\.|mr\./g, '') + '@school.edu';
    
    const teacher = {
      id: teacherId,
      fullName: name,
      email: email,
      role: 'Teacher',
      isActive: true,
      createdAt: Date.now(),
      lastLoginAt: Date.now() - random(0, 7 * 24 * 60 * 60 * 1000),
      profilePicture: '',
      bio: 'Experienced educator passionate about teaching and student success.',
      specialization: randomChoice(courseCategories),
      yearsOfExperience: random(1, 20)
    };
    
    batch.set(db.collection('users').doc(teacherId), teacher);
    count++;
  });
  
  // Create students
  studentNames.forEach((name, index) => {
    const studentId = `student_${Date.now()}_${index}`;
    const email = name.toLowerCase().replace(/\s+/g, '.') + '@student.edu';
    
    const student = {
      id: studentId,
      fullName: name,
      email: email,
      role: 'Student',
      isActive: true,
      createdAt: Date.now(),
      lastLoginAt: Date.now() - random(0, 3 * 24 * 60 * 60 * 1000),
      profilePicture: '',
      grade: random(9, 13),
      dateOfBirth: Date.now() - random(15 * 365 * 24 * 60 * 60 * 1000, 18 * 365 * 24 * 60 * 60 * 1000),
      parentEmail: `parent.${name.toLowerCase().replace(/\s+/g, '.')}@email.com`
    };
    
    batch.set(db.collection('users').doc(studentId), student);
    count++;
  });
  
  await batch.commit();
  console.log(`✅ Created ${count} users (${teacherNames.length} teachers, ${studentNames.length} students)`);
  return count;
}

async function createCourses() {
  console.log('📚 Creating courses...');
  const batch = db.batch();
  let count = 0;
  
  courseCategories.forEach(category => {
    const titles = courseTitles[category] || [`${category} Course`];
    titles.forEach(title => {
      const courseId = `course_${category.toLowerCase().replace(/\s+/g, '_')}_${title.toLowerCase().replace(/\s+/g, '_')}_${Date.now()}`;
      
      const course = {
        id: courseId,
        title: title,
        category: category,
        description: `Comprehensive ${title} course covering fundamental concepts and practical applications.`,
        instructor: randomChoice(teacherNames),
        duration: `${random(8, 16)} weeks`,
        difficulty: randomChoice(['Beginner', 'Intermediate', 'Advanced']),
        maxStudents: random(20, 50),
        currentEnrollment: random(5, 30),
        price: Math.round((Math.random() * 450 + 50) * 100) / 100,
        rating: Math.round((Math.random() * 1.5 + 3.5) * 10) / 10,
        isActive: true,
        createdAt: Date.now(),
        startDate: Date.now() + random(0, 30 * 24 * 60 * 60 * 1000),
        endDate: Date.now() + random(60 * 24 * 60 * 60 * 1000, 120 * 24 * 60 * 60 * 1000)
      };
      
      batch.set(db.collection('courses').doc(courseId), course);
      count++;
    });
  });
  
  await batch.commit();
  console.log(`✅ Created ${count} courses`);
  return count;
}

async function createEnrollments() {
  console.log('📝 Creating enrollments...');
  
  // Get all students and courses
  const studentsSnapshot = await db.collection('users').where('role', '==', 'Student').get();
  const coursesSnapshot = await db.collection('courses').get();
  
  const students = studentsSnapshot.docs;
  const courses = coursesSnapshot.docs;
  
  const batch = db.batch();
  let count = 0;
  
  students.forEach(student => {
    const studentId = student.id;
    const enrollmentCount = random(2, 6); // Each student enrolls in 2-5 courses
    const selectedCourses = courses.sort(() => 0.5 - Math.random()).slice(0, enrollmentCount);
    
    selectedCourses.forEach(course => {
      const courseId = course.id;
      const enrollmentId = `enrollment_${studentId}_${courseId}`;
      
      const enrollment = {
        id: enrollmentId,
        studentId: studentId,
        courseId: courseId,
        enrollmentDate: Date.now() - random(0, 60 * 24 * 60 * 60 * 1000),
        status: randomChoice(['Active', 'Completed', 'In Progress']),
        progress: Math.round(Math.random() * 100 * 100) / 100,
        grade: Math.random() > 0.5 ? Math.round((Math.random() * 35 + 60) * 100) / 100 : null,
        lastAccessed: Date.now() - random(0, 7 * 24 * 60 * 60 * 1000)
      };
      
      batch.set(db.collection('enrollments').doc(enrollmentId), enrollment);
      count++;
    });
  });
  
  await batch.commit();
  console.log(`✅ Created ${count} enrollments`);
  return count;
}

async function createAssignments() {
  console.log('📋 Creating assignments...');
  
  const coursesSnapshot = await db.collection('courses').get();
  const courses = coursesSnapshot.docs;
  
  const batch = db.batch();
  let count = 0;
  
  courses.forEach(course => {
    const courseId = course.id;
    const courseTitle = course.data().title || 'Course';
    const assignmentCount = random(3, 8);
    
    for (let i = 0; i < assignmentCount; i++) {
      const assignmentId = `assignment_${courseId}_${i}`;
      
      const assignment = {
        id: assignmentId,
        courseId: courseId,
        title: `${courseTitle} Assignment ${i + 1}`,
        description: 'Complete the assigned tasks and submit your work by the due date.',
        type: randomChoice(['Quiz', 'Essay', 'Project', 'Lab Report', 'Presentation']),
        maxPoints: random(50, 200),
        dueDate: Date.now() + random(1 * 24 * 60 * 60 * 1000, 30 * 24 * 60 * 60 * 1000),
        createdAt: Date.now(),
        isActive: true,
        instructions: 'Follow the guidelines provided in class and submit your completed work.',
        submissionFormat: randomChoice(['PDF', 'Word Document', 'Online Form', 'Video'])
      };
      
      batch.set(db.collection('assignments').doc(assignmentId), assignment);
      count++;
    }
  });
  
  await batch.commit();
  console.log(`✅ Created ${count} assignments`);
  return count;
}

async function createAnalytics() {
  console.log('📊 Creating analytics...');
  
  const studentsSnapshot = await db.collection('users').where('role', '==', 'Student').get();
  const students = studentsSnapshot.docs;
  
  const batch = db.batch();
  let count = 0;
  
  students.forEach(student => {
    const studentId = student.id;
    const analyticsId = `analytics_${studentId}`;
    
    const analytics = {
      id: analyticsId,
      studentId: studentId,
      totalCoursesEnrolled: random(2, 10),
      completedCourses: random(0, 5),
      averageGrade: Math.round((Math.random() * 30 + 65) * 100) / 100,
      studyStreak: random(0, 30),
      totalStudyHours: Math.round((Math.random() * 190 + 10) * 100) / 100,
      pendingAssignments: random(0, 8),
      completedAssignments: random(5, 25),
      lastUpdated: Date.now(),
      monthlyProgress: {
        january: Math.round(Math.random() * 100 * 100) / 100,
        february: Math.round(Math.random() * 100 * 100) / 100,
        march: Math.round(Math.random() * 100 * 100) / 100,
        april: Math.round(Math.random() * 100 * 100) / 100,
        may: Math.round(Math.random() * 100 * 100) / 100,
        june: Math.round(Math.random() * 100 * 100) / 100
      }
    };
    
    batch.set(db.collection('analytics').doc(analyticsId), analytics);
    count++;
  });
  
  await batch.commit();
  console.log(`✅ Created ${count} analytics records`);
  return count;
}

async function createMaterials() {
  console.log('📄 Creating materials...');
  
  const coursesSnapshot = await db.collection('courses').get();
  const courses = coursesSnapshot.docs;
  
  const batch = db.batch();
  let count = 0;
  
  courses.forEach(course => {
    const courseId = course.id;
    const materialCount = random(3, 8);
    
    for (let i = 0; i < materialCount; i++) {
      const materialId = `material_${courseId}_${i}`;
      const materialTypes = ['video', 'document', 'quiz', 'assignment'];
      const type = randomChoice(materialTypes);
      
      const material = {
        id: materialId,
        courseId: courseId,
        title: `Course Material ${i + 1}`,
        type: type,
        url: `https://example.com/materials/${materialId}`,
        duration: type === 'video' ? random(300, 3600) : null,
        uploadedAt: Date.now(),
        isActive: true,
        description: 'Educational material for course content.',
        fileSize: random(1024 * 1024, 100 * 1024 * 1024) // 1MB to 100MB
      };
      
      batch.set(db.collection('materials').doc(materialId), material);
      count++;
    }
  });
  
  await batch.commit();
  console.log(`✅ Created ${count} materials`);
  return count;
}

async function seedDatabase() {
  try {
    console.log('🌱 Starting database seeding...');
    
    // Test connectivity
    await testConnectivity();
    
    let totalRecords = 0;
    
    // Create data in sequence
    totalRecords += await createUsers();
    totalRecords += await createCourses();
    totalRecords += await createEnrollments();
    totalRecords += await createAssignments();
    totalRecords += await createAnalytics();
    totalRecords += await createMaterials();
    
    console.log(`\n🎉 Database seeding completed successfully! Total records: ${totalRecords}`);
    
  } catch (error) {
    console.error(`\n💥 Database seeding failed: ${error.message}`);
    console.error(error.stack);
  }
}

// Run the seeder
seedDatabase().then(() => {
  console.log('✅ Seeding process finished');
  process.exit(0);
}).catch(error => {
  console.error('❌ Seeding process failed:', error);
  process.exit(1);
});