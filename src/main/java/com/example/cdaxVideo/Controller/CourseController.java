    package com.example.cdaxVideo.Controller;

    import com.example.cdaxVideo.DTO.CourseResponseDTO;
    import com.example.cdaxVideo.DTO.ModuleResponseDTO;
    import com.example.cdaxVideo.DTO.StreakDayDTO;
    import com.example.cdaxVideo.DTO.StreakSummaryDTO;
    import com.example.cdaxVideo.DTO.VideoResponseDTO;
    import com.example.cdaxVideo.Entity.*;
    import com.example.cdaxVideo.Repository.CourseRepository;
    import com.example.cdaxVideo.Repository.UserRepository;
    import com.example.cdaxVideo.Repository.VideoRepository;
    import com.example.cdaxVideo.Repository.AssessmentRepository;
    import com.example.cdaxVideo.Repository.UserVideoProgressRepository;
    import com.example.cdaxVideo.Entity.Module;
    import com.example.cdaxVideo.Repository.UserCoursePurchaseRepository;
    import com.example.cdaxVideo.Service.CourseService;
    import com.example.cdaxVideo.Service.StreakService;
    import com.example.cdaxVideo.DTO.StreakSummaryDTO;
    import com.example.cdaxVideo.DTO.StreakDayDTO;
    import java.time.LocalDate;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;


