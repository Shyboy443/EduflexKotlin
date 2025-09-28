const admin = require('firebase-admin');

// For local development with emulator, no authentication needed
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';

admin.initializeApp({
  projectId: "eduflex-f62b5"
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

async function seedDatabase() {
  try {
    console.log('🌱 Starting database seeding...');
    
    await testConnectivity();
    
    const courseCount = await createCourses();
    
    console.log(`🎉 Database seeding completed successfully!`);
    console.log(`📊 Summary: ${courseCount} courses created`);
    
  } catch (error) {
    console.error('💥 Database seeding failed:', error.message);
    console.error(error);
  } finally {
    console.log('✅ Seeding process finished');
    process.exit(0);
  }
}

// Run the seeding process
seedDatabase();