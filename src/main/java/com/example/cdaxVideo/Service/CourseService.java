package com.example.cdaxVideo.Service;

import com.example.cdaxVideo.DTO.CourseResponseDTO;
import com.example.cdaxVideo.Entity.*;
import com.example.cdaxVideo.Entity.Module;
import com.example.cdaxVideo.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {
    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    @Autowired private CourseRepository courseRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private UserCoursePurchaseRepository purchaseRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserVideoActivityRepository userVideoActivityRepository;
    @Autowired private UserVideoProgressRepository userVideoProgressRepository;
    @Autowired private UserModuleProgressRepository userModuleProgressRepository;
    @Autowired private UserAssessmentProgressRepository userAssessmentProgressRepository;
    @Autowired private UserCoursePurchaseRepository userCoursePurchaseRepository;

    public List<CourseResponseDTO> getDashboardCourses(Long userId) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new RuntimeException("User not found"));
        List<Course> courses;
        if (user.getIsNewUser() != null && user.getIsNewUser() == 1) {
            courses = courseRepository.findAll();
        } else {
            courses = courseRepository.findBySubscribedUsers_Id(userId);
        }   
        return courses.stream().map(CourseResponseDTO::new).collect(Collectors.toList());
    }

    public List<Course> getSubscribedCourses(Long userId) {
        List<Course> courses = courseRepository.findBySubscribedUsers_Id(userId);

        for (Course course : courses) {
            course.setPurchased(true);
            if (course.getModules() == null || course.getModules().isEmpty()) {
                continue;
            }

            Module firstModule = course.getModules().get(0);
            List<Video> videos = firstModule.getVideos();

            if (videos == null) continue;

            for (int i = 0; i < videos.size(); i++) {
                Video v = videos.get(i);
                v.setLocked(i >= 3);
            }

            firstModule.setLocked(false);
            firstModule.setAssessmentLocked(true);
        }

        return courses;
    }

    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    public List<Course> getAllCoursesWithModulesAndVideos() {
        List<Course> courses = courseRepository.findAllWithModules();
        for (Course course : courses) {
            for (Module module : course.getModules()) {
                module.setVideos(videoRepository.findByModuleId(module.getId()));
            }
        }
        return courses;
    }

    public Optional<Course> getCourseByIdWithModulesAndVideos(Long id) {
        Optional<Course> optionalCourse = courseRepository.findByIdWithModules(id);
        optionalCourse.ifPresent(course -> {
            for (Module module : course.getModules()) {
                module.setVideos(videoRepository.findByModuleId(module.getId()));
            }
        });
        return optionalCourse;
    }

// In CourseService.java
public List<Course> getPublicCourses() {
    System.out.println("📚 CourseService.getPublicCourses() called");
    
    try {
        // Get all courses with basic information (without sensitive data)
        List<Course> courses = courseRepository.findAll();
        
        // Clear any user-specific data for public access
        for (Course course : courses) {
            // Reset purchase/subscription status
            course.setPurchased(false);
            
            // You might want to clear other sensitive data if needed
            // course.setPrice(0.0); // Optionally hide pricing
            // course.setDiscountPercentage(0); // Optionally hide discounts
            
            // Keep only basic information for public view
            System.out.println("   ├─ Course: " + course.getTitle() + 
                             " (ID: " + course.getId() + ")");
        }
        
        System.out.println("   ✅ Returning " + courses.size() + " public courses");
        return courses;
        
    } catch (Exception e) {
        System.out.println("❌ Error in getPublicCourses: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>(); // Return empty list on error
    }
}
    
    // FIXED: Enhanced search with keyword support
    public List<Course> enhancedSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCoursesWithModulesAndVideos();
        }
        
        String searchTerm = keyword.trim().toLowerCase();
        List<Course> allCourses = getAllCoursesWithModulesAndVideos();
        
        return allCourses.stream()
            .filter(course -> {
                // Check title
                if (course.getTitle() != null && course.getTitle().toLowerCase().contains(searchTerm)) {
                    return true;
                }
                
                // Check tags
                if (course.getTags() != null) {
                    for (String tag : course.getTags()) {
                        if (tag.toLowerCase().contains(searchTerm)) {
                            return true;
                        }
                    }
                }
                
                // Check category
                if (course.getCategory() != null && course.getCategory().toLowerCase().contains(searchTerm)) {
                    return true;
                }
                
                // Check subcategory
                if (course.getSubCategory() != null && course.getSubCategory().toLowerCase().contains(searchTerm)) {
                    return true;
                }
                
                // Check instructor
                if (course.getInstructor() != null && course.getInstructor().toLowerCase().contains(searchTerm)) {
                    return true;
                }
                
                return false;
            })
            .sorted((c1, c2) -> {
                int score1 = calculateRelevance(c1, searchTerm);
                int score2 = calculateRelevance(c2, searchTerm);
                return Integer.compare(score2, score1);
            })
            .collect(Collectors.toList());
    }

    // FIXED: Helper method for relevance scoring
    private int calculateRelevance(Course course, String searchTerm) {
        int score = 0;
        String searchLower = searchTerm.toLowerCase();
        
        // Title matches
        if (course.getTitle() != null) {
            String titleLower = course.getTitle().toLowerCase();
            if (titleLower.equals(searchLower)) {
                score += 10;
            } else if (titleLower.contains(searchLower)) {
                score += 5;
            }
            if (titleLower.startsWith(searchLower)) {
                score += 3;
            }
        }
        
        // Tag matches
        if (course.getTags() != null) {
            for (String tag : course.getTags()) {
                String tagLower = tag.toLowerCase();
                if (tagLower.equals(searchLower)) {
                    score += 4;
                } else if (tagLower.contains(searchLower)) {
                    score += 2;
                }
            }
        }
        
        // Category matches
        if (course.getCategory() != null && course.getCategory().toLowerCase().contains(searchLower)) {
            score += 2;
        }
        
        // Subcategory matches
        if (course.getSubCategory() != null && course.getSubCategory().toLowerCase().contains(searchLower)) {
            score += 1;
        }
        
        return score;
    }

    // FIXED: Get search suggestions (for autocomplete)
    public List<String> getSearchSuggestions(String query) {
        if (query == null || query.length() < 2) {
            return new ArrayList<>();
        }
        
        String queryLower = query.toLowerCase();
        List<String> suggestions = new ArrayList<>();
        List<Course> allCourses = courseRepository.findAll();
        
        // Get course titles starting with query
        for (Course course : allCourses) {
            if (suggestions.size() >= 5) break;
            if (course.getTitle() != null && course.getTitle().toLowerCase().startsWith(queryLower)) {
                suggestions.add(course.getTitle());
            }
        }
        
        // Get matching tags
        Set<String> tagSuggestions = new HashSet<>();
        for (Course course : allCourses) {
            if (course.getTags() != null) {
                for (String tag : course.getTags()) {
                    if (tag.toLowerCase().contains(queryLower)) {
                        tagSuggestions.add(tag);
                    }
                }
            }
        }
        
        // Add tags to suggestions (limit to 5)
        int tagCount = 0;
        for (String tag : tagSuggestions) {
            if (tagCount >= 5) break;
            suggestions.add(tag);
            tagCount++;
        }
        
        // If no suggestions, add popular tags
        if (suggestions.isEmpty()) {
            List<String> popularTags = getPopularTags();
            for (int i = 0; i < Math.min(5, popularTags.size()); i++) {
                suggestions.add(popularTags.get(i));
            }
        }
        
        // Remove duplicates and limit to 10
        return suggestions.stream()
            .distinct()
            .limit(10)
            .collect(Collectors.toList());
    }

    // FIXED: Get popular tags for display
    public List<String> getPopularTags() {
        List<Course> allCourses = getAllCoursesWithModulesAndVideos();
        Map<String, Integer> tagCount = new HashMap<>();
        
        // Count tag occurrences
        for (Course course : allCourses) {
            if (course.getTags() != null) {
                for (String tag : course.getTags()) {
                    tagCount.put(tag, tagCount.getOrDefault(tag, 0) + 1);
                }
            }
        }
        
        // Sort by frequency and get top tags
        return tagCount.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue() - e1.getValue())
            .map(Map.Entry::getKey)
            .limit(15)
            .collect(Collectors.toList());
    }

    // FIXED: Get courses by specific tag
    public List<Course> getCoursesByTag(String tagName) {
        List<Course> allCourses = getAllCoursesWithModulesAndVideos();
        String tagLower = tagName.toLowerCase();
        
        return allCourses.stream()
            .filter(course -> course.getTags() != null && 
                     course.getTags().stream()
                         .anyMatch(tag -> tag.toLowerCase().contains(tagLower)))
            .collect(Collectors.toList());
    }

    // FIXED: Advanced search with filters
    public List<Course> advancedSearch(String keyword, String category, 
                                       Double minPrice, Double maxPrice, 
                                       Double minRating, String level) {
        // Start with search results or all courses
        List<Course> results = keyword != null && !keyword.isEmpty() 
            ? enhancedSearch(keyword) 
            : getAllCoursesWithModulesAndVideos();
        
        // Apply filters
        return results.stream()
            .filter(course -> category == null || category.isEmpty() || 
                     (course.getCategory() != null && course.getCategory().equalsIgnoreCase(category)))
            .filter(course -> minPrice == null || 
                     (course.getPrice() != null && course.getPrice() >= minPrice))
            .filter(course -> maxPrice == null || 
                     (course.getPrice() != null && course.getPrice() <= maxPrice))
            .filter(course -> minRating == null || 
                     (course.getRating() != null && course.getRating() >= minRating))
            .filter(course -> level == null || level.isEmpty() || 
                     (course.getLevel() != null && course.getLevel().equalsIgnoreCase(level)))
            .collect(Collectors.toList());
    }

    // FIXED: Keep your original searchCourses method for backward compatibility
    public List<Course> searchCourses(String keyword) {
        return courseRepository.findByTitleContainingIgnoreCase(keyword);
    }

    // Fetch courses user has NOT purchased yet
