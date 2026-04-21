package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 테마별 일일 퍼즐 엔티티
 */
@Entity
@Table(name = "theme_daily_puzzles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeDailyPuzzle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "theme", nullable = false, length = 50)
    private String theme;
    
    @Column(name = "puzzle_date", nullable = false)
    private LocalDate puzzleDate;
    
    @Column(name = "puzzle_id", nullable = false)
    private Integer puzzleId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "puzzle_data", columnDefinition = "TEXT")
    private String puzzleData;

    @Column(name = "is_active", nullable = false)
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
}
