package com.example.cdaxVideo.Entity;


import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "user_course_purchase")
public class UserCoursePurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")  // ← ADD THIS
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", referencedColumnName = "id")  // ← ADD THIS
    private Course course;

    private Date purchasedOn = new Date();

    // ---------------- GETTERS & SETTERS ----------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Date getPurchasedOn() {
        return purchasedOn;
    }

    public void setPurchasedOn(Date purchasedOn) {
        this.purchasedOn = purchasedOn;
    }
}
