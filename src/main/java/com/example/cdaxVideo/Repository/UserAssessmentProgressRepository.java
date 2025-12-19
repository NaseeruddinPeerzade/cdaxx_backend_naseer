package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.UserAssessmentProgress;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAssessmentProgressRepository extends JpaRepository<UserAssessmentProgress, Long> {
    Optional<UserAssessmentProgress> findByUserAndAssessment(User user, Assessment assessment);
    boolean existsByUserAndAssessment(User user, Assessment assessment);
}
