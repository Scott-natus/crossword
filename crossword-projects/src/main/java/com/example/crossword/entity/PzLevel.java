package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 퍼즐 레벨 엔티티
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-23
 */
@Entity
@Table(name = "puzzle_levels", schema = "public")
@Getter
@Setter
public class PzLevel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "level", nullable = false, unique = true)
    private Integer level;
    
    @Column(name = "level_name", nullable = false)
    private String levelName;
    
    @Column(name = "word_difficulty")
    private Integer wordDifficulty;
    
    @Column(name = "hint_difficulty")
    private Integer hintDifficulty;
    
    @Column(name = "intersection_count")
    private Integer intersectionCount;
    
    @Column(name = "time_limit")
    private Integer timeLimit;
    
    @Column(name = "clear_condition")
    private String clearCondition;
    
    @Column(name = "word_count")
    private Integer wordCount;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
