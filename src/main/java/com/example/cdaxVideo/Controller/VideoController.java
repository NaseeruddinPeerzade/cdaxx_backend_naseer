package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.DTO.VideoCompletionRequestDTO;
import com.example.cdaxVideo.DTO.VideoProgressDTO;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Entity.UserVideoProgress;
import com.example.cdaxVideo.Repository.UserRepository;
import com.example.cdaxVideo.Service.VideoService;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    @Autowired
    private UserRepository userRepository; 
    
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);
    
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * POST /api/videos/{videoId}/complete
     * Marks a video as completed
     */
@PostMapping("/{videoId}/complete") 
public ResponseEntity<Map<String, Object>> markVideoAsCompleted(
        @PathVariable Long videoId,
        @RequestParam Long userId,
        @RequestParam(required = false) Long courseId,
        @RequestParam(required = false) Long moduleId,
        HttpServletRequest request) {
    
    try {
        logger.info("🎬 === VIDEO COMPLETION REQUEST ===");
        logger.info("URL: {}", request.getRequestURL());
        logger.info("Query String: {}", request.getQueryString());
        logger.info("Method: {}", request.getMethod());
        logger.info("Headers - Authorization: {}", request.getHeader("Authorization"));
        logger.info("Parameters - userId: {}, courseId: {}, moduleId: {}, videoId: {}", 
            userId, courseId, moduleId, videoId);
        
        // ✅ CRITICAL FIX: Get authenticated user from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("❌ No authentication found in SecurityContext");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Not authenticated");
            errorResponse.put("code", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        String authenticatedEmail = authentication.getName();
        logger.info("✅ Authenticated email from JWT: {}", authenticatedEmail);
        
        // ✅ Find user by email (from JWT) to get their actual ID
        User authenticatedUser = userRepository.findByEmail(authenticatedEmail)
            .orElseThrow(() -> {
                logger.error("❌ User not found with email: {}", authenticatedEmail);
                return new RuntimeException("User not found");
            });
        
        logger.info("✅ Authenticated user ID from DB: {}", authenticatedUser.getId());
        logger.info("✅ Request user ID parameter: {}", userId);
        
        // ✅ CRITICAL: Use the authenticated user's ID, NOT the parameter
        // This ensures JWT user always matches the user we're updating
        Long actualUserId = authenticatedUser.getId();
        
        // ✅ Optional: Log mismatch but still use authenticated user
        if (!actualUserId.equals(userId)) {
            logger.warn("⚠️ User ID mismatch! JWT user ID: {}, Request param user ID: {}", 
                actualUserId, userId);
            logger.info("⚠️ Using JWT user ID ({}) instead of parameter ({})", 
                actualUserId, userId);
        }
        
        // ✅ Create request with authenticated user's ID
        VideoCompletionRequestDTO requestDTO = new VideoCompletionRequestDTO();
        requestDTO.setVideoId(videoId);
        requestDTO.setUserId(actualUserId);  // ← USE AUTHENTICATED USER ID
        requestDTO.setCourseId(courseId);
        requestDTO.setModuleId(moduleId);
        
        logger.info("📦 Calling videoService with: userId={}", actualUserId);
        
        UserVideoProgress progress = videoService.markVideoAsCompleted(requestDTO);
        
        // ✅ ADDED: Log the actual result from VideoService
        logger.info("📊 VideoService returned:");
        logger.info("   ├─ Progress ID: {}", progress.getId());
        logger.info("   ├─ Video ID: {}", progress.getVideo().getId());
        logger.info("   ├─ User ID: {}", progress.getUser().getId());
        logger.info("   ├─ Completed: {}", progress.isCompleted());
        logger.info("   ├─ Unlocked: {}", progress.isUnlocked());
        logger.info("   ├─ Completed On: {}", progress.getCompletedOn());
        logger.info("   ├─ Watched Seconds: {}", progress.getWatchedSeconds());
        logger.info("   └─ Manually Completed: {}", progress.getManuallyCompleted());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Video marked as completed successfully");
        response.put("videoId", videoId);
        response.put("userId", actualUserId);  // ← RETURN ACTUAL USER ID
        
        // ✅ CRITICAL FIX: Return the ACTUAL completion status from database
        response.put("completed", progress.isCompleted());  // ← NOT always true!
        response.put("unlocked", progress.isUnlocked());     // ← NOT always true!
        response.put("completedOn", progress.getCompletedOn());
        
        // ✅ ADDED: Return actual watched time
        response.put("watchedSeconds", progress.getWatchedSeconds());
        
        // ✅ ADDED: Warn if not actually completed
        if (!progress.isCompleted()) {
            logger.warn("⚠️ WARNING: Video {} was NOT marked as completed for user {}!", 
                       videoId, actualUserId);
            response.put("warning", "Video was not marked as completed - check requirements");
        } else {
            logger.info("✅ Video {} successfully completed for user {}", videoId, actualUserId);
        }
        
        return ResponseEntity.ok(response);
        
    } catch (RuntimeException e) {
        logger.error("❌ Error marking video as completed: {}", e.getMessage(), e); // Added stack trace
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", e.getMessage());
        errorResponse.put("code", "VALIDATION_ERROR");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    } catch (Exception e) {
        logger.error("❌ Unexpected error marking video as completed: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "Internal server error");
        errorResponse.put("code", "INTERNAL_ERROR");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
    /**
     * POST /api/videos/{videoId}/progress
     * Updates video progress (watch time, position, etc.)
     */
    @PostMapping("/{videoId}/progress")
    public ResponseEntity<Map<String, Object>> updateVideoProgress(
            @PathVariable Long videoId,
            @RequestBody VideoProgressDTO progressDTO) {
        
        try {
            logger.debug("Updating video progress: {}", progressDTO);
            
            // Ensure the videoId in path matches the DTO
            progressDTO.setVideoId(videoId);
            
            UserVideoProgress progress = videoService.updateVideoProgress(progressDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video progress updated successfully");
            response.put("videoId", videoId);
            response.put("userId", progressDTO.getUserId());
            response.put("watchedSeconds", progress.getWatchedSeconds());
            response.put("lastPositionSeconds", progress.getLastPositionSeconds());
            response.put("forwardJumpsCount", progress.getForwardJumpsCount());
            response.put("completed", progress.isCompleted());
            response.put("unlocked", progress.isUnlocked());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error updating video progress: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error updating video progress: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /api/videos/{videoId}/progress
     * Gets video progress for a user
     */
    @GetMapping("/{videoId}/progress")
    public ResponseEntity<Map<String, Object>> getVideoProgress(
            @PathVariable Long videoId,
            @RequestParam Long userId) {
        
        try {
            logger.debug("Getting video progress - Video: {}, User: {}", videoId, userId);
            
            VideoProgressDTO progress = videoService.getVideoProgress(videoId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("progress", progress);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error getting video progress: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error getting video progress: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * POST /api/videos/{videoId}/unlock
     * Unlocks a video for a user
     */
    @PostMapping("/{videoId}/unlock")
    public ResponseEntity<Map<String, Object>> unlockVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId) {
        
        try {
            logger.info("Unlocking video - Video: {}, User: {}", videoId, userId);
            
            UserVideoProgress progress = videoService.unlockVideo(videoId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video unlocked successfully");
            response.put("videoId", videoId);
            response.put("userId", userId);
            response.put("unlocked", true);
            response.put("unlockedOn", progress.getUnlockedOn());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error unlocking video: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * POST /api/videos/{videoId}/manual-complete
     * Manually marks a video as completed (admin override)
     */
    @PostMapping("/{videoId}/manual-complete")
    public ResponseEntity<Map<String, Object>> manuallyCompleteVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId) {
        
        try {
            logger.info("Manual video completion - Video: {}, User: {}", videoId, userId);
            
            UserVideoProgress progress = videoService.manuallyCompleteVideo(videoId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video manually marked as completed");
            response.put("videoId", videoId);
            response.put("userId", userId);
            response.put("completed", true);
            response.put("manuallyCompleted", true);
            response.put("completedOn", progress.getCompletedOn());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error in manual video completion: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "VideoService");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}