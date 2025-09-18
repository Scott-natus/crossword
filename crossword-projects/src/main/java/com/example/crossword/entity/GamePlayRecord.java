package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 게임 플레이 상세 기록을 저장하는 Entity
 * Laravel의 game_play_records 테이블과 매핑
 */
@Entity
@Table(name = "game_play_records",
       indexes = {
           @Index(name = "idx_game_play_records_session_id", columnList = "game_session_id"),
           @Index(name = "idx_game_play_records_step_number", columnList = "step_number")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;
    
    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;
    
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;
    
    @Column(name = "action_data", columnDefinition = "jsonb")
    private String actionData;
    
    @Column(name = "is_correct")
    private Boolean isCorrect;
    
    @Column(name = "time_spent_seconds")
    @Builder.Default
    private Integer timeSpentSeconds = 0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 액션 타입을 반환
     * @return 액션 타입 열거형
     */
    public ActionType getActionTypeEnum() {
        return switch (actionType) {
            case "WORD_INPUT" -> ActionType.WORD_INPUT;
            case "HINT_USED" -> ActionType.HINT_USED;
            case "WORD_CHECK" -> ActionType.WORD_CHECK;
            case "GAME_START" -> ActionType.GAME_START;
            case "GAME_END" -> ActionType.GAME_END;
            case "PAUSE" -> ActionType.PAUSE;
            case "RESUME" -> ActionType.RESUME;
            default -> ActionType.UNKNOWN;
        };
    }
    
    /**
     * 액션이 정답인지 확인
     * @return 정답 여부
     */
    public boolean isCorrectAnswer() {
        return isCorrect != null && isCorrect;
    }
    
    /**
     * 소요 시간을 분:초 형식으로 반환
     * @return 소요 시간 문자열
     */
    public String getTimeSpentFormatted() {
        if (timeSpentSeconds == null || timeSpentSeconds == 0) {
            return "0초";
        }
        
        int minutes = timeSpentSeconds / 60;
        int seconds = timeSpentSeconds % 60;
        
        if (minutes > 0) {
            return String.format("%d분 %d초", minutes, seconds);
        } else {
            return String.format("%d초", seconds);
        }
    }
    
    /**
     * 액션 타입 열거형
     */
    public enum ActionType {
        WORD_INPUT("단어 입력"),
        HINT_USED("힌트 사용"),
        WORD_CHECK("단어 확인"),
        GAME_START("게임 시작"),
        GAME_END("게임 종료"),
        PAUSE("일시정지"),
        RESUME("재개"),
        UNKNOWN("알 수 없음");
        
        private final String description;
        
        ActionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
