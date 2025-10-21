package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 퍼즐 힌트 엔티티
 * Laravel의 pz_hints 테이블과 매핑
 */
@Entity
@Table(name = "pz_hints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PzHint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    @JsonIgnore
    private PzWord word;
    
    @Column(name = "hint_text", nullable = false, columnDefinition = "TEXT")
    private String hintText;
    
    @Column(name = "hint_type")
    private String hintType = "TEXT";
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "audio_url")
    private String audioUrl;
    
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;
    
    @Column(name = "difficulty")
    private Integer difficulty;
    
    @Column(name = "language_code", length = 10)
    private String languageCode = "ko";
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
}
