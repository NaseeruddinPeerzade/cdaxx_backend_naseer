package com.example.cdaxVideo.Repository;


import com.example.cdaxVideo.Entity.UserCoursePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserCoursePurchaseRepository extends JpaRepository<UserCoursePurchase, Long> {
        // Add explicit query to be sure
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM UserCoursePurchase u " +
           "WHERE u.user.id = :userId AND u.course.id = :courseId")
    boolean existsByUserIdAndCourseId(@Param("userId") Long userId, 
                                      @Param("courseId") Long courseId);
    
    List<UserCoursePurchase> findByUserId(Long userId);
    
    // Add this for debugging
    @Query("SELECT u FROM UserCoursePurchase u WHERE u.user.id = :userId")
    List<UserCoursePurchase> findByUserIdWithQuery(@Param("userId") Long userId);
}
