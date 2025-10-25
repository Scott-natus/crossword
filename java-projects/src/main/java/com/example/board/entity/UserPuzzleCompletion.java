package com.example.board.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 퍼즐 완료 기록 엔티티
 */
@Entity
@Table(name = "user_puzzle_completions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPuzzleCompletion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "user_id", length = 100)
    private String userId;
    
    @Column(name = "theme", nullable = false, length = 50)
    private String theme;
    
    @Column(name = "puzzle_date", nullable = false)
    private LocalDate puzzleDate;
    
    @Column(name = "completion_time", nullable = false)
    private Integer completionTime;
    
    @Column(name = "hints_used", nullable = false)
    private Integer hintsUsed = 0;
    
    @Column(name = "wrong_attempts", nullable = false)
    private Integer wrongAttempts = 0;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
    }
}
