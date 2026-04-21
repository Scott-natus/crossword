package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * 퍼즐 그리드 템플릿 엔티티
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-23
 */
@Entity
@Table(name = "puzzle_grid_templates", schema = "public")
@Getter
@Setter
public class PzGridTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "level_id")
    private Integer levelId;
    
    @Column(name = "template_name")
    private String templateName;
    
    @Column(name = "grid_width")
    private Integer gridWidth;
    
    @Column(name = "grid_height")
    private Integer gridHeight;
    
    @Column(name = "word_count")
    private Integer wordCount;
    
    @Column(name = "intersection_count")
    private Integer intersectionCount;
    
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grid_pattern")
    private String gridPattern; // JSON 형태로 그리드 패턴 저장
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "word_positions")
    private String wordPositions; // JSON 형태로 단어 위치 정보 저장
    
    @Column(name = "category")
    private String category = "general";
    
    @Column(name = "difficulty_rating")
    private Integer difficultyRating;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_words_snapshot", columnDefinition = "TEXT")
    private String extractedWordsSnapshot;

    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
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
    
    /**
     * 그리드 크기를 문자열로 반환 (예: "5×5")
     */
    public String getGridSize() {
        return gridWidth + "×" + gridHeight;
    }
    
    /**
     * 레벨 이름을 반환 (예: "레벨 1 실마리 발견자 (Clue Spotter)")
     */
    public String getLevelName() {
        if (levelId == null) return "미지정";
        
        String levelType;
        if (levelId <= 10) {
            levelType = "실마리 발견자 (Clue Spotter)";
        } else if (levelId <= 20) {
            levelType = "단서 수집가 (Clue Collector)";
        } else if (levelId <= 30) {
            levelType = "퍼즐 마스터 (Puzzle Master)";
        } else {
            levelType = "크로스워드 전문가 (Crossword Expert)";
        }
        
        return "레벨 " + levelId + " " + levelType;
    }
}