public List<CourseResponseDTO> getAvailableCoursesForUser(Long userId) {
    List<Course> allCourses = courseRepository.findAllWithModules();
    List<Course> purchased = getCoursesForUser(userId);

    Set<Long> purchasedIds = purchased.stream().map(Course::getId).collect(Collectors.toSet());

    List<Course> available = new ArrayList<>();
    for (Course c : allCourses) {
        if (!purchasedIds.contains(c.getId())) {
            available.add(c);
        }
    }
    return available.stream().map(CourseResponseDTO::new).collect(Collectors.toList());
}

// Dashboard stats for cards
public Map<String, Object> getDashboardStats(Long userId) {
    List<Course> courses = getCoursesForUser(userId);

    int totalCourses = courses.size();
    int inProgress = (int) courses.stream()
            .filter(c -> c.getModules().stream()
                    .anyMatch(m -> m.getVideos().stream().anyMatch(v -> !v.isLocked() && !v.isCompleted()))
            ).count();
    int totalVideos = courses.stream()
            .mapToInt(c -> c.getModules().stream()
                    .mapToInt(m -> m.getVideos().size()).sum())
            .sum();
    int completedVideos = courses.stream()
            .mapToInt(c -> c.getModules().stream()
                    .mapToInt(m -> (int) m.getVideos().stream().filter(Video::isCompleted).count()).sum())
            .sum();
    int progressPercent = totalVideos == 0 ? 0 : (completedVideos * 100 / totalVideos);

    Map<String,Object> stats = new HashMap<>();
    stats.put("totalCourses", totalCourses);
    stats.put("inProgressCourses", inProgress);
    stats.put("totalVideos", totalVideos);
    stats.put("progressPercentage", progressPercent);

    return stats;
}


    // ----- MODULE -----
    public Module saveModule(Long courseId, Module module) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid courseId"));
        module.setCourse(course);
        return moduleRepository.save(module);
    }

    public List<Module> getModulesByCourseId(Long courseId) {
        return moduleRepository.findByCourseId(courseId);
    }

    public Optional<Module> getModuleById(Long id) {
        return moduleRepository.findById(id);
    }

    // ----- VIDEO -----
    public Video saveVideo(Long moduleId, Video video) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid moduleId"));
        video.setModule(module);
        return videoRepository.save(video);
    }

    public List<Video> getVideosByModuleId(Long moduleId) {
        return videoRepository.findByModuleId(moduleId);
    }

    // ----- ASSESSMENT -----
    public Assessment saveAssessment(Long moduleId, Assessment assessment) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid moduleId"));
        assessment.setModule(module);
        return assessmentRepository.save(assessment);
    }

    public List<Assessment> getAssessmentsByModuleId(Long moduleId) {
        return assessmentRepository.findByModuleId(moduleId);
    }
    //-----------------------------------------------------------------------------

