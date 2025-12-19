package com.example.cdaxVideo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int durationSec;

    // CHANGE: Remove @JsonBackReference, use @JsonIgnore
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // CHANGE: Remove @JsonManagedReference, use @JsonIgnoreProperties
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("module")  // This tells Jackson to ignore 'module' field in Video objects
    private List<Video> videos = new ArrayList<>();
    
    @Transient
    @JsonProperty("isLocked")
    private boolean isLocked = true;

    @Transient
    @JsonProperty("assessmentLocked")
    private boolean assessmentLocked = true;

    // Constructors, getters, setters remain the same...
    public Module() {}

    public Module(String title, int durationSec) {
        this.title = title;
        this.durationSec = durationSec;
    }

    // Getters and Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDurationSec() { return durationSec; }
    public void setDurationSec(int durationSec) { this.durationSec = durationSec; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public List<Video> getVideos() { return videos; }
    public void setVideos(List<Video> videos) { this.videos = videos; }
    
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { this.isLocked = locked; }

    public boolean isAssessmentLocked() { return assessmentLocked; }
    public void setAssessmentLocked(boolean assessmentLocked) { this.assessmentLocked = assessmentLocked; }
}