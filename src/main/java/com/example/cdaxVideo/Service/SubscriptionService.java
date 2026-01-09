package com.example.cdaxVideo.Service;

import com.example.cdaxVideo.Entity.UserSubscription;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Entity.Course;
import com.example.cdaxVideo.Repository.UserSubscriptionRepository;
import com.example.cdaxVideo.Repository.UserRepository;
import com.example.cdaxVideo.Repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionService {

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;

    // ==================== CHECK SUBSCRIPTIONS ====================

    /**
     * Check if user has active subscription for a specific course
     */
    public Boolean hasActiveSubscription(Long userId, Long courseId) {
        Optional<UserSubscription> subscription = userSubscriptionRepository
                .findActiveByUserIdAndCourseId(userId, courseId);
        
        if (subscription.isPresent()) {
            UserSubscription sub = subscription.get();
            return sub.isValid();
        }
        return false;
    }

    /**
     * Check if user has any active subscription (any course)
     */
    public Boolean hasAnyActiveSubscription(Long userId) {
        List<UserSubscription> activeSubs = userSubscriptionRepository
                .findByUserIdAndIsActive(userId, true);
        
        return activeSubs.stream()
                .anyMatch(UserSubscription::isValid);
    }

    // ==================== GET SUBSCRIPTION DETAILS ====================

    /**
     * Get detailed subscription information for a user
     */
    public Map<String, Object> getSubscriptionDetails(Long userId) {
        Map<String, Object> details = new HashMap<>();
        
        List<UserSubscription> subscriptions = userSubscriptionRepository
                .findByUserId(userId);
        
        // Find active and valid subscriptions
        List<Map<String, Object>> validSubscriptions = new ArrayList<>();
        List<Map<String, Object>> expiredSubscriptions = new ArrayList<>();
        
        for (UserSubscription sub : subscriptions) {
            Map<String, Object> subDetails = new HashMap<>();
            subDetails.put("subscriptionId", sub.getId());
            subDetails.put("courseId", sub.getCourse().getId());
            subDetails.put("courseTitle", sub.getCourse().getTitle());
            subDetails.put("subscriptionType", sub.getSubscriptionType());
            subDetails.put("startDate", sub.getStartDate());
            subDetails.put("expiryDate", sub.getExpiryDate());
            subDetails.put("isActive", sub.getIsActive());
            subDetails.put("isValid", sub.isValid());
            subDetails.put("isExpired", sub.isExpired());
            subDetails.put("daysRemaining", calculateDaysRemaining(sub));
            
            if (sub.isValid()) {
                validSubscriptions.add(subDetails);
            } else if (Boolean.TRUE.equals(sub.getIsActive()) && sub.isExpired()) {
                expiredSubscriptions.add(subDetails);
            }
        }
        
        details.put("userId", userId);
        details.put("hasActiveSubscription", !validSubscriptions.isEmpty());
        details.put("activeSubscriptionCount", validSubscriptions.size());
        details.put("validSubscriptions", validSubscriptions);
        details.put("expiredSubscriptions", expiredSubscriptions);
        details.put("totalSubscriptions", subscriptions.size());
        
        return details;
    }

    // ==================== CREATE SUBSCRIPTION ====================

    /**
     * Create a new subscription
     */
    public Map<String, Object> createSubscription(
            Long userId, 
            Long courseId, 
            String subscriptionType,
            Integer durationMonths) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        // Check if user already has an active subscription for this course
        Optional<UserSubscription> existing = userSubscriptionRepository
                .findActiveByUserIdAndCourseId(userId, courseId);
        
        if (existing.isPresent() && existing.get().isValid()) {
            throw new RuntimeException("User already has an active subscription for this course");
        }
        
        // Create new subscription
        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setCourse(course);
        subscription.setSubscriptionType(subscriptionType);
        subscription.setIsActive(true);
        subscription.setStartDate(LocalDateTime.now());
        
        // Calculate expiry date based on subscription type
        LocalDateTime expiryDate = calculateExpiryDate(subscriptionType, durationMonths);
        subscription.setExpiryDate(expiryDate);
        
        // Save subscription
        subscription = userSubscriptionRepository.save(subscription);
        
        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscription.getId());
        response.put("message", "Subscription created successfully");
        response.put("startDate", subscription.getStartDate());
        response.put("expiryDate", subscription.getExpiryDate());
        response.put("isActive", true);
        
        return response;
    }

    // ==================== CANCEL SUBSCRIPTION ====================

    /**
     * Cancel a subscription
     */
    public Map<String, Object> cancelSubscription(Long userId, Long subscriptionId) {
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        // Verify ownership
        if (!subscription.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to cancel this subscription");
        }
        
        // Cancel subscription
        subscription.setIsActive(false);
        subscription = userSubscriptionRepository.save(subscription);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscription.getId());
        response.put("message", "Subscription cancelled successfully");
        response.put("isActive", false);
        response.put("cancelledAt", LocalDateTime.now());
        
        return response;
    }

    // ==================== RENEW SUBSCRIPTION ====================

    /**
     * Renew an existing subscription
     */
    public Map<String, Object> renewSubscription(
            Long userId, 
            Long subscriptionId, 
            String subscriptionType,
            Integer durationMonths) {
        
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        // Verify ownership
        if (!subscription.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to renew this subscription");
        }
        
        // Calculate new expiry date
        LocalDateTime newExpiryDate;
        if (subscription.getExpiryDate() != null && 
            subscription.getExpiryDate().isAfter(LocalDateTime.now())) {
            // Extend from current expiry date
            newExpiryDate = extendDate(subscription.getExpiryDate(), subscriptionType, durationMonths);
        } else {
            // Start from now
            newExpiryDate = calculateExpiryDate(subscriptionType, durationMonths);
        }
        
        // Update subscription
        subscription.setSubscriptionType(subscriptionType);
        subscription.setIsActive(true);
        subscription.setExpiryDate(newExpiryDate);
        subscription = userSubscriptionRepository.save(subscription);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscription.getId());
        response.put("message", "Subscription renewed successfully");
        response.put("newExpiryDate", subscription.getExpiryDate());
        response.put("isActive", true);
        
        return response;
    }

    // ==================== GET ALL SUBSCRIPTIONS ====================

    /**
     * Get all subscriptions for a user
     */
    public Map<String, Object> getAllUserSubscriptions(Long userId) {
        List<UserSubscription> subscriptions = userSubscriptionRepository
                .findByUserId(userId);
        
        List<Map<String, Object>> subscriptionList = subscriptions.stream()
                .map(sub -> {
                    Map<String, Object> subMap = new HashMap<>();
                    subMap.put("subscriptionId", sub.getId());
                    subMap.put("courseId", sub.getCourse().getId());
                    subMap.put("courseTitle", sub.getCourse().getTitle());
                    subMap.put("subscriptionType", sub.getSubscriptionType());
                    subMap.put("startDate", sub.getStartDate());
                    subMap.put("expiryDate", sub.getExpiryDate());
                    subMap.put("isActive", sub.getIsActive());
                    subMap.put("isValid", sub.isValid());
                    subMap.put("isExpired", sub.isExpired());
                    subMap.put("daysRemaining", calculateDaysRemaining(sub));
                    subMap.put("createdAt", sub.getCreatedAt());
                    return subMap;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("totalSubscriptions", subscriptions.size());
        response.put("subscriptions", subscriptionList);
        
        return response;
    }

    // ==================== GET SUBSCRIPTION HISTORY ====================

    /**
     * Get subscription history for a specific course
     */
    public Map<String, Object> getSubscriptionHistory(Long userId, Long courseId) {
        List<UserSubscription> allSubscriptions = userSubscriptionRepository
                .findByUserId(userId);
        
        List<UserSubscription> courseSubscriptions = allSubscriptions.stream()
                .filter(sub -> sub.getCourse().getId().equals(courseId))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .collect(Collectors.toList());
        
        List<Map<String, Object>> history = courseSubscriptions.stream()
                .map(sub -> {
                    Map<String, Object> historyEntry = new HashMap<>();
                    historyEntry.put("subscriptionId", sub.getId());
                    historyEntry.put("subscriptionType", sub.getSubscriptionType());
                    historyEntry.put("startDate", sub.getStartDate());
                    historyEntry.put("expiryDate", sub.getExpiryDate());
                    historyEntry.put("isActive", sub.getIsActive());
                    historyEntry.put("status", getSubscriptionStatus(sub));
                    historyEntry.put("createdAt", sub.getCreatedAt());
                    return historyEntry;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("courseId", courseId);
        response.put("totalHistoryEntries", history.size());
        response.put("history", history);
        
        return response;
    }

    // ==================== VALIDATE SUBSCRIPTION ====================

    /**
     * Validate if a subscription is still active and not expired
     */
    public Map<String, Object> validateSubscription(Long userId, Long subscriptionId) {
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        // Verify ownership
        if (!subscription.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to validate this subscription");
        }
        
        boolean isValid = subscription.isValid();
        boolean isExpired = subscription.isExpired();
        long daysRemaining = calculateDaysRemaining(subscription);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscriptionId);
        response.put("isValid", isValid);
        response.put("isExpired", isExpired);
        response.put("isActive", subscription.getIsActive());
        response.put("daysRemaining", daysRemaining);
        response.put("expiryDate", subscription.getExpiryDate());
        response.put("status", getSubscriptionStatus(subscription));
        
        return response;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calculate expiry date based on subscription type
     */
    private LocalDateTime calculateExpiryDate(String subscriptionType, Integer durationMonths) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (subscriptionType.toUpperCase()) {
            case "MONTHLY":
                return now.plusMonths(durationMonths != null ? durationMonths : 1);
            case "YEARLY":
                return now.plusYears(durationMonths != null ? durationMonths / 12 : 1);
            case "LIFETIME":
                return now.plusYears(100); // Essentially lifetime
            case "TRIAL":
                return now.plusDays(7); // 7-day trial
            default:
                return now.plusMonths(1); // Default to monthly
        }
    }

    /**
     * Extend an existing date
     */
    private LocalDateTime extendDate(LocalDateTime currentDate, String subscriptionType, Integer durationMonths) {
        switch (subscriptionType.toUpperCase()) {
            case "MONTHLY":
                return currentDate.plusMonths(durationMonths != null ? durationMonths : 1);
            case "YEARLY":
                return currentDate.plusYears(durationMonths != null ? durationMonths / 12 : 1);
            default:
                return currentDate.plusMonths(1);
        }
    }

    /**
     * Calculate days remaining for subscription
     */
    private long calculateDaysRemaining(UserSubscription subscription) {
        if (subscription.getExpiryDate() == null || 
            !Boolean.TRUE.equals(subscription.getIsActive())) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(subscription.getExpiryDate())) {
            return 0;
        }
        
        return ChronoUnit.DAYS.between(now, subscription.getExpiryDate());
    }

    /**
     * Get subscription status as string
     */
    private String getSubscriptionStatus(UserSubscription subscription) {
        if (!Boolean.TRUE.equals(subscription.getIsActive())) {
            return "CANCELLED";
        }
        
        if (subscription.isExpired()) {
            return "EXPIRED";
        }
        
        if (subscription.isValid()) {
            long daysRemaining = calculateDaysRemaining(subscription);
            if (daysRemaining <= 7) {
                return "EXPIRING_SOON";
            }
            return "ACTIVE";
        }
        
        return "INACTIVE";
    }
}