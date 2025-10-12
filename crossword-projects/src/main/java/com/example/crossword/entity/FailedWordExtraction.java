package com.example.crossword.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "failed_word_extractions")
public class FailedWordExtraction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "template_id")
    private Integer templateId;
    
    @Column(name = "level")
    private Integer level;
    
    @Column(name = "word_difficulty")
    private Integer wordDifficulty;
    
    @Column(name = "hint_difficulty")
    private Integer hintDifficulty;
    
    @Column(name = "intersection_count")
    private Integer intersectionCount;
    
    @Column(name = "failed_word_id")
    private Integer failedWordId;
    
    @Column(name = "failed_word_position", columnDefinition = "jsonb")
    private String failedWordPosition;
    
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
    
    @Column(name = "confirmed_words", columnDefinition = "jsonb")
    private String confirmedWords;
    
    @Column(name = "intersection_requirements", columnDefinition = "jsonb")
    private String intersectionRequirements;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 기본 생성자
    public FailedWordExtraction() {
        this.createdAt = LocalDateTime.now();
    }
    
    // 생성자
    public FailedWordExtraction(Integer templateId, Integer level, Integer wordDifficulty, 
                               Integer hintDifficulty, Integer intersectionCount,
                               Integer failedWordId, Map<String, Object> failedWordPosition,
                               String failureReason, Map<Integer, String> confirmedWords,
                               Object intersectionRequirements, Integer retryCount) {
        this();
        this.templateId = templateId;
        this.level = level;
        this.wordDifficulty = wordDifficulty;
        this.hintDifficulty = hintDifficulty;
        this.intersectionCount = intersectionCount;
        this.failedWordId = failedWordId;
        this.failedWordPosition = failedWordPosition != null ? failedWordPosition.toString() : null;
        this.failureReason = failureReason;
        this.confirmedWords = confirmedWords != null ? confirmedWords.toString() : null;
        this.intersectionRequirements = intersectionRequirements != null ? intersectionRequirements.toString() : null;
        this.retryCount = retryCount;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public Integer getWordDifficulty() {
        return wordDifficulty;
    }
    
    public void setWordDifficulty(Integer wordDifficulty) {
        this.wordDifficulty = wordDifficulty;
    }
    
    public Integer getHintDifficulty() {
        return hintDifficulty;
    }
    
    public void setHintDifficulty(Integer hintDifficulty) {
        this.hintDifficulty = hintDifficulty;
    }
    
    public Integer getIntersectionCount() {
        return intersectionCount;
    }
    
    public void setIntersectionCount(Integer intersectionCount) {
        this.intersectionCount = intersectionCount;
    }
    
    public Integer getFailedWordId() {
        return failedWordId;
    }
    
    public void setFailedWordId(Integer failedWordId) {
        this.failedWordId = failedWordId;
    }
    
    public String getFailedWordPosition() {
        return failedWordPosition;
    }
    
    public void setFailedWordPosition(String failedWordPosition) {
        this.failedWordPosition = failedWordPosition;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public String getConfirmedWords() {
        return confirmedWords;
    }
    
    public void setConfirmedWords(String confirmedWords) {
        this.confirmedWords = confirmedWords;
    }
    
    public String getIntersectionRequirements() {
        return intersectionRequirements;
    }
    
    public void setIntersectionRequirements(String intersectionRequirements) {
        this.intersectionRequirements = intersectionRequirements;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "FailedWordExtraction{" +
                "id=" + id +
                ", templateId=" + templateId +
                ", level=" + level +
                ", wordDifficulty=" + wordDifficulty +
                ", hintDifficulty=" + hintDifficulty +
                ", intersectionCount=" + intersectionCount +
                ", failedWordId=" + failedWordId +
                ", failureReason='" + failureReason + '\'' +
                ", retryCount=" + retryCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