@Transactional(rollbackFor = Exception.class)
public Map<String, Object> submitAssessment(
        Long userId,
        Long assessmentId,
        Map<Long, String> answers) {
    
    try {
        System.out.println("=== ASSESSMENT SUBMIT START ===");
        
        // 1. Basic validation
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
        Module module = assessment.getModule(); // Get the module
        
        // 2. Calculate score
        List<Question> questions = questionRepository.findByAssessmentId(assessmentId);
        int totalMarks = 0;
        int obtainedMarks = 0;
        
        // 🆕 Store question-by-question results
        List<Map<String, Object>> questionResults = new ArrayList<>();
        
        for (Question q : questions) {
            totalMarks += q.getMarks();
            String userAnswer = answers.get(q.getId());
            boolean isCorrect = userAnswer != null && userAnswer.equalsIgnoreCase(q.getCorrectAnswer());
            
            if (isCorrect) {
                obtainedMarks += q.getMarks();
            }
            
            // Store question result
            Map<String, Object> qResult = new HashMap<>();
            qResult.put("questionId", q.getId());
            qResult.put("userAnswer", userAnswer);
            qResult.put("correctAnswer", q.getCorrectAnswer());
            qResult.put("correct", isCorrect);
            qResult.put("marks", q.getMarks());
            questionResults.add(qResult);
        }
        
        double percentage = (double) obtainedMarks / totalMarks * 100;
        boolean passed = percentage >= 70.0;
        
        System.out.println("Score: " + obtainedMarks + "/" + totalMarks + " = " + percentage + "%");
        
        // 3. **FORCE CREATE/UPDATE ASSESSMENT PROGRESS**
        Optional<UserAssessmentProgress> existing = userAssessmentProgressRepository
                .findByUserAndAssessment(user, assessment);
        
        UserAssessmentProgress progress;
        
        if (existing.isPresent()) {
            progress = existing.get();
            System.out.println("Found existing progress, ID=" + progress.getId());
        } else {
            System.out.println("Creating new assessment progress row");
            progress = new UserAssessmentProgress();
            progress.setUser(user);
            progress.setAssessment(assessment);
            progress.setUnlocked(true);
            progress.setUnlockedOn(new Date());
            progress = userAssessmentProgressRepository.save(progress);
            System.out.println("New progress saved with ID=" + progress.getId());
        }
        
        // 4. Update assessment progress fields
        progress.setAttempts(progress.getAttempts() == null ? 1 : progress.getAttempts() + 1);
        progress.setObtainedMarks(obtainedMarks);
        progress.setTotalMarks(totalMarks);
        progress.setPercentage(percentage);
        progress.setPassed(passed);
        progress.setSubmittedOn(new Date());
        
        if (passed && progress.getPassedOn() == null) {
            progress.setPassedOn(new Date());
        }
        
        // 5. **FORCE SAVE ASSESSMENT PROGRESS**
        userAssessmentProgressRepository.saveAndFlush(progress);
        System.out.println("Assessment progress updated successfully, ID=" + progress.getId());
        
        // 6. **UPDATE MODULE PROGRESS IF ASSESSMENT PASSED**
        boolean moduleCompleted = false;
        if (passed && module != null) {
            try {
                Optional<UserModuleProgress> moduleProgressOpt = userModuleProgressRepository
                    .findByUserAndModule(user, module);
                
                UserModuleProgress moduleProgress;
                if (moduleProgressOpt.isPresent()) {
                    moduleProgress = moduleProgressOpt.get();
                    System.out.println("Found existing module progress");
                } else {
                    moduleProgress = new UserModuleProgress();
                    moduleProgress.setUser(user);
                    moduleProgress.setModule(module);
                    System.out.println("Creating new module progress");
                }
                
                // Mark module as completed
                moduleProgress.markAsCompleted();
                moduleProgress.setAssessmentPassed(true);
                moduleProgress.setAssessmentPassedOn(new Date());
                
                userModuleProgressRepository.save(moduleProgress);
                moduleCompleted = true;
                System.out.println("✅ Module marked as completed!");
                
            } catch (Exception e) {
                System.err.println("❌ Module progress update failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 7. Unlock next module if passed
        boolean nextModuleUnlocked = false;
        if (passed && module != null) {
            try {
                nextModuleUnlocked = unlockNextModuleAfterPassing(
                    userId,
                    module.getCourse().getId(),
                    module.getId()
                );
                System.out.println("Next module unlocked: " + nextModuleUnlocked);
            } catch (Exception e) {
                System.err.println("Module unlock failed (non-critical): " + e.getMessage());
            }
        }
        
        // 8. Return comprehensive response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("passed", passed);
        response.put("obtainedMarks", obtainedMarks);
        response.put("totalMarks", totalMarks);
        response.put("percentage", percentage);
        response.put("nextModuleUnlocked", nextModuleUnlocked);
        response.put("moduleCompleted", moduleCompleted);
        response.put("questionResults", questionResults);
        response.put("message", passed 
            ? "Congratulations! You passed with " + String.format("%.1f", percentage) + "%!" 
            : "You scored " + String.format("%.1f", percentage) + "%. Need 70% to pass. Try again!");
        
        System.out.println("=== ASSESSMENT SUBMIT END ===");
        return response;
        
    } catch (Exception e) {
        System.err.println("❌ FATAL ERROR in submitAssessment:");
        e.printStackTrace();
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "Failed to submit assessment: " + e.getMessage());
        return errorResponse;
    }
}

/**
 * Unlock next module after passing assessment (≥70%)
 */
@Transactional
public boolean unlockNextModuleAfterPassing(
        Long userId,
        Long courseId,
        Long currentModuleId
) {
    System.out.println("🔓 unlockNextModuleAfterPassing START");

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

    // ✅ Fetch modules FIRST (now they come ordered from repository)
    List<Module> modules = moduleRepository.findByCourseId(courseId);

    if (modules.isEmpty()) {
        System.out.println("❌ No modules found for course");
        return false;
    }

    // ✅ Debug logging - show module order
    System.out.println("🔓 Modules order:");
    for (int i = 0; i < modules.size(); i++) {
        System.out.println("   [" + i + "] moduleId=" + modules.get(i).getId() + 
                         ", title=" + modules.get(i).getTitle());
    }

    // ✅ Find current module index
    int currentIndex = -1;
    for (int i = 0; i < modules.size(); i++) {
        if (modules.get(i).getId().equals(currentModuleId)) {
            currentIndex = i;
            break;
        }
    }

    if (currentIndex == -1) {
        System.out.println("❌ Current module not found in course modules");
        return false;
    }

    if (currentIndex + 1 >= modules.size()) {
        System.out.println("ℹ️ Current module is the last module");
        return false;
    }

    Module nextModule = modules.get(currentIndex + 1);
    System.out.println("🔓 Next module ID = " + nextModule.getId());

    // ✅ Check user-module progress
    Optional<UserModuleProgress> existingProgress =
            userModuleProgressRepository.findByUserAndModule(user, nextModule);

    if (existingProgress.isPresent() && existingProgress.get().isUnlocked()) {
        System.out.println("ℹ️ Next module already unlocked");
        return true;
    }

    // ✅ Unlock next module
    UserModuleProgress moduleProgress = existingProgress.orElseGet(() -> {
        UserModuleProgress p = new UserModuleProgress();
        p.setUser(user);
        p.setModule(nextModule);
        return p;
    });

    moduleProgress.setUnlocked(true);
    moduleProgress.setUnlockedOn(new Date());
    userModuleProgressRepository.save(moduleProgress);

    System.out.println("✅ Next module unlocked");

    // ✅ Unlock first video of next module
    List<Video> nextVideos = videoRepository.findByModuleId(nextModule.getId());
    if (!nextVideos.isEmpty()) {
        Video firstVideo = nextVideos.get(0);

        UserVideoProgress videoProgress =
                userVideoProgressRepository.findByUserAndVideo(user, firstVideo)
                        .orElseGet(() -> {
                            UserVideoProgress v = new UserVideoProgress();
                            v.setUser(user);
                            v.setVideo(firstVideo);
                            return v;
                        });

        videoProgress.setUnlocked(true);
        videoProgress.setUnlockedOn(new Date());
        userVideoProgressRepository.save(videoProgress);

        System.out.println("🎬 First video unlocked");
    }

    System.out.println("🔓 unlockNextModuleAfterPassing END");
    return true;
}

/**
 * Get assessment status for a user
 */
public Map<String, Object> getAssessmentStatus(Long userId, Long assessmentId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new RuntimeException("Assessment not found"));
    
    Optional<UserAssessmentProgress> progressOpt = userAssessmentProgressRepository
            .findByUserAndAssessment(user, assessment);
    
    Map<String, Object> status = new HashMap<>();
    status.put("assessmentId", assessmentId);
    status.put("assessmentTitle", assessment.getTitle());
    status.put("totalMarks", assessment.getTotalMarks());
    
    if (progressOpt.isPresent()) {
        UserAssessmentProgress progress = progressOpt.get();
        status.put("unlocked", progress.getUnlocked());
        status.put("attempts", progress.getAttempts());
        status.put("passed", progress.isPassed());
        status.put("obtainedMarks", progress.getObtainedMarks());
        status.put("totalMarksAttempted", progress.getTotalMarks());
        status.put("percentage", progress.getPercentage());
        status.put("lastSubmitted", progress.getSubmittedOn());
        status.put("passedOn", progress.getPassedOn());
        status.put("unlockedOn", progress.getUnlockedOn());
        
        // Calculate if user can retake
        boolean canRetake = Boolean.TRUE.equals(progress.getUnlocked()) && !progress.isPassed();
        status.put("canRetake", canRetake);
    } else {
        status.put("unlocked", false);
        status.put("attempts", 0);
        status.put("passed", false);
        status.put("canRetake", false);
    }
    
    return status;
}

/**
 * Check if user can attempt assessment (all videos completed)
 */
public boolean canAttemptAssessment(Long userId, Long assessmentId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new RuntimeException("Assessment not found"));
    
    Module module = assessment.getModule();
    
    // Check if assessment is unlocked for user
    Optional<UserAssessmentProgress> progressOpt = userAssessmentProgressRepository
            .findByUserAndAssessment(user, assessment);
    
    if (progressOpt.isPresent() && Boolean.TRUE.equals(progressOpt.get().getUnlocked())) {
        // Check if user already passed
        if (progressOpt.get().isPassed()) {
            return false; // Already passed, no need to retake unless you allow it
        }
        return true;
    }
    
    // Check if all videos in module are completed
    List<Video> moduleVideos = videoRepository.findByModuleId(module.getId());
    if (moduleVideos.isEmpty()) {
        return false;
    }
    
    int completedVideos = 0;
    for (Video video : moduleVideos) {
        Optional<UserVideoProgress> videoProgress = userVideoProgressRepository
                .findByUserAndVideo(user, video);
        
        if (videoProgress.isPresent() && videoProgress.get().isCompleted()) {
            completedVideos++;
        }
    }
    
    // All videos must be completed to attempt assessment
    return completedVideos == moduleVideos.size();
}

