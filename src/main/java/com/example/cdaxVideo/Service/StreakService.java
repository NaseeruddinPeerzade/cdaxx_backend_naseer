package com.example.cdaxVideo.Service;


import com.example.cdaxVideo.Entity.UserVideoActivity;
import com.example.cdaxVideo.Repository.UserVideoActivityRepository;
import com.example.cdaxVideo.Repository.UserRepository;
import com.example.cdaxVideo.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class StreakService {

    @Autowired
    private UserVideoActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Record a video completion for given User (by id). This increments today's counter.
     */
    @Transactional
    public void recordVideoCompletionForUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        String email = user.getEmail();
        recordVideoCompletionByEmailAndUser(email, userId);
    }

    /**
     * Record a video completion for given email (if you only have email)
     */
    @Transactional
    public void recordVideoCompletionByEmail(String email) {
        recordVideoCompletionByEmailAndUser(email, null);
    }

    @Transactional
    protected void recordVideoCompletionByEmailAndUser(String email, Long userIdOrNull) {
        LocalDate today = LocalDate.now();
        Optional<UserVideoActivity> opt;
        if (userIdOrNull != null) {
            opt = activityRepository.findByUserIdAndDate(userIdOrNull, today);
            if (opt.isEmpty()) opt = activityRepository.findByEmailAndDate(email, today);
        } else {
            opt = activityRepository.findByEmailAndDate(email, today);
        }

        if (opt.isPresent()) {
            UserVideoActivity a = opt.get();
            a.increment();
            activityRepository.save(a);
        } else {
            UserVideoActivity a = new UserVideoActivity();
            a.setEmail(email);
            a.setUserId(userIdOrNull);
            a.setDate(today);
            a.setVideosWatched(1);
            activityRepository.save(a);
        }
    }

    /**
     * Fetch streak/activity by userId
     */
    public List<UserVideoActivity> getStreakByUserId(Long userId) {
        return activityRepository.findByUserIdOrderByDateAsc(userId);
    }

    /**
     * Fetch streak/activity by email
     */
    public List<UserVideoActivity> getStreakByEmail(String email) {
        return activityRepository.findByEmailOrderByDateAsc(email);
    }
}
