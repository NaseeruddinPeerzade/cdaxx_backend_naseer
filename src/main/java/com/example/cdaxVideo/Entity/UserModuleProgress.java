package com.example.cdaxVideo.Entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "user_module_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "module_id"}))
public class UserModuleProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Module module;

    private boolean unlocked = false;
    private boolean assessmentPassed = false;
    private Date unlockedOn;
    private Date assessmentPassedOn;

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public boolean isAssessmentPassed() { return assessmentPassed; }
    public void setAssessmentPassed(boolean assessmentPassed) { this.assessmentPassed = assessmentPassed; }

    public Date getUnlockedOn() { return unlockedOn; }
    public void setUnlockedOn(Date unlockedOn) { this.unlockedOn = unlockedOn; }

    public Date getAssessmentPassedOn() { return assessmentPassedOn; }
    public void setAssessmentPassedOn(Date assessmentPassedOn) { this.assessmentPassedOn = assessmentPassedOn; }
}
