package com.example.crossword.repository;

import com.example.crossword.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 단어 정보를 관리하는 Repository
 * Laravel의 Word 모델과 동일한 기능을 제공
 */
@Repository
public interface WordRepository extends JpaRepository<Word, Integer> {
    
    /**
     * 활성 상태인 단어만 조회
     */
    List<Word> findByIsActiveTrue();
    
    /**
     * 활성 상태인 단어를 페이징으로 조회
     */
    Page<Word> findByIsActiveTrue(Pageable pageable);
    
    /**
     * 특정 난이도의 단어 조회
     */
    List<Word> findByDifficultyAndIsActiveTrue(Integer difficulty);
    
    /**
     * 특정 난이도의 단어를 페이징으로 조회
     */
    Page<Word> findByDifficultyAndIsActiveTrue(Integer difficulty, Pageable pageable);
    
    /**
     * 특정 카테고리의 단어 조회
     */
    List<Word> findByCategoryAndIsActiveTrue(String category);
    
    /**
     * 특정 카테고리의 단어를 페이징으로 조회
     */
    Page<Word> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    
    /**
     * 난이도와 카테고리로 단어 조회
     */
    List<Word> findByDifficultyAndCategoryAndIsActiveTrue(Integer difficulty, String category);
    
    /**
     * 난이도와 카테고리로 단어를 페이징으로 조회
     */
    Page<Word> findByDifficultyAndCategoryAndIsActiveTrue(Integer difficulty, String category, Pageable pageable);
    
    /**
     * 특정 길이의 단어 조회
     */
    List<Word> findByLengthAndIsActiveTrue(Integer length);
    
    /**
     * 특정 길이의 단어를 페이징으로 조회
     */
    Page<Word> findByLengthAndIsActiveTrue(Integer length, Pageable pageable);
    
    /**
     * 난이도 범위로 단어 조회
     */
    @Query("SELECT w FROM Word w WHERE w.difficulty BETWEEN :minDifficulty AND :maxDifficulty AND w.isActive = true")
    List<Word> findByDifficultyRangeAndIsActiveTrue(@Param("minDifficulty") Integer minDifficulty, 
                                                   @Param("maxDifficulty") Integer maxDifficulty);
    
    /**
     * 난이도 범위로 단어를 페이징으로 조회
     */
    @Query("SELECT w FROM Word w WHERE w.difficulty BETWEEN :minDifficulty AND :maxDifficulty AND w.isActive = true")
    Page<Word> findByDifficultyRangeAndIsActiveTrue(@Param("minDifficulty") Integer minDifficulty, 
                                                   @Param("maxDifficulty") Integer maxDifficulty, 
                                                   Pageable pageable);
    
    /**
     * 길이 범위로 단어 조회
     */
    @Query("SELECT w FROM Word w WHERE w.length BETWEEN :minLength AND :maxLength AND w.isActive = true")
    List<Word> findByLengthRangeAndIsActiveTrue(@Param("minLength") Integer minLength, 
                                               @Param("maxLength") Integer maxLength);
    
    /**
     * 길이 범위로 단어를 페이징으로 조회
     */
    @Query("SELECT w FROM Word w WHERE w.length BETWEEN :minLength AND :maxLength AND w.isActive = true")
    Page<Word> findByLengthRangeAndIsActiveTrue(@Param("minLength") Integer minLength, 
                                               @Param("maxLength") Integer maxLength, 
                                               Pageable pageable);
    
    /**
     * 단어명으로 정확히 일치하는 단어 조회
     */
    Optional<Word> findFirstByWordAndIsActiveTrue(String word);
    
    /**
     * 단어명에 특정 문자열이 포함된 단어 조회
     */
    List<Word> findByWordContainingIgnoreCaseAndIsActiveTrue(String word);
    
    /**
     * 단어명에 특정 문자열이 포함된 단어를 페이징으로 조회
     */
    Page<Word> findByWordContainingIgnoreCaseAndIsActiveTrue(String word, Pageable pageable);
    
    /**
     * 랜덤하게 단어 조회 (크로스워드 퍼즐 생성용)
     */
    @Query(value = "SELECT * FROM pz_words WHERE is_active = true ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Word> findRandomWords(@Param("limit") Integer limit);
    
    /**
     * 특정 난이도의 랜덤 단어 조회
     */
    @Query(value = "SELECT * FROM pz_words WHERE difficulty = :difficulty AND is_active = true ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Word> findRandomWordsByDifficulty(@Param("difficulty") Integer difficulty, @Param("limit") Integer limit);
    
    /**
     * 특정 카테고리의 랜덤 단어 조회
     */
    @Query(value = "SELECT * FROM pz_words WHERE category = :category AND is_active = true ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Word> findRandomWordsByCategory(@Param("category") String category, @Param("limit") Integer limit);
    
    /**
     * 특정 길이의 랜덤 단어 조회
     */
    @Query(value = "SELECT * FROM pz_words WHERE length = :length AND is_active = true ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Word> findRandomWordsByLength(@Param("length") Integer length, @Param("limit") Integer limit);
    
    /**
     * 난이도별 단어 개수 조회
     */
    @Query("SELECT w.difficulty, COUNT(w) FROM Word w WHERE w.isActive = true GROUP BY w.difficulty ORDER BY w.difficulty")
    List<Object[]> countWordsByDifficulty();
    
    /**
     * 카테고리별 단어 개수 조회
     */
    @Query("SELECT w.category, COUNT(w) FROM Word w WHERE w.isActive = true GROUP BY w.category ORDER BY w.category")
    List<Object[]> countWordsByCategory();
    
    /**
     * 길이별 단어 개수 조회
     */
    @Query("SELECT w.length, COUNT(w) FROM Word w WHERE w.isActive = true GROUP BY w.length ORDER BY w.length")
    List<Object[]> countWordsByLength();
    
    /**
     * 전체 활성 단어 개수 조회
     */
    long countByIsActiveTrue();
    
    /**
     * 특정 난이도의 활성 단어 개수 조회
     */
    long countByDifficultyAndIsActiveTrue(Integer difficulty);
    
    /**
     * 특정 카테고리의 활성 단어 개수 조회
     */
    long countByCategoryAndIsActiveTrue(String category);
    
    /**
     * 특정 길이의 활성 단어 개수 조회
     */
    long countByLengthAndIsActiveTrue(Integer length);
    
    /**
     * 모든 카테고리 목록 조회
     */
    @Query("SELECT DISTINCT w.category FROM Word w WHERE w.isActive = true AND w.category IS NOT NULL ORDER BY w.category")
    List<String> findDistinctCategories();
    
    /**
     * 힌트가 있는 단어 개수 조회
     */
    @Query("SELECT COUNT(DISTINCT w.id) FROM Word w WHERE w.isActive = true AND EXISTS (SELECT 1 FROM Hint h WHERE h.word = w)")
    long countWordsWithHints();
    
    /**
     * 정제완료된 단어 개수 조회
     */
    long countByConfYn(String confYn);
    
    /**
     * 힌트가 없는 활성 단어 개수 조회
     */
    @Query("SELECT COUNT(w) FROM Word w WHERE w.isActive = true AND NOT EXISTS (SELECT 1 FROM Hint h WHERE h.word = w)")
    long countActiveWordsWithoutHints();
    
    /**
     * 힌트가 없는 단어 개수 조회
     */
    @Query("SELECT COUNT(DISTINCT w.id) FROM Word w WHERE w.isActive = true AND NOT EXISTS (SELECT 1 FROM Hint h WHERE h.word = w)")
    long countWordsWithoutHints();
}