/**
 * Get assessment with questions for a user
 */
public Map<String, Object> getAssessmentWithQuestions(Long userId, Long assessmentId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new RuntimeException("Assessment not found"));
    
    // Check if user can attempt
    boolean canAttempt = canAttemptAssessment(userId, assessmentId);
    
    if (!canAttempt) {
        throw new RuntimeException("Cannot attempt assessment. Complete all videos first or assessment is already passed.");
    }
    
    // Get questions
    List<Question> questions = questionRepository.findByAssessmentId(assessmentId);
    
    // Prepare response
    Map<String, Object> response = new HashMap<>();
    response.put("assessment", assessment);
    response.put("questions", questions);
    response.put("totalQuestions", questions.size());
    
    // Calculate total marks
    int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
    response.put("totalMarks", totalMarks);
    
    // Get previous attempt info if any
    Optional<UserAssessmentProgress> progressOpt = userAssessmentProgressRepository
            .findByUserAndAssessment(user, assessment);
    
    if (progressOpt.isPresent()) {
        UserAssessmentProgress progress = progressOpt.get();
        response.put("previousAttempts", progress.getAttempts());
        response.put("bestScore", progress.getObtainedMarks());
        response.put("bestPercentage", progress.getPercentage());
    } else {
        response.put("previousAttempts", 0);
    }
    
    return response;
}







    // ----- QUESTION -----
    public Question saveQuestion(Long assessmentId, Question question) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid assessmentId"));
        question.setAssessment(assessment);
        return questionRepository.save(question);
    }

    public List<Question> getQuestionsByAssessmentId(Long assessmentId) {
        return questionRepository.findByAssessmentId(assessmentId);
    }


    /**
     * Purchase course:
     * - create purchase record
     * - unlock first module for user
     * - unlock first 3 videos of first module (or less if module shorter)
     */
@Transactional
public String purchaseCourse(Long userId, Long courseId) {
    boolean alreadyExists = purchaseRepository.existsByUserIdAndCourseId(userId, courseId);
    if (alreadyExists) {
        return "Already purchased";
    }

    User user = userRepository.findById(userId).orElseThrow();
    Course course = courseRepository.findById(courseId).orElseThrow();

    UserCoursePurchase ucp = new UserCoursePurchase();
    ucp.setUser(user);
    ucp.setCourse(course);

    purchaseRepository.save(ucp);

    // ✅ ADD THIS: Mark user as NOT new after purchase
    if (user.getIsNewUser() != null && user.getIsNewUser() == 1) {
        user.setIsNewUser(0); // Set to 0 (false)
        userRepository.save(user);
        logger.info("✅ User {} marked as NOT new after purchasing course: {}", user.getId(), course.getTitle());
    }

    // --- Unlock first module and first 3 videos for the user ---
    List<Module> modules = moduleRepository.findByCourseId(courseId);
    if (!modules.isEmpty()) {
        Module firstModule = modules.get(0);

        // create or update module progress (unlocked)
        UserModuleProgress ump = userModuleProgressRepository.findByUserAndModule(user, firstModule)
                .orElseGet(() -> {
                    UserModuleProgress nm = new UserModuleProgress();
                    nm.setUser(user);
                    nm.setModule(firstModule);
                    return nm;
                });
        ump.setUnlocked(true);
        ump.setUnlockedOn(new Date());
        userModuleProgressRepository.save(ump);

        // unlock first 3 videos (or fewer if module has fewer videos)
        List<Video> videos = videoRepository.findByModuleId(firstModule.getId());
        int toUnlock = Math.min(3, videos.size());
        for (int i = 0; i < toUnlock; i++) {
            Video v = videos.get(i);
            UserVideoProgress uvp = userVideoProgressRepository.findByUserAndVideo(user, v)
                    .orElseGet(() -> {
                        UserVideoProgress nv = new UserVideoProgress();
                        nv.setUser(user);
                        nv.setVideo(v);
                        return nv;
                    });
            uvp.setUnlocked(true);
            uvp.setUnlockedOn(new Date());
            userVideoProgressRepository.save(uvp);
        }
    }

    return "Purchase successful";
}

    /**
     * Return courses for user with transient flags applied so frontend can render locked/unlocked/completed state.
     */