import java.util.*;
    import java.util.stream.Collectors;

    @RestController
    @RequestMapping("/api")
    @CrossOrigin(origins = "*")
    public class CourseController {

        @Autowired
        private CourseService courseService;

        @Autowired
        private UserCoursePurchaseRepository userCoursePurchaseRepository; 

        @Autowired
        private UserRepository userRepository; 

        @Autowired
        private AssessmentRepository assessmentRepository; 

        @Autowired
        private VideoRepository videoRepository; 

        @Autowired
        private UserVideoProgressRepository userVideoProgressRepository; 

        @Autowired
        private CourseRepository courseRepository; 

        @Autowired
        private StreakService streakService;

        // ---------------------- COURSE APIs ----------------------
        @PostMapping("/courses")
        public ResponseEntity<Course> createCourse(@RequestBody Course course) {
            return ResponseEntity.ok(courseService.saveCourse(course));
        }

        @GetMapping("/dashboard/courses")
        public ResponseEntity<Map<String, Object>> getDashboardCourses(@RequestParam Long userId) {
            Map<String, Object> response = new HashMap<>();
            response.put("data", courseService.getCoursesForUser(userId));
            return ResponseEntity.ok(response);
        }

        @GetMapping("/courses")
        public ResponseEntity<Map<String, Object>> getCourses(
                @RequestParam(required = false) Long userId,
                @RequestParam(required = false) String search
        ) {
            List<Course> courses;

            if (search != null && !search.trim().isEmpty()) {
                // FIXED: Use enhancedSearch instead of searchCourses
                courses = courseService.enhancedSearch(search);
            } else {
                courses = courseService.getAllCoursesWithModulesAndVideos();
            }

            if (userId != null) {
                for (Course course : courses) {
                    boolean isPurchased = userCoursePurchaseRepository
                            .existsByUserIdAndCourseId(userId, course.getId());
                    course.setPurchased(isPurchased);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", courses);
            return ResponseEntity.ok(response);
        }

        // FIXED: Advanced search endpoint - added missing parameters
        @GetMapping("/courses/advanced-search")
        public ResponseEntity<Map<String, Object>> advancedSearch(
                @RequestParam(required = false) Long userId,
                @RequestParam(required = false) String search,
                @RequestParam(required = false) String category,
                @RequestParam(required = false) String level,
                @RequestParam(required = false) Double minPrice,
                @RequestParam(required = false) Double maxPrice,
                @RequestParam(required = false) Double minRating
        ) {
            List<Course> courses = courseService.advancedSearch(
                search, category, minPrice, maxPrice, minRating, level);
            
            if (userId != null) {
                for (Course course : courses) {
                    boolean isPurchased = userCoursePurchaseRepository
                            .existsByUserIdAndCourseId(userId, course.getId());
                    course.setPurchased(isPurchased);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", courses);
            return ResponseEntity.ok(response);
        }

        // FIXED: Get search suggestions endpoint
        @GetMapping("/courses/search/suggestions")
        public ResponseEntity<Map<String, Object>> getSearchSuggestions(
                @RequestParam String query
        ) {
            List<String> suggestions = courseService.getSearchSuggestions(query);
            
            Map<String, Object> response = new HashMap<>();
            response.put("suggestions", suggestions);
            return ResponseEntity.ok(response);
        }

        // FIXED: Get popular tags endpoint
        @GetMapping("/courses/tags/popular")
        public ResponseEntity<Map<String, Object>> getPopularTags() {
            List<String> tags = courseService.getPopularTags();
            
            Map<String, Object> response = new HashMap<>();
            response.put("tags", tags);
            return ResponseEntity.ok(response);
        }

        // FIXED: Get courses by tag endpoint
        @GetMapping("/courses/tag/{tagName}")
        public ResponseEntity<Map<String, Object>> getCoursesByTag(
                @PathVariable String tagName,
                @RequestParam(required = false) Long userId
        ) {
            List<Course> courses = courseService.getCoursesByTag(tagName);
            
            if (userId != null) {
                for (Course course : courses) {
                    boolean isPurchased = userCoursePurchaseRepository
                            .existsByUserIdAndCourseId(userId, course.getId());
                    course.setPurchased(isPurchased);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", courses);
            return ResponseEntity.ok(response);
        }

@GetMapping("/courses/{id}")
public ResponseEntity<Map<String, Object>> getCourse(
        @PathVariable Long id,
        @RequestParam Long userId) {
    
    try {
        Course course = courseService.getCourseForUser(userId, id);
        
        // Create response without circular references
        Map<String, Object> response = new HashMap<>();
        response.put("id", course.getId());
        response.put("title", course.getTitle());
        response.put("description", course.getDescription());
        response.put("thumbnailUrl", course.getThumbnailUrl());
        response.put("instructor", course.getInstructor());
        response.put("isPurchased", course.isPurchased());
        response.put("isSubscribed", course.isSubscribed());
        
        // Add modules without circular references
        if (course.getModules() != null && !course.getModules().isEmpty()) {
            List<Map<String, Object>> modules = course.getModules().stream()
                .map(module -> {
                    Map<String, Object> moduleMap = new HashMap<>();
                    moduleMap.put("id", module.getId());
                    moduleMap.put("title", module.getTitle());
                    moduleMap.put("durationSec", module.getDurationSec());
                    moduleMap.put("isLocked", module.isLocked());
                    moduleMap.put("assessmentLocked", module.isAssessmentLocked());
                    
                    // Add videos without circular references
                    if (module.getVideos() != null) {
                        List<Map<String, Object>> videos = module.getVideos().stream()
                            .map(video -> {
                                Map<String, Object> videoMap = new HashMap<>();
                                videoMap.put("id", video.getId());
                                videoMap.put("title", video.getTitle());
                                videoMap.put("duration", video.getDuration());
                                videoMap.put("isLocked", video.isLocked());
                                videoMap.put("isCompleted", video.isCompleted());
                                videoMap.put("displayOrder", video.getDisplayOrder());
                                videoMap.put("isPreview", video.getIsPreview());
                                // ✅ ADD THESE TWO CRITICAL FIELDS:
                                videoMap.put("videoUrl", video.getVideoUrl());
                                videoMap.put("youtubeId", video.getYoutubeId());
                                return videoMap;
                            })
                            .collect(Collectors.toList());
                        moduleMap.put("videos", videos);
                    }
                    
                    return moduleMap;
                })
                .collect(Collectors.toList());
            response.put("modules", modules);
        }
        
        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("data", response);
        return ResponseEntity.ok(finalResponse);
        
    } catch (Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

        @GetMapping("/courses/subscribed/{userId}")
        public ResponseEntity<List<Course>> getSubscribedCourses(
                @PathVariable Long userId) {
            List<Course> courses = courseService.getSubscribedCourses(userId);
            return ResponseEntity.ok(courses);
        }

        // ---------------------- MODULE APIs ----------------------
        @PostMapping("/modules")
        public ResponseEntity<?> addModule(
                @RequestParam("courseId") Long courseId,
                @RequestBody Module module) {
            try {
                return ResponseEntity.ok(courseService.saveModule(courseId, module));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
@GetMapping("/modules/course/{courseId}")
public ResponseEntity<Map<String, Object>> getModulesByCourse(
        @PathVariable Long courseId,
        @RequestParam Long userId) {

    try {
        // 1️⃣ Fetch course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // 2️⃣ Check if user purchased the course
        boolean isPurchased = userCoursePurchaseRepository
                .existsByUserIdAndCourseId(userId, courseId);

        // 3️⃣ Build base response
        Map<String, Object> response = new HashMap<>();
        response.put("courseId", course.getId());
        response.put("courseTitle", course.getTitle());
        response.put("isPurchased", isPurchased);

        // 4️⃣ Calculate module count and total duration
        int moduleCount = course.getModules() != null ? course.getModules().size() : 0;
        int totalDurationSec = 0;
        if (course.getModules() != null) {
            for (Module module : course.getModules()) {
                if (module.getVideos() != null) {
                    for (Video video : module.getVideos()) {
                        totalDurationSec += video.getDuration(); // duration in seconds
                    }
                }
            }
        }
        response.put("moduleCount", moduleCount);
        response.put("totalDurationSec", totalDurationSec);

        // 5️⃣ If purchased, return detailed module/video stats
        if (isPurchased) {
            CourseResponseDTO courseDTO = new CourseResponseDTO(course);
            courseDTO.setIsPurchased(true);
            courseDTO.setIsSubscribed(true);

            applyUserVideoProgress(courseDTO, userId, true);

            response.put("modules", courseDTO.getModules());          // detailed modules
            response.put("totalModules", courseDTO.getTotalModules()); 
            response.put("totalVideos", courseDTO.getTotalVideos());
            response.put("completedModules", courseDTO.getCompletedModules());
            response.put("completedVideos", courseDTO.getCompletedVideos());
            response.put("progressPercent", courseDTO.getProgressPercent());
            response.put("isCompleted", courseDTO.getIsCompleted());
        }

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}


private void applyUserVideoProgress(CourseResponseDTO courseDTO, Long userId, boolean isCoursePurchased) {
    System.out.println("=== APPLY USER VIDEO PROGRESS ===");
    System.out.println("User ID: " + userId);
    System.out.println("Course purchased: " + isCoursePurchased);
    System.out.println("Total modules: " + courseDTO.getModules().size());
    
    // Get user's video progress from database
    List<UserVideoProgress> userProgress = userVideoProgressRepository.findByUserId(userId);
    System.out.println("User progress records found: " + userProgress.size());
    
    // Create a map for quick lookup: videoId -> progress
    Map<Long, UserVideoProgress> progressMap = userProgress.stream()
        .collect(Collectors.toMap(
            up -> up.getVideo().getId(),
            up -> up
        ));
    
    // Track if previous module was completed
    boolean previousModuleCompleted = true;
    
    // Track overall course stats
    int totalCourseVideos = 0;
    int totalCompletedCourseVideos = 0;
    int totalCompletedModules = 0;
    
    // Apply progress to videos in DTO
    for (int moduleIndex = 0; moduleIndex < courseDTO.getModules().size(); moduleIndex++) {
        ModuleResponseDTO module = courseDTO.getModules().get(moduleIndex);
        System.out.println("\nProcessing module [" + moduleIndex + "]: " + 
                          module.getTitle() + " (ID: " + module.getId() + ")");
        
        // Determine if this is the first module
        boolean isFirstModule = moduleIndex == 0;
        
        System.out.println("  Is first module: " + isFirstModule);
        System.out.println("  Is course purchased: " + isCoursePurchased);
        
        if (isCoursePurchased) {
            // If course is purchased, check module sequence
            if (isFirstModule) {
                // First module is always unlocked for purchased courses
                module.setIsLocked(false);
                System.out.println("  ✓ First module unlocked (course purchased)");
            } else {
                // For subsequent modules, check if previous module was completed
                module.setIsLocked(!previousModuleCompleted);
                System.out.println("  Module locked status: " + (!previousModuleCompleted) + 
                                " (previous module completed: " + previousModuleCompleted + ")");
            }
        } else {
            // If course not purchased, only first module is unlocked
            module.setIsLocked(!isFirstModule);
            System.out.println("  Module locked (course not purchased): " + (!isFirstModule));
        }
        
        // Initialize module video counters
        int moduleTotalVideos = 0;
        int moduleCompletedVideos = 0;
        boolean moduleHasUnlockedVideo = false;
        boolean allVideosCompleted = true;
        
        if (module.getVideos() != null) {
            System.out.println("  Module has " + module.getVideos().size() + " videos");
            moduleTotalVideos = module.getVideos().size();
            totalCourseVideos += moduleTotalVideos;
            
            // FOR DEBUGGING: Print all video details
            System.out.println("  Video list:");
            for (VideoResponseDTO video : module.getVideos()) {
                System.out.println("    - ID: " + video.getId() + 
                                 ", Title: " + video.getTitle() + 
                                 ", DisplayOrder: " + video.getDisplayOrder() + 
                                 ", Current isLocked: " + video.getIsLocked());
            }
            
            for (VideoResponseDTO video : module.getVideos()) {
                System.out.println("    Processing video: " + video.getTitle() + 
                                 " (ID: " + video.getId() + 
                                 ", DisplayOrder: " + video.getDisplayOrder() + ")");
                
                // SPECIAL LOGIC FOR SUBSCRIBED COURSES: Unlock first 3 videos of first module
                if (isCoursePurchased && isFirstModule && video.getDisplayOrder() <= 3) {
                    System.out.println("    ✓ Special rule: First 3 videos of first module for subscribed course");
                    video.setIsLocked(false);
                    
                    UserVideoProgress progress = progressMap.get(video.getId());
                    if (progress != null) {
                        System.out.println("      Progress exists - Completed: " + progress.isCompleted());
                        video.setIsCompleted(progress.isCompleted());
                        if (progress.isCompleted()) {
                            moduleCompletedVideos++;
                            totalCompletedCourseVideos++;
                        }
                    } else {
                        System.out.println("      No progress record, marking as not completed");
                        video.setIsCompleted(false);
                    }
                    continue; // Skip the rest of the logic for these videos
                }
                
                UserVideoProgress progress = progressMap.get(video.getId());
                if (progress != null) {
                    System.out.println("      Progress found - Unlocked: " + progress.isUnlocked() + 
                                    ", Completed: " + progress.isCompleted());
                    
                    video.setIsLocked(!progress.isUnlocked());
                    video.setIsCompleted(progress.isCompleted());
                    
                    if (progress.isUnlocked()) {
                        moduleHasUnlockedVideo = true;
                    }
                    
                    // Count completed videos
                    if (progress.isCompleted()) {
                        moduleCompletedVideos++;
                        totalCompletedCourseVideos++;
                    }
                    
                    if (!progress.isCompleted()) {
                        allVideosCompleted = false;
                    }
                } else {
                    System.out.println("      No progress record found");
                    
                    // Apply default logic based on module and purchase status
                    boolean isFirstVideo = video.getDisplayOrder() == 1;
                    
                    System.out.println("      Is first video (displayOrder==1): " + isFirstVideo);
                    System.out.println("      Module isLocked: " + module.getIsLocked());
                    
                    if (Boolean.FALSE.equals(module.getIsLocked()) && isFirstVideo) {
                        // First video of an unlocked module is unlocked
                        video.setIsLocked(false);
                        moduleHasUnlockedVideo = true;
                        System.out.println("      ✓ First video unlocked (module unlocked)");
                    } else {
                        video.setIsLocked(true);
                        System.out.println("      ✗ Video locked (not first video or module locked)");
                    }
                    video.setIsCompleted(false);
                    allVideosCompleted = false;
                }
            }
        }
        
        // Set module stats using the helper method from ModuleResponseDTO
        module.calculateStatsFromVideos();
        
        // Override with our calculated values
        module.setCompletedVideos(moduleCompletedVideos);
        module.setTotalVideos(moduleTotalVideos);
        
        // Calculate progress percentage
        int moduleProgressPercent = moduleTotalVideos > 0 ? 
            (moduleCompletedVideos * 100) / moduleTotalVideos : 0;
        module.setProgressPercent(moduleProgressPercent);
        
        // Determine if module is completed
        boolean moduleCompleted = Boolean.FALSE.equals(module.getIsLocked()) && 
                                 moduleTotalVideos > 0 && 
                                 moduleCompletedVideos == moduleTotalVideos;
        module.setIsCompleted(moduleCompleted);
        
        if (moduleCompleted) {
            totalCompletedModules++;
        }
        
        System.out.println("  Module stats - Completed: " + moduleCompletedVideos + 
                         "/" + moduleTotalVideos + " videos, Progress: " + 
                         moduleProgressPercent + "%");
        
        // Update module completion tracking
        previousModuleCompleted = Boolean.FALSE.equals(module.getIsLocked()) && allVideosCompleted;
        System.out.println("  Module all videos completed: " + allVideosCompleted);
        System.out.println("  Previous module completed for next: " + previousModuleCompleted);
        
        // If module is locked but has an unlocked video from progress, unlock the module
        if (Boolean.TRUE.equals(module.getIsLocked()) && moduleHasUnlockedVideo) {
            module.setIsLocked(false);
            System.out.println("  ✓ Module unlocked (has unlocked videos from progress)");
        }
    }
    
    // Set course-level stats in CourseResponseDTO
    int courseProgressPercent = totalCourseVideos > 0 ? 
        (totalCompletedCourseVideos * 100) / totalCourseVideos : 0;
    
    // Update the CourseResponseDTO fields
    courseDTO.setTotalVideos(totalCourseVideos);
    courseDTO.setCompletedVideos(totalCompletedCourseVideos);
    courseDTO.setCompletedModules(totalCompletedModules);  // This is the key field!
    courseDTO.setProgressPercent((double) courseProgressPercent);
    courseDTO.setIsCompleted(totalCompletedCourseVideos == totalCourseVideos && totalCourseVideos > 0);
    
    System.out.println("\n=== COURSE STATS ===");
    System.out.println("Total videos: " + totalCourseVideos);
    System.out.println("Completed videos: " + totalCompletedCourseVideos);
    System.out.println("Completed modules: " + totalCompletedModules);
    System.out.println("Course progress: " + courseProgressPercent + "%");
}

@GetMapping("/modules/{id}")
public ResponseEntity<?> getModule(@PathVariable Long id) {
    try {
        Optional<Module> moduleOpt = courseService.getModuleById(id);
        
        if (moduleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Module module = moduleOpt.get();
        
        // Create response without circular references
        Map<String, Object> response = new HashMap<>();
        response.put("id", module.getId());
        response.put("title", module.getTitle());
        response.put("durationSec", module.getDurationSec());
        
        if (module.getCourse() != null) {
            response.put("courseId", module.getCourse().getId());
            response.put("courseTitle", module.getCourse().getTitle());
        }
        
        // Add videos without circular references
        if (module.getVideos() != null && !module.getVideos().isEmpty()) {
            List<Map<String, Object>> videos = module.getVideos().stream()
                .map(video -> {
                    Map<String, Object> videoMap = new HashMap<>();
                    videoMap.put("id", video.getId());
                    videoMap.put("title", video.getTitle());
                    videoMap.put("duration", video.getDuration());
                    videoMap.put("displayOrder", video.getDisplayOrder());
                    videoMap.put("isPreview", video.getIsPreview());
                    // ✅ ADD THESE FIELDS:
                    videoMap.put("videoUrl", video.getVideoUrl());
                    videoMap.put("youtubeId", video.getYoutubeId());
                    // Don't include module to avoid circular reference
                    return videoMap;
                })
                .collect(Collectors.toList());
            response.put("videos", videos);
        }
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", e.getMessage()));
    }
}


        // ---------------------- VIDEO APIs ----------------------
        @PostMapping("/videos")
        public ResponseEntity<?> addVideo(
                @RequestParam("moduleId") Long moduleId,
                @RequestBody Video video) {
            try {
                return ResponseEntity.ok(courseService.saveVideo(moduleId, video));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }

        @GetMapping("/modules/{moduleId}/videos")
        public ResponseEntity<List<Video>> getVideosByModule(@PathVariable Long moduleId) {
            return ResponseEntity.ok(courseService.getVideosByModuleId(moduleId));
        }

        // ---------------------- ASSESSMENT APIs ----------------------
        @PostMapping("/assessments")
        public ResponseEntity<?> addAssessment(
                @RequestParam("moduleId") Long moduleId,
                @RequestBody Assessment assessment) {
            try {
                return ResponseEntity.ok(courseService.saveAssessment(moduleId, assessment));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }

        @GetMapping("/modules/{moduleId}/assessments")
        public ResponseEntity<List<Assessment>> getAssessmentsByModule(@PathVariable Long moduleId) {
            return ResponseEntity.ok(courseService.getAssessmentsByModuleId(moduleId));
        }

        // ---------------------- QUESTION APIs ----------------------
        @PostMapping("/questions")
        public ResponseEntity<?> addQuestion(
                @RequestParam("assessmentId") Long assessmentId,
                @RequestBody Question question) {
            try {
                return ResponseEntity.ok(courseService.saveQuestion(assessmentId, question));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }

        @GetMapping("/assessments/{assessmentId}/questions")
        public ResponseEntity<Map<String, Object>> getQuestionsByAssessment(@PathVariable Long assessmentId) {
            Map<String, Object> response = new HashMap<>();
            response.put("assessmentId", assessmentId);
            response.put("questions", courseService.getQuestionsByAssessmentId(assessmentId));
            return ResponseEntity.ok(response);

        }
        //-----------------------------Submit Assessment and Check its Status-----------------------
        @PostMapping("/debug/submit")
    public ResponseEntity<?> debugSubmitAssessment(@RequestBody Map<String, Object> payload) {
        System.out.println("🔴 DEBUG SUBMIT ENDPOINT HIT!");
        System.out.println("Full payload: " + payload);
        
        // Extract parameters
        Long userId = Long.valueOf(payload.get("userId").toString());
        Long assessmentId = Long.valueOf(payload.get("assessmentId").toString());
        
        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) payload.get("answers");
        
        System.out.println("userId: " + userId);
        System.out.println("assessmentId: " + assessmentId);
        System.out.println("answers: " + answers);
        
        // Call your service
        Map<String, Object> result = courseService.submitAssessment(userId, assessmentId, 
            answers.entrySet().stream()
                .collect(Collectors.toMap(
                    e -> Long.valueOf(e.getKey()), 
                    e -> e.getValue()
                )));
        
        return ResponseEntity.ok(result);
    }
        




        
        
        @PostMapping("/course/assessment/submit")
        public ResponseEntity<?> submitAssessment(
                @RequestParam Long userId,
                @RequestParam Long assessmentId,
                @RequestBody Map<Long, String> answers) {
                    
            
            try {
                Map<String, Object> result = courseService.submitAssessment(userId, assessmentId, answers);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
                ));
            }
        }
        
        @GetMapping("/course/assessment/status")
        public ResponseEntity<?> getAssessmentStatus(
                @RequestParam Long userId,
                @RequestParam Long assessmentId) {
            
            try {
                Map<String, Object> status = courseService.getAssessmentStatus(userId, assessmentId);
                return ResponseEntity.ok(status);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
                ));
            }
        }



        // Add this endpoint to your CourseController
    @GetMapping("/course/assessment/questions")
    public ResponseEntity<?> getAssessmentQuestions(
            @RequestParam Long userId,
            @RequestParam Long assessmentId) {
        
        try {
            Map<String, Object> assessmentData = courseService.getAssessmentWithQuestions(userId, assessmentId);
            return ResponseEntity.ok(assessmentData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    // Add this to CourseController
    @GetMapping("/course/assessment/can-attempt")
    public ResponseEntity<?> canAttemptAssessment(
            @RequestParam Long userId,
            @RequestParam Long assessmentId) {
        
        try {
            boolean canAttempt = courseService.canAttemptAssessment(userId, assessmentId);
            return ResponseEntity.ok(Map.of(
                "canAttempt", canAttempt
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }


        // ---------------------- PURCHASE / UNLOCK / PROGRESS APIs ----------------------
        @PostMapping("/purchase")
        public ResponseEntity<Map<String, Object>> purchaseCourse(
                @RequestParam Long userId,
                @RequestParam Long courseId) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", courseService.purchaseCourse(userId, courseId));
            return ResponseEntity.ok(resp);
        }
        
        @PostMapping("/modules/{moduleId}/unlock-assessment")
        public ResponseEntity<Map<String,Object>> unlockAssessment(
                @PathVariable Long moduleId,
                @RequestParam Long userId) {
            Map<String,Object> resp = new HashMap<>();
            resp.put("success", courseService.unlockAssessmentForModule(userId, moduleId));
            return ResponseEntity.ok(resp);
        }

        @PostMapping("/modules/{moduleId}/unlock-next")
        public ResponseEntity<Map<String,Object>> unlockNextModule(
                @PathVariable Long moduleId,
                @RequestParam Long userId,
                @RequestParam Long courseId) {
            Map<String,Object> resp = new HashMap<>();
            resp.put("success", courseService.unlockNextModuleForUser(userId, courseId, moduleId));
            return ResponseEntity.ok(resp);
        }

// In CourseController.java

@GetMapping("/streak/course/{courseId}")
public ResponseEntity<?> getCourseStreak(
        @PathVariable Long courseId,
        @RequestParam Long userId) {
    
    try {
        StreakSummaryDTO streakSummary = streakService.getCourseStreak(userId, courseId);
        return ResponseEntity.ok(streakSummary);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    }
}

@GetMapping("/streak/overview")
public ResponseEntity<?> getUserStreakOverview(@RequestParam Long userId) {
    try {
        Map<String, Object> streakOverview = streakService.getUserStreakOverview(userId);
        return ResponseEntity.ok(streakOverview);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    }
}

@GetMapping("/streak/day/{courseId}")
public ResponseEntity<?> getDayDetails(
        @PathVariable Long courseId,
        @RequestParam Long userId,
        @RequestParam String date) {
    
    try {
        LocalDate localDate = LocalDate.parse(date);
        StreakDayDTO dayDetails = streakService.getDayDetails(userId, courseId, localDate);
        return ResponseEntity.ok(dayDetails);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    }
}

// Update the existing profile/streak endpoint
@GetMapping("/profile/streak")
public ResponseEntity<?> getStreak(
        @RequestParam Long userId,
        @RequestParam(required = false) Long courseId) {
    
    try {
        if (courseId != null) {
            // Course-specific streak
            StreakSummaryDTO streak = streakService.getCourseStreak(userId, courseId);
            return ResponseEntity.ok(streak);
        } else {
            // Overview for all courses
            Map<String, Object> overview = streakService.getUserStreakOverview(userId);
            return ResponseEntity.ok(overview);
        }
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    }
}




    // In CourseController.java

    @GetMapping("/user/{userId}/progress/overall")
    public ResponseEntity<?> getUserOverallProgress(@PathVariable Long userId) {
        try {
            Map<String, Object> progress = courseService.getUserOverallProgress(userId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/course/{courseId}/progress")
    public ResponseEntity<?> getCourseProgress(
            @PathVariable Long courseId,
            @RequestParam Long userId) {
        
        try {
            Map<String, Object> progress = courseService.calculateCourseProgress(userId, courseId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/dashboard/stats")
        public ResponseEntity<?> getDashboardStats(
                @RequestParam Long userId,
                @RequestParam(required = false) Long courseId) {
            
            try {
                System.out.println("📊 Dashboard stats request:");
                System.out.println("   ├─ User ID: " + userId);
                System.out.println("   ├─ Course ID: " + (courseId != null ? courseId : "Not specified"));
                
                // 1. Get overall progress
                Map<String, Object> overallProgress = courseService.getUserOverallProgress(userId);
                
                // 2. Get course-specific stats
                List<Map<String, Object>> courseStats = courseService.getUserCourseStats(userId);
                
                System.out.println("   ├─ Total courses found: " + courseStats.size());
                System.out.println("   ├─ Course stats available: " + (courseStats != null));
                
                // 3. Build response
                Map<String, Object> response = new HashMap<>();
                
                // Overall stats
                response.put("totalCourses", overallProgress.get("totalCourses"));
                response.put("completedCourses", overallProgress.get("completedCourses"));
                response.put("inProgressCourses", overallProgress.get("inProgressCourses"));
                response.put("totalVideos", overallProgress.get("totalVideos"));
                response.put("completedVideos", overallProgress.get("completedVideos"));
                response.put("overallProgress", overallProgress.get("overallVideoProgress"));
                response.put("completedModules", overallProgress.get("completedModules"));
                
                // Course breakdown
                response.put("courseStats", courseStats != null ? courseStats : new ArrayList<>());
                
                // 4. If specific course is selected, add its details
                if (courseId != null && courseStats != null) {
                    // Find the selected course in the list
                    Optional<Map<String, Object>> selectedCourse = courseStats.stream()
                        .filter(stat -> courseId.equals(stat.get("courseId")))
                        .findFirst();
                    
                    if (selectedCourse.isPresent()) {
                        response.put("selectedCourseId", courseId);
                        System.out.println("   └─ Selected course found: " + selectedCourse.get().get("courseTitle"));
                    } else {
                        System.out.println("   └─ Selected course not found in user's enrolled courses");
                    }
                }
                
                // Print debug info
                System.out.println("📊 Response structure:");
                System.out.println("   ├─ totalCourses: " + response.get("totalCourses"));
                System.out.println("   ├─ courseStats count: " + 
                    (response.get("courseStats") instanceof List ? ((List<?>)response.get("courseStats")).size() : "N/A"));
                
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                System.err.println("❌ Error in dashboard stats: " + e.getMessage());
                e.printStackTrace();
                
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
            }
        }


@GetMapping("/debug/assessment-status/{userId}/{assessmentId}")
public ResponseEntity<?> debugAssessmentStatus(
        @PathVariable Long userId,
        @PathVariable Long assessmentId) {
    
    User user = userRepository.findById(userId).orElseThrow();
    Assessment assessment = assessmentRepository.findById(assessmentId).orElseThrow();
    Module module = assessment.getModule();
    
    List<Video> videos = videoRepository.findByModuleId(module.getId());
    
    List<Map<String, Object>> videoStatus = new ArrayList<>();
    for (Video video : videos) {
        Map<String, Object> status = new HashMap<>();
        status.put("videoId", video.getId());
        status.put("videoTitle", video.getTitle());
        
        Optional<UserVideoProgress> uvp = userVideoProgressRepository
                .findByUserAndVideo(user, video);
        
        status.put("hasProgress", uvp.isPresent());
        status.put("isUnlocked", uvp.map(UserVideoProgress::isUnlocked).orElse(false));
        status.put("isCompleted", uvp.map(UserVideoProgress::isCompleted).orElse(false));
        
        videoStatus.add(status);
    }
    
    return ResponseEntity.ok(Map.of(
        "assessmentId", assessmentId,
        "assessmentTitle", assessment.getTitle(),
        "moduleId", module.getId(),
        "totalVideos", videos.size(),
        "videoStatus", videoStatus
    ));
}

    }

