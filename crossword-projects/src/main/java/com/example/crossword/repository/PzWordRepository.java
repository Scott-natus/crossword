package com.example.crossword.repository;

import com.example.crossword.entity.PzWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 퍼즐 단어 리포지토리
 */
@Repository
public interface PzWordRepository extends JpaRepository<PzWord, Integer> {
    
    /**
     * 활성화된 단어 조회
     */
    List<PzWord> findByIsActiveTrue();
    
    /**
     * 카테고리별 활성화된 단어 조회
     */
    List<PzWord> findByCategoryAndIsActiveTrue(String category);
    
    /**
     * 난이도별 활성화된 단어 조회
     */
    List<PzWord> findByDifficultyAndIsActiveTrue(Integer difficulty);
    
    /**
     * 길이별 활성화된 단어 조회
     */
    List<PzWord> findByLengthAndIsActiveTrue(Integer length);
    
    /**
     * 특정 길이 범위의 활성화된 단어 조회
     */
    @Query("SELECT w FROM PzWord w WHERE w.length BETWEEN :minLength AND :maxLength AND w.isActive = true")
    List<PzWord> findByLengthRangeAndIsActiveTrue(@Param("minLength") Integer minLength, @Param("maxLength") Integer maxLength);
    
    /**
     * 단어로 조회
     */
    Optional<PzWord> findByWordAndIsActiveTrue(String word);
}
