package com.example.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 사용자 퍼즐 게임 정보를 저장하는 Entity
 * Laravel의 user_puzzle_games 테이블과 매핑
 */
@Entity
@Table(name = "user_puzzle_games",
       indexes = {
           @Index(name = "idx_user_puzzle_games_user_id_active", columnList = "user_id, is_active"),
           @Index(name = "idx_user_puzzle_games_ranking", columnList = "ranking"),
           @Index(name = "idx_user_puzzle_games_guest_id", columnList = "guest_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPuzzleGame {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "current_level", nullable = false)
    @Builder.Default
    private Integer currentLevel = 1;
    
    @Column(name = "first_attempt_at")
    private LocalDateTime firstAttemptAt;
    
    @Column(name = "total_play_time", nullable = false)
    @Builder.Default
    private Integer totalPlayTime = 0;
    
    @Column(name = "accuracy_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal accuracyRate = BigDecimal.ZERO;
    
    @Column(name = "total_correct_answers", nullable = false)
    @Builder.Default
    private Integer totalCorrectAnswers = 0;
    
    @Column(name = "total_wrong_answers", nullable = false)
    @Builder.Default
    private Integer totalWrongAnswers = 0;
    
    @Column(name = "current_level_correct_answers", nullable = false)
    @Builder.Default
    private Integer currentLevelCorrectAnswers = 0;
    
    @Column(name = "current_level_wrong_answers", nullable = false)
    @Builder.Default
    private Integer currentLevelWrongAnswers = 0;
    
    @Column(name = "ranking", nullable = false)
    @Builder.Default
    private Integer ranking = 0;
    
    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "guest_id", columnDefinition = "uuid")
    private UUID guestId;
    
    // 현재 퍼즐 세션 정보
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_puzzle_data")
    private String currentPuzzleData;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_game_state")
    private String currentGameState;
    
    @Column(name = "current_puzzle_started_at")
    private LocalDateTime currentPuzzleStartedAt;
    
    @Column(name = "has_active_puzzle", nullable = false)
    @Builder.Default
    private Boolean hasActivePuzzle = false;
    
    @Column(name = "theme")
    private String theme;
    
    @CreationTimestamp
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * 활성 퍼즐이 있는지 확인
     * @return 활성 퍼즐 존재 여부
     */
    public boolean hasActivePuzzle() {
        return hasActivePuzzle != null && hasActivePuzzle && currentPuzzleData != null;
    }
    
    /**
     * 현재 퍼즐 데이터를 Map으로 반환
     * @return 퍼즐 데이터 Map
     */
    public Map<String, Object> getCurrentPuzzleData() {
        if (currentPuzzleData == null) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(currentPuzzleData, Map.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 현재 퍼즐 데이터를 설정
     * @param puzzleData 퍼즐 데이터 Map
     */
    public void setCurrentPuzzleData(Map<String, Object> puzzleData) {
        if (puzzleData == null) {
            this.currentPuzzleData = null;
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.currentPuzzleData = mapper.writeValueAsString(puzzleData);
            System.out.println("DEBUG: currentPuzzleData 저장 성공, 길이: " + (this.currentPuzzleData != null ? this.currentPuzzleData.length() : 0));
        } catch (Exception e) {
            this.currentPuzzleData = null;
            System.out.println("DEBUG: currentPuzzleData 저장 실패: " + e.getMessage());
        }
    }
    
    /**
     * 현재 게임 상태를 Map으로 반환
     * @return 게임 상태 Map
     */
    public Map<String, Object> getCurrentGameState() {
        if (currentGameState == null) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(currentGameState, Map.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 현재 게임 상태를 설정
     * @param gameState 게임 상태 Map
     */
    public void setCurrentGameState(Map<String, Object> gameState) {
        if (gameState == null) {
            this.currentGameState = null;
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.currentGameState = mapper.writeValueAsString(gameState);
        } catch (Exception e) {
            this.currentGameState = null;
        }
    }
    
    /**
     * 활성 퍼즐을 시작
     * @param puzzleData 퍼즐 데이터
     * @param gameState 게임 상태
     */
    public void startActivePuzzle(Map<String, Object> puzzleData, Map<String, Object> gameState) {
        this.hasActivePuzzle = true;
        this.currentPuzzleStartedAt = LocalDateTime.now();
        setCurrentPuzzleData(puzzleData);
        setCurrentGameState(gameState);
    }
    
    /**
     * 활성 퍼즐을 완료
     */
    public void completeActivePuzzle() {
        this.hasActivePuzzle = false;
        this.currentPuzzleData = null;
        this.currentGameState = null;
        this.currentPuzzleStartedAt = null;
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
        this.currentLevelCorrectAnswers++;
        calculateAccuracyRate();
    }
    
    /**
     * 오답을 추가
     */
    public void addWrongAnswer() {
        this.totalWrongAnswers++;
        this.currentLevelWrongAnswers++;
        calculateAccuracyRate();
    }
    
    /**
     * 레벨을 클리어하고 다음 레벨로 진행
     */
    public void levelUp() {
        this.currentLevel++;
        this.currentLevelCorrectAnswers = 0;
        this.currentLevelWrongAnswers = 0;
        completeActivePuzzle();
    }
    
    /**
     * 마지막 플레이 시간을 업데이트
     */
    public void updateLastPlayedAt() {
        this.lastPlayedAt = LocalDateTime.now();
    }
    
    
    /**
     * 게임 상태 설정 (라라벨과 동일한 로직)
     */
    public void setGameState(String gameState) {
        this.currentGameState = gameState;
    }
    
    /**
     * 게임 상태 조회 (라라벨과 동일한 로직)
     */
    public String getGameState() {
        return this.currentGameState;
    }
}
