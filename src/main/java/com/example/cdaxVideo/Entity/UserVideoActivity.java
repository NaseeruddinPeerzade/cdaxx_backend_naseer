package com.example.cdaxVideo.Entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_video_activity", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "date"})
})
public class UserVideoActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // store email to avoid joining user table every time; still keep userId if needed
    @Column(nullable = false)
    private String email;

    // optional user id for faster querying by user id
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer videosWatched = 0;

    public UserVideoActivity() {}

    public UserVideoActivity(String email, Long userId, LocalDate date, Integer videosWatched) {
        this.email = email;
        this.userId = userId;
        this.date = date;
        this.videosWatched = videosWatched;
    }

    public void increment() {
        if (this.videosWatched == null) this.videosWatched = 0;
        this.videosWatched++;
    }

    // getters & setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getVideosWatched() { return videosWatched; }
    public void setVideosWatched(Integer videosWatched) { this.videosWatched = videosWatched; }
}
