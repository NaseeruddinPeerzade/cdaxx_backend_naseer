package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    
    // Find active subscription for user and course
    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.course.id = :courseId AND us.isActive = true")
    Optional<UserSubscription> findActiveByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
    
    // Find all active subscriptions for a user
    List<UserSubscription> findByUserIdAndIsActive(Long userId, Boolean isActive);
    
    // Find all subscriptions for a user (active + inactive)
    List<UserSubscription> findByUserId(Long userId);
    
    // Find subscriptions that are expiring soon (e.g., within 7 days)
    @Query("SELECT us FROM UserSubscription us WHERE us.expiryDate BETWEEN :now AND :futureDate AND us.isActive = true")
    List<UserSubscription> findExpiringSoon(@Param("now") LocalDateTime now, 
                                           @Param("futureDate") LocalDateTime futureDate);
    
    // Check if user has any active subscription
    boolean existsByUserIdAndIsActive(Long userId, Boolean isActive);
    
    // Find subscription by user and course (any status)
    Optional<UserSubscription> findByUserIdAndCourseId(Long userId, Long courseId);
}