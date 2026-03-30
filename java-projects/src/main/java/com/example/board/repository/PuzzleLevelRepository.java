package com.example.board.repository;

import com.example.board.entity.PuzzleLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 퍼즐 레벨 정보를 관리하는 Repository
 * Laravel의 PuzzleLevel 모델과 동일한 기능을 제공
 */
@Repository
public interface PuzzleLevelRepository extends JpaRepository<PuzzleLevel, Long> {
    
    /**
     * 특정 레벨 번호로 퍼즐 레벨 조회
     */
    Optional<PuzzleLevel> findByLevel(Integer level);
    
    /**
     * 레벨 번호로 퍼즐 레벨 존재 여부 확인
     */
    boolean existsByLevel(Integer level);
    
    /**
     * 레벨 번호 순으로 모든 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findAllByOrderByLevelAsc();
    
    /**
     * 레벨 번호 순으로 모든 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findAllByOrderByLevelAsc(Pageable pageable);
    
    /**
     * 특정 단어 개수 이상의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByWordCountGreaterThanEqual(Integer wordCount);
    
    /**
     * 특정 단어 개수 이상의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByWordCountGreaterThanEqual(Integer wordCount, Pageable pageable);
    
    /**
     * 특정 단어 개수 이하의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByWordCountLessThanEqual(Integer wordCount);
    
    /**
     * 특정 단어 개수 이하의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByWordCountLessThanEqual(Integer wordCount, Pageable pageable);
    
    /**
     * 단어 개수 범위로 퍼즐 레벨 조회
     */
    @Query("SELECT pl FROM PuzzleLevel pl WHERE pl.wordCount BETWEEN :minWordCount AND :maxWordCount ORDER BY pl.level")
    List<PuzzleLevel> findByWordCountRange(@Param("minWordCount") Integer minWordCount, 
                                          @Param("maxWordCount") Integer maxWordCount);
    
    /**
     * 단어 개수 범위로 퍼즐 레벨을 페이징으로 조회
     */
    @Query("SELECT pl FROM PuzzleLevel pl WHERE pl.wordCount BETWEEN :minWordCount AND :maxWordCount ORDER BY pl.level")
    Page<PuzzleLevel> findByWordCountRange(@Param("minWordCount") Integer minWordCount, 
                                          @Param("maxWordCount") Integer maxWordCount, 
                                          Pageable pageable);
    
    /**
     * 특정 단어 난이도 이상의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByWordDifficultyGreaterThanEqual(Integer wordDifficulty);
    
    /**
     * 특정 단어 난이도 이상의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByWordDifficultyGreaterThanEqual(Integer wordDifficulty, Pageable pageable);
    
    /**
     * 특정 단어 난이도 이하의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByWordDifficultyLessThanEqual(Integer wordDifficulty);
    
    /**
     * 특정 단어 난이도 이하의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByWordDifficultyLessThanEqual(Integer wordDifficulty, Pageable pageable);
    
    /**
     * 특정 힌트 난이도 이상의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByHintDifficultyGreaterThanEqual(Integer hintDifficulty);
    
    /**
     * 특정 힌트 난이도 이상의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByHintDifficultyGreaterThanEqual(Integer hintDifficulty, Pageable pageable);
    
    /**
     * 특정 힌트 난이도 이하의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByHintDifficultyLessThanEqual(Integer hintDifficulty);
    
    /**
     * 특정 힌트 난이도 이하의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByHintDifficultyLessThanEqual(Integer hintDifficulty, Pageable pageable);
    
    /**
     * 특정 교차점 개수 이상의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByIntersectionCountGreaterThanEqual(Integer intersectionCount);
    
    /**
     * 특정 교차점 개수 이상의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByIntersectionCountGreaterThanEqual(Integer intersectionCount, Pageable pageable);
    
    /**
     * 특정 교차점 개수 이하의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByIntersectionCountLessThanEqual(Integer intersectionCount);
    
    /**
     * 특정 교차점 개수 이하의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByIntersectionCountLessThanEqual(Integer intersectionCount, Pageable pageable);
    
    /**
     * 특정 시간 제한 이상의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByTimeLimitGreaterThanEqual(Integer timeLimit);
    
    /**
     * 특정 시간 제한 이상의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByTimeLimitGreaterThanEqual(Integer timeLimit, Pageable pageable);
    
    /**
     * 특정 시간 제한 이하의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByTimeLimitLessThanEqual(Integer timeLimit);
    
    /**
     * 특정 시간 제한 이하의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByTimeLimitLessThanEqual(Integer timeLimit, Pageable pageable);
    
    /**
     * 특정 클리어 조건의 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByClearCondition(Integer clearCondition);
    
    /**
     * 특정 클리어 조건의 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByClearCondition(Integer clearCondition, Pageable pageable);
    
    /**
     * 특정 업데이트자로 업데이트된 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByUpdatedBy(String updatedBy);
    
    /**
     * 특정 업데이트자로 업데이트된 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByUpdatedBy(String updatedBy, Pageable pageable);
    
    /**
     * 레벨명에 특정 문자열이 포함된 퍼즐 레벨 조회
     */
    List<PuzzleLevel> findByLevelNameContainingIgnoreCase(String levelName);
    
    /**
     * 레벨명에 특정 문자열이 포함된 퍼즐 레벨을 페이징으로 조회
     */
    Page<PuzzleLevel> findByLevelNameContainingIgnoreCase(String levelName, Pageable pageable);
    
    /**
     * 가장 높은 레벨 번호 조회
     */
    @Query("SELECT MAX(pl.level) FROM PuzzleLevel pl")
    Optional<Integer> findMaxLevel();
    
    /**
     * 가장 낮은 레벨 번호 조회
     */
    @Query("SELECT MIN(pl.level) FROM PuzzleLevel pl")
    Optional<Integer> findMinLevel();
    
    /**
     * 특정 레벨 범위의 퍼즐 레벨 조회
     */
    @Query("SELECT pl FROM PuzzleLevel pl WHERE pl.level BETWEEN :minLevel AND :maxLevel ORDER BY pl.level")
    List<PuzzleLevel> findByLevelRange(@Param("minLevel") Integer minLevel, 
                                      @Param("maxLevel") Integer maxLevel);
    
    /**
     * 특정 레벨 범위의 퍼즐 레벨을 페이징으로 조회
     */
    @Query("SELECT pl FROM PuzzleLevel pl WHERE pl.level BETWEEN :minLevel AND :maxLevel ORDER BY pl.level")
    Page<PuzzleLevel> findByLevelRange(@Param("minLevel") Integer minLevel, 
                                      @Param("maxLevel") Integer maxLevel, 
                                      Pageable pageable);
    
    /**
     * 단어 개수별 퍼즐 레벨 개수 조회
     */
    @Query("SELECT pl.wordCount, COUNT(pl) FROM PuzzleLevel pl GROUP BY pl.wordCount ORDER BY pl.wordCount")
    List<Object[]> countLevelsByWordCount();
    
    /**
     * 단어 난이도별 퍼즐 레벨 개수 조회
     */
    @Query("SELECT pl.wordDifficulty, COUNT(pl) FROM PuzzleLevel pl GROUP BY pl.wordDifficulty ORDER BY pl.wordDifficulty")
    List<Object[]> countLevelsByWordDifficulty();
    
    /**
     * 힌트 난이도별 퍼즐 레벨 개수 조회
     */
    @Query("SELECT pl.hintDifficulty, COUNT(pl) FROM PuzzleLevel pl GROUP BY pl.hintDifficulty ORDER BY pl.hintDifficulty")
    List<Object[]> countLevelsByHintDifficulty();
    
    /**
     * 교차점 개수별 퍼즐 레벨 개수 조회
     */
    @Query("SELECT pl.intersectionCount, COUNT(pl) FROM PuzzleLevel pl GROUP BY pl.intersectionCount ORDER BY pl.intersectionCount")
    List<Object[]> countLevelsByIntersectionCount();
    
    /**
     * 시간 제한별 퍼즐 레벨 개수 조회
     */
    @Query("SELECT pl.timeLimit, COUNT(pl) FROM PuzzleLevel pl GROUP BY pl.timeLimit ORDER BY pl.timeLimit")
    List<Object[]> countLevelsByTimeLimit();
    
    /**
     * 클리어 조건별 퍼즐 레벨 개수 조회
     */
    @Query("SELECT pl.clearCondition, COUNT(pl) FROM PuzzleLevel pl GROUP BY pl.clearCondition ORDER BY pl.clearCondition")
    List<Object[]> countLevelsByClearCondition();
    
    /**
     * 업데이트자별 퍼즐 레벨 개수 조회
     */
    @Query("SELECT pl.updatedBy, COUNT(pl) FROM PuzzleLevel pl WHERE pl.updatedBy IS NOT NULL GROUP BY pl.updatedBy ORDER BY pl.updatedBy")
    List<Object[]> countLevelsByUpdatedBy();
    
    /**
     * 전체 퍼즐 레벨 개수 조회
     */
    long count();
    
    /**
     * 특정 단어 개수의 퍼즐 레벨 개수 조회
     */
    long countByWordCount(Integer wordCount);
    
    /**
     * 특정 단어 난이도의 퍼즐 레벨 개수 조회
     */
    long countByWordDifficulty(Integer wordDifficulty);
    
    /**
     * 특정 힌트 난이도의 퍼즐 레벨 개수 조회
     */
    long countByHintDifficulty(Integer hintDifficulty);
    
    /**
     * 특정 교차점 개수의 퍼즐 레벨 개수 조회
     */
    long countByIntersectionCount(Integer intersectionCount);
    
    /**
     * 특정 시간 제한의 퍼즐 레벨 개수 조회
     */
    long countByTimeLimit(Integer timeLimit);
    
    /**
     * 특정 클리어 조건의 퍼즐 레벨 개수 조회
     */
    long countByClearCondition(Integer clearCondition);
}
