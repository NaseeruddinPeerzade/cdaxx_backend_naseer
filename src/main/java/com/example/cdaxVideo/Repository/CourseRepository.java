package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // Fetch only courses + modules (not videos here)
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.modules")
    List<Course> findAllWithModules();

    // Fetch single course + modules (not videos here)
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.modules WHERE c.id = :id")
    Optional<Course> findByIdWithModules(Long id);

    List<Course> findBySubscribedUsers_Id(Long userId);

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