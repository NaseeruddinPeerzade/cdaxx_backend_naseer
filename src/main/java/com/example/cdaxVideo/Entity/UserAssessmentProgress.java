package com.example.cdaxVideo.Entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "user_assessment_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "assessment_id"}))
public class UserAssessmentProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Assessment assessment;
    
    @Column(nullable = false)
    private Integer attempts = 0;

    private boolean passed = false;
    private Date passedOn;
    
    @Column(nullable = false)
    private Boolean unlocked = false;

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Assessment getAssessment() { return assessment; }
    public void setAssessment(Assessment assessment) { this.assessment = assessment; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public Date getPassedOn() { return passedOn; }
    public void setPassedOn(Date passedOn) { this.passedOn = passedOn; }
    
	public Integer getAttempts() {
		 return attempts == null ? 0 : attempts;
	}
	public void setAttempts(Integer attempts) {
		this.attempts = (attempts == null ? 0 : attempts);
	}
	public Boolean getUnlocked() {
		return unlocked == null ? false : unlocked;
	}
	public void setUnlocked(Boolean unlocked) {
		this.unlocked = (unlocked == null ? false : unlocked);
	}
	
    
}
