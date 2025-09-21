package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 퍼즐 단어 엔티티
 * Laravel의 PzWord 모델과 정확히 동일한 구조로 구현
 */
@Entity
@Table(name = "pz_words")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PzWord {
    
    // 난이도 상수 (라라벨과 동일)
    public static final int DIFFICULTY_EASY = 1;
    public static final int DIFFICULTY_MEDIUM = 2;
    public static final int DIFFICULTY_HARD = 3;
    public static final int DIFFICULTY_VERY_HARD = 4;
    public static final int DIFFICULTY_EXTREME = 5;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "word", length = 255, nullable = false)
    private String word;
    
    @Column(name = "length", nullable = false)
    private Integer length;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // JPA 생명주기 콜백 (라라벨의 boot 메서드와 동일)
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // 단어 저장 시 글자수 자동 계산 (라라벨과 동일)
        if (word != null) {
            length = word.length();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // 단어 수정 시 글자수 자동 계산 (라라벨과 동일)
        if (word != null) {
            length = word.length();
        }
    }
    
    // 난이도 텍스트 반환 (라라벨의 getDifficultyTextAttribute와 동일)
    public String getDifficultyText() {
        switch (difficulty) {
            case DIFFICULTY_EASY: return "쉬움";
            case DIFFICULTY_MEDIUM: return "보통";
            case DIFFICULTY_HARD: return "어려움";
            case DIFFICULTY_VERY_HARD: return "매우 어려움";
            case DIFFICULTY_EXTREME: return "극도 어려움";
            default: return "알 수 없음";
        }
    }
}
