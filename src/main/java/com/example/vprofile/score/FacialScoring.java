package com.example.vprofile.score;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class FacialScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long videoId;

    private double smileScore;
    private double eyeContactScore;
    private double straightFaceScore;
    private double totalScore;
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public FacialScoring() {
    }

    public FacialScoring(Long id, Long videoId, double smileScore, double eyeContactScore,
            double straightFaceScore,double totalScore, LocalDateTime createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.smileScore = smileScore;
        this.eyeContactScore = eyeContactScore;
        this.straightFaceScore = straightFaceScore;
        this.totalScore = totalScore;
        this.createdAt = createdAt;
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

    public double getSmileScore() {
        return smileScore;
    }

    public void setSmileScore(double smileScore) {
        this.smileScore = smileScore;
    }

    public double getEyeContactScore() {
        return eyeContactScore;
    }

    public void setEyeContactScore(double eyeContactScore) {
        this.eyeContactScore = eyeContactScore;
    }

    public double getStraightFaceScore() {
        return straightFaceScore;
    }

    public void setStraightFaceScore(double straightFaceScore) {
        this.straightFaceScore = straightFaceScore;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
