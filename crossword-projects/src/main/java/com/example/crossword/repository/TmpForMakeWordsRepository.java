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
}
