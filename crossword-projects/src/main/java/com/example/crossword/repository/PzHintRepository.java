package com.example.crossword.repository;

import com.example.crossword.entity.PzHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 퍼즐 힌트 리포지토리
 */
@Repository
public interface PzHintRepository extends JpaRepository<PzHint, Integer> {
    
    /**
     * 단어 ID로 힌트 조회
     */
    @Query("SELECT h FROM PzHint h WHERE h.word.id = :wordId ORDER BY h.id ASC")
    List<PzHint> findByWordIdOrderById(@Param("wordId") Integer wordId);
    
    /**
     * 단어 ID로 주요 힌트 조회
     */
    @Query("SELECT h FROM PzHint h WHERE h.word.id = :wordId AND h.isPrimary = true ORDER BY h.id ASC")
    List<PzHint> findPrimaryHintsByWordId(@Param("wordId") Integer wordId);
    
    /**
     * 단어 ID로 텍스트 힌트 조회
     */
    @Query("SELECT h FROM PzHint h WHERE h.word.id = :wordId AND h.hintType = 'TEXT' ORDER BY h.id ASC")
    List<PzHint> findTextHintsByWordId(@Param("wordId") Integer wordId);
    
    /**
     * 단어 ID와 is_primary 조건으로 힌트 조회 (난이도 순서로 정렬)
     */
    @Query("SELECT h FROM PzHint h WHERE h.word.id = :wordId AND h.isPrimary = :isPrimary ORDER BY h.difficulty ASC")
    List<PzHint> findByWordIdAndIsPrimaryOrderByDifficulty(@Param("wordId") Integer wordId, @Param("isPrimary") Boolean isPrimary);
    
    /**
     * 단어 ID로 힌트 조회하되 특정 힌트 ID는 제외 (난이도 순서로 정렬)
     */
    @Query("SELECT h FROM PzHint h WHERE h.word.id = :wordId AND h.id != :excludeHintId ORDER BY h.difficulty ASC")
    List<PzHint> findByWordIdAndIdNotOrderByDifficulty(@Param("wordId") Integer wordId, @Param("excludeHintId") Integer excludeHintId);
    
}
