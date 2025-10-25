package com.example.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 단어 생성을 위한 임시 테이블 엔티티
 */
@Entity
@Table(name = "tmp_for_make_words")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmpForMakeWords {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "category", length = 100, nullable = false)
    private String category;
    
    @Column(name = "words", length = 30, nullable = false)
    private String words;
    
    @CreationTimestamp
    @Column(name = "regdt")
    private LocalDateTime regdt;
    
    @Column(name = "hint_yn")
    @Builder.Default
    private Boolean hintYn = false;
}
