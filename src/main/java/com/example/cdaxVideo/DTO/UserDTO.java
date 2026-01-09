package com.example.cdaxVideo.DTO;

import com.example.cdaxVideo.Entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    
    private String profileImage;
    private String role;
    
    @JsonProperty("isNewUser")
    private Boolean isNewUser;
    
    @JsonProperty("isActive")
    private Boolean isActive;
    
    @JsonProperty("isEmailVerified")
    private Boolean isEmailVerified;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private Integer enrolledCoursesCount = 0;
    private Boolean subscribed = false;

    // Constructors
    public UserDTO() {
    }

    // Constructor that accepts a User entity
    public UserDTO(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.address = user.getAddress();
        this.dateOfBirth = user.getDateOfBirth();
        this.profileImage = user.getProfileImage();
        this.role = user.getRole();
        this.isNewUser = user.getIsNewUser() != null && user.getIsNewUser() == 1;
        this.isActive = user.getIsActive();
        this.isEmailVerified = user.getIsEmailVerified();
        this.createdAt = user.getCreatedAt();
    }

    // Builder pattern constructor for updates
    public UserDTO(User user, Integer enrolledCoursesCount, Boolean subscribed) {
        this(user);
        this.enrolledCoursesCount = enrolledCoursesCount;
        this.subscribed = subscribed;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIsNewUser() {
        return isNewUser;
    }

    public void setIsNewUser(Boolean isNewUser) {
        this.isNewUser = isNewUser;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public void setIsEmailVerified(Boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getEnrolledCoursesCount() {
        return enrolledCoursesCount;
    }

    public void setEnrolledCoursesCount(Integer enrolledCoursesCount) {
        this.enrolledCoursesCount = enrolledCoursesCount;
    }

    public Boolean getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(Boolean subscribed) {
        this.subscribed = subscribed;
    }

    // Helper methods
    public String getFullName() {
        return (firstName != null ? firstName : "") + 
               (lastName != null ? " " + lastName : "");
    }

    // Builder pattern methods
    public UserDTO withEnrolledCoursesCount(Integer count) {
        this.enrolledCoursesCount = count;
        return this;
    }

    public UserDTO withSubscribed(Boolean subscribed) {
        this.subscribed = subscribed;
        return this;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", role='" + role + '\'' +
                ", isNewUser=" + isNewUser +
                '}';
    }
}