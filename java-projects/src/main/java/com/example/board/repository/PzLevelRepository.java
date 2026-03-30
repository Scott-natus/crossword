package com.example.board.repository;

import com.example.board.entity.PzLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 퍼즐 레벨 리포지토리
 * 
 * @author Board Team
 * @version 1.0.0
 * @since 2025-10-26
 */
@Repository
public interface PzLevelRepository extends JpaRepository<PzLevel, Long> {
    
    /**
     * 레벨 번호로 레벨 조회
     */
    Optional<PzLevel> findByLevel(Integer level);
    
    /**
     * 레벨 번호 존재 여부 확인
     */
    boolean existsByLevel(Integer level);
    
    /**
     * 레벨 번호로 삭제
     */
    void deleteByLevel(Integer level);
    
    /**
     * 검색 조건으로 레벨 조회
     */
    @Query("SELECT l FROM PzLevel l WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           " LOWER(l.levelName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " CAST(l.level AS string) LIKE CONCAT('%', :searchTerm, '%')) AND " +
           "(:minWords IS NULL OR l.wordCount >= :minWords) AND " +
           "(:maxWords IS NULL OR l.wordCount <= :maxWords) AND " +
           "(:wordDifficulty IS NULL OR l.wordDifficulty = :wordDifficulty) AND " +
           "(:hintDifficulty IS NULL OR l.hintDifficulty = :hintDifficulty)")
    Page<PzLevel> findBySearchCriteria(
        @Param("searchTerm") String searchTerm,
        @Param("minWords") Integer minWords,
        @Param("maxWords") Integer maxWords,
        @Param("wordDifficulty") Integer wordDifficulty,
        @Param("hintDifficulty") Integer hintDifficulty,
        Pageable pageable);
    
    /**
     * 전체 레벨 개수 조회
     */
    long count();
    
    /**
     * 레벨 번호 순으로 모든 레벨 조회
     */
    @Query("SELECT l FROM PzLevel l ORDER BY l.level ASC")
    java.util.List<PzLevel> findAllOrderByLevel();
}