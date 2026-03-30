package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 게임 세션 정보를 저장하는 Entity
 * Laravel의 game_sessions 테이블과 매핑
 */
@Entity
@Table(name = "game_sessions",
       indexes = {
           @Index(name = "idx_game_sessions_created_at", columnList = "created_at"),
           @Index(name = "idx_game_sessions_user_id", columnList = "user_id"),
           @Index(name = "idx_game_sessions_word_id", columnList = "word_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;
    
    @Column(name = "session_started_at", nullable = false)
    @Builder.Default
    private LocalDateTime sessionStartedAt = LocalDateTime.now();
    
    @Column(name = "session_ended_at")
    private LocalDateTime sessionEndedAt;
    
    @Column(name = "total_play_time")
    @Builder.Default
    private Integer totalPlayTime = 0;
    
    @Column(name = "accuracy_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal accuracyRate = BigDecimal.ZERO;
    
    @Column(name = "total_correct_answers")
    @Builder.Default
    private Integer totalCorrectAnswers = 0;
    
    @Column(name = "total_wrong_answers")
    @Builder.Default
    private Integer totalWrongAnswers = 0;
    
    @Column(name = "hints_used_count")
    @Builder.Default
    private Integer hintsUsedCount = 0;
    
    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // 연관관계 매핑
    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GamePlayRecord> gamePlayRecords;
    
    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HintUsageRecord> hintUsageRecords;
    
    /**
     * 게임 세션이 완료되었는지 확인
     * @return 완료 여부
     */
    public boolean isGameCompleted() {
        return isCompleted != null && isCompleted;
    }
    
    /**
     * 게임 세션을 완료 처리
     */
    public void completeGame() {
        this.isCompleted = true;
        this.sessionEndedAt = LocalDateTime.now();
        calculateAccuracyRate();
    }
    
    /**
     * 정확도를 계산하여 업데이트
     */
    public void calculateAccuracyRate() {
        int totalAnswers = totalCorrectAnswers + totalWrongAnswers;
        if (totalAnswers > 0) {
            this.accuracyRate = BigDecimal.valueOf((double) totalCorrectAnswers / totalAnswers * 100)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.accuracyRate = BigDecimal.ZERO;
        }
    }
    
    /**
     * 정답을 추가
     */
    public void addCorrectAnswer() {
        this.totalCorrectAnswers++;
        calculateAccuracyRate();
    }
    
    /**
     * 오답을 추가
     */
    public void addWrongAnswer() {
        this.totalWrongAnswers++;
        calculateAccuracyRate();
    }
    
    /**
     * 힌트 사용을 추가
     */
    public void addHintUsed() {
        this.hintsUsedCount++;
    }
    
    /**
     * 플레이 시간을 업데이트
     * @param playTimeSeconds 플레이 시간 (초)
     */
    public void updatePlayTime(int playTimeSeconds) {
        this.totalPlayTime = playTimeSeconds;
    }
    
    /**
     * 게임 세션의 지속 시간을 계산
     * @return 지속 시간 (초)
     */
    public long getSessionDuration() {
        if (sessionEndedAt != null) {
            return java.time.Duration.between(sessionStartedAt, sessionEndedAt).getSeconds();
        } else {
            return java.time.Duration.between(sessionStartedAt, LocalDateTime.now()).getSeconds();
        }
    }
    
    /**
     * 정확도를 퍼센트 문자열로 반환
     * @return 정확도 문자열
     */
    public String getAccuracyRateFormatted() {
        return String.format("%.2f%%", accuracyRate);
    }
}
