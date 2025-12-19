package com.example.cdaxVideo.Entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "user_video_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_id"}))
public class UserVideoProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Video video;

    private boolean unlocked = false;
    private boolean completed = false;
    private Date unlockedOn;
    private Date completedOn;

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Date getUnlockedOn() { return unlockedOn; }
    public void setUnlockedOn(Date unlockedOn) { this.unlockedOn = unlockedOn; }

    public Date getCompletedOn() { return completedOn; }
    public void setCompletedOn(Date completedOn) { this.completedOn = completedOn; }
}
