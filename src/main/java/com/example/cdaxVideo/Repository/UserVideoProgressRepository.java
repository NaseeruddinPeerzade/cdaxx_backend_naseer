package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.UserVideoProgress;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserVideoProgressRepository extends JpaRepository<UserVideoProgress, Long> {
    Optional<UserVideoProgress> findByUserAndVideo(User user, Video video);
    boolean existsByUserAndVideo(User user, Video video);
}
