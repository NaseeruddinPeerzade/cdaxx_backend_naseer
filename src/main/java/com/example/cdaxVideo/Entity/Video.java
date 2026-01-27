    package com.example.cdaxVideo.Entity;
    import com.fasterxml.jackson.annotation.JsonIgnore;
    import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
    import com.fasterxml.jackson.annotation.JsonProperty;
    import jakarta.persistence.*;
    import java.time.LocalDateTime;
    import java.util.HashSet;
    import java.util.Set;

    @Entity
    @Table(name = "videos")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String title;

        @Column(nullable = false)
        private String videoUrl;

        @Column
        private String youtubeId;

        @Column(nullable = false)
        private Integer duration; // Duration in seconds

        @JsonIgnore
        @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "module_id", nullable = false)
        private Module module;

        @Column(nullable = false)
        private Integer displayOrder;

        @Column(name = "is_preview", nullable = false)
        private Boolean isPreview = false;

        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;

        @JsonIgnore
        @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
        private Set<UserVideoProgress> userProgress = new HashSet<>();

        /* =========================
        RUNTIME / RESPONSE FIELDS
        ========================= */

        @Transient
        @JsonProperty("isLocked")
        private boolean isLocked = true;

        @Transient
        @JsonProperty("isCompleted")
        private boolean isCompleted = false;

        // Constructors
        public Video() {
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }

        public Video(String title, String videoUrl, Integer duration, Module module) {
            this();
            this.title = title;
            this.videoUrl = videoUrl;
            this.duration = duration;
            this.module = module;
        }

        // Getters and Setters (DB fields)

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getYoutubeId() {
            return youtubeId;
        }

        public void setYoutubeId(String youtubeId) {
            this.youtubeId = youtubeId;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public Module getModule() {
            return module;
        }

        public void setModule(Module module) {
            this.module = module;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public Boolean getIsPreview() {
            return isPreview;
        }

        public void setIsPreview(Boolean isPreview) {
            this.isPreview = isPreview;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public Set<UserVideoProgress> getUserProgress() {
            return userProgress;
        }

        public void setUserProgress(Set<UserVideoProgress> userProgress) {
            this.userProgress = userProgress;
        }

        /* =========================
        REQUIRED BY CourseService
        ========================= */

        public boolean isLocked() {
            return isLocked;
        }

        public void setLocked(boolean locked) {
            this.isLocked = locked;
        }

        public boolean isCompleted() {
            return isCompleted;
        }

        public void setCompleted(boolean completed) {
            this.isCompleted = completed;
        }

        @PreUpdate
        public void preUpdate() {
            this.updatedAt = LocalDateTime.now();
        }
    }
