package com.example.crossword.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 퍼즐 단어 정보를 저장하는 Entity
 * Laravel의 pz_words 테이블과 매핑
 */
@Entity
@Table(name = "pz_words")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Word {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "word", nullable = false, length = 255)
    private String word;
    
    @Column(name = "length", nullable = false)
    private Integer length;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 연관관계 매핑
    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Hint> hints;
    
    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<GameSession> gameSessions;
    
    // 힌트 개수 (조회 시 계산)
    @Transient
    private Integer hintCount;
    
    /**
     * 단어의 난이도 레벨을 반환
     * @return 난이도 레벨 (1-5)
     */
    public String getDifficultyLevel() {
        if (difficulty == null) return "미분류";
        
        return switch (difficulty) {
            case 1 -> "매우 쉬움";
            case 2 -> "쉬움";
            case 3 -> "보통";
            case 4 -> "어려움";
            case 5 -> "매우 어려움";
            default -> "미분류";
        };
    }
    
    /**
     * 단어가 활성 상태인지 확인
     * @return 활성 상태 여부
     */
    public boolean isWordActive() {
        return isActive != null && isActive;
    }
}