public List<Course> getCoursesForUser(Long userId) {
    System.out.println("=== GET COURSES FOR USER " + userId + " ===");
    
    // Get all published courses WITH modules and videos
    List<Course> courses = getAllCoursesWithModulesAndVideos();
    
    System.out.println("Total courses found: " + courses.size());
    
    for (Course course : courses) {
        // Check if user purchased the course
        boolean isPurchased = userCoursePurchaseRepository
                .existsByUserIdAndCourseId(userId, course.getId());
        course.setPurchased(isPurchased);
        
        // Debug: Check modules
        System.out.println("Course: " + course.getTitle() + 
                         " | Purchased: " + isPurchased +
                         " | Modules: " + (course.getModules() != null ? course.getModules().size() : "null"));
        
        if (course.getModules() != null) {
            for (Module module : course.getModules()) {
                System.out.println("  - Module: " + module.getTitle() + 
                                 " | Videos: " + (module.getVideos() != null ? module.getVideos().size() : 0));
            }
        }
    }
    
    return courses;
}



    /**
     * Return single course with user-specific transient flags applied.
     */
    public Course getCourseForUser(Long userId, Long courseId) {

        Course course = getCourseByIdWithModulesAndVideos(courseId).orElseThrow();

        boolean purchased = purchaseRepository.existsByUserIdAndCourseId(userId, course.getId());
        course.setPurchased(purchased);

        // Ensure videos loaded and then apply user progress
        for (Module m : course.getModules()) {
            m.setVideos(videoRepository.findByModuleId(m.getId()));
        }

        applyUserProgressToCourse(course, userId);

        return course;
    }


    // Unlock a specific video for a user (creates or updates a progress record)
    @Transactional
    public boolean unlockVideoForUser(Long userId, Long courseId, Long moduleId, Long videoId) {
        User user = userRepository.findById(userId).orElseThrow();
        Video video = videoRepository.findById(videoId).orElseThrow();
        Optional<UserVideoProgress> opt = userVideoProgressRepository.findByUserAndVideo(user, video);
        UserVideoProgress p = opt.orElseGet(() -> {
            UserVideoProgress newP = new UserVideoProgress();
            newP.setUser(user);
            newP.setVideo(video);
            return newP;
        });
        p.setUnlocked(true);
        p.setUnlockedOn(new Date());
        userVideoProgressRepository.save(p);
        return true;
    }


    /**
     * Mark a video completed & unlock next video (if exists). If current is last, unlock module assessment.
     */
@Transactional
public boolean completeVideoAndUnlockNext(Long userId, Long courseId, Long moduleId, Long videoId) {
    User user = userRepository.findById(userId).orElseThrow();
    Video current = videoRepository.findById(videoId).orElseThrow();

    // mark current as completed
    UserVideoProgress currentProgress = userVideoProgressRepository.findByUserAndVideo(user, current)
        .orElseGet(() -> {
            UserVideoProgress nv = new UserVideoProgress();
            nv.setUser(user);
            nv.setVideo(current);
            return nv;
        });
        
    currentProgress.setUnlocked(true);
    currentProgress.setCompleted(true);
    currentProgress.setCompletedOn(new Date());
    userVideoProgressRepository.save(currentProgress);

    // Find module and its videos (fetch fresh list)
    Module module = moduleRepository.findById(moduleId).orElseThrow();
    List<Video> moduleVideos = videoRepository.findByModuleId(module.getId());

    int idx = -1;
    for (int i = 0; i < moduleVideos.size(); i++) {
        if (moduleVideos.get(i).getId().equals(current.getId())) {
            idx = i;
            break;
        }
    }

    // 1. Unlock next video if exists
    if (idx >= 0 && idx + 1 < moduleVideos.size()) {
        Video next = moduleVideos.get(idx + 1);
        UserVideoProgress nextProgress = userVideoProgressRepository.findByUserAndVideo(user, next)
                .orElseGet(() -> {
                    UserVideoProgress nv = new UserVideoProgress();
                    nv.setUser(user);
                    nv.setVideo(next);
                    return nv;
                });
        if (!nextProgress.isUnlocked()) {
            nextProgress.setUnlocked(true);
            nextProgress.setUnlockedOn(new Date());
            userVideoProgressRepository.save(nextProgress);
        }
    }
    
    // 2. Check if ALL videos in module are completed
    boolean allVideosCompleted = checkIfAllVideosCompleted(userId, moduleId);
    
    // 3. Only unlock assessment if ALL videos are completed
    if (allVideosCompleted) {
        unlockAssessmentForModule(userId, moduleId);
        logger.info("✅ All videos completed in module {}, assessment unlocked", moduleId);
    } else {
        logger.info("ℹ️ Module {}: {}/{} videos completed, assessment remains locked", 
                   moduleId, getCompletedVideosCount(userId, moduleId), moduleVideos.size());
    }

    return true;
}

/**
 * Check if ALL videos in a module are completed by the user
 */
private boolean checkIfAllVideosCompleted(Long userId, Long moduleId) {
    User user = userRepository.findById(userId).orElseThrow();
    List<Video> moduleVideos = videoRepository.findByModuleId(moduleId);
    
    if (moduleVideos.isEmpty()) {
        return false;
    }
    
    for (Video video : moduleVideos) {
        Optional<UserVideoProgress> progress = userVideoProgressRepository.findByUserAndVideo(user, video);
        if (progress.isEmpty() || !progress.get().isCompleted()) {
            return false; // Found a video that's not completed
        }
    }
    return true; // All videos completed
}

/**
 * Get count of completed videos in a module
 */
private int getCompletedVideosCount(Long userId, Long moduleId) {
    User user = userRepository.findById(userId).orElseThrow();
    List<Video> moduleVideos = videoRepository.findByModuleId(moduleId);
    
    int completedCount = 0;
    for (Video video : moduleVideos) {
        Optional<UserVideoProgress> progress = userVideoProgressRepository.findByUserAndVideo(user, video);
        if (progress.isPresent() && progress.get().isCompleted()) {
            completedCount++;
        }
    }
    return completedCount;
}


    /**
     * Unlock assessment for a module for a user.
     * We create (or fetch) UserAssessmentProgress record. Existence of this record implies assessment is unlocked.
     * Also create module progress (unlocked) so frontend can detect unlocked module status.
     */
 @Transactional
