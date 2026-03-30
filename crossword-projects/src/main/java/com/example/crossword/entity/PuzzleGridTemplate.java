package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * 퍼즐 그리드 템플릿 엔티티
 * Laravel의 puzzle_grid_templates 테이블과 매핑
 */
@Entity
@Table(name = "puzzle_grid_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleGridTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "level_id", nullable = false)
    private Integer levelId;
    
    @Column(name = "template_name", nullable = false)
    private String templateName;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grid_pattern", nullable = false)
    private List<List<Integer>> gridPattern;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "word_positions", nullable = false)
    private List<Map<String, Object>> wordPositions;
    
    @Column(name = "grid_width", nullable = false)
    private Integer gridWidth;
    
    @Column(name = "grid_height", nullable = false)
    private Integer gridHeight;
    
    @Column(name = "difficulty_rating", nullable = false)
    private Integer difficultyRating;
    
    @Column(name = "word_count", nullable = false)
    private Integer wordCount;
    
    @Column(name = "intersection_count", nullable = false)
    private Integer intersectionCount;
    
    @Column(name = "category", nullable = false)
    private String category = "general";
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
