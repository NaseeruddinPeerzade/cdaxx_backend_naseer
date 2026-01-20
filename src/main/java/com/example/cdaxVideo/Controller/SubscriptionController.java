package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.Service.AuthService;
import com.example.cdaxVideo.Service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private SubscriptionService subscriptionService; // We'll create this next

    // ==================== GET SUBSCRIPTION STATUS ====================
    
    /**
     * Get current user's subscription status
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Get user ID from email
            Long userId = authService.getUserByEmail(email).getId();
            
            // Get subscription details
            Map<String, Object> subscriptionDetails = subscriptionService.getSubscriptionDetails(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscription", subscriptionDetails);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CHECK COURSE SUBSCRIPTION ====================
    
    /**
     * Check if user has active subscription for a specific course
     */
    @GetMapping("/check/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> checkCourseSubscription(@PathVariable Long courseId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Get user ID from email
            Long userId = authService.getUserByEmail(email).getId();
            
            // Check subscription
            boolean hasSubscription = subscriptionService.hasActiveSubscription(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasSubscription", hasSubscription);
            response.put("courseId", courseId);
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CREATE SUBSCRIPTION ====================
    
    /**
     * Create a new subscription for a course
     */
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createSubscription(
            @RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Get user ID from email
            Long userId = authService.getUserByEmail(email).getId();
            
            // Extract request data
            Long courseId = Long.valueOf(request.get("courseId").toString());
            String subscriptionType = (String) request.get("subscriptionType"); // "MONTHLY", "YEARLY", "LIFETIME"
            Integer durationMonths = (Integer) request.get("durationMonths"); // Optional
            
            // Create subscription
            Map<String, Object> result = subscriptionService.createSubscription(
                    userId, courseId, subscriptionType, durationMonths);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription created successfully");
            response.put("subscription", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CANCEL SUBSCRIPTION ====================
    
    /**
     * Cancel an active subscription
     */
    @PostMapping("/cancel/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> cancelSubscription(@PathVariable Long subscriptionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Get user ID from email
            Long userId = authService.getUserByEmail(email).getId();
            
            // Cancel subscription
            Map<String, Object> result = subscriptionService.cancelSubscription(userId, subscriptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription cancelled successfully");
            response.put("subscription", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== GET ALL USER SUBSCRIPTIONS ====================
    
    /**
     * Get all subscriptions for current user (active + inactive)
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAllSubscriptions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Get user ID from email
            Long userId = authService.getUserByEmail(email).getId();
            
            // Get all subscriptions
            Map<String, Object> result = subscriptionService.getAllUserSubscriptions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscriptions", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== RENEW SUBSCRIPTION ====================
    
    /**
     * Renew an expiring subscription
     */
    @PostMapping("/renew/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> renewSubscription(
            @PathVariable Long subscriptionId,
            @RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Get user ID from email
            Long userId = authService.getUserByEmail(email).getId();
            
            // Extract request data
            String subscriptionType = (String) request.get("subscriptionType");
            Integer durationMonths = (Integer) request.get("durationMonths");
            
            // Renew subscription
            Map<String, Object> result = subscriptionService.renewSubscription(
                    userId, subscriptionId, subscriptionType, durationMonths);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription renewed successfully");
            response.put("subscription", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== GET SUBSCRIPTION HISTORY ====================
    
    /**
     * Get subscription history for a course
     */
    @GetMapping("/history/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSubscriptionHistory(@PathVariable Long courseId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Get user ID from email
            Long userId = authService.getUserByEmail(email).getId();
            
            // Get subscription history
            Map<String, Object> result = subscriptionService.getSubscriptionHistory(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("history", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CHECK SUBSCRIPTION VALIDITY ====================
    
    /**
     * Check if subscription is still valid (not expired)
     */
    @GetMapping("/validate/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> validateSubscription(@PathVariable Long subscriptionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Get user ID from email
            Long userId = authService.getUserByEmail(email).getId();
            
            // Validate subscription
            Map<String, Object> result = subscriptionService.validateSubscription(userId, subscriptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("validation", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}