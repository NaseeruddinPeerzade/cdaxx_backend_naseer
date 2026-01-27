package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.UserVideoProgress;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserVideoProgressRepository extends JpaRepository<UserVideoProgress, Long> {
    
    // Basic methods (will need @Transactional if accessing relationships)
    Optional<UserVideoProgress> findByUserAndVideo(User user, Video video);
    Optional<UserVideoProgress> findByUserIdAndVideoId(Long userId, Long videoId);
    boolean existsByUserIdAndVideoIdAndCompletedTrue(Long userId, Long videoId);
    List<UserVideoProgress> findByUserId(Long userId);
    List<UserVideoProgress> findByUserIdAndCompletedTrue(Long userId);
    List<UserVideoProgress> findByVideoId(Long videoId);

    // ========== OPTIMIZED METHODS WITH JOIN FETCH ==========

    // ✅ 1. For streak calculations (full chain: progress → video → module → course)
    @Query("SELECT DISTINCT uvp FROM UserVideoProgress uvp " +
           "JOIN FETCH uvp.video v " +
           "JOIN FETCH v.module m " +
           "JOIN FETCH m.course c " +
           "WHERE uvp.user.id = :userId " +
           "AND c.id = :courseId " +
           "AND uvp.lastUpdatedAt >= :startDate " +
           "ORDER BY uvp.lastUpdatedAt ASC")
    List<UserVideoProgress> findForStreakCalculation(
        @Param("userId") Long userId,
        @Param("courseId") Long courseId,
        @Param("startDate") LocalDateTime startDate
    );

    // ✅ 2. Get all progress with video details (for dashboard)
    @Query("SELECT DISTINCT uvp FROM UserVideoProgress uvp " +
           "JOIN FETCH uvp.video v " +
           "JOIN FETCH v.module m " +
           "WHERE uvp.user.id = :userId")
    List<UserVideoProgress> findByUserIdWithVideoDetails(@Param("userId") Long userId);

    // ✅ 3. Get progress for specific course (for course progress)
    @Query("SELECT DISTINCT uvp FROM UserVideoProgress uvp " +
           "JOIN FETCH uvp.video v " +
           "JOIN FETCH v.module m " +
           "JOIN FETCH m.course c " +
           "WHERE uvp.user.id = :userId AND c.id = :courseId")
    List<UserVideoProgress> findByUserIdAndCourseIdWithDetails(
        @Param("userId") Long userId, 
        @Param("courseId") Long courseId
    );

    // ✅ 4. Completed videos with details
    @Query("SELECT DISTINCT uvp FROM UserVideoProgress uvp " +
           "JOIN FETCH uvp.video v " +
           "JOIN FETCH v.module m " +
           "WHERE uvp.user.id = :userId AND uvp.completed = true")
    List<UserVideoProgress> findCompletedVideosWithDetails(@Param("userId") Long userId);

    // ✅ 5. Your existing method (already good)
    @Query("SELECT uvp FROM UserVideoProgress uvp " +
           "JOIN FETCH uvp.video v " +
           "JOIN v.module m " +
           "JOIN m.course c " +
           "WHERE uvp.user.id = :userId " +
           "AND c.id = :courseId " +
           "AND uvp.lastUpdatedAt >= :startDateTime " +
           "AND uvp.lastUpdatedAt < :endDateTime " +
           "AND uvp.watchedSeconds > 0 " +
           "ORDER BY uvp.lastUpdatedAt DESC")
    List<UserVideoProgress> findByUserIdAndCourseIdAndDateRangeWithVideo(
        @Param("userId") Long userId,
        @Param("courseId") Long courseId,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    // ✅ 6. Count completed videos (optimized version)
    @Query("SELECT COUNT(uvp) FROM UserVideoProgress uvp " +
           "JOIN uvp.video v " +
           "JOIN v.module m " +
           "JOIN m.course c " +
           "WHERE uvp.user.id = :userId AND c.id = :courseId AND uvp.completed = true")
    Long countCompletedVideosByUserAndCourse(@Param("userId") Long userId, 
                                             @Param("courseId") Long courseId);

    // ✅ 7. Get recent activity (for activity feed)
    @Query("SELECT uvp FROM UserVideoProgress uvp " +
           "JOIN FETCH uvp.video v " +
           "JOIN FETCH v.module m " +
           "JOIN FETCH m.course c " +
           "WHERE uvp.user.id = :userId " +
           "ORDER BY uvp.lastUpdatedAt DESC " +
           "LIMIT :limit")
    List<UserVideoProgress> findRecentActivity(
        @Param("userId") Long userId,
        @Param("limit") int limit
    );
}