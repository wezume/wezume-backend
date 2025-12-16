package com.example.vprofile.videofolder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;

@Entity
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileName;
    private String filePath;
    private String jobId;
    private String college;
    private String roleCode;
    private String audioFilePath;
    @Column(columnDefinition = "LONGTEXT")
    private String embeddingVector;
    private Long userId;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Transient
    private Integer confidence;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    private String url;
    @Column(name = "transcription", columnDefinition = "LONGTEXT")
    private String transcription; // Add transcription field
    private String thumbnailurl;// Field to store video as byte array

    public Video() {
    }

    // Add this constructor to match the parameters being passed in the service
    public Video(String fileName, String thumbnailurl, Long userId, String transcription, String audioFilePath,
            String url, String jobid, String college, String roleCode, String embeddingVector) {
        this.fileName = fileName;
        this.thumbnailurl = thumbnailurl;
        this.userId = userId;
        this.college = college;
        this.roleCode = roleCode;
        this.jobId = jobid;
        this.embeddingVector = embeddingVector;
        this.transcription = transcription;
        this.audioFilePath = audioFilePath;
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailurl;
    }

    public void setThumbnailUrl(String thumbnailurl) {
        this.thumbnailurl = thumbnailurl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTranscription() {
        return transcription; // Getter for transcription
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription; // Setter for transcription
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(String embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }
}