public boolean unlockAssessmentForModule(Long userId, Long moduleId) {

    User user = userRepository.findById(userId).orElseThrow();
    Module module = moduleRepository.findById(moduleId).orElseThrow();

    List<Assessment> assessments = assessmentRepository.findByModuleId(module.getId());

    for (Assessment a : assessments) {

        UserAssessmentProgress ap = userAssessmentProgressRepository
                .findByUserAndAssessment(user, a)
                .orElseGet(() -> {
                    UserAssessmentProgress newAP = new UserAssessmentProgress();
                    newAP.setUser(user);
                    newAP.setAssessment(a);
                    newAP.setAttempts(0);   // <-- FIXED
                    newAP.setPassed(false);
                    newAP.setPassedOn(null);
                    newAP.setUnlocked(true);
                    return newAP;
                });

        // 🔥 FINAL SAFETY — in case old row had null
        if (ap.getAttempts() == null) ap.setAttempts(0);

        ap.setUnlocked(true);
        userAssessmentProgressRepository.save(ap);
    }

    // unlock module also
    UserModuleProgress mp = userModuleProgressRepository
            .findByUserAndModule(user, module)
            .orElseGet(() -> {
                UserModuleProgress nm = new UserModuleProgress();
                nm.setUser(user);
                nm.setModule(module);
                return nm;
            });

    mp.setUnlocked(true);
    mp.setUnlockedOn(new Date());
    userModuleProgressRepository.save(mp);

    return true;
}


    /**
     * Unlock next module after assessment passed by user.
     * This should be invoked only after you mark UserAssessmentProgress.passed = true in your assessment submit handler.
     */
    @Transactional
    public boolean unlockNextModuleForUser(Long userId, Long courseId, Long moduleId) {
        User user = userRepository.findById(userId).orElseThrow();

        // find course modules ordered (we assume course.getModules() returns in order)
        Long courseIdFromModule = moduleRepository.findById(moduleId)
                .orElseThrow().getCourse().getId();
        Course course = getCourseByIdWithModulesAndVideos(courseIdFromModule).orElseThrow();
        List<Module> modules = course.getModules();

        int mIndex = -1;
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).getId().equals(moduleId)) {
                mIndex = i;
                break;
            }
        }

        if (mIndex >= 0 && mIndex + 1 < modules.size()) {
            Module nextModule = modules.get(mIndex + 1);

            // unlock next module
            UserModuleProgress mp = userModuleProgressRepository.findByUserAndModule(user, nextModule)
                    .orElseGet(() -> {
                        UserModuleProgress nm = new UserModuleProgress();
                        nm.setUser(user);
                        nm.setModule(nextModule);
                        return nm;
                    });
            mp.setUnlocked(true);
            mp.setUnlockedOn(new Date());
            userModuleProgressRepository.save(mp);

            // unlock first video of next module
            List<Video> nextVideos = videoRepository.findByModuleId(nextModule.getId());
            if (!nextVideos.isEmpty()) {
                Video first = nextVideos.get(0);
                UserVideoProgress vp = userVideoProgressRepository.findByUserAndVideo(user, first)
                        .orElseGet(() -> {
                            UserVideoProgress nv = new UserVideoProgress();
                            nv.setUser(user);
                            nv.setVideo(first);
                            return nv;
                        });
                vp.setUnlocked(true);
                vp.setUnlockedOn(new Date());
                userVideoProgressRepository.save(vp);
            }
            return true;
        }

        return false;
    }


    /**
     * Populate transient flags (isLocked/isCompleted/assessmentLocked) on modules & videos
     * based on user progress repositories so frontend can render correct state.
     *
     * Important: this uses existence of progress rows:
     *  - moduleUnlocked = exists(user,module)
     *  - assessmentUnlocked = exists(user,assessment)
     *  - video unlocked/completed = values from UserVideoProgress
     */
private void applyUserProgressToCourse(Course course, Long userId) {
    if (course == null) return;

    // ✅ FIRST: check purchase status
    boolean isPurchased =
        userCoursePurchaseRepository.existsByUserIdAndCourseId(userId, course.getId());

    System.out.println("🎯 DEBUG PURCHASE CHECK:");
    System.out.println("   ├─ User ID: " + userId);
    System.out.println("   ├─ Course ID: " + course.getId());
    System.out.println("   ├─ Course Title: " + course.getTitle());
    System.out.println("✅ Final purchase status: " + isPurchased);

    // ===============================
    // 🚫 NOT PURCHASED COURSE LOGIC
    // ===============================
    if (!isPurchased) {
        for (Module module : course.getModules()) {
            boolean isFirstModule = course.getModules().indexOf(module) == 0;
            module.setLocked(!isFirstModule);

            List<Video> videos = videoRepository.findByModuleId(module.getId());
            module.getVideos().clear();
            module.getVideos().addAll(videos);

            for (int i = 0; i < videos.size(); i++) {
                Video video = videos.get(i);

                // Only FIRST video of FIRST module unlocked
                if (isFirstModule && i == 0) {
                    video.setLocked(false);
                } else {
                    video.setLocked(true);
                }

                video.setCompleted(false);
            }
        }
        return; // ⛔ stop here
    }

    // ===============================
    // ✅ PURCHASED COURSE LOGIC
    // ===============================

    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
        System.out.println("⚠️ WARNING: User not found with ID: " + userId);
        return;
    }
    User user = userOpt.get();

    for (Module module : course.getModules()) {
// FIX: Check if module is actually unlocked in database
Optional<UserModuleProgress> moduleProgress = userModuleProgressRepository.findByUserAndModule(user, module);
boolean moduleUnlocked = moduleProgress.map(UserModuleProgress::isUnlocked).orElse(false);

// First module always unlocked for purchased courses
if (course.getModules().indexOf(module) == 0) {
    moduleUnlocked = true;
}

        module.setLocked(!moduleUnlocked);

        List<Video> videos = videoRepository.findByModuleId(module.getId());
        module.getVideos().clear();
        module.getVideos().addAll(videos);

        for (int i = 0; i < videos.size(); i++) {
            Video video = videos.get(i);
            Optional<UserVideoProgress> up =
                userVideoProgressRepository.findByUserAndVideo(user, video);

            if (up.isPresent()) {
                UserVideoProgress uvp = up.get();
                video.setLocked(!uvp.isUnlocked());
                video.setCompleted(uvp.isCompleted());
            } else {
                if (moduleUnlocked) {
                    if (course.getModules().indexOf(module) == 0) {
                        // First module: first 3 videos unlocked
                        if (i < 3) {
                            video.setLocked(false);
                        } else {
                            boolean prevCompleted =
                                checkPreviousVideosCompleted(user, module, i);
                            video.setLocked(!prevCompleted);
                        }
                    } else {
                        boolean prevModuleCompleted =
                            checkPreviousModuleCompleted(user, course, module);
                        video.setLocked(!(prevModuleCompleted && i == 0));
                    }
                } else {
                    video.setLocked(true);
                }
                video.setCompleted(false);
            }
        }
    }
}


private boolean checkPreviousVideosCompleted(User user, Module module, int currentVideoIndex) {
    if (currentVideoIndex == 0) return false; // First video
    
    List<Video> videos = videoRepository.findByModuleId(module.getId());
    
    // Check if all previous videos are completed
    for (int i = 0; i < currentVideoIndex; i++) {
        Video previousVideo = videos.get(i);
        Optional<UserVideoProgress> progress = userVideoProgressRepository.findByUserAndVideo(user, previousVideo);
        
        if (progress.isEmpty() || !progress.get().isCompleted()) {
            return false; // Previous video not completed
        }
    }
    return true; // All previous videos completed
}

private boolean checkPreviousModuleCompleted(User user, Course course, Module currentModule) {
    int currentIndex = course.getModules().indexOf(currentModule);
    if (currentIndex <= 0) return false; // First module
    
    Module previousModule = course.getModules().get(currentIndex - 1);
    List<Video> previousVideos = videoRepository.findByModuleId(previousModule.getId());
    
    // Check if all videos in previous module are completed
    for (Video video : previousVideos) {
        Optional<UserVideoProgress> progress = userVideoProgressRepository.findByUserAndVideo(user, video);
        
        if (progress.isEmpty() || !progress.get().isCompleted()) {
            return false; // Video not completed
        }
    }
    return true; // All videos in previous module completed
}

    // private void applyUserProgressToCourse(Course course, Long userId) {
    //     if (course == null) return;
    //     User user = userRepository.findById(userId).orElseThrow();

    //     List<Module> modules = course.getModules();
    //     for (Module module : modules) {

    //         // module unlocked?
    //         boolean moduleUnlocked = userModuleProgressRepository.existsByUserAndModule(user, module);
    //         module.setLocked(!moduleUnlocked);

    //         // assessment unlocked? We'll consider assessment unlocked when a UserAssessmentProgress record exists
    //         List<Assessment> assessments = assessmentRepository.findByModuleId(module.getId());
    //         boolean assessmentUnlocked = false;
    //         for (Assessment a : assessments) {
    //             if (userAssessmentProgressRepository.existsByUserAndAssessment(user, a)) {
    //                 assessmentUnlocked = true;
    //                 break;
    //             }
    //         }
    //         module.setAssessmentLocked(!assessmentUnlocked);

    //         // videos: set locked/completed from UserVideoProgress
    //         List<Video> videos = videoRepository.findByModuleId(module.getId());
    //         module.getVideos().clear();    
    //         module.getVideos().addAll(videos);
            
    //         for (Video v : videos) {
    //             Optional<UserVideoProgress> up = userVideoProgressRepository.findByUserAndVideo(user, v);
    //             if (up.isPresent()) {
    //                 UserVideoProgress uvp = up.get();
    //                 v.setLocked(!uvp.isUnlocked());
    //                 v.setCompleted(uvp.isCompleted());
    //             } else {
    //                 // default: locked unless moduleUnlocked AND there is a progress record (purchase flow creates those)
    //                 v.setLocked(true);
    //                 v.setCompleted(false);
    //             }
    //         }
    //     }
    // }


