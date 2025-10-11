package com.example.crossword.repository;

import com.example.crossword.entity.UserWrongAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 오답 기록 Repository
 */
@Repository
public interface UserWrongAnswerRepository extends JpaRepository<UserWrongAnswer, Long> {
    
    /**
     * 특정 사용자의 오답 기록 조회
     */
    List<UserWrongAnswer> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 특정 사용자의 오답 기록을 페이징으로 조회
     */
    Page<UserWrongAnswer> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 단어의 오답 기록 조회
     */
    List<UserWrongAnswer> findByWordIdOrderByCreatedAtDesc(Long wordId);
    
    /**
     * 특정 카테고리의 오답 기록 조회
     */
    List<UserWrongAnswer> findByCategoryOrderByCreatedAtDesc(String category);
    
    /**
     * 특정 레벨의 오답 기록 조회
     */
    List<UserWrongAnswer> findByLevelOrderByCreatedAtDesc(Integer level);
    
    /**
     * 특정 기간의 오답 기록 조회
     */
    List<UserWrongAnswer> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 오답으로 자주 등록된 단어들 조회 (상위 N개)
     */
    @Query("SELECT u.correctAnswer, u.category, COUNT(*) as wrongCount, COUNT(DISTINCT u.userId) as userCount " +
           "FROM UserWrongAnswer u " +
           "GROUP BY u.correctAnswer, u.category " +
           "ORDER BY wrongCount DESC, userCount DESC")
    List<Object[]> findMostWrongAnswers(Pageable pageable);
    
    /**
     * 카테고리별 오답 통계 조회
     */
    @Query("SELECT u.category, COUNT(*) as totalWrong, COUNT(DISTINCT u.correctAnswer) as wrongWords, COUNT(DISTINCT u.userId) as users " +
           "FROM UserWrongAnswer u " +
           "GROUP BY u.category " +
           "ORDER BY totalWrong DESC")
    List<Object[]> findWrongAnswerStatisticsByCategory();
    
    /**
     * 특정 사용자의 총 오답 수 조회
     */
    long countByUserId(Long userId);
    
    /**
     * 특정 단어의 총 오답 수 조회
     */
    long countByWordId(Long wordId);
    
    /**
     * 특정 카테고리의 총 오답 수 조회
     */
    long countByCategory(String category);
    
    /**
     * 특정 레벨의 총 오답 수 조회
     */
    long countByLevel(Integer level);
    
    /**
     * 특정 사용자의 특정 단어 오답 기록 조회
     */
    List<UserWrongAnswer> findByUserIdAndWordIdOrderByCreatedAtDesc(Long userId, Long wordId);
    
    /**
     * 특정 사용자의 특정 카테고리 오답 기록 조회
     */
    List<UserWrongAnswer> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, String category);
    
    /**
     * 특정 사용자의 특정 레벨 오답 기록 조회
     */
    List<UserWrongAnswer> findByUserIdAndLevelOrderByCreatedAtDesc(Long userId, Integer level);
}
