package com.example.cdaxVideo.DTO;

import java.time.LocalDateTime;

public class CartItemDTO {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private String thumbnailUrl;
    private Double price; // Current price (after discount if any)
    private Integer duration; // in minutes
    private LocalDateTime addedAt;
    
    // NEW DISCOUNT FIELDS
    private boolean hasDiscount;
    private Double originalPrice;
    private Double discountedPrice;
    private Double discountPercentage;
    
    // Constructors
    public CartItemDTO() {}
    
    public CartItemDTO(Long id, Long courseId, String courseTitle, String thumbnailUrl, 
                      Double price, Integer duration, LocalDateTime addedAt) {
        this.id = id;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.thumbnailUrl = thumbnailUrl;
        this.price = price;
        this.duration = duration;
        this.addedAt = addedAt;
        this.hasDiscount = false;
        this.originalPrice = price;
        this.discountedPrice = price;
        this.discountPercentage = 0.0;
    }
    
    // Getters and Setters for existing fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
    
    // NEW: Getters and Setters for discount fields
    public boolean isHasDiscount() { return hasDiscount; }
    public void setHasDiscount(boolean hasDiscount) { this.hasDiscount = hasDiscount; }
    
    public Double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Double originalPrice) { this.originalPrice = originalPrice; }
    
    public Double getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(Double discountedPrice) { this.discountedPrice = discountedPrice; }
    
    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }
    
    // Helper method to calculate discount
    public void calculateDiscount(Double original, Double discounted) {
        if (original != null && discounted != null && original > 0 && discounted < original) {
            this.hasDiscount = true;
            this.originalPrice = original;
            this.discountedPrice = discounted;
            this.price = discounted; // Set current price to discounted price
            this.discountPercentage = ((original - discounted) / original) * 100;
        } else {
            this.hasDiscount = false;
            this.originalPrice = original;
            this.discountedPrice = original;
            this.price = original;
            this.discountPercentage = 0.0;
        }
    }
}