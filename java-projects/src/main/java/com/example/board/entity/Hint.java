package com.example.board.entity;

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
 * 단어 힌트 정보를 저장하는 Entity
 * Laravel의 pz_hints 테이블과 매핑
 */
@Entity
@Table(name = "pz_hints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    @JsonIgnore
    private Word word;
    
    @Column(name = "hint_text", nullable = false, columnDefinition = "TEXT")
    private String hintText;
    
    @Column(name = "hint_type", length = 20)
    @Builder.Default
    private String hintType = "TEXT";
    
    @Column(name = "image_url", length = 255)
    private String imageUrl;
    
    @Column(name = "audio_url", length = 255)
    private String audioUrl;
    
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Column(name = "difficulty")
    private Integer difficulty;
    
    @Column(name = "correction_status", length = 1)
    @Builder.Default
    private String correctionStatus = "n";
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 연관관계 매핑
    @OneToMany(mappedBy = "hint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HintUsageRecord> hintUsageRecords;
    
    /**
     * 힌트 타입을 반환
     * @return 힌트 타입 (TEXT, IMAGE, AUDIO)
     */
    public HintType getHintTypeEnum() {
        return switch (hintType) {
            case "TEXT" -> HintType.TEXT;
            case "IMAGE" -> HintType.IMAGE;
            case "AUDIO" -> HintType.AUDIO;
            default -> HintType.TEXT;
        };
    }
    
    /**
     * 힌트가 주 힌트인지 확인
     * @return 주 힌트 여부
     */
    public boolean isPrimaryHint() {
        return isPrimary != null && isPrimary;
    }
    
    /**
     * 힌트가 수정되었는지 확인
     * @return 수정 상태 여부
     */
    public boolean isCorrected() {
        return "y".equals(correctionStatus);
    }
    
    /**
     * 힌트 타입 열거형
     */
    public enum HintType {
        TEXT("텍스트"),
        IMAGE("이미지"),
        AUDIO("오디오");
        
        private final String description;
        
        HintType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
