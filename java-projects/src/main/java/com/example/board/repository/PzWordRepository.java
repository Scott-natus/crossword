package com.example.board.repository;

import com.example.board.entity.PzWord;
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
    
    /**
     * 단어와 카테고리로 조회 (중복 체크용)
     */
    Optional<PzWord> findByWordAndCategory(String word, String category);
    
    /**
     * 고유한 카테고리 목록 조회
     */
    @Query("SELECT DISTINCT w.category FROM PzWord w WHERE w.isActive = true ORDER BY w.category")
    List<String> findDistinctCategories();
    
    /**
     * 난이도별 통계 조회
     */
    @Query("SELECT w.difficulty, COUNT(w) FROM PzWord w WHERE w.isActive = true GROUP BY w.difficulty ORDER BY w.difficulty")
    List<Object[]> getDifficultyStats();
    
    /**
     * 활성화된 단어 수 조회
     */
    long countByIsActiveTrue();
    
    /**
     * 힌트가 있는 단어 수 조회 (라라벨과 동일)
     */
    @Query("SELECT COUNT(DISTINCT w.id) FROM PzWord w WHERE w.isActive = true AND EXISTS (SELECT 1 FROM PzHint h WHERE h.word.id = w.id)")
    long countWordsWithHints();
    
    /**
     * 난이도별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.difficulty = :difficulty AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByDifficultyAndIsActive(@Param("difficulty") Integer difficulty, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 난이도별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.difficulty = :difficulty")
    org.springframework.data.domain.Page<PzWord> findByDifficulty(@Param("difficulty") Integer difficulty, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화 상태별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByIsActive(@Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 카테고리별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.category = :category")
    org.springframework.data.domain.Page<PzWord> findByCategory(@Param("category") String category, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 카테고리와 활성화 상태별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.category = :category AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByCategoryAndIsActive(@Param("category") String category, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 카테고리와 난이도별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.category = :category AND w.difficulty = :difficulty")
    org.springframework.data.domain.Page<PzWord> findByCategoryAndDifficulty(@Param("category") String category, @Param("difficulty") Integer difficulty, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 전체 단어 수 조회
     */
    long count();
    
    /**
     * 비활성화된 단어 수 조회
     */
    long countByIsActiveFalse();
    
    /**
     * 힌트가 없는 단어 수 조회 (라라벨과 동일)
     */
    @Query("SELECT COUNT(w) FROM PzWord w WHERE w.isActive = true AND NOT EXISTS (SELECT 1 FROM PzHint h WHERE h.word.id = w.id)")
    long countWordsWithoutHints();
    
    /**
     * 단어 검색 (대소문자 무시, 활성화 상태 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndIsActive(@Param("search") String search, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search)")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCase(@Param("search") String search, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 카테고리, 난이도, 활성화 상태별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.category = :category AND w.difficulty = :difficulty AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByCategoryAndDifficultyAndIsActive(@Param("category") String category, @Param("difficulty") Integer difficulty, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 카테고리 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.category = :category")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndCategory(@Param("search") String search, @Param("category") String category, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 난이도, 활성화 상태 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.difficulty = :difficulty AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndDifficultyAndIsActive(@Param("search") String search, @Param("difficulty") Integer difficulty, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 난이도 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.difficulty = :difficulty")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndDifficulty(@Param("search") String search, @Param("difficulty") Integer difficulty, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 존재 여부 확인
     */
    boolean existsByWord(String word);
    
    /**
     * 카테고리와 단어 조합으로 존재 여부 확인
     */
    boolean existsByCategoryAndWord(String category, String word);
    
    /**
     * 단어 검색 (대소문자 무시, 카테고리, 난이도, 활성화 상태 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.category = :category AND w.difficulty = :difficulty AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndCategoryAndDifficultyAndIsActive(@Param("search") String search, @Param("category") String category, @Param("difficulty") Integer difficulty, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 카테고리, 난이도 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.category = :category AND w.difficulty = :difficulty")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndCategoryAndDifficulty(@Param("search") String search, @Param("category") String category, @Param("difficulty") Integer difficulty, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 카테고리, 활성화 상태 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.category = :category AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndCategoryAndIsActive(@Param("search") String search, @Param("category") String category, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화된 단어 조회 (페이징) - 힌트 개수 포함
     */
    @Query("SELECT w, COUNT(h) FROM PzWord w LEFT JOIN w.hints h WHERE w.isActive = true GROUP BY w.id ORDER BY COUNT(h) DESC, w.id DESC")
    org.springframework.data.domain.Page<Object[]> findActiveWordsWithHintCount(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화된 단어 검색 (페이징) - 힌트 개수 포함
     */
    @Query("SELECT w, COUNT(h) FROM PzWord w LEFT JOIN w.hints h WHERE w.isActive = true AND (LOWER(w.word) LIKE LOWER(:search) OR LOWER(w.category) LIKE LOWER(:search)) GROUP BY w.id ORDER BY COUNT(h) DESC, w.id DESC")
    org.springframework.data.domain.Page<Object[]> findActiveWordsWithSearchAndHintCount(@Param("search") String search, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화된 단어 조회 (페이징) - 힌트 보유 단어만
     */
    @Query("SELECT w, COUNT(h) FROM PzWord w INNER JOIN w.hints h WHERE w.isActive = true GROUP BY w.id ORDER BY COUNT(h) DESC, w.id DESC")
    org.springframework.data.domain.Page<Object[]> findActiveWordsWithHintCountAndHasHints(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화된 단어 조회 (페이징) - 힌트 없는 단어만
     */
    @Query("SELECT w, 0 FROM PzWord w WHERE w.isActive = true AND NOT EXISTS (SELECT 1 FROM PzHint h WHERE h.word.id = w.id) ORDER BY w.id DESC")
    org.springframework.data.domain.Page<Object[]> findActiveWordsWithHintCountAndNoHints(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화된 단어 검색 (페이징) - 힌트 보유 단어만
     */
    @Query("SELECT w, COUNT(h) FROM PzWord w INNER JOIN w.hints h WHERE w.isActive = true AND (LOWER(w.word) LIKE LOWER(:search) OR LOWER(w.category) LIKE LOWER(:search)) GROUP BY w.id ORDER BY COUNT(h) DESC, w.id DESC")
    org.springframework.data.domain.Page<Object[]> findActiveWordsWithSearchAndHintCountAndHasHints(@Param("search") String search, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화된 단어 검색 (페이징) - 힌트 없는 단어만
     */
    @Query("SELECT w, 0 FROM PzWord w WHERE w.isActive = true AND (LOWER(w.word) LIKE LOWER(:search) OR LOWER(w.category) LIKE LOWER(:search)) AND NOT EXISTS (SELECT 1 FROM PzHint h WHERE h.word.id = w.id) ORDER BY w.id DESC")
    org.springframework.data.domain.Page<Object[]> findActiveWordsWithSearchAndHintCountAndNoHints(@Param("search") String search, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화된 단어 수 조회 (힌트 개수 포함) - 페이징용
     */
    @Query("SELECT COUNT(DISTINCT w.id) FROM PzWord w LEFT JOIN w.hints h WHERE w.isActive = true")
    long countActiveWordsWithHintCount();
    
    /**
     * 활성화된 단어 검색 수 조회 (힌트 개수 포함) - 페이징용
     */
    @Query("SELECT COUNT(DISTINCT w.id) FROM PzWord w LEFT JOIN w.hints h WHERE w.isActive = true AND (LOWER(w.word) LIKE LOWER(:search) OR LOWER(w.category) LIKE LOWER(:search))")
    long countActiveWordsWithSearchAndHintCount(@Param("search") String search);
    
    /**
     * 활성화된 단어 조회 (페이징) - 기존 방식 (호환성 유지)
     */
    @Query("SELECT w FROM PzWord w WHERE w.isActive = true")
    org.springframework.data.domain.Page<PzWord> findActiveWords(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 활성화된 단어 검색 (페이징) - 기존 방식 (호환성 유지)
     */
    @Query("SELECT w FROM PzWord w WHERE w.isActive = true AND (LOWER(w.word) LIKE LOWER(:search) OR LOWER(w.category) LIKE LOWER(:search))")
    org.springframework.data.domain.Page<PzWord> findActiveWordsWithSearch(@Param("search") String search, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 카테고리별 통계 조회
     */
    @Query("SELECT w.category, COUNT(w) FROM PzWord w WHERE w.isActive = true GROUP BY w.category ORDER BY COUNT(w) DESC")
    List<Object[]> getCategoryStats();

    /**
     * 테마별 승인된 단어 조회 (K-POP 테마용)
     */
    @Query("SELECT w FROM PzWord w WHERE w.cat2 = :theme AND w.isApproved = :isApproved")
    List<PzWord> findByCat2AndIsApproved(@Param("theme") String theme, @Param("isApproved") Boolean isApproved);
    
    /**
     * 길이와 난이도 리스트로 단어 조회 (단어 추출용) - 랜덤 1개
     */
    @Query(value = "SELECT * FROM pz_words WHERE length = :length AND difficulty IN :difficulties AND is_active = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    PzWord findByLengthAndDifficultyInAndIsActiveTrueRandom(@Param("length") Integer length, @Param("difficulties") List<Integer> difficulties);

    /**
     * 길이와 난이도 리스트로 단어 조회 (단어 추출용) - 기존 방식 (호환성)
     */
    @Query("SELECT w FROM PzWord w WHERE w.length = :length AND w.difficulty IN :difficulties AND w.isActive = true")
    List<PzWord> findByLengthAndDifficultyInAndIsActiveTrue(@Param("length") Integer length, @Param("difficulties") List<Integer> difficulties);

    /**
     * 정제상태별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.confYn = :confYn")
    org.springframework.data.domain.Page<PzWord> findByConfYn(@Param("confYn") String confYn, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 정제상태 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.confYn = :confYn")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndConfYn(@Param("search") String search, @Param("confYn") String confYn, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 난이도와 정제상태별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.difficulty = :difficulty AND w.confYn = :confYn")
    org.springframework.data.domain.Page<PzWord> findByDifficultyAndConfYn(@Param("difficulty") Integer difficulty, @Param("confYn") String confYn, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 난이도, 정제상태 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.difficulty = :difficulty AND w.confYn = :confYn")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndDifficultyAndConfYn(@Param("search") String search, @Param("difficulty") Integer difficulty, @Param("confYn") String confYn, org.springframework.data.domain.Pageable pageable);

    /**
     * 난이도와 길이로 단어 조회 (퍼즐 생성용)
     */
    @Query("SELECT w FROM PzWord w WHERE w.difficulty = :difficulty AND w.length = :length AND w.isActive = true")
    List<PzWord> findByDifficultyAndLength(@Param("difficulty") Integer difficulty, @Param("length") Integer length);

    /**
     * 정제상태와 활성화상태별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.confYn = :confYn AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByConfYnAndIsActive(@Param("confYn") String confYn, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 정제상태, 활성화상태 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.confYn = :confYn AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndConfYnAndIsActive(@Param("search") String search, @Param("confYn") String confYn, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 난이도와 정제상태, 활성화상태별 단어 조회 (페이징)
     */
    @Query("SELECT w FROM PzWord w WHERE w.difficulty = :difficulty AND w.confYn = :confYn AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByDifficultyAndConfYnAndIsActive(@Param("difficulty") Integer difficulty, @Param("confYn") String confYn, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 단어 검색 (대소문자 무시, 난이도, 정제상태, 활성화상태 포함)
     */
    @Query("SELECT w FROM PzWord w WHERE LOWER(w.word) LIKE LOWER(:search) AND w.difficulty = :difficulty AND w.confYn = :confYn AND w.isActive = :isActive")
    org.springframework.data.domain.Page<PzWord> findByWordContainingIgnoreCaseAndDifficultyAndConfYnAndIsActive(@Param("search") String search, @Param("difficulty") Integer difficulty, @Param("confYn") String confYn, @Param("isActive") Boolean isActive, org.springframework.data.domain.Pageable pageable);

    /**
     * 난이도와 길이로 단어 조회 (퍼즐 생성용) - 이미 사용된 단어 제외
     */
    @Query("SELECT w FROM PzWord w WHERE w.difficulty = :difficulty AND w.length = :length AND w.isActive = true AND w.word NOT IN :usedWords")
    List<PzWord> findByDifficultyAndLengthExcludingUsed(@Param("difficulty") Integer difficulty, @Param("length") Integer length, @Param("usedWords") List<String> usedWords);

    /**
     * 퍼즐 게임 생성 전용 - 난이도 범위와 길이로 단어 조회 (getAllowedDifficulties 사용)
     */
    @Query("SELECT w FROM PzWord w WHERE w.difficulty IN :difficulties AND w.length = :length AND w.isActive = true")
    List<PzWord> findForPuzzleGenerationByDifficultyInAndLength(@Param("difficulties") List<Integer> difficulties, @Param("length") Integer length);

    /**
     * 퍼즐 게임 생성 전용 - 난이도 범위와 길이로 단어 조회 (이미 사용된 단어 제외)
     */
    @Query("SELECT w FROM PzWord w WHERE w.difficulty IN :difficulties AND w.length = :length AND w.isActive = true AND w.word NOT IN :usedWords")
    List<PzWord> findForPuzzleGenerationByDifficultyInAndLengthExcludingUsed(@Param("difficulties") List<Integer> difficulties, @Param("length") Integer length, @Param("usedWords") List<String> usedWords);

    /**
     * 교차점 음절 조건을 포함한 단어 조회 (성능 최적화) - 사용된 단어 제외
     */
    @Query(value = "SELECT * FROM pz_words WHERE length = :length AND difficulty IN :difficulties AND is_active = true AND word NOT IN :usedWords AND SUBSTRING(word, :position, 1) = :syllable ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    PzWord findByLengthAndDifficultyWithSyllable(@Param("length") Integer length, @Param("difficulties") List<Integer> difficulties, @Param("usedWords") List<String> usedWords, @Param("position") Integer position, @Param("syllable") String syllable);

    /**
     * 교차점 음절 조건을 포함한 단어 조회 (여러 교차점 지원) - 사용된 단어 제외
     */
    @Query(value = "SELECT * FROM pz_words WHERE length = :length AND difficulty IN :difficulties AND is_active = true AND word NOT IN :usedWords AND SUBSTRING(word, :position1, 1) = :syllable1 AND SUBSTRING(word, :position2, 1) = :syllable2 ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    PzWord findByLengthAndDifficultyWithTwoSyllables(@Param("length") Integer length, @Param("difficulties") List<Integer> difficulties, @Param("usedWords") List<String> usedWords, @Param("position1") Integer position1, @Param("syllable1") String syllable1, @Param("position2") Integer position2, @Param("syllable2") String syllable2);

    /**
     * 교차점 음절 조건을 포함한 단어 조회 (성능 최적화) - 사용된 단어 제외 없음
     */
    @Query(value = "SELECT * FROM pz_words WHERE length = :length AND difficulty IN :difficulties AND is_active = true AND SUBSTRING(word, :position, 1) = :syllable ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    PzWord findByLengthAndDifficultyWithSyllableNoExclude(@Param("length") Integer length, @Param("difficulties") List<Integer> difficulties, @Param("position") Integer position, @Param("syllable") String syllable);

    /**
     * 교차점 음절 조건을 포함한 단어 조회 (여러 교차점 지원) - 사용된 단어 제외 없음
     */
    @Query(value = "SELECT * FROM pz_words WHERE length = :length AND difficulty IN :difficulties AND is_active = true AND SUBSTRING(word, :position1, 1) = :syllable1 AND SUBSTRING(word, :position2, 1) = :syllable2 ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    PzWord findByLengthAndDifficultyWithTwoSyllablesNoExclude(@Param("length") Integer length, @Param("difficulties") List<Integer> difficulties, @Param("position1") Integer position1, @Param("syllable1") String syllable1, @Param("position2") Integer position2, @Param("syllable2") String syllable2);

}