//     private void applyModulePageLocks(List<Module> modules, Long userId, Long courseId) {
//     boolean isPurchased = userCoursePurchaseRepository.existsByUserIdAndCourseId(userId, courseId);

//     for (int i = 0; i < modules.size(); i++) {
//         Module module = modules.get(i);

//         if (!isPurchased) {
//             // Lock all modules for non-purchased courses
//             module.setLocked(i != 0); // Only first module unlocked if you want
//         } else {
//             module.setLocked(false); // All modules unlocked for purchased course
//         }

//         // Lock videos accordingly
//         List<Video> videos = module.getVideos();
//         for (int j = 0; j < videos.size(); j++) {
//             Video video = videos.get(j);
//             if (!isPurchased) {
//                 video.setLocked(!(i == 0 && j == 0)); // only first video of first module unlocked
//             } else {
//                 video.setLocked(false); // all videos unlocked
//             }
//         }
//     }
// }


    public List<Module> getModulesForUser(Long userId, Long courseId) {
    List<Module> modules = moduleRepository.findByCourseId(courseId);

    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) return modules;
    User user = userOpt.get();

    boolean isPurchased = userCoursePurchaseRepository.existsByUserIdAndCourseId(userId, courseId);

    for (int mIndex = 0; mIndex < modules.size(); mIndex++) {
        Module module = modules.get(mIndex);

        boolean moduleUnlocked = isPurchased &&
                (mIndex == 0 || checkPreviousModuleCompleted(user, modules, mIndex));

        module.setLocked(!moduleUnlocked);

        List<Video> videos = videoRepository.findByModuleId(module.getId());
        module.getVideos().clear();
        module.getVideos().addAll(videos);

        for (int vIndex = 0; vIndex < videos.size(); vIndex++) {
            Video video = videos.get(vIndex);

            Optional<UserVideoProgress> progress = userVideoProgressRepository.findByUserAndVideo(user, video);
            if (progress.isPresent()) {
                video.setLocked(!progress.get().isUnlocked());
                video.setCompleted(progress.get().isCompleted());
            } else {
                if (moduleUnlocked) {
                    // first module, first 3 videos unlocked
                    if (mIndex == 0 && vIndex < 3) {
                        video.setLocked(false);
                    } else {
                        // unlock only if all previous videos in module completed
                        video.setLocked(!checkPreviousVideosCompleted(user, module, vIndex));
                    }
                } else {
                    video.setLocked(true);
                }
                video.setCompleted(false);
            }
        }
    }

    return modules;
}

// Helper for module page
private boolean checkPreviousModuleCompleted(User user, List<Module> modules, int currentIndex) {
    if (currentIndex == 0) return true;
    Module prev = modules.get(currentIndex - 1);
    List<Video> prevVideos = videoRepository.findByModuleId(prev.getId());
    for (Video v : prevVideos) {
        Optional<UserVideoProgress> p = userVideoProgressRepository.findByUserAndVideo(user, v);
        if (p.isEmpty() || !p.get().isCompleted()) return false;
    }
    return true;
}

    public List<Module> getModulesByCourseForUser(Long courseId, Long userId) {
    List<Module> modules = moduleRepository.findByCourseId(courseId);

    // Load videos for each module
    for (Module m : modules) {
        List<Video> videos = videoRepository.findByModuleId(m.getId());
        m.getVideos().clear();
        m.getVideos().addAll(videos);
    }

    // Fetch the course to reuse existing logic
    Course course = getCourseByIdWithModulesAndVideos(courseId).orElseThrow();
    applyUserProgressToCourse(course, userId);

    // Now each module in `course.getModules()` has correct locked/unlocked flags
    return course.getModules();
}




// In CourseService.java
/**
 * Calculate detailed progress for a user's course
 */
