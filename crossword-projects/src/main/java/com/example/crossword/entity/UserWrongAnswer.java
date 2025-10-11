package com.example.crossword.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 오답 기록을 저장하는 Entity
 * Laravel의 user_wrong_answers 테이블과 매핑
 */
@Entity
@Table(name = "user_wrong_answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWrongAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "word_id", nullable = false)
    private Long wordId;
    
    @Column(name = "user_answer", nullable = false, columnDefinition = "TEXT")
    private String userAnswer;
    
    @Column(name = "correct_answer", nullable = false, columnDefinition = "TEXT")
    private String correctAnswer;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "level", nullable = false)
    private Integer level;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
