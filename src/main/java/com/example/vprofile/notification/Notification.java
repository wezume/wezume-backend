package com.example.vprofile.notification;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long videoId; // Simplified to store just the video ID instead of the entire Video object

    private String likerName;

    private Long userId; // Simplified to store just the user ID instead of the entire User object

    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime timestamp;

    // Constructors, Getters, and Setters
    public Notification() {
    }

    public Notification(Long id, Long videoId, String likerName, Long userId, boolean isRead, LocalDateTime timestamp) {
        this.id = id;
        this.videoId = videoId;
        this.likerName = likerName;
        this.userId = userId;
        this.isRead = isRead;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getLikerName() {
        return likerName;
    }

    public void setLikerName(String likerName) {
        this.likerName = likerName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
