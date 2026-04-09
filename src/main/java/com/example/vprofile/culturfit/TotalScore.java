package com.example.vprofile.culturfit;

import jakarta.persistence.*;

@Entity(name = "CultureFitTotalScore")
@Table(name = "culture_fit_total_score")
public class TotalScore {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "video_id")
    private Long videoId;

    @Column(name = "emotional_score")
    private Double emotionalScore;

    @Column(name = "clarity_score")
    private Double clarityScore;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "authenticity_score")
    private Double authenticityScore;

    @Column(name = "total_score")
    private Double totalScore;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public Double getEmotionalScore() { return emotionalScore; }
    public void setEmotionalScore(Double emotionalScore) { this.emotionalScore = emotionalScore; }

    public Double getClarityScore() { return clarityScore; }
    public void setClarityScore(Double clarityScore) { this.clarityScore = clarityScore; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Double getAuthenticityScore() { return authenticityScore; }
    public void setAuthenticityScore(Double authenticityScore) { this.authenticityScore = authenticityScore; }

    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
}
