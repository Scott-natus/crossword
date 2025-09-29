package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 퍼즐 레벨 정보를 저장하는 Entity
 * Laravel의 puzzle_levels 테이블과 매핑
 */
@Entity
@Table(name = "puzzle_levels", 
       uniqueConstraints = @UniqueConstraint(name = "puzzle_levels_level_unique", columnNames = "level"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleLevel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "level", nullable = false, unique = true)
    private Integer level;
    
    @Column(name = "level_name", nullable = false, length = 255)
    private String levelName;
    
    @Column(name = "word_count", nullable = false)
    private Integer wordCount;
    
    @Column(name = "word_difficulty", nullable = false)
    private Integer wordDifficulty;
    
    @Column(name = "hint_difficulty", nullable = false)
    private Integer hintDifficulty;
    
    @Column(name = "intersection_count", nullable = false)
    private Integer intersectionCount;
    
    @Column(name = "time_limit", nullable = false)
    private Integer timeLimit;
    
    @Column(name = "clear_condition")
    @Builder.Default
    private Integer clearCondition = 1;
    
    @Column(name = "updated_by", length = 255)
    private String updatedBy;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 레벨의 난이도를 반환
     * @return 난이도 설명
     */
    public String getDifficultyDescription() {
        if (level == null) return "미분류";
        
        return switch (level) {
            case 1, 2, 3 -> "초급";
            case 4, 5, 6 -> "중급";
            case 7, 8, 9 -> "고급";
            default -> "전문가";
        };
    }
    
    /**
     * 시간 제한을 분:초 형식으로 반환
     * @return 시간 제한 문자열
     */
    public String getTimeLimitFormatted() {
        if (timeLimit == null) return "제한 없음";
        
        int minutes = timeLimit / 60;
        int seconds = timeLimit % 60;
        
        if (minutes > 0) {
            return String.format("%d분 %d초", minutes, seconds);
        } else {
            return String.format("%d초", seconds);
        }
    }
    
    /**
     * 레벨이 활성 상태인지 확인
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return level != null && level > 0;
    }
    
    /**
     * 클리어 조건을 반환
     * @return 클리어 조건 설명
     */
    public String getClearConditionDescription() {
        if (clearCondition == null) return "조건 없음";
        
        return switch (clearCondition) {
            case 1 -> "모든 단어 완성";
            case 2 -> "80% 이상 완성";
            case 3 -> "60% 이상 완성";
            default -> "기타 조건";
        };
    }
}
