package com.example.cdaxVideo.Repository;


import com.example.cdaxVideo.Entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByModuleId(Long moduleId);
}
