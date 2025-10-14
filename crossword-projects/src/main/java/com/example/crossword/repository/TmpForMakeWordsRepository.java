package com.example.crossword.repository;

import com.example.crossword.entity.TmpForMakeWords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TmpForMakeWords 엔티티 리포지토리
 */
@Repository
public interface TmpForMakeWordsRepository extends JpaRepository<TmpForMakeWords, Integer> {
    
    /**
     * 힌트가 생성되지 않은 단어들 조회
     * @return hint_yn = false인 단어 목록
     */
    @Query("SELECT t FROM TmpForMakeWords t WHERE t.hintYn = false ORDER BY t.regdt ASC")
    List<TmpForMakeWords> findWordsWithoutHints();
    
    /**
     * 특정 카테고리의 힌트가 생성되지 않은 단어들 조회
     * @param category 카테고리
     * @return 해당 카테고리의 hint_yn = false인 단어 목록
     */
    @Query("SELECT t FROM TmpForMakeWords t WHERE t.category = :category AND t.hintYn = false ORDER BY t.regdt ASC")
    List<TmpForMakeWords> findWordsWithoutHintsByCategory(@Param("category") String category);
    
    /**
     * 힌트가 생성되지 않은 단어 개수 조회
     * @return hint_yn = false인 단어 개수
     */
    @Query("SELECT COUNT(t) FROM TmpForMakeWords t WHERE t.hintYn = false")
    long countWordsWithoutHints();
    
    /**
     * 특정 단어가 이미 존재하는지 확인
     * @param words 단어
     * @return 존재 여부
     */
    boolean existsByWords(String words);
    
    /**
     * 카테고리별 단어 개수 조회
     * @return 카테고리별 단어 개수
     */
    @Query("SELECT t.category, COUNT(t) FROM TmpForMakeWords t GROUP BY t.category ORDER BY COUNT(t) DESC")
    List<Object[]> countWordsByCategory();

    /**
     * 주어진 단어 목록 중에서 이미 존재하는 단어들 조회 (배치 중복 체크)
     * @param words 체크할 단어 목록
     * @return 이미 존재하는 단어 목록
     */
    @Query("SELECT t.words FROM TmpForMakeWords t WHERE t.words IN :words")
    List<String> findExistingWords(@Param("words") List<String> words);

    /**
     * 자음-모음 조합 중에서 성공한 조합들 조회
     * @return 성공한 조합 목록 (예: "ㄱㅏ", "ㄱㅑ" 등)
     */
    @Query("SELECT DISTINCT SUBSTRING(t.category, 6) FROM TmpForMakeWords t WHERE t.category LIKE '자음모음:%'")
    List<String> findSuccessfulCombinations();
    
    /**
     * 최근 등록된 단어들을 등록일시 내림차순으로 조회
     * @param limit 조회할 개수
     * @return 최근 등록된 단어 목록
     */
    @Query("SELECT t FROM TmpForMakeWords t ORDER BY t.regdt DESC")
    List<TmpForMakeWords> findTopNByOrderByRegdtDesc(int limit);
}
