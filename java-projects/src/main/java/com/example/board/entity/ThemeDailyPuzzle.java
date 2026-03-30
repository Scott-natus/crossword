package com.example.board.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

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
    private Integer id;
    
    @Column(name = "theme", nullable = false, length = 50)
    private String theme;
    
    @Column(name = "puzzle_date", nullable = false)
    private LocalDate puzzleDate;
    
    @Column(name = "puzzle_id", nullable = false)
    private Integer puzzleId;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "puzzle_data", columnDefinition = "TEXT")
    private String puzzleData;
    
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
     * 퍼즐 데이터를 Map으로 반환
     * @return 퍼즐 데이터 Map
     */
    public Map<String, Object> getPuzzleDataAsMap() {
        if (puzzleData == null) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(puzzleData, Map.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 퍼즐 데이터를 Map에서 설정
     * @param puzzleDataMap 퍼즐 데이터 Map
     */
    public void setPuzzleDataFromMap(Map<String, Object> puzzleDataMap) {
        if (puzzleDataMap == null) {
            this.puzzleData = null;
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.puzzleData = mapper.writeValueAsString(puzzleDataMap);
        } catch (Exception e) {
            this.puzzleData = null;
        }
    }
}
