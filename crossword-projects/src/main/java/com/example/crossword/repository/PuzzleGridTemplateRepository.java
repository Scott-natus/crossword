package com.example.crossword.repository;

import com.example.crossword.entity.PuzzleGridTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 퍼즐 그리드 템플릿 리포지토리
 */
@Repository
public interface PuzzleGridTemplateRepository extends JpaRepository<PuzzleGridTemplate, Integer> {
    
    /**
     * 레벨별 활성화된 템플릿 조회
     */
    List<PuzzleGridTemplate> findByLevelIdAndIsActiveTrue(Integer levelId);
    
    /**
     * 레벨별 랜덤 템플릿 조회
     */
    @Query("SELECT t FROM PuzzleGridTemplate t WHERE t.levelId = :levelId AND t.isActive = true ORDER BY RANDOM()")
    List<PuzzleGridTemplate> findRandomByLevelIdAndIsActiveTrue(@Param("levelId") Integer levelId);
    
    /**
     * 레벨별 첫 번째 템플릿 조회
     */
    @Query("SELECT t FROM PuzzleGridTemplate t WHERE t.levelId = :levelId AND t.isActive = true ORDER BY t.id LIMIT 1")
    Optional<PuzzleGridTemplate> findFirstByLevelIdAndIsActiveTrue(@Param("levelId") Integer levelId);
    
    /**
     * 조건에 맞는 템플릿 조회 (퍼즐 생성용)
     */
    @Query("SELECT t FROM PuzzleGridTemplate t WHERE t.wordCount = :wordCount AND t.intersectionCount = :intersectionCount AND t.isActive = true")
    List<PuzzleGridTemplate> findByConditions(@Param("wordCount") Integer wordCount, @Param("intersectionCount") Integer intersectionCount);
}
