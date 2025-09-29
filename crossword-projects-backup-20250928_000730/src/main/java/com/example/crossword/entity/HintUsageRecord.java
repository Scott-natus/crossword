package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 힌트 사용 기록을 저장하는 Entity
 * Laravel의 hint_usage_records 테이블과 매핑
 */
@Entity
@Table(name = "hint_usage_records",
       indexes = {
           @Index(name = "idx_hint_usage_records_hint_id", columnList = "hint_id"),
           @Index(name = "idx_hint_usage_records_session_id", columnList = "game_session_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintUsageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hint_id", nullable = false)
    private Hint hint;
    
    @Column(name = "used_at", nullable = false)
    @Builder.Default
    private LocalDateTime usedAt = LocalDateTime.now();
    
    @Column(name = "hint_number", nullable = false)
    private Integer hintNumber;
    
    @Column(name = "is_helpful")
    private Boolean isHelpful;
    
    @CreationTimestamp
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 힌트가 도움이 되었는지 확인
     * @return 도움 여부
     */
    public boolean isHelpfulHint() {
        return isHelpful != null && isHelpful;
    }
    
    /**
     * 힌트 사용 시간을 상대적 시간으로 반환
     * @return 상대적 시간 문자열
     */
    public String getUsedAtRelative() {
        if (usedAt == null) return "알 수 없음";
        
        LocalDateTime now = LocalDateTime.now();
        long seconds = java.time.Duration.between(usedAt, now).getSeconds();
        
        if (seconds < 60) {
            return seconds + "초 전";
        } else if (seconds < 3600) {
            return (seconds / 60) + "분 전";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "시간 전";
        } else {
            return (seconds / 86400) + "일 전";
        }
    }
    
    /**
     * 힌트 번호를 문자열로 반환
     * @return 힌트 번호 문자열
     */
    public String getHintNumberFormatted() {
        if (hintNumber == null) return "알 수 없음";
        return hintNumber + "번 힌트";
    }
}
