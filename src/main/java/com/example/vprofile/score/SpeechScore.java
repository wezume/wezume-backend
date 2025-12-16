package com.example.vprofile.score;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class SpeechScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long videoId;

    private double fillerWordScore;
    private double speechRateScore;
    private double sentenceStructureScore;
    private double articulationScore;
    private double pitchScore;
    private double energyScore;
    private double toneScore;
    private double emotionScore;

    private double totalScore;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public SpeechScore() {
    }

    public SpeechScore(Long id, Long videoId, double fillerWordScore, double speechRateScore,
            double sentenceStructureScore, double articulationScore, double totalScore, LocalDateTime createdAt, double pitchScore,
            double energyScore, double toneScore, double emotionScore) {
        this.id = id;
        this.videoId = videoId;
        this.fillerWordScore = fillerWordScore;
        this.speechRateScore = speechRateScore;
        this.sentenceStructureScore = sentenceStructureScore;
        this.articulationScore = articulationScore;
        this.totalScore = totalScore;
        this.createdAt = createdAt;
        this.pitchScore = pitchScore;
        this.energyScore = energyScore;
        this.toneScore = toneScore;
        this.emotionScore = emotionScore;
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

    public double getFillerWordScore() {
        return fillerWordScore;
    }

    public void setFillerWordScore(double fillerWordScore) {
        this.fillerWordScore = fillerWordScore;
    }

    public double getSpeechRateScore() {
        return speechRateScore;
    }

    public void setSpeechRateScore(double speechRateScore) {
        this.speechRateScore = speechRateScore;
    }

    public double getSentenceStructureScore() {
        return sentenceStructureScore;
    }

    public void setSentenceStructureScore(double sentenceStructureScore) {
        this.sentenceStructureScore = sentenceStructureScore;
    }

    public double getArticulationScore() {
        return articulationScore;
    }

    public void setArticulationScore(double articulationScore) {
        this.articulationScore = articulationScore;
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

    public double getPitchScore() {
        return pitchScore;
    }
    public void setPitchScore(double pitchScore) {
        this.pitchScore = pitchScore;
    }
    public double getEnergyScore() {
        return energyScore;
    }
    public void setEnergyScore(double energyScore) {
        this.energyScore = energyScore;
    }
    public double getToneScore() {
        return toneScore;
    }
    public void setToneScore(double toneScore) {
        this.toneScore = toneScore;
    }
    public double getEmotionScore() {
        return emotionScore;
    }
    public void setEmotionScore(double emotionScore) {
        this.emotionScore = emotionScore;
    }
}
