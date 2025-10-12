package com.example.crossword.repository;

import com.example.crossword.entity.FailedWordExtraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FailedWordExtractionRepository extends JpaRepository<FailedWordExtraction, Long> {
    
    /**
     * 특정 레벨의 실패한 단어 추출 기록 조회
     */
    List<FailedWordExtraction> findByLevelOrderByCreatedAtDesc(Integer level);
    
    /**
     * 특정 템플릿 ID의 실패한 단어 추출 기록 조회
     */
    List<FailedWordExtraction> findByTemplateIdOrderByCreatedAtDesc(Integer templateId);
    
    /**
     * 특정 기간 동안의 실패한 단어 추출 기록 조회
     */
    List<FailedWordExtraction> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 특정 실패 이유로 실패한 단어 추출 기록 조회
     */
    List<FailedWordExtraction> findByFailureReasonContainingOrderByCreatedAtDesc(String failureReason);
    
    /**
     * 특정 단어 ID로 실패한 기록 조회
     */
    List<FailedWordExtraction> findByFailedWordIdOrderByCreatedAtDesc(Integer failedWordId);
    
    /**
     * 최근 N개의 실패한 단어 추출 기록 조회
     */
    @Query("SELECT f FROM FailedWordExtraction f ORDER BY f.createdAt DESC")
    List<FailedWordExtraction> findTopNByOrderByCreatedAtDesc(@Param("limit") int limit);
    
    /**
     * 레벨별 실패 횟수 통계
     */
    @Query("SELECT f.level, COUNT(f) FROM FailedWordExtraction f GROUP BY f.level ORDER BY f.level")
    List<Object[]> getFailureCountByLevel();
    
    /**
     * 실패 이유별 통계
     */
    @Query("SELECT f.failureReason, COUNT(f) FROM FailedWordExtraction f GROUP BY f.failureReason ORDER BY COUNT(f) DESC")
    List<Object[]> getFailureCountByReason();
    
    /**
     * 특정 레벨에서 가장 많이 실패한 단어 ID 조회
     */
    @Query("SELECT f.failedWordId, COUNT(f) FROM FailedWordExtraction f WHERE f.level = :level GROUP BY f.failedWordId ORDER BY COUNT(f) DESC")
    List<Object[]> getMostFailedWordsByLevel(@Param("level") Integer level);
}
