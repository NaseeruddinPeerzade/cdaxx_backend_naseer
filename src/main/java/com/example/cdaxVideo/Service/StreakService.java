// Replace your entire StreakService.java with this complete version:

package com.example.cdaxVideo.Service;

import com.example.cdaxVideo.DTO.*;
import com.example.cdaxVideo.Entity.*;
import com.example.cdaxVideo.Entity.Module;
import com.example.cdaxVideo.Repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StreakService {
    
    private final UserStreakRepository userStreakRepository;
    private final UserVideoProgressRepository userVideoProgressRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final ModuleRepository moduleRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(StreakService.class);
    private static final int STREAK_CYCLE_DAYS = 30;
    
    // ========== UPDATE STREAK METHODS ==========
    
    @Transactional
    public void updateStreakForVideoWatch(Long userId, Long courseId, Long videoId, 
                                          Integer watchedSeconds, boolean isCompleted) {
        
        try {
            logger.info("🎯 Starting streak update for user: {}, course: {}, video: {}", 
                       userId, courseId, videoId);
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
            
            LocalDate today = LocalDate.now();
            
            logger.info("📅 Today's date: {}", today);
            
            // Get or create streak for today
            UserStreak streak = userStreakRepository
                .findByUserIdAndCourseIdAndStreakDate(userId, courseId, today)
                .orElseGet(() -> {
                    logger.info("📝 Creating new streak record for user {} on {}", userId, today);
                    UserStreak newStreak = new UserStreak();
                    newStreak.setUser(user);
                    newStreak.setCourse(course);
                    newStreak.setStreakDate(today);
                    newStreak.setCreatedAt(LocalDateTime.now());
                    newStreak.setIsActiveDay(false);
                    newStreak.setWatchedSeconds(0);
                    newStreak.setCompletedVideosCount(0);
                    newStreak.setProgressPercentage(0.0);
                    return newStreak;
                });
            
            // ✅ CRITICAL FIX: Set isActiveDay to true
            streak.setIsActiveDay(true);
            streak.setUpdatedAt(LocalDateTime.now());
            
            // Update watched seconds
            int previousWatched = streak.getWatchedSeconds();
            streak.addWatchedSeconds(watchedSeconds);
            logger.info("⏱️ Updated watched seconds: {} -> {}", previousWatched, streak.getWatchedSeconds());
            
            // Update total available seconds if not set
            if (streak.getTotalAvailableSeconds() == 0) {
                logger.info("📊 Calculating total available seconds for course: {}", course.getTitle());
                
                int[] totals = calculateCourseTotals(courseId);
                streak.setTotalAvailableSeconds(totals[0]);
                streak.setTotalVideosCount(totals[1]);
                
                logger.info("📈 Course has {} videos with total {} seconds", totals[1], totals[0]);
            }
            
            // Update video counts
            if (isCompleted) {
                int previousCompleted = streak.getCompletedVideosCount();
                streak.setCompletedVideosCount(previousCompleted + 1);
                logger.info("✅ Incremented completed videos: {} -> {}", 
                          previousCompleted, streak.getCompletedVideosCount());
            }
            
            // ✅ Calculate progress percentage
            if (streak.getTotalAvailableSeconds() > 0) {
                double progress = ((double) streak.getWatchedSeconds() / streak.getTotalAvailableSeconds()) * 100;
                streak.setProgressPercentage(progress);
                logger.info("📊 Progress percentage: {}% ({} / {} seconds)", 
                          String.format("%.2f", progress), 
                          streak.getWatchedSeconds(), 
                          streak.getTotalAvailableSeconds());
            }
            
            // Save the streak
            UserStreak savedStreak = userStreakRepository.save(streak);
            logger.info("💾 Saved streak record with ID: {}", savedStreak.getId());
            
            logger.info("✅ Streak updated successfully for user {} in course {}", userId, courseId);
            
        } catch (Exception e) {
            logger.error("❌ Error updating streak: {}", e.getMessage(), e);
        }
    }
    
    // ========== GET STREAK METHODS ==========
    
    /**
     * Get 30-day streak for a specific course
     */
    public StreakSummaryDTO getCourseStreak(Long userId, Long courseId) {
        logger.info("📊 Getting 30-day streak for user: {}, course: {}", userId, courseId);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(STREAK_CYCLE_DAYS - 1);
        
        List<UserStreak> streaks = userStreakRepository
            .findByUserIdAndCourseIdAndStreakDateBetween(userId, courseId, startDate, endDate);
        
        logger.info("📅 Found {} streak records between {} and {}", 
                   streaks.size(), startDate, endDate);
        
        return buildStreakSummary(userId, courseId, streaks, startDate, endDate, false);
    }
    
    /**
     * Get month-based streak for a specific course
     */
    public StreakSummaryDTO getCourseStreakForMonth(Long userId, Long courseId, LocalDate monthDate) {
        logger.info("📊 Getting month streak for user: {}, course: {}, month: {}-{}", 
                   userId, courseId, monthDate.getYear(), monthDate.getMonth());
        
        // Get first and last day of the month
        LocalDate firstDay = monthDate.withDayOfMonth(1);
        LocalDate lastDay = monthDate.withDayOfMonth(monthDate.lengthOfMonth());
        
        logger.info("📅 Month range: {} to {} ({} days)", 
                   firstDay, lastDay, monthDate.lengthOfMonth());
        
        // Get streaks for the entire month
        List<UserStreak> streaks = userStreakRepository
            .findByUserIdAndCourseIdAndStreakDateBetween(userId, courseId, firstDay, lastDay);
        
        logger.info("📅 Found {} streak records for month {}-{}", 
                   streaks.size(), monthDate.getYear(), monthDate.getMonth());
        
        return buildStreakSummary(userId, courseId, streaks, firstDay, lastDay, true);
    }
    
    /**
     * Get streak for all courses (profile page)
     */
    public Map<String, Object> getUserStreakOverview(Long userId) {
        logger.info("📊 Getting streak overview for user: {}", userId);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(STREAK_CYCLE_DAYS - 1);
        
        List<UserStreak> allStreaks = userStreakRepository
            .findByUserIdAndStreakDateBetween(userId, startDate, endDate);
        
        logger.info("📅 Found {} total streak records for user {}", allStreaks.size(), userId);
        
        // Group by course
        Map<Course, List<UserStreak>> streaksByCourse = allStreaks.stream()
            .collect(Collectors.groupingBy(UserStreak::getCourse));
        
        List<StreakSummaryDTO> courseSummaries = new ArrayList<>();
        
        for (Map.Entry<Course, List<UserStreak>> entry : streaksByCourse.entrySet()) {
            Course course = entry.getKey();
            List<UserStreak> courseStreaks = entry.getValue();
            
            logger.info("📚 Processing course: {} with {} streak records", 
                       course.getTitle(), courseStreaks.size());
            
            StreakSummaryDTO summary = buildStreakSummary(
                userId, course.getId(), courseStreaks, startDate, endDate, false);
            courseSummaries.add(summary);
        }
        
        // Calculate overall stats
        int totalActiveDays = (int) allStreaks.stream()
            .filter(UserStreak::getIsActiveDay)
            .count();
        
        Map<String, Object> response = new HashMap<>();
        response.put("courseSummaries", courseSummaries);
        response.put("totalActiveDays", totalActiveDays);
        response.put("currentCycleStart", startDate.toString());
        response.put("currentCycleEnd", endDate.toString());
        response.put("cycleDurationDays", STREAK_CYCLE_DAYS);
        
        logger.info("📈 User {} has {} active days in current cycle", userId, totalActiveDays);
        
        return response;
    }
    
    /**
     * Get detailed day information
     */
    public StreakDayDTO getDayDetails(Long userId, Long courseId, LocalDate date) {
        logger.info("📅 Getting day details for user: {}, course: {}, date: {}", 
                   userId, courseId, date);
        
        Optional<UserStreak> streakOpt = userStreakRepository
            .findByUserIdAndCourseIdAndStreakDate(userId, courseId, date);
        
        if (streakOpt.isEmpty()) {
            logger.info("📭 No streak record found for date {}", date);
            return createEmptyDayDTO(date);
        }
        
        UserStreak streak = streakOpt.get();
        logger.info("📋 Found streak record: {} seconds watched, {}% progress", 
                   streak.getWatchedSeconds(), streak.getProgressPercentage());
        
        StreakDayDTO dto = convertToDayDTO(streak);
        
        // Fetch and set video details
        List<VideoProgressDetailDTO> videoDetails = getVideoDetailsForDay(userId, courseId, date);
        dto.setVideoDetails(videoDetails);
        
        logger.info("📊 Returning {} video details for date {}", videoDetails.size(), date);
        
        return dto;
    }
    
    // ========== BUILD STREAK SUMMARY METHODS ==========
    
    /**
     * Build streak summary - unified method for both 30-day and month views
     */
    private StreakSummaryDTO buildStreakSummary(Long userId, Long courseId, 
                                                List<UserStreak> streaks, 
                                                LocalDate startDate, LocalDate endDate,
                                                boolean isMonthView) {
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        
        logger.info("📅 Building {} streak summary for course: {}", 
                   isMonthView ? "month" : "30-day", course.getTitle());
        
        // Create map for quick lookup
        Map<LocalDate, UserStreak> streakMap = streaks.stream()
            .collect(Collectors.toMap(UserStreak::getStreakDate, s -> s));
        
        // Build day calendar
        List<StreakDayDTO> days = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            UserStreak streak = streakMap.get(currentDate);
            if (streak != null) {
                StreakDayDTO dayDTO = convertToDayDTO(streak);
                
                // Fetch video details for active days
                if (Boolean.TRUE.equals(streak.getIsActiveDay())) {
                    List<VideoProgressDetailDTO> videoDetails = getVideoDetailsForDay(userId, courseId, currentDate);
                    dayDTO.setVideoDetails(videoDetails);
                }
                
                days.add(dayDTO);
            } else {
                days.add(createEmptyDayDTO(currentDate));
            }
            currentDate = currentDate.plusDays(1);
        }
        
        // Calculate streak stats
        int currentStreak = calculateCurrentStreak(days);
        int longestStreak = calculateLongestStreak(days);
        
        // Calculate progress
        double overallProgress = calculateOverallProgress(days);
        
        // Create summary
        StreakSummaryDTO summary = new StreakSummaryDTO();
        summary.setCourseId(courseId);
        summary.setCourseTitle(course.getTitle());
        summary.setCurrentStreakDays(currentStreak);
        summary.setLongestStreakDays(longestStreak);
        summary.setOverallProgress(overallProgress);
        summary.setLastActiveDate(getLastActiveDate(streaks));
        
        // Set appropriate day list
        if (isMonthView) {
            summary.setMonthDays(days);
            summary.setMonthStartDate(startDate);
            summary.setMonthEndDate(endDate);
            summary.setMonthName(getMonthName(startDate.getMonthValue()));
            summary.setYear(startDate.getYear());
            
            // Calculate month-specific stats
            int activeDays = (int) days.stream()
                .filter(day -> Boolean.TRUE.equals(day.getIsActiveDay()))
                .count();
            summary.setActiveDaysInMonth(activeDays);
            summary.setTotalDaysInMonth(startDate.lengthOfMonth());
            
            logger.info("📈 Month streak calculated: {} days current, {} days longest, {} active days, {:.1f}% progress", 
                       currentStreak, longestStreak, activeDays, overallProgress);
        } else {
            summary.setLast30Days(days);
            logger.info("📈 30-day streak calculated: {} days current, {} days longest, {:.1f}% overall", 
                       currentStreak, longestStreak, overallProgress);
        }
        
        return summary;
    }
    
    // ========== HELPER METHODS ==========
    
    private StreakDayDTO convertToDayDTO(UserStreak streak) {
        StreakDayDTO dto = new StreakDayDTO();
        dto.setDate(streak.getStreakDate());
        dto.setWatchedSeconds(streak.getWatchedSeconds());
        dto.setTotalAvailableSeconds(streak.getTotalAvailableSeconds());
        dto.setProgressPercentage(streak.getProgressPercentage());
        dto.setIsActiveDay(streak.getIsActiveDay());
        dto.setColorCode(getColorCode(streak.getProgressPercentage()));
        dto.setVideoDetails(new ArrayList<>()); // Will be populated later if needed
        return dto;
    }
    
    private StreakDayDTO createEmptyDayDTO(LocalDate date) {
        StreakDayDTO dto = new StreakDayDTO();
        dto.setDate(date);
        dto.setWatchedSeconds(0);
        dto.setTotalAvailableSeconds(0);
        dto.setProgressPercentage(0.0);
        dto.setIsActiveDay(false);
        dto.setColorCode(getColorCode(0.0));
        dto.setVideoDetails(new ArrayList<>());
        return dto;
    }
    
    /**
     * Get video progress details for a specific day
     */
    private List<VideoProgressDetailDTO> getVideoDetailsForDay(Long userId, Long courseId, LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            List<UserVideoProgress> progressList = userVideoProgressRepository
                .findByUserIdAndCourseIdAndDateRange(userId, courseId, startOfDay, endOfDay);
            
            if (progressList == null || progressList.isEmpty()) {
                return new ArrayList<>();
            }
            
            return progressList.stream()
                .map(this::convertToVideoProgressDetailDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error getting video details for day {}: {}", date, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private VideoProgressDetailDTO convertToVideoProgressDetailDTO(UserVideoProgress progress) {
        Video video = progress.getVideo();
        VideoProgressDetailDTO dto = new VideoProgressDetailDTO();
        
        dto.setVideoId(video.getId());
        dto.setVideoTitle(video.getTitle());
        dto.setWatchedSeconds(progress.getWatchedSeconds());
        dto.setVideoDuration(video.getDuration());
        
        // Calculate progress percentage
        double videoProgress = 0.0;
        if (video.getDuration() != null && video.getDuration() > 0) {
            videoProgress = (progress.getWatchedSeconds() * 100.0) / video.getDuration();
        }
        dto.setVideoProgress(videoProgress);
        dto.setIsCompleted(progress.isCompleted());
        
        if (progress.getLastUpdatedAt() != null) {
            dto.setWatchedDate(progress.getLastUpdatedAt().toLocalDate());
        }
        
        return dto;
    }
    
    /**
     * Calculate course totals (duration and video count)
     */
    private int[] calculateCourseTotals(Long courseId) {
        List<Module> modules = moduleRepository.findByCourseId(courseId);
        int totalDuration = 0;
        int totalVideos = 0;
        
        for (Module module : modules) {
            List<Video> moduleVideos = videoRepository.findByModuleId(module.getId());
            totalVideos += moduleVideos.size();
            for (Video v : moduleVideos) {
                totalDuration += v.getDuration();
            }
        }
        
        return new int[]{totalDuration, totalVideos};
    }
    
    /**
     * Color coding based on progress percentage
     */
    private String getColorCode(Double percentage) {
        if (percentage == null || percentage == 0) return "#E5E7EB"; // Gray
        if (percentage < 25) return "#FEF3C7"; // Light yellow
        if (percentage < 50) return "#FDE68A"; // Yellow
        if (percentage < 75) return "#FBBF24"; // Orange
        if (percentage < 100) return "#F59E0B"; // Dark orange
        return "#10B981"; // Green for 100%
    }
    
    /**
     * Calculate current streak from days list
     */
    private int calculateCurrentStreak(List<StreakDayDTO> days) {
        int streak = 0;
        LocalDate today = LocalDate.now();
        
        // Sort days newest first
        List<StreakDayDTO> sortedDays = new ArrayList<>(days);
        sortedDays.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        
        for (StreakDayDTO day : sortedDays) {
            if (day.getDate().isAfter(today)) {
                continue; // Skip future dates
            }
            
            if (day.getDate().equals(today)) {
                // Today - check if there's significant progress
                if (day.getProgressPercentage() != null && day.getProgressPercentage() >= 5.0) {
                    streak++;
                } else {
                    break;
                }
            } else if (Boolean.TRUE.equals(day.getIsActiveDay())) {
                streak++;
            } else {
                break;
            }
        }
        
        return streak;
    }
    
    /**
     * Calculate longest streak from days list
     */
    private int calculateLongestStreak(List<StreakDayDTO> days) {
        int maxStreak = 0;
        int currentStreak = 0;
        
        // Sort chronologically
        List<StreakDayDTO> sortedDays = new ArrayList<>(days);
        sortedDays.sort(Comparator.comparing(StreakDayDTO::getDate));
        
        for (StreakDayDTO day : sortedDays) {
            if (Boolean.TRUE.equals(day.getIsActiveDay())) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }
        
        return maxStreak;
    }
    
    /**
     * Calculate overall progress from days list
     */
    private double calculateOverallProgress(List<StreakDayDTO> days) {
        // Calculate average progress of active days
        return days.stream()
            .filter(day -> Boolean.TRUE.equals(day.getIsActiveDay()))
            .mapToDouble(StreakDayDTO::getProgressPercentage)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Get last active date from streaks
     */
    private LocalDate getLastActiveDate(List<UserStreak> streaks) {
        return streaks.stream()
            .filter(streak -> Boolean.TRUE.equals(streak.getIsActiveDay()))
            .map(UserStreak::getStreakDate)
            .max(LocalDate::compareTo)
            .orElse(null);
    }
    
    /**
     * Get month name from month number
     */
    private String getMonthName(int month) {
        String[] monthNames = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        return monthNames[month - 1];
    }
    
    /**
     * Get today's streak data
     */
    public UserStreak getTodayStreak(Long userId, Long courseId) {
        LocalDate today = LocalDate.now();
        return userStreakRepository
            .findByUserIdAndCourseIdAndStreakDate(userId, courseId, today)
            .orElse(null);
    }
    
    /**
     * Get streak for specific date
     */
    public UserStreak getStreakForDate(Long userId, Long courseId, LocalDate date) {
        return userStreakRepository
            .findByUserIdAndCourseIdAndStreakDate(userId, courseId, date)
            .orElse(null);
    }
    
    /**
     * Create test streak data (for debugging)
     */
    @Transactional
    public UserStreak createTestStreak(Long userId, Long courseId, LocalDate date, int watchedSeconds) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            Optional<UserStreak> existing = userStreakRepository
                .findByUserIdAndCourseIdAndStreakDate(userId, courseId, date);
            
            if (existing.isPresent()) {
                logger.info("📊 Streak already exists for date {}", date);
                return existing.get();
            }
            
            UserStreak streak = new UserStreak();
            streak.setUser(user);
            streak.setCourse(course);
            streak.setStreakDate(date);
            streak.setCreatedAt(LocalDateTime.now());
            streak.setUpdatedAt(LocalDateTime.now());
            streak.setIsActiveDay(true);
            streak.setWatchedSeconds(watchedSeconds);
            
            int[] totals = calculateCourseTotals(courseId);
            streak.setTotalAvailableSeconds(totals[0]);
            streak.setTotalVideosCount(totals[1]);
            
            if (totals[0] > 0) {
                double progress = ((double) watchedSeconds / totals[0]) * 100;
                streak.setProgressPercentage(progress);
            }
            
            UserStreak saved = userStreakRepository.save(streak);
            logger.info("✅ Created test streak with ID: {}", saved.getId());
            
            return saved;
            
        } catch (Exception e) {
            logger.error("❌ Error creating test streak: {}", e.getMessage(), e);
            throw e;
        }
    }
}