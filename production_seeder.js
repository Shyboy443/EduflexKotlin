// Production Firebase seeder script
const { initializeApp } = require('firebase/app');
const { getFirestore, collection, addDoc, doc, setDoc } = require('firebase/firestore');
const { getAuth, signInWithEmailAndPassword } = require('firebase/auth');

// Firebase configuration (from google-services.json)
const firebaseConfig = {
  apiKey: "AIzaSyChfxtlW1XrXI5gLH7tjsxDNOTfgWyoQ0Y",
  authDomain: "eduflex-f62b5.firebaseapp.com",
  databaseURL: "https://eduflex-f62b5-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "eduflex-f62b5",
  storageBucket: "eduflex-f62b5.firebasestorage.app",
  messagingSenderId: "706298535818",
  appId: "1:706298535818:android:68de04b0808358b03c7273"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// Admin credentials for authentication
const ADMIN_EMAIL = 'admin@eduflex.com';
const ADMIN_PASSWORD = 'admin123';

// Sample data
const categories = [
  'Programming', 'Data Science', 'Web Development', 'Mobile Development', 
  'Machine Learning', 'Cybersecurity', 'Cloud Computing', 'DevOps', 
  'UI/UX Design', 'Digital Marketing'
];

const courseTitles = [
  'Introduction to JavaScript', 'Python for Beginners', 'React Native Development',
  'Data Analysis with Python', 'Machine Learning Fundamentals', 'Cybersecurity Basics',
  'AWS Cloud Practitioner', 'Docker and Kubernetes', 'UI/UX Design Principles',
  'Digital Marketing Strategy', 'Advanced Java Programming', 'iOS Development with Swift',
  'Angular Framework', 'Database Design', 'Artificial Intelligence', 'Network Security',
  'Google Cloud Platform', 'CI/CD Pipeline', 'Graphic Design', 'Social Media Marketing'
];

const teacherNames = [
  'Dr. Sarah Johnson', 'Prof. Michael Chen', 'Dr. Emily Rodriguez', 'Prof. David Kim',
  'Dr. Lisa Wang', 'Prof. James Wilson', 'Dr. Maria Garcia', 'Prof. Robert Taylor',
  'Dr. Jennifer Lee', 'Prof. Christopher Brown'
];

const difficulties = ['Beginner', 'Intermediate', 'Advanced'];

function getRandomElement(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function getRandomNumber(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function generateCourseId() {
  return Math.random().toString(36).substr(2, 9);
}

async function createCourse() {
  const courseId = generateCourseId();
  const category = getRandomElement(categories);
  const title = getRandomElement(courseTitles);
  const instructor = getRandomElement(teacherNames);
  const difficulty = getRandomElement(difficulties);
  const duration = getRandomNumber(4, 16);
  const maxStudents = getRandomNumber(20, 100);
  const currentEnrollment = getRandomNumber(0, maxStudents);
  const price = getRandomNumber(29, 299);
  const rating = (Math.random() * 2 + 3).toFixed(1); // 3.0 to 5.0
  const teacherId = `teacher_${Math.random().toString(36).substr(2, 9)}`;

  // Create EnhancedCourse-compatible structure
  const course = {
    id: courseId,
    title: title,
    subtitle: `Master ${title} fundamentals`,
    description: `Learn ${title.toLowerCase()} with hands-on projects and real-world examples. This comprehensive course covers all essential concepts and practical applications.`,
    longDescription: `This comprehensive ${title} course is designed for learners who want to master the fundamentals and advance their skills. Through hands-on projects, real-world examples, and expert guidance, you'll gain practical experience that you can apply immediately in your career.`,
    
    // Instructor as nested object
    instructor: instructor,
    teacherId: teacherId,
    
    // Category as string (will be converted to CourseCategory in app)
    category: category,
    subcategory: "",
    
    // Difficulty as string (will be converted to enum in app)
    difficulty: difficulty,
    language: "en",
    
    // Duration and pricing as nested-compatible structure
    duration: `${duration}h`,
    
    // Pricing structure
    price: price,
    currency: "USD",
    isFree: price === 0,
    
    // URLs and media
    thumbnailUrl: `https://picsum.photos/400/300?random=${courseId}`,
    previewVideoUrl: "",
    images: [],
    
    // Course metadata
    tags: [category.toLowerCase(), difficulty.toLowerCase()],
    learningObjectives: [
      `Understand core ${title} concepts`,
      `Build practical projects`,
      `Apply best practices`,
      `Solve real-world problems`
    ],
    prerequisites: difficulty === 'Beginner' ? [] : ['Basic programming knowledge'],
    targetAudience: [`${difficulty} level learners`, 'Professionals seeking to upskill'],
    
    // Settings object for EnhancedCourse compatibility
    settings: {
      isPublished: true,
      allowEnrollment: true,
      maxStudents: maxStudents,
      autoApproveEnrollment: true
    },
    
    // Enrollment info
    enrollmentInfo: {
      totalEnrolled: currentEnrollment,
      maxStudents: maxStudents,
      waitlistCount: 0
    },
    
    // Course structure
    courseStructure: {
      totalDuration: duration * 3600000, // Convert hours to milliseconds
      moduleCount: Math.floor(duration / 2), // Roughly 2 hours per module
      lessonCount: duration * 3, // Roughly 3 lessons per hour
      totalModules: Math.floor(duration / 2)
    },
    
    // Analytics
    analytics: {
      totalViews: getRandomNumber(100, 1000),
      averageRating: parseFloat(rating),
      totalRatings: getRandomNumber(10, 100),
      completionRate: Math.random() * 0.4 + 0.6 // 60-100%
    },
    
    // Status and timestamps
    status: "PUBLISHED",
    isActive: true,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    publishedAt: Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000, // Published within last 30 days
    version: "1.0"
  };

  try {
    await setDoc(doc(db, 'courses', courseId), course);
    console.log(`✓ Created course: ${title} (${courseId})`);
    return course;
  } catch (error) {
    console.error(`✗ Failed to create course ${title}:`, error);
    throw error;
  }
}

async function seedDatabase() {
  console.log('🌱 Starting production database seeding...');
  console.log(`📡 Connecting to Firebase project: ${firebaseConfig.projectId}`);
  
  try {
    // First, authenticate as admin
    console.log('🔐 Authenticating as admin...');
    await signInWithEmailAndPassword(auth, ADMIN_EMAIL, ADMIN_PASSWORD);
    console.log('✅ Successfully authenticated as admin');
    
    const courses = [];
    const numberOfCourses = 43;
    
    console.log(`📚 Creating ${numberOfCourses} courses...`);
    
    for (let i = 0; i < numberOfCourses; i++) {
      const course = await createCourse();
      courses.push(course);
      
      // Add a small delay to avoid rate limiting
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    
    console.log(`\n🎉 Successfully created ${courses.length} courses in production database!`);
    console.log('\n📊 Course distribution by category:');
    
    const categoryCount = {};
    courses.forEach(course => {
      categoryCount[course.category] = (categoryCount[course.category] || 0) + 1;
    });
    
    Object.entries(categoryCount).forEach(([category, count]) => {
      console.log(`  ${category}: ${count} courses`);
    });
    
    console.log('\n✅ Production database seeding completed successfully!');
    console.log('🔄 You can now test the app - courses should appear in the Student Dashboard.');
    
  } catch (error) {
    console.error('❌ Error seeding production database:', error);
    if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password') {
      console.error('💡 Make sure the admin account exists. Run create_sample_accounts.js first.');
    }
    process.exit(1);
  }
}

// Run the seeder
seedDatabase().then(() => {
  console.log('🏁 Seeding process finished.');
  process.exit(0);
}).catch((error) => {
  console.error('💥 Seeding process failed:', error);
  process.exit(1);
});