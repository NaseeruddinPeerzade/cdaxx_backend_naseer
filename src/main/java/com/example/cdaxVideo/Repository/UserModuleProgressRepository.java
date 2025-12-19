package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.UserModuleProgress;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserModuleProgressRepository extends JpaRepository<UserModuleProgress, Long> {
    Optional<UserModuleProgress> findByUserAndModule(User user, Module module);
    boolean existsByUserAndModule(User user, Module module);
}
