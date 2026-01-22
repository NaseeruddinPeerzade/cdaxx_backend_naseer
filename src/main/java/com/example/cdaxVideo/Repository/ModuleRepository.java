package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {
    // ✅ ORDER BY id instead of displayOrder
    @Query("SELECT m FROM Module m WHERE m.course.id = :courseId ORDER BY m.id ASC")
    List<Module> findByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT COUNT(m) FROM Module m WHERE m.course.id = :courseId")
    Long countByCourseId(@Param("courseId") Long courseId);
        // NEW: Eagerly load videos
    @Query("SELECT DISTINCT m FROM Module m " +
           "LEFT JOIN FETCH m.videos " +
           "LEFT JOIN FETCH m.course " +
           "WHERE m.course.id = :courseId " +
           "ORDER BY m.id")
    List<Module> findByCourseIdWithVideos(@Param("courseId") Long courseId);
}