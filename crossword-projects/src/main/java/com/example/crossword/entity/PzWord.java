package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 퍼즐 단어 엔티티
 * Laravel의 pz_words 테이블과 매핑
 */
@Entity
@Table(name = "pz_words")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PzWord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "category", length = 50, nullable = false)
    private String category;
    
    @Column(name = "word", length = 50, nullable = false)
    private String word;
    
    @Column(name = "length", nullable = false)
    private Integer length;
    
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty = 3;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
}
