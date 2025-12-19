package com.example.cdaxVideo.DTO;

import com.example.cdaxVideo.Entity.Course;

public class CourseDTO {

    private Long id;
    private String title;
    private String description;
    private String thumbnailImage;

    public CourseDTO() {}

    // Constructor to map from Course entity
    public CourseDTO(Course course) {
        this.id = course.getId();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.thumbnailImage = course.getThumbnailUrl();
    }

    // Getters and setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailImage() {
        return thumbnailImage;
    }

    public void setThumbnailImage(String thumbnailImage) {
        this.thumbnailImage = thumbnailImage;
    }
}
