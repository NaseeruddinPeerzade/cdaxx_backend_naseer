package com.example.cdaxVideo.Service;

import com.example.cdaxVideo.DTO.CourseDTO;
import com.example.cdaxVideo.Entity.*;
import com.example.cdaxVideo.Entity.Module;
import com.example.cdaxVideo.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired private CourseRepository courseRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private UserCoursePurchaseRepository purchaseRepository;
    @Autowired private UserRepository userRepository;

    @Autowired private UserVideoProgressRepository userVideoProgressRepository;
    @Autowired private UserModuleProgressRepository userModuleProgressRepository;
    @Autowired private UserAssessmentProgressRepository userAssessmentProgressRepository;
    @Autowired private UserCoursePurchaseRepository userCoursePurchaseRepository;

    public List<CourseDTO> getDashboardCourses(Long userId) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new RuntimeException("User not found"));
        List<Course> courses;
        if (user.getIsNewUser() != null && user.getIsNewUser() == 1) {
            courses = courseRepository.findAll();
        } else {
            courses = courseRepository.findBySubscribedUsers_Id(userId);
        }
        return courses.stream().map(CourseDTO::new).collect(Collectors.toList());
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
public List<CourseDTO> getAvailableCoursesForUser(Long userId) {
    List<Course> allCourses = courseRepository.findAllWithModules();
    List<Course> purchased = getCoursesForUser(userId);

    Set<Long> purchasedIds = purchased.stream().map(Course::getId).collect(Collectors.toSet());

    List<Course> available = new ArrayList<>();
    for (Course c : allCourses) {
        if (!purchasedIds.contains(c.getId())) {
            available.add(c);
        }
    }
    return available.stream().map(CourseDTO::new).collect(Collectors.toList());
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

        UserCoursePurchase ucp = new UserCoursePurchase();
        User user = userRepository.findById(userId).orElseThrow();
        Course course = courseRepository.findById(courseId).orElseThrow();

        ucp.setUser(user);
        ucp.setCourse(course);

        purchaseRepository.save(ucp);

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

    List<Course> courses = getAllCoursesWithModulesAndVideos();

    for (Course c : courses) {

        boolean purchased = purchaseRepository.existsByUserIdAndCourseId(userId, c.getId());
        c.setPurchased(purchased);

        for (Module m : c.getModules()) {
            m.setVideos(videoRepository.findByModuleId(m.getId()));
        }

        applyUserProgressToCourse(c, userId);
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
        } else {
            // last video -> unlock assessment for this module for the user
            unlockAssessmentForModule(userId, moduleId);
        }

        return true;
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

    

}