public Map<String, Object> calculateCourseProgress(Long userId, Long courseId) {
    System.out.println("🎯 Calculating progress for user " + userId + ", course " + courseId);
    
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    Course course = getCourseByIdWithModulesAndVideos(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
    
    int totalVideos = 0;
    int completedVideos = 0;
    int totalModules = course.getModules().size();
    int completedModules = 0;
    boolean isCourseCompleted = true;
    
    List<Map<String, Object>> moduleProgressList = new ArrayList<>();
    
    // Calculate progress for each module
    for (Module module : course.getModules()) {
        Map<String, Object> moduleProgress = new HashMap<>();
        moduleProgress.put("moduleId", module.getId());
        moduleProgress.put("moduleTitle", module.getTitle());
        
        // Get all videos in this module
        List<Video> videos = videoRepository.findByModuleId(module.getId());
        int moduleTotalVideos = videos.size();
        int moduleCompletedVideos = 0;
        
        // Check video completion
        for (Video video : videos) {
            totalVideos++;
            Optional<UserVideoProgress> videoProgress = userVideoProgressRepository
                    .findByUserAndVideo(user, video);
            
            if (videoProgress.isPresent() && videoProgress.get().isCompleted()) {
                completedVideos++;
                moduleCompletedVideos++;
            }
        }
        
        // Calculate module video completion percentage
        double moduleVideoPercent = moduleTotalVideos > 0 ? 
                ((double) moduleCompletedVideos / moduleTotalVideos) * 100 : 0;
        
        moduleProgress.put("totalVideos", moduleTotalVideos);
        moduleProgress.put("completedVideos", moduleCompletedVideos);
        moduleProgress.put("videoProgress", Math.round(moduleVideoPercent));
        
        // Check if module is completed (all videos + assessment if exists)
        boolean moduleCompleted = moduleCompletedVideos == moduleTotalVideos;
        
        // Check assessment completion if module has assessment
        if (moduleCompleted) {
            List<Assessment> assessments = assessmentRepository.findByModuleId(module.getId());
            if (!assessments.isEmpty()) {
                for (Assessment assessment : assessments) {
                    Optional<UserAssessmentProgress> assessmentProgress = userAssessmentProgressRepository
                            .findByUserAndAssessment(user, assessment);
                    
                    boolean assessmentPassed = assessmentProgress.isPresent() && 
                                              assessmentProgress.get().isPassed();
                    moduleCompleted = moduleCompleted && assessmentPassed;
                    
                    moduleProgress.put("assessmentPassed", assessmentPassed);
                    moduleProgress.put("assessmentScore", assessmentProgress
                            .map(ap -> ap.getPercentage())
                            .orElse(0.0));
                }
            }
        }
        
        // Check module progress in user_module_progress
        Optional<UserModuleProgress> userModuleProgress = userModuleProgressRepository
                .findByUserAndModule(user, module);
        
        boolean moduleMarkedCompleted = userModuleProgress
                .map(UserModuleProgress::isCompleted)
                .orElse(false);
        
        moduleProgress.put("completed", moduleCompleted || moduleMarkedCompleted);
        moduleProgress.put("unlocked", userModuleProgress
                .map(UserModuleProgress::isUnlocked)
                .orElse(false));
        
        if (moduleCompleted || moduleMarkedCompleted) {
            completedModules++;
        } else {
            isCourseCompleted = false;
        }
        
        moduleProgressList.add(moduleProgress);
    }
    
    // Calculate overall course progress
    double overallProgress = totalVideos > 0 ? 
            ((double) completedVideos / totalVideos) * 100 : 0.0;
    
    // Check if course is marked as completed in database
    boolean coursePurchased = purchaseRepository.existsByUserIdAndCourseId(userId, courseId);
    
    Map<String, Object> result = new HashMap<>();
    result.put("courseId", courseId);
    result.put("courseTitle", course.getTitle());
    result.put("courseDescription", course.getDescription());
    result.put("purchased", coursePurchased);
    result.put("totalModules", totalModules);
    result.put("completedModules", completedModules);
    result.put("totalVideos", totalVideos);
    result.put("completedVideos", completedVideos);
    result.put("progressPercent", Math.round(overallProgress));
    result.put("isCompleted", isCourseCompleted);
    result.put("moduleProgress", moduleProgressList);
    
    System.out.println("📊 Progress Calculation Result:");
    System.out.println("   ├─ Total Videos: " + totalVideos);
    System.out.println("   ├─ Completed Videos: " + completedVideos);
    System.out.println("   ├─ Progress: " + Math.round(overallProgress) + "%");
    System.out.println("   ├─ Total Modules: " + totalModules);
    System.out.println("   ├─ Completed Modules: " + completedModules);
    System.out.println("   └─ Course Completed: " + isCourseCompleted);
    
    return result;
}

/**
 * Get overall progress for a user across all courses
 */
public Map<String, Object> getUserOverallProgress(Long userId) {
    System.out.println("🎯 Calculating overall progress for user " + userId);
    
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Get all purchased courses
    List<Course> purchasedCourses = courseRepository.findBySubscribedUsers_Id(userId);
    
    int totalCourses = purchasedCourses.size();
    int completedCourses = 0;
    int totalVideos = 0;
    int completedVideos = 0;
    int totalModules = 0;
    int completedModules = 0;
    
    List<Map<String, Object>> courseProgressList = new ArrayList<>();
    
    for (Course course : purchasedCourses) {
        Map<String, Object> courseProgress = calculateCourseProgress(userId, course.getId());
        courseProgressList.add(courseProgress);
        
        totalVideos += (int) courseProgress.get("totalVideos");
        completedVideos += (int) courseProgress.get("completedVideos");
        totalModules += (int) courseProgress.get("totalModules");
        completedModules += (int) courseProgress.get("completedModules");
        
        if ((boolean) courseProgress.get("isCompleted")) {
            completedCourses++;
        }
    }
    
    // Calculate overall percentages
    double overallVideoProgress = totalVideos > 0 ? 
            ((double) completedVideos / totalVideos) * 100 : 0.0;
    
    double overallModuleProgress = totalModules > 0 ? 
            ((double) completedModules / totalModules) * 100 : 0.0;
    
    double overallCourseProgress = totalCourses > 0 ? 
            ((double) completedCourses / totalCourses) * 100 : 0.0;
    
    Map<String, Object> result = new HashMap<>();
    result.put("userId", userId);
    result.put("totalCourses", totalCourses);
    result.put("completedCourses", completedCourses);
    result.put("inProgressCourses", totalCourses - completedCourses);
    result.put("totalModules", totalModules);
    result.put("completedModules", completedModules);
    result.put("totalVideos", totalVideos);
    result.put("completedVideos", completedVideos);
    result.put("overallVideoProgress", Math.round(overallVideoProgress));
    result.put("overallModuleProgress", Math.round(overallModuleProgress));
    result.put("overallCourseProgress", Math.round(overallCourseProgress));
    result.put("courseProgress", courseProgressList);
    
    System.out.println("📊 Overall Progress Summary:");
    System.out.println("   ├─ Total Courses: " + totalCourses);
    System.out.println("   ├─ Completed Courses: " + completedCourses);
    System.out.println("   ├─ Total Videos: " + totalVideos);
    System.out.println("   ├─ Completed Videos: " + completedVideos);
    System.out.println("   ├─ Video Progress: " + Math.round(overallVideoProgress) + "%");
    System.out.println("   └─ Course Progress: " + Math.round(overallCourseProgress) + "%");
    
    return result;
}

public List<Map<String, Object>> getUserCourseStats(Long userId) {
    List<Map<String, Object>> courseStats = new ArrayList<>();
    
    // Get user's enrolled courses
    List<Course> enrolledCourses = courseRepository.findBySubscribedUsers_Id(userId);
    
    // Get user entity
    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
        return courseStats;
    }
    User user = userOpt.get();
    
    for (Course course : enrolledCourses) {
        Map<String, Object> stat = new HashMap<>();
        
        // Basic course info
        stat.put("courseId", course.getId());
        stat.put("courseTitle", course.getTitle());
        
        // Get total videos in course
        Long totalVideos = videoRepository.countByCourseId(course.getId());
        
        // Get completed videos from UserVideoProgress
        int completedVideos = 0;
        
        // Get all modules in course
        List<Module> modules = moduleRepository.findByCourseId(course.getId());
        
        for (Module module : modules) {
            // Get all videos in module
            List<Video> videos = videoRepository.findByModuleId(module.getId());
            
            for (Video video : videos) {
                Optional<UserVideoProgress> videoProgress = userVideoProgressRepository
                    .findByUserAndVideo(user, video);
                
                if (videoProgress.isPresent() && videoProgress.get().isCompleted()) {
                    completedVideos++;
                }
            }
        }
        
        // Get total and completed modules
        Long totalModules = moduleRepository.countByCourseId(course.getId());
        
        int completedModules = 0;
        for (Module module : modules) {
            Optional<UserModuleProgress> moduleProgress = userModuleProgressRepository
                .findByUserAndModule(user, module);
            
            if (moduleProgress.isPresent() && moduleProgress.get().isCompleted()) {
                completedModules++;
            }
        }
        
        // Calculate progress percentage
        int progressPercent = 0;
        if (totalVideos > 0) {
            progressPercent = (int) ((completedVideos * 100) / totalVideos);
        }
        
        boolean isCompleted = progressPercent >= 100;
        
        // Add to stats
        stat.put("totalVideos", totalVideos);
        stat.put("completedVideos", completedVideos);
        stat.put("totalModules", totalModules);
        stat.put("completedModules", completedModules);
        stat.put("progressPercent", progressPercent);
        stat.put("isCompleted", isCompleted);
        
        courseStats.add(stat);
    }
    
    return courseStats;
}
    

}
