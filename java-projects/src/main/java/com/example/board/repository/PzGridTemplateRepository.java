package com.example.board.repository;

import com.example.board.entity.PzGridTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 퍼즐 그리드 템플릿 리포지토리
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-23
 */
@Repository
public interface PzGridTemplateRepository extends JpaRepository<PzGridTemplate, Long> {
    
    // 레벨별 템플릿 조회
    List<PzGridTemplate> findByLevelIdOrderByCreatedAtDesc(Integer levelId);
    
    // 활성화된 템플릿만 조회
    List<PzGridTemplate> findByIsActiveTrueOrderByLevelIdAscCreatedAtDesc();
    
    // 특정 레벨의 활성화된 템플릿 조회
    List<PzGridTemplate> findByLevelIdAndIsActiveTrueOrderByCreatedAtDesc(Integer levelId);
    
    Page<PzGridTemplate> findByIsActiveTrue(Pageable pageable);
    
    // 템플릿 이름으로 검색
    List<PzGridTemplate> findByTemplateNameContainingIgnoreCase(String templateName);
    
    // 레벨 범위로 검색
    List<PzGridTemplate> findByLevelIdBetween(Integer minLevelId, Integer maxLevelId);
    
    // 그리드 크기로 검색
    List<PzGridTemplate> findByGridWidthAndGridHeight(Integer width, Integer height);
    
    
    // 통계 정보 조회
    @Query("SELECT COUNT(p) FROM PzGridTemplate p WHERE p.isActive = true")
    Long countActiveTemplates();
    
    @Query("SELECT AVG(p.wordCount) FROM PzGridTemplate p WHERE p.isActive = true")
    Double findAverageWordCount();
    
    @Query("SELECT MAX(p.wordCount) FROM PzGridTemplate p WHERE p.isActive = true")
    Integer findMaxWordCount();
    
    @Query("SELECT MIN(p.wordCount) FROM PzGridTemplate p WHERE p.isActive = true")
    Integer findMinWordCount();
    
    @Query("SELECT AVG(p.intersectionCount) FROM PzGridTemplate p WHERE p.isActive = true")
    Double findAverageIntersectionCount();
    
    @Query("SELECT MAX(p.intersectionCount) FROM PzGridTemplate p WHERE p.isActive = true")
    Integer findMaxIntersectionCount();
    
    @Query("SELECT MIN(p.intersectionCount) FROM PzGridTemplate p WHERE p.isActive = true")
    Integer findMinIntersectionCount();
    
    // 레벨별 템플릿 개수
    @Query("SELECT p.levelId, COUNT(p) FROM PzGridTemplate p WHERE p.isActive = true GROUP BY p.levelId ORDER BY p.levelId")
    List<Object[]> countTemplatesByLevel();
    
    // 복합 검색 쿼리
    @Query("SELECT p FROM PzGridTemplate p WHERE " +
           "(:levelId IS NULL OR p.levelId = :levelId) AND " +
           "(:templateName IS NULL OR LOWER(p.templateName) LIKE LOWER(CONCAT('%', :templateName, '%'))) AND " +
           "(:minWordCount IS NULL OR p.wordCount >= :minWordCount) AND " +
           "(:maxWordCount IS NULL OR p.wordCount <= :maxWordCount) AND " +
           "(:minIntersectionCount IS NULL OR p.intersectionCount >= :minIntersectionCount) AND " +
           "(:maxIntersectionCount IS NULL OR p.intersectionCount <= :maxIntersectionCount)")
    Page<PzGridTemplate> findBySearchCriteria(
            @Param("levelId") Integer levelId,
            @Param("templateName") String templateName,
            @Param("minWordCount") Integer minWordCount,
            @Param("maxWordCount") Integer maxWordCount,
            @Param("minIntersectionCount") Integer minIntersectionCount,
            @Param("maxIntersectionCount") Integer maxIntersectionCount,
            Pageable pageable);
}