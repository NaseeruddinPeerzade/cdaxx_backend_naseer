package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.ShoppingCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCartItem, Long> {
    
    // Find all cart items for a user
    @Query("SELECT s FROM ShoppingCartItem s WHERE s.user.id = :userId")
    List<ShoppingCartItem> findByUserId(@Param("userId") Long userId);
    
    // Find specific cart item
    @Query("SELECT s FROM ShoppingCartItem s WHERE s.user.id = :userId AND s.course.id = :courseId")
    Optional<ShoppingCartItem> findByUserIdAndCourseId(@Param("userId") Long userId, 
                                                      @Param("courseId") Long courseId);
    
    // Check if course is in cart
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM ShoppingCartItem s " +
           "WHERE s.user.id = :userId AND s.course.id = :courseId")
    boolean existsByUserIdAndCourseId(@Param("userId") Long userId, 
                                     @Param("courseId") Long courseId);
    
    // Delete cart item by user and course
    @Modifying // ADD THIS
    @Transactional // ADD THIS (or ensure service is @Transactional)
    @Query("DELETE FROM ShoppingCartItem s WHERE s.user.id = :userId AND s.course.id = :courseId")
    void deleteByUserIdAndCourseId(@Param("userId") Long userId, 
                                   @Param("courseId") Long courseId);
    
    // Delete all cart items for user
    @Modifying // ADD THIS
    @Transactional // ADD THIS (or ensure service is @Transactional)
    @Query("DELETE FROM ShoppingCartItem s WHERE s.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
    
    // Count cart items for user
    @Query("SELECT COUNT(s) FROM ShoppingCartItem s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}