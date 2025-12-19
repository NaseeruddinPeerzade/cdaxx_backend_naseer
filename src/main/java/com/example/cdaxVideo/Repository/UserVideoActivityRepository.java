package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.UserVideoActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserVideoActivityRepository extends JpaRepository<UserVideoActivity, Long> {

    List<UserVideoActivity> findByEmailOrderByDateAsc(String email);

    List<UserVideoActivity> findByUserIdOrderByDateAsc(Long userId);

    Optional<UserVideoActivity> findByEmailAndDate(String email, LocalDate date);

    Optional<UserVideoActivity> findByUserIdAndDate(Long userId, LocalDate date);
}

