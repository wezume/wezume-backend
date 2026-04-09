package com.example.vprofile.culturfit;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "score_statistics")
public class ScoreStatistics {
    
    @Id
    @Column(name = "score_type")
    private String scoreType;
    
    @Column(name = "mean_value", nullable = false)
    private Double meanValue;
    
    @Column(name = "std_dev_value", nullable = false)
    private Double stdDevValue;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    // Constructors
    public ScoreStatistics() {}
    
    public ScoreStatistics(String scoreType, Double meanValue, Double stdDevValue) {
        this.scoreType = scoreType;
        this.meanValue = meanValue;
        this.stdDevValue = stdDevValue;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getScoreType() { return scoreType; }
    public void setScoreType(String scoreType) { this.scoreType = scoreType; }
    
    public Double getMeanValue() { return meanValue; }
    public void setMeanValue(Double meanValue) { this.meanValue = meanValue; }
    
    public Double getStdDevValue() { return stdDevValue; }
    public void setStdDevValue(Double stdDevValue) { this.stdDevValue = stdDevValue; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    @Override
    public String toString() {
        return "ScoreStatistics{" +
                "scoreType='" + scoreType + '\'' +
                ", meanValue=" + meanValue +
                ", stdDevValue=" + stdDevValue +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
