package com.example.cdaxVideo.DTO;

import com.example.cdaxVideo.Entity.User;

public class UserDTO {

    private Long id;
    private String firstName;
    private String role;
    private Boolean isNewUser;

    // Constructor that accepts a User entity
    public UserDTO(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.role = user.getRole();
        this.isNewUser = user.getIsNewUser() != null && user.getIsNewUser() == 1;
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
}
