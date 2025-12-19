package com.example.cdaxVideo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
// import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String description;
    
    @Column(name = "thumbnail_image")
     @JsonProperty("thumbnailUrl")
    private String thumbnailUrl;
    
    // NEW FIELDS
    @Transient
    private String formattedDuration;
    
    @Column(name = "short_description")
    private String shortDescription;
    
    private String instructor;
    
    @Column(name = "instructor_id")
    private String instructorId;
    
    @Column(name = "banner_image")
    private String bannerImage;
    
    private Double price;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(name = "discount_price")
    private Double discountPrice;
    
    private Double rating;
    
    @Column(name = "total_ratings")
    private Integer totalRatings;
    
    @Column(name = "enrolled_students")
    private Integer enrolledStudents;
    
    @Column(name = "total_duration")
    private Integer totalDuration; // minutes
    
    private String level;
    private String category;
    
    @Column(name = "sub_category")
    private String subCategory;
    
    @ElementCollection
    @CollectionTable(name = "course_tags", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();
    
    @JsonProperty("isPublished")
    @Column(name = "is_published")
    private Boolean isPublished;
    
    @JsonProperty("isFeatured")
    @Column(name = "is_featured")
    private Boolean isFeatured;
    
    @JsonProperty("isPopular")
    @Column(name = "is_popular")
    private Boolean isPopular;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Transient
private double progressPercentage;

@Transient  
private double progressPercent;
    
    // Existing relationships
    // CHANGE: Remove @JsonManagedReference, use @JsonIgnoreProperties
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"course", "videos.module"}) // Ignore course in modules AND module in videos
    private List<Module> modules = new ArrayList<>();
    
    @JsonProperty("isSubscribed")
    @Transient
    private boolean purchased;
    
    @Transient
    private int totalModules;

    // CHANGE: Add @JsonIgnore to prevent User → Course → User loop
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_course_purchase",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> subscribedUsers = new ArrayList<>();
    
    // Constructors
    public Course() {
        this.tags = new ArrayList<>();
        this.isPublished = false;
        this.isFeatured = false;
        this.isPopular = false;
        this.rating = 0.0;
        this.totalRatings = 0;
        this.enrolledStudents = 0;
        this.totalDuration = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.modules = new ArrayList<>();
    }
    
    public Course(String title, String description, String instructor) {
        this();
        this.title = title;
        this.description = description;
        this.instructor = instructor;
    }
    
    // Lifecycle callback to calculate formatted duration
    @PostLoad
    @PostPersist
    @PostUpdate
    public void calculateDerivedFields() {
        // Calculate formatted duration
        if (totalDuration != null) {
            int hours = totalDuration / 60;
            int minutes = totalDuration % 60;
            if (hours > 0) {
                this.formattedDuration = minutes > 0 ? hours + "h " + minutes + "m" : hours + "h";
            } else {
                this.formattedDuration = minutes + "m";
            }
        }
        
        // Calculate total modules
        this.totalModules = modules != null ? modules.size() : 0;
        
        // Auto-update updatedAt timestamp
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters for existing fields
    // Getter and Setter
    public List<User> getSubscribedUsers() {
        return subscribedUsers;
    }

    public void setSubscribedUsers(List<User> subscribedUsers) {
        this.subscribedUsers = subscribedUsers;
    }

    public double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(double progressPercentage) { 
    this.progressPercentage = progressPercentage;
    this.progressPercent = progressPercentage / 100.0;
}

    public double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(double progressPercent) { 
    this.progressPercent = progressPercent;
    this.progressPercentage = progressPercent * 100.0;
}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public List<Module> getModules() { return modules; }
    public void setModules(List<Module> modules) { 
        this.modules = modules; 
        this.totalModules = modules != null ? modules.size() : 0;
    }
    
    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }
    
    public int getTotalModules() { 
        return this.modules != null ? this.modules.size() : totalModules;
    }
    
    public void setTotalModules(int totalModules) { 
        this.totalModules = totalModules; 
    }
    
    // Getters and Setters for NEW fields
    public String getFormattedDuration() {
        if (formattedDuration == null && totalDuration != null) {
            calculateDerivedFields();
        }
        return formattedDuration;
    }
    
    public void setFormattedDuration(String formattedDuration) {
        this.formattedDuration = formattedDuration;
    }
    
    public String getShortDescription() {
        return shortDescription;
    }
    
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
    
    public String getInstructor() {
        return instructor;
    }
    
    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }
    
    public String getInstructorId() {
        return instructorId;
    }
    
    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }
    
    public String getBannerImage() {
        return bannerImage;
    }
    
    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }
    
    public Double getDiscountPrice() {
        return discountPrice;
    }
    
    public void setDiscountPrice(Double discountPrice) {
        this.discountPrice = discountPrice;
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public Integer getTotalRatings() {
        return totalRatings;
    }
    
    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }
    
    public Integer getEnrolledStudents() {
        return enrolledStudents;
    }
    
    public void setEnrolledStudents(Integer enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }
    
    public Integer getTotalDuration() {
        return totalDuration;
    }
    
    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
        // Recalculate formatted duration
        if (totalDuration != null) {
            calculateDerivedFields();
        }
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSubCategory() {
        return subCategory;
    }
    
    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }
    
    public Boolean getIsPublished() {
        return isPublished;
    }
    
    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }
    
    public Boolean getIsFeatured() {
        return isFeatured;
    }
    
    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }
    
    public Boolean getIsPopular() {
        return isPopular;
    }
    
    public void setIsPopular(Boolean isPopular) {
        this.isPopular = isPopular;
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
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    // Helper methods
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
    }
    
    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }
    
    public void addModule(Module module) {
        if (this.modules == null) {
            this.modules = new ArrayList<>();
        }
        module.setCourse(this);
        this.modules.add(module);
        this.totalModules = this.modules.size();
    }
    
    public void removeModule(Module module) {
        if (this.modules != null) {
            this.modules.remove(module);
            module.setCourse(null);
            this.totalModules = this.modules.size();
        }
    }
    
    // Utility methods for frontend compatibility
    @Transient
    @JsonProperty("hasDiscount")
    public boolean hasDiscount() {
        return discountPrice != null && discountPrice < price && price > 0;
    }
    
    @Transient
    @JsonProperty("discountPercentage")
    public Double getDiscountPercentage() {
        if (!hasDiscount()) return 0.0;
        return ((price - discountPrice) / price) * 100;
    }
    
    @Transient
    @JsonProperty("effectivePrice")
    public Double getEffectivePrice() {
        return hasDiscount() ? discountPrice : price;
    }
    
    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", instructor='" + instructor + '\'' +
                ", price=" + price +
                ", rating=" + rating +
                ", level='" + level + '\'' +
                '}';
    }
}