package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 퍼즐 게임 기록을 저장하는 Entity
 * Laravel의 puzzle_game_records 테이블과 매핑
 */
@Entity
@Table(name = "puzzle_game_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleGameRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "level_played", nullable = false)
    private Integer levelPlayed;
    
    @Column(name = "game_status", nullable = false, length = 20)
    private String gameStatus; // 'completed', 'failed', 'abandoned'
    
    @Column(name = "score", nullable = false)
    @Builder.Default
    private Integer score = 0;
    
    @Column(name = "play_time", nullable = false)
    @Builder.Default
    private Integer playTime = 0;
    
    @Column(name = "hints_used", nullable = false)
    @Builder.Default
    private Integer hintsUsed = 0;
    
    @Column(name = "words_found", nullable = false)
    @Builder.Default
    private Integer wordsFound = 0;
    
    @Column(name = "total_words", nullable = false)
    @Builder.Default
    private Integer totalWords = 0;
    
    @Column(name = "accuracy", nullable = false)
    @Builder.Default
    private Double accuracy = 0.0;
    
    @Column(name = "level_before", nullable = false)
    private Integer levelBefore;
    
    @Column(name = "level_after", nullable = false)
    private Integer levelAfter;
    
    @Column(name = "level_up", nullable = false)
    @Builder.Default
    private Boolean levelUp = false;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "game_data", columnDefinition = "jsonb")
    private String gameData; // JSON 형태로 저장
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
