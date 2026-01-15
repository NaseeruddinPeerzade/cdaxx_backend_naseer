package com.example.cdaxVideo.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class StreakSummaryDTO {
    private Long courseId;
    private String courseTitle;
    private Integer currentStreakDays;
    private Integer longestStreakDays;
    private Double overallProgress;
    private LocalDate lastActiveDate;
    
    // For 30-day view (existing)
    private List<StreakDayDTO> last30Days;
    
    // NEW: For month-based view
    private List<StreakDayDTO> monthDays;
    private LocalDate monthStartDate;
    private LocalDate monthEndDate;
    private String monthName;
    private Integer year;
    private Integer activeDaysInMonth;
    private Integer totalDaysInMonth;
    
    // Constructor for backward compatibility
    public StreakSummaryDTO() {}
    
    // Helper method to check if this is a month-based response
    public boolean isMonthView() {
        return monthDays != null && !monthDays.isEmpty();
    }
    
    // Get the appropriate days list based on view
    public List<StreakDayDTO> getDays() {
        return isMonthView() ? monthDays : last30Days;
    }
    
    // Calculate active days in the current view
    public int calculateActiveDays() {
        List<StreakDayDTO> days = getDays();
        if (days == null || days.isEmpty()) return 0;
        
        return (int) days.stream()
            .filter(day -> day.getIsActiveDay() != null && day.getIsActiveDay())
            .count();
    }
}