package com.example.board.repository;

import com.example.board.entity.PzWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 활성화된 단어를 페이징으로 조회
     */
    Page<PzWord> findByIsActiveTrue(Pageable pageable);
    
    /**
     * 카테고리별 활성화된 단어 조회
     */
    List<PzWord> findByCategoryAndIsActiveTrue(String category);
    
    /**
     * 카테고리별 활성화된 단어를 페이징으로 조회
     */
    Page<PzWord> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    
    /**
     * 난이도별 활성화된 단어 조회
     */
    List<PzWord> findByDifficultyAndIsActiveTrue(Integer difficulty);
    
    /**
     * 난이도별 활성화된 단어를 페이징으로 조회
     */
    Page<PzWord> findByDifficultyAndIsActiveTrue(Integer difficulty, Pageable pageable);
    
    /**
     * 길이별 활성화된 단어 조회
     */
    List<PzWord> findByLengthAndIsActiveTrue(Integer length);
    
    /**
     * 길이별 활성화된 단어를 페이징으로 조회
     */
    Page<PzWord> findByLengthAndIsActiveTrue(Integer length, Pageable pageable);
    
    /**
     * 특정 길이 범위의 활성화된 단어 조회
     */
    @Query("SELECT w FROM PzWord w WHERE w.length BETWEEN :minLength AND :maxLength AND w.isActive = true")
    List<PzWord> findByLengthRangeAndIsActiveTrue(@Param("minLength") Integer minLength, @Param("maxLength") Integer maxLength);
    
    /**
     * 특정 길이 범위의 활성화된 단어를 페이징으로 조회
     */
    @Query("SELECT w FROM PzWord w WHERE w.length BETWEEN :minLength AND :maxLength AND w.isActive = true")
    Page<PzWord> findByLengthRangeAndIsActiveTrue(@Param("minLength") Integer minLength, @Param("maxLength") Integer maxLength, Pageable pageable);
    
    /**
     * 단어로 조회
     */
    Optional<PzWord> findByWordAndIsActiveTrue(String word);
    
    /**
     * 단어로 조회 (활성화 여부 무관)
     */
    Optional<PzWord> findByWord(String word);
    
    /**
     * 카테고리와 승인 상태로 조회 (8081과 동일)
     */
    @Query("SELECT w FROM PzWord w WHERE w.cat2 = :theme AND w.isApproved = :isApproved")
    List<PzWord> findByCat2AndIsApproved(@Param("theme") String theme, @Param("isApproved") Boolean isApproved);
    
    /**
     * 힌트가 없는 단어 개수 조회
     */
    @Query("SELECT COUNT(w) FROM PzWord w WHERE w.id NOT IN (SELECT DISTINCT h.word.id FROM PzHint h)")
    Long countWordsWithoutHints();

    /**
     * 단어 검색 (페이징)
     */
    Page<PzWord> findByWordContaining(String word, Pageable pageable);

    /**
     * 난이도로 검색 (페이징)
     */
    Page<PzWord> findByDifficulty(Integer difficulty, Pageable pageable);
    
    /**
     * 정제 상태로 검색 (페이징)
     */
    Page<PzWord> findByConfYn(String confYn, Pageable pageable);
    
    /**
     * 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByIsActive(Boolean isActive, Pageable pageable);
    
    /**
     * 단어와 난이도로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingAndDifficulty(String word, Integer difficulty, Pageable pageable);

    /**
     * 단어와 정제 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingAndConfYn(String word, String confYn, Pageable pageable);

    /**
     * 단어와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingAndIsActive(String word, Boolean isActive, Pageable pageable);
    
    /**
     * 난이도와 정제 상태로 검색 (페이징)
     */
    Page<PzWord> findByDifficultyAndConfYn(Integer difficulty, String confYn, Pageable pageable);
    
    /**
     * 난이도와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByDifficultyAndIsActive(Integer difficulty, Boolean isActive, Pageable pageable);
    
    /**
     * 정제 상태와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByConfYnAndIsActive(String confYn, Boolean isActive, Pageable pageable);
    
    /**
     * 단어, 난이도, 정제 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingAndDifficultyAndConfYn(String word, Integer difficulty, String confYn, Pageable pageable);
    
    /**
     * 단어, 난이도, 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingAndDifficultyAndIsActive(String word, Integer difficulty, Boolean isActive, Pageable pageable);
    
    /**
     * 단어, 정제 상태, 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingAndConfYnAndIsActive(String word, String confYn, Boolean isActive, Pageable pageable);
    
    /**
     * 난이도, 정제 상태, 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByDifficultyAndConfYnAndIsActive(Integer difficulty, String confYn, Boolean isActive, Pageable pageable);
    
    /**
     * 모든 조건으로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingAndDifficultyAndConfYnAndIsActive(String word, Integer difficulty, String confYn, Boolean isActive, Pageable pageable);
    
    // 추가 메서드들 (8081 서비스와 동일)
    
    /**
     * 단어 존재 여부 확인
     */
    boolean existsByWord(String word);
    
    /**
     * 단어와 카테고리로 조회
     */
    Optional<PzWord> findByWordAndCategory(String word, String category);
    
    /**
     * 대소문자 무시 단어 검색 (페이징)
     */
    Page<PzWord> findByWordContainingIgnoreCase(String word, Pageable pageable);
    
    /**
     * 대소문자 무시 단어와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingIgnoreCaseAndIsActive(String word, Boolean isActive, Pageable pageable);
    
    /**
     * 대소문자 무시 단어와 난이도로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingIgnoreCaseAndDifficulty(String word, Integer difficulty, Pageable pageable);
    
    /**
     * 대소문자 무시 단어와 카테고리로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingIgnoreCaseAndCategory(String word, String category, Pageable pageable);
    
    /**
     * 대소문자 무시 단어와 난이도와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingIgnoreCaseAndDifficultyAndIsActive(String word, Integer difficulty, Boolean isActive, Pageable pageable);
    
    /**
     * 대소문자 무시 단어와 카테고리와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingIgnoreCaseAndCategoryAndIsActive(String word, String category, Boolean isActive, Pageable pageable);
    
    /**
     * 대소문자 무시 단어와 카테고리와 난이도로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingIgnoreCaseAndCategoryAndDifficulty(String word, String category, Integer difficulty, Pageable pageable);
    
    /**
     * 대소문자 무시 단어와 카테고리와 난이도와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByWordContainingIgnoreCaseAndCategoryAndDifficultyAndIsActive(String word, String category, Integer difficulty, Boolean isActive, Pageable pageable);
    
    /**
     * 카테고리로 검색 (페이징)
     */
    Page<PzWord> findByCategory(String category, Pageable pageable);
    
    /**
     * 카테고리와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByCategoryAndIsActive(String category, Boolean isActive, Pageable pageable);
    
    /**
     * 카테고리와 난이도로 검색 (페이징)
     */
    Page<PzWord> findByCategoryAndDifficulty(String category, Integer difficulty, Pageable pageable);
    
    /**
     * 카테고리와 난이도와 활성 상태로 검색 (페이징)
     */
    Page<PzWord> findByCategoryAndDifficultyAndIsActive(String category, Integer difficulty, Boolean isActive, Pageable pageable);
    
    /**
     * 활성 단어 개수 조회
     */
    long countByIsActiveTrue();
    
    /**
     * 비활성 단어 개수 조회
     */
    long countByIsActiveFalse();
    
    /**
     * 힌트가 있는 단어 개수 조회
     */
    @Query("SELECT COUNT(DISTINCT w.id) FROM PzWord w WHERE w.isActive = true AND EXISTS (SELECT 1 FROM PzHint h WHERE h.word = w)")
    long countWordsWithHints();
    
    /**
     * 모든 카테고리 목록 조회
     */
    @Query("SELECT DISTINCT w.category FROM PzWord w WHERE w.isActive = true AND w.category IS NOT NULL ORDER BY w.category")
    List<String> findDistinctCategories();
    
    /**
     * 난이도별 통계 조회
     */
    @Query("SELECT w.difficulty, COUNT(w) FROM PzWord w WHERE w.isActive = true GROUP BY w.difficulty ORDER BY w.difficulty")
    List<Object[]> getDifficultyStats();
    
    // 퍼즐 생성용 메서드들 (간단한 버전)
    @Query("SELECT w FROM PzWord w WHERE w.difficulty IN :difficulties AND w.length = :length AND w.isActive = true")
    List<PzWord> findForPuzzleGenerationByDifficultyInAndLength(@Param("difficulties") List<Integer> difficulties, @Param("length") Integer length);
    
    @Query("SELECT w FROM PzWord w WHERE w.difficulty IN :difficulties AND w.length = :length AND w.isActive = true AND w.word NOT IN :usedWords")
    List<PzWord> findForPuzzleGenerationByDifficultyInAndLengthExcludingUsed(@Param("difficulties") List<Integer> difficulties, @Param("length") Integer length, @Param("usedWords") List<String> usedWords);
    
    @Query(value = "SELECT * FROM pz_words WHERE length = :length AND difficulty IN :difficulties AND is_active = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    PzWord findByLengthAndDifficultyInAndIsActiveTrueRandom(@Param("length") Integer length, @Param("difficulties") List<Integer> difficulties);
}