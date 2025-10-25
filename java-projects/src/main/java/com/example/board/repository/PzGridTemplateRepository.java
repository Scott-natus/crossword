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
    
    @Query("SELECT MAX(p.intersectionCount) FROM PzGridTemplate p WHERE p.isActive = true")
    Integer findMaxIntersectionCount();
    
    @Query("SELECT MIN(p.intersectionCount) FROM PzGridTemplate p WHERE p.isActive = true")
    Integer findMinIntersectionCount();
    
    // 레벨별 템플릿 수 통계
    @Query("SELECT p.levelId, COUNT(p.id) FROM PzGridTemplate p WHERE p.isActive = true GROUP BY p.levelId ORDER BY p.levelId")
    List<Object[]> countTemplatesByLevel();
    
    // 그리드 크기별 템플릿 수 통계
    @Query("SELECT CONCAT(p.gridWidth, '×', p.gridHeight), COUNT(p.id) FROM PzGridTemplate p WHERE p.isActive = true GROUP BY p.gridWidth, p.gridHeight ORDER BY p.gridWidth, p.gridHeight")
    List<Object[]> countTemplatesByGridSize();
    
    
    // 레벨별 샘플 템플릿 조회 (각 레벨당 최대 3개)
    @Query(value = """
        SELECT * FROM (
            SELECT *, ROW_NUMBER() OVER (PARTITION BY level_id ORDER BY created_at DESC) as rn
            FROM puzzle_grid_templates 
            WHERE is_active = true
        ) t 
        WHERE t.rn <= 3 
        ORDER BY level_id, created_at DESC
        """, nativeQuery = true)
    List<Object[]> findSampleTemplatesByLevelRaw();
    
    // 특정 레벨의 샘플 템플릿 조회
    @Query("SELECT p FROM PzGridTemplate p WHERE p.levelId = :levelId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<PzGridTemplate> findSampleTemplatesBySpecificLevel(@Param("levelId") Integer levelId);
    
    // 복잡한 검색 쿼리 (다중 조건)
    @Query("""
        SELECT p FROM PzGridTemplate p 
        WHERE p.isActive = true 
        AND (:levelId IS NULL OR p.levelId = :levelId)
        AND (:templateName IS NULL OR LOWER(p.templateName) LIKE LOWER(CONCAT('%', :templateName, '%')))
        AND (:minWordCount IS NULL OR p.wordCount >= :minWordCount)
        AND (:maxWordCount IS NULL OR p.wordCount <= :maxWordCount)
        AND (:minIntersectionCount IS NULL OR p.intersectionCount >= :minIntersectionCount)
        AND (:maxIntersectionCount IS NULL OR p.intersectionCount <= :maxIntersectionCount)
        ORDER BY p.levelId ASC, p.createdAt DESC
        """)
    List<PzGridTemplate> findTemplatesWithFilters(
        @Param("levelId") Integer levelId,
        @Param("templateName") String templateName,
        @Param("minWordCount") Integer minWordCount,
        @Param("maxWordCount") Integer maxWordCount,
        @Param("minIntersectionCount") Integer minIntersectionCount,
        @Param("maxIntersectionCount") Integer maxIntersectionCount
    );
}
