package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // ✅ Fetch only courses + modules (with ORDER BY)
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.modules m ORDER BY m.id ASC")
    List<Course> findAllWithModules();

    // ✅ Fetch single course + modules (with ORDER BY)
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.modules m WHERE c.id = :id ORDER BY m.id ASC")
    Optional<Course> findByIdWithModules(Long id);

    // ✅ FIXED: Use EXISTS subquery for collection navigation
    @Query("SELECT DISTINCT c FROM Course c " +
           "LEFT JOIN FETCH c.modules m " +
           "WHERE EXISTS (" +
           "  SELECT 1 FROM c.subscribedUsers u WHERE u.id = :userId" +
           ") " +
           "ORDER BY m.id ASC")
    List<Course> findBySubscribedUsers_Id(@Param("userId") Long userId);

    // Alternative: Simple Spring Data JPA method (if you don't need modules fetched)
    List<Course> findBySubscribedUsersId(Long userId);

    List<Course> findByTitleContainingIgnoreCase(String title);

    // SIMPLIFIED VERSIONS THAT WILL WORK:

    // 1. Search by tag (contains)
    @Query("SELECT c FROM Course c WHERE :tag MEMBER OF c.tags")
    List<Course> findByTag(@Param("tag") String tag);

    // 2. Search by title OR tags (case-insensitive)
    @Query("SELECT c FROM Course c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT t FROM c.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> searchByTitleOrTags(@Param("keyword") String keyword);

    // 3. Get all courses with tags
    @Query("SELECT c FROM Course c WHERE c.tags IS NOT EMPTY")
    List<Course> findAllWithTags();
} 