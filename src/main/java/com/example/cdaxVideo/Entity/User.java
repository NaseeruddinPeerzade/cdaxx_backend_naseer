package com.example.cdaxVideo.Entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String confirmPassword;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String address;

    @Column(nullable = false)
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    @JsonProperty("isActive")
    private boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    @JsonProperty("isEmailVerified")
    private boolean isEmailVerified = false;

    @Column(name = "is_new_user", nullable = false)
    @JsonProperty("isNewUser")
    private Integer isNewUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Embedded
    private UserPreferences preferences;

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.preferences = new UserPreferences();
        this.role = "USER";
        this.isActive = true;
        this.isEmailVerified = false;
        this.isNewUser = 1;
    }

    public User(String firstName, String lastName, String email, String password) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @ManyToMany(mappedBy = "subscribedUsers")
    private List<Course> subscribedCourses = new ArrayList<>();

    public List<Course> getSubscribedCourses() {
        return subscribedCourses;
    }

    public void setSubscribedCourses(List<Course> subscribedCourses) {
        this.subscribedCourses = subscribedCourses;
    }


    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public void setIsEmailVerified(boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
    }

public Integer getIsNewUser() {
    return isNewUser;
}

// Boolean helper if you want
@Transient
public boolean isNewUserFlag() {
    return isNewUser != null && isNewUser == 1;
}

// Setter
public void setIsNewUser(Integer isNewUser) {
    this.isNewUser = isNewUser;
}

// Optional: set using boolean
public void setIsNewUserFlag(boolean isNewUser) {
    this.isNewUser = isNewUser ? 1 : 0;
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

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferences preferences) {
        this.preferences = preferences;
    }

    // Helper methods
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Transient
    public String getDisplayName() {
        String fullName = getFullName().trim();
        return !fullName.isEmpty() ? fullName : email;
    }

    // UserPreferences Embedded Class
    @Embeddable
    public static class UserPreferences {
        
        @Column(name = "notifications_enabled")
        private boolean notificationsEnabled = true;
        
        @Column(name = "email_notifications")
        private boolean emailNotifications = true;
        
        @Column(name = "push_notifications")
        private boolean pushNotifications = true;
        
        @Column(name = "theme")
        private String theme = "system";
        
        @Column(name = "language")
        private String language = "en";
        
        @Column(name = "analytics_enabled")
        private boolean analyticsEnabled = false;

        // Constructors
        public UserPreferences() {
            // Default values already set in field declarations
        }

        // Getters and Setters
        public boolean isNotificationsEnabled() {
            return notificationsEnabled;
        }

        public void setNotificationsEnabled(boolean notificationsEnabled) {
            this.notificationsEnabled = notificationsEnabled;
        }

        public boolean isEmailNotifications() {
            return emailNotifications;
        }

        public void setEmailNotifications(boolean emailNotifications) {
            this.emailNotifications = emailNotifications;
        }

        public boolean isPushNotifications() {
            return pushNotifications;
        }

        public void setPushNotifications(boolean pushNotifications) {
            this.pushNotifications = pushNotifications;
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public boolean isAnalyticsEnabled() {
            return analyticsEnabled;
        }

        public void setAnalyticsEnabled(boolean analyticsEnabled) {
            this.analyticsEnabled = analyticsEnabled;
        }

        @Override
        public String toString() {
            return "UserPreferences{" +
                    "notificationsEnabled=" + notificationsEnabled +
                    ", emailNotifications=" + emailNotifications +
                    ", pushNotifications=" + pushNotifications +
                    ", theme='" + theme + '\'' +
                    ", language='" + language + '\'' +
                    ", analyticsEnabled=" + analyticsEnabled +
                    '}';
        }
    }

    // Business methods
    public void markAsVerified() {
        this.isEmailVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateToReturningUser() {
        this.isNewUser = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivateAccount() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activateAccount() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    public boolean isInstructor() {
        return "INSTRUCTOR".equalsIgnoreCase(this.role);
    }

    public boolean isStudent() {
        return "STUDENT".equalsIgnoreCase(this.role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", isNewUser=" + isNewUser +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}