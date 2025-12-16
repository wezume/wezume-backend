package com.example.vprofile.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profanity")
public class Profanity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "contains_profanity", nullable = false)
    private Boolean containsProfanity;

    // Constructors
    public Profanity() {
    }

    public Profanity(Long userId, Long videoId, Boolean containsProfanity) {
        this.userId = userId;
        this.videoId = videoId;
        this.containsProfanity = containsProfanity;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Boolean getContainsProfanity() {
        return containsProfanity;
    }

    public void setContainsProfanity(Boolean containsProfanity) {
        this.containsProfanity = containsProfanity;
    }
}

