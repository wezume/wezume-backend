package com.example.vprofile.score;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class TotalScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long videoId;

    private double ClarityScore;
    private double ConfidenceScore;
    private double AuthenticityScore;
    private double EmotionalScore;
    private double totalScore;
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public TotalScore() {
    }

    public TotalScore(Long id, Long videoId, double ClarityScore, double ConfidenceScore,
            double AuthenticityScore,double EmotionalScore, double totalScore, LocalDateTime createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.ClarityScore = ClarityScore;
        this.ConfidenceScore = ConfidenceScore;
        this.AuthenticityScore = AuthenticityScore;
        this.EmotionalScore = EmotionalScore;
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

    public double getClarityScore() {
        return ClarityScore;
    }

    public void setClarityScore(double ClarityScore) {
        this.ClarityScore = ClarityScore;
    }

    public double getConfidenceScore() {
        return ConfidenceScore;
    }

    public void setConfidenceScore(double ConfidenceScore) {
        this.ConfidenceScore = ConfidenceScore;
    }

    public double getAuthenticityScore() {
        return AuthenticityScore;
    }

    public void setAuthenticityScore(double AuthenticityScore) {
        this.AuthenticityScore = AuthenticityScore;
    }
    public double getEmotionalScore() {
        return EmotionalScore;
    }

    public void setEmotionalScore(double EmotionalScore) {
        this.EmotionalScore = EmotionalScore;
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
