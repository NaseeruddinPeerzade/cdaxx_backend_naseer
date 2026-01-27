package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.ShoppingCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCartItem, Long> {
    
    // ========== OPTIMIZED METHODS WITH JOIN FETCH ==========
    
    // ✅ 1. Get cart items with course details (CRITICAL FIX)
    @Query("SELECT DISTINCT s FROM ShoppingCartItem s " +
           "JOIN FETCH s.course c " +  // Fetch course eagerly to prevent LazyInitializationException
           "WHERE s.user.id = :userId " +
           "ORDER BY s.addedAt DESC")
    List<ShoppingCartItem> findByUserIdWithCourse(@Param("userId") Long userId);
    
    // ✅ 2. Get cart item with full details
    @Query("SELECT s FROM ShoppingCartItem s " +
           "JOIN FETCH s.course c " +
           "JOIN FETCH s.user u " +
           "WHERE s.user.id = :userId AND s.course.id = :courseId")
    Optional<ShoppingCartItem> findByUserIdAndCourseIdWithDetails(
        @Param("userId") Long userId, 
        @Param("courseId") Long courseId
    );
    
    // ✅ 3. Existing basic methods (keep for backward compatibility)
    @Query("SELECT s FROM ShoppingCartItem s WHERE s.user.id = :userId")
    List<ShoppingCartItem> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT s FROM ShoppingCartItem s WHERE s.user.id = :userId AND s.course.id = :courseId")
    Optional<ShoppingCartItem> findByUserIdAndCourseId(@Param("userId") Long userId, 
                                                      @Param("courseId") Long courseId);
    
    // ✅ 4. Check if course is in cart
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM ShoppingCartItem s " +
           "WHERE s.user.id = :userId AND s.course.id = :courseId")
    boolean existsByUserIdAndCourseId(@Param("userId") Long userId, 
                                     @Param("courseId") Long courseId);
    
    // ✅ 5. Delete operations
    @Modifying
    @Transactional
    @Query("DELETE FROM ShoppingCartItem s WHERE s.user.id = :userId AND s.course.id = :courseId")
    void deleteByUserIdAndCourseId(@Param("userId") Long userId, 
                                   @Param("courseId") Long courseId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ShoppingCartItem s WHERE s.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
    
    // ✅ 6. Count cart items
    @Query("SELECT COUNT(s) FROM ShoppingCartItem s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    // ✅ 7. Get cart summary (course count and total price)
    @Query("SELECT new map(" +
           "COUNT(s) as itemCount, " +
           "SUM(CASE WHEN c.discountPrice IS NOT NULL THEN c.discountPrice ELSE c.price END) as totalPrice" +
           ") " +
           "FROM ShoppingCartItem s " +
           "JOIN s.course c " +
           "WHERE s.user.id = :userId")
    Map<String, Object> getCartSummary(@Param("userId") Long userId);
}