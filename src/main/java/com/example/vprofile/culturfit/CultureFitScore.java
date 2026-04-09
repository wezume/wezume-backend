package com.example.vprofile.culturfit;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "culture_fit_scores")
public class CultureFitScore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "candidate_id", nullable = false)
    private Integer candidateId;
    
    @Column(name = "evaluator_name")
    private String evaluatorName;
    
    @Column(name = "communication_score")
    private Integer communicationScore;
    
    @Column(name = "teamwork_score")
    private Integer teamworkScore;
    
    @Column(name = "adaptability_score")
    private Integer adaptabilityScore;
    
    @Column(name = "values_alignment_score")
    private Integer valuesAlignmentScore;
    
    @Column(name = "overall_score")
    private Double overallScore;
    
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;
    
    @Column(name = "decision", length = 10)
    private String decision;
    
    @Column(name = "evaluated_on")
    private LocalDate evaluatedOn;
    
    // Transient fields for normalized scores (not persisted in database)
    @Transient
    private Double normalizedCommunicationScore;
    
    @Transient
    private Double normalizedTeamworkScore;
    
    @Transient
    private Double normalizedAdaptabilityScore;
    
    @Transient
    private Double normalizedValuesAlignmentScore;
    
    @Transient
    private Double normalizedOverallScore;
    
    // Constructors
    public CultureFitScore() {}
    
    public CultureFitScore(Integer candidateId, String evaluatorName, Integer communicationScore, 
                          Integer teamworkScore, Integer adaptabilityScore, Integer valuesAlignmentScore, 
                          String feedback, String decision) {
        this.candidateId = candidateId;
        this.evaluatorName = evaluatorName;
        this.communicationScore = communicationScore;
        this.teamworkScore = teamworkScore;
        this.adaptabilityScore = adaptabilityScore;
        this.valuesAlignmentScore = valuesAlignmentScore;
        this.feedback = feedback;
        this.decision = decision;
        this.calculateOverallScore();
        this.evaluatedOn = LocalDate.now();
    }
    
    // Business logic
    public void calculateOverallScore() {
        if (communicationScore != null && teamworkScore != null && 
            adaptabilityScore != null && valuesAlignmentScore != null) {
            this.overallScore = (communicationScore + teamworkScore + adaptabilityScore + valuesAlignmentScore) / 4.0;
        }
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getCandidateId() { return candidateId; }
    public void setCandidateId(Integer candidateId) { this.candidateId = candidateId; }
    
    public String getEvaluatorName() { return evaluatorName; }
    public void setEvaluatorName(String evaluatorName) { this.evaluatorName = evaluatorName; }
    
    public Integer getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(Integer communicationScore) { this.communicationScore = communicationScore; }
    
    public Integer getTeamworkScore() { return teamworkScore; }
    public void setTeamworkScore(Integer teamworkScore) { this.teamworkScore = teamworkScore; }
    
    public Integer getAdaptabilityScore() { return adaptabilityScore; }
    public void setAdaptabilityScore(Integer adaptabilityScore) { this.adaptabilityScore = adaptabilityScore; }
    
    public Integer getValuesAlignmentScore() { return valuesAlignmentScore; }
    public void setValuesAlignmentScore(Integer valuesAlignmentScore) { this.valuesAlignmentScore = valuesAlignmentScore; }
    
    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }
    
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    
    public LocalDate getEvaluatedOn() { return evaluatedOn; }
    public void setEvaluatedOn(LocalDate evaluatedOn) { this.evaluatedOn = evaluatedOn; }
    
    public Double getNormalizedCommunicationScore() { return normalizedCommunicationScore; }
    public void setNormalizedCommunicationScore(Double normalizedCommunicationScore) { 
        this.normalizedCommunicationScore = normalizedCommunicationScore; 
    }
    
    public Double getNormalizedTeamworkScore() { return normalizedTeamworkScore; }
    public void setNormalizedTeamworkScore(Double normalizedTeamworkScore) { 
        this.normalizedTeamworkScore = normalizedTeamworkScore; 
    }
    
    public Double getNormalizedAdaptabilityScore() { return normalizedAdaptabilityScore; }
    public void setNormalizedAdaptabilityScore(Double normalizedAdaptabilityScore) { 
        this.normalizedAdaptabilityScore = normalizedAdaptabilityScore; 
    }
    
    public Double getNormalizedValuesAlignmentScore() { return normalizedValuesAlignmentScore; }
    public void setNormalizedValuesAlignmentScore(Double normalizedValuesAlignmentScore) { 
        this.normalizedValuesAlignmentScore = normalizedValuesAlignmentScore; 
    }
    
    public Double getNormalizedOverallScore() { return normalizedOverallScore; }
    public void setNormalizedOverallScore(Double normalizedOverallScore) { 
        this.normalizedOverallScore = normalizedOverallScore; 
    }
    
    @Override
    public String toString() {
        return "CultureFitScore{" +
                "id=" + id +
                ", candidateId=" + candidateId +
                ", overallScore=" + overallScore +
                ", decision='" + decision + '\'' +
                '}';
    }
}
