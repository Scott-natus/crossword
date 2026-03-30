package com.example.crossword.repository;

import com.example.crossword.entity.PzLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PzLevelRepository extends JpaRepository<PzLevel, Long> {
    
    /**
     * 레벨 번호로 레벨 조회
     */
    Optional<PzLevel> findByLevel(Integer level);
    
    /**
     * 레벨 번호로 레벨 존재 여부 확인
     */
    boolean existsByLevel(Integer level);
    
    /**
     * 레벨 번호로 레벨 삭제
     */
    void deleteByLevel(Integer level);
    
    /**
     * 레벨 번호 순으로 정렬하여 모든 레벨 조회
     */
    List<PzLevel> findAllByOrderByLevelAsc();
    
    /**
     * 페이징 처리된 레벨 목록 조회 (레벨 번호 순)
     */
    Page<PzLevel> findAllByOrderByLevelAsc(Pageable pageable);
    
    /**
     * 레벨 이름으로 검색 (페이징)
     */
    @Query("SELECT l FROM PzLevel l WHERE l.levelName LIKE %:searchTerm% ORDER BY l.level ASC")
    Page<PzLevel> findByLevelNameContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * 단어 수 범위로 검색 (페이징)
     */
    @Query("SELECT l FROM PzLevel l WHERE l.wordCount BETWEEN :minWords AND :maxWords ORDER BY l.level ASC")
    Page<PzLevel> findByWordCountBetween(@Param("minWords") Integer minWords, @Param("maxWords") Integer maxWords, Pageable pageable);
    
    /**
     * 난이도로 검색 (페이징)
     */
    @Query("SELECT l FROM PzLevel l WHERE l.wordDifficulty = :difficulty OR l.hintDifficulty = :difficulty ORDER BY l.level ASC")
    Page<PzLevel> findByDifficulty(@Param("difficulty") Integer difficulty, Pageable pageable);
    
    /**
     * 복합 검색 (페이징)
     */
    @Query("SELECT l FROM PzLevel l WHERE " +
           "(:searchTerm IS NULL OR l.levelName LIKE %:searchTerm%) AND " +
           "(:minWords IS NULL OR l.wordCount >= :minWords) AND " +
           "(:maxWords IS NULL OR l.wordCount <= :maxWords) AND " +
           "(:wordDifficulty IS NULL OR l.wordDifficulty = :wordDifficulty) AND " +
           "(:hintDifficulty IS NULL OR l.hintDifficulty = :hintDifficulty) " +
           "ORDER BY l.level ASC")
    Page<PzLevel> findBySearchCriteria(@Param("searchTerm") String searchTerm,
                                       @Param("minWords") Integer minWords,
                                       @Param("maxWords") Integer maxWords,
                                       @Param("wordDifficulty") Integer wordDifficulty,
                                       @Param("hintDifficulty") Integer hintDifficulty,
                                       Pageable pageable);
    
    /**
     * 레벨 통계 조회
     */
    @Query("SELECT COUNT(l) FROM PzLevel l")
    long countTotalLevels();
    
    /**
     * 평균 단어 수 조회
     */
    @Query("SELECT AVG(l.wordCount) FROM PzLevel l")
    Double getAverageWordCount();
    
    /**
     * 최대 단어 수 조회
     */
    @Query("SELECT MAX(l.wordCount) FROM PzLevel l")
    Integer getMaxWordCount();
    
    /**
     * 최소 단어 수 조회
     */
    @Query("SELECT MIN(l.wordCount) FROM PzLevel l")
    Integer getMinWordCount();
    
    /**
     * 난이도별 레벨 수 조회
     */
    @Query("SELECT l.wordDifficulty, COUNT(l) FROM PzLevel l GROUP BY l.wordDifficulty ORDER BY l.wordDifficulty")
    List<Object[]> getLevelCountByWordDifficulty();
    
    /**
     * 힌트 난이도별 레벨 수 조회
     */
    @Query("SELECT l.hintDifficulty, COUNT(l) FROM PzLevel l GROUP BY l.hintDifficulty ORDER BY l.hintDifficulty")
    List<Object[]> getLevelCountByHintDifficulty();
}

