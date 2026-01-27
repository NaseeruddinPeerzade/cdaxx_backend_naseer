package com.example.cdaxVideo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(
    name = "user_video_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_id"})
)
public class UserVideoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* =========================
       REQUIRED RELATIONSHIPS
       ========================= */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    /* =========================
       PROGRESS STATE
       ========================= */

    @Column(nullable = false)
    private boolean unlocked = false;

    @Column(nullable = false)
    private boolean completed = false;

    /* =========================
       WATCH TIME TRACKING
       ========================= */

    @Column(name = "watched_seconds")
    private Integer watchedSeconds = 0;

    @Column(name = "last_position_seconds")
    private Integer lastPositionSeconds = 0;

    @Column(name = "forward_jumps_count")
    private Integer forwardJumpsCount = 0;

    /* =========================
       TIMESTAMPS
       ========================= */

    @Column(name = "unlocked_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date unlockedOn;

    @Column(name = "completed_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedOn;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    /* =========================
       MANUAL COMPLETION FLAG
       ========================= */

    @Column(name = "manually_completed")
    private Boolean manuallyCompleted = false;

    /* =========================
       GETTERS & SETTERS
       ========================= */

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
        if (unlocked && unlockedOn == null) {
            this.unlockedOn = new Date();
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && completedOn == null) {
            this.completedOn = new Date();
        }
    }

    public Integer getWatchedSeconds() {
        return watchedSeconds;
    }

    public void setWatchedSeconds(Integer watchedSeconds) {
        this.watchedSeconds = watchedSeconds != null ? watchedSeconds : 0;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void addWatchedSeconds(Integer seconds) {
        if (seconds != null && seconds > 0) {
            this.watchedSeconds =
                (this.watchedSeconds == null ? 0 : this.watchedSeconds) + seconds;
            this.lastUpdatedAt = LocalDateTime.now();
        }
    }

    public Integer getLastPositionSeconds() {
        return lastPositionSeconds;
    }

    public void setLastPositionSeconds(Integer lastPositionSeconds) {
        this.lastPositionSeconds = lastPositionSeconds != null ? lastPositionSeconds : 0;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public Integer getForwardJumpsCount() {
        return forwardJumpsCount;
    }

    public void incrementForwardJumpsCount() {
        this.forwardJumpsCount =
            (this.forwardJumpsCount == null ? 0 : this.forwardJumpsCount) + 1;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    public void setForwardJumpsCount(Integer forwardJumpsCount) {
        this.forwardJumpsCount = forwardJumpsCount != null ? forwardJumpsCount : 0;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /* ======== MISSING SETTERS (FIX) ======== */

    public Date getUnlockedOn() {
        return unlockedOn;
    }

    public void setUnlockedOn(Date unlockedOn) {
        this.unlockedOn = unlockedOn;
    }

    public Date getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(Date completedOn) {
        this.completedOn = completedOn;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Boolean getManuallyCompleted() {
        return manuallyCompleted;
    }

    public void setManuallyCompleted(Boolean manuallyCompleted) {
        this.manuallyCompleted = manuallyCompleted;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    
    

    /* =========================
       BUSINESS LOGIC
       ========================= */

    public boolean shouldMarkCompleted(Integer videoDurationSeconds) {
        if (videoDurationSeconds == null || videoDurationSeconds <= 0) {
            return false;
        }

        boolean watchedEnough =
            watchedSeconds != null &&
            watchedSeconds >= (int) (videoDurationSeconds * 0.95);

        boolean notSkippedTooMuch =
            forwardJumpsCount == null || forwardJumpsCount < 10;

        return watchedEnough && notSkippedTooMuch;
    }

    /* =========================
       LIFECYCLE CALLBACKS
       ========================= */

    @PrePersist
    protected void onCreate() {
        if (unlocked && unlockedOn == null) {
            unlockedOn = new Date();
        }
        lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
