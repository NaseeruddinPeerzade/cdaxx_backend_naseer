package com.example.cdaxVideo.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String videoUrl;

    // CHANGE: Remove @JsonBackReference, use @JsonIgnore
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;

    @Transient
    @JsonProperty("isLocked")
    private boolean isLocked = true;

    @Transient
    @JsonProperty("isCompleted")
    private boolean isCompleted = false;
    
    // Constructors
    public Video() {}

    public Video(String title, String videoUrl) {
        this.title = title;
        this.videoUrl = videoUrl;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }
    
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { this.isLocked = locked; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }

}