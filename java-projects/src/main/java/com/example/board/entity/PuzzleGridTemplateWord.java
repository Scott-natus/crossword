package com.example.board.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * 퍼즐 그리드 템플릿 단어 연결 엔티티
 * Laravel의 puzzle_grid_template_word 테이블과 매핑
 */
@Entity
@Table(name = "puzzle_grid_template_word")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleGridTemplateWord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private PuzzleGridTemplate template;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private PzWord word;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "word_positions")
    private List<Map<String, Object>> wordPositions;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
