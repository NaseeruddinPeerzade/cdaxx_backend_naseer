package com.example.cdaxVideo.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "userProgress"})
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int durationSec;

    // FIX: Add @JsonIgnoreProperties to prevent circular reference
    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "userProgress"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // FIX: Add @JsonIgnoreProperties
    @JsonIgnore
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "userProgress"})
    private List<Video> videos = new ArrayList<>();
    
    @Transient
    @JsonProperty("isLocked")
    private boolean isLocked = true;

    @Transient
    @JsonProperty("assessmentLocked")
    private boolean assessmentLocked = true;

    // Constructors
    public Module() {
        this.videos = new ArrayList<>();
    }

    public Module(String title, int durationSec) {
        this();
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

    // ========== CRITICAL FIX: Videos Collection Methods ==========
    
    // GETTER - Ensure collection is never null
    public List<Video> getVideos() { 
        if (this.videos == null) {
            this.videos = new ArrayList<>();
        }
        return videos; 
    }
    
    // FIXED: Safe setVideos method that handles lazy initialization
    public void setVideos(List<Video> videos) {
        // SAFE APPROACH: Check if collection is already initialized
        if (this.videos == null) {
            this.videos = new ArrayList<>();
        }
        
        // If we have an existing collection, check if it's a Hibernate proxy
        boolean canClear = true;
        try {
            // Try to check if it's a Hibernate PersistentCollection
            if (this.videos instanceof org.hibernate.collection.spi.PersistentCollection) {
                org.hibernate.collection.spi.PersistentCollection pc = 
                    (org.hibernate.collection.spi.PersistentCollection) this.videos;
                
                // Check if the collection is initialized
                if (!pc.wasInitialized()) {
                    // If it's not initialized, we shouldn't clear it
                    // Instead, create a new list
                    this.videos = new ArrayList<>();
                    canClear = false;
                }
            }
        } catch (Exception e) {
            // If any exception occurs, create a new list
            this.videos = new ArrayList<>();
            canClear = false;
        }
        
        // Clear existing collection if safe to do so
        if (canClear) {
            this.videos.clear();
        }
        
        // Add all new videos
        if (videos != null) {
            for (Video video : videos) {
                addVideo(video);
            }
        }
    }
    
    // Helper method to add video (maintains bidirectional relationship)
    public void addVideo(Video video) {
        if (this.videos == null) {
            this.videos = new ArrayList<>();
        }
        if (!this.videos.contains(video)) {
            this.videos.add(video);
            video.setModule(this); // Set the back reference
        }
    }
    
    // Helper method to remove video
    public void removeVideo(Video video) {
        if (this.videos != null && this.videos.contains(video)) {
            this.videos.remove(video);
            video.setModule(null); // Remove the back reference
        }
    }
    
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { this.isLocked = locked; }

    public boolean isAssessmentLocked() { return assessmentLocked; }
    public void setAssessmentLocked(boolean assessmentLocked) { this.assessmentLocked = assessmentLocked; }
    
    // ========== Equals and HashCode (Important for collections) ==========
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Module)) return false;
        return id != null && id.equals(((Module) o).getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    // ========== toString() for debugging ==========
    @Override
    public String toString() {
        return "Module{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", videosCount=" + (videos != null ? videos.size() : 0) +
                ", isLocked=" + isLocked +
                '}';
    }
}