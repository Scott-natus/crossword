package com.example.board.repository;

import com.example.board.entity.UserPuzzleCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 사용자 퍼즐 완료 기록 레포지토리
 */
@Repository
public interface UserPuzzleCompletionRepository extends JpaRepository<UserPuzzleCompletion, Integer> {
    
    /**
     * 테마와 날짜로 완료 기록 조회 (완료 시간 순)
     */
    List<UserPuzzleCompletion> findByThemeAndPuzzleDateOrderByCompletionTimeAsc(String theme, LocalDate puzzleDate);
    
    /**
     * 테마와 날짜 이후의 완료 기록 조회
     */
    List<UserPuzzleCompletion> findByThemeAndPuzzleDateAfterOrderByCompletionTimeAsc(String theme, LocalDate startDate);
    
    /**
     * 사용자별 완료 기록 조회
     */
    List<UserPuzzleCompletion> findByUserIdOrderByCompletedAtDesc(String userId);
    
    /**
     * 테마별 사용자 완료 기록 조회
     */
    List<UserPuzzleCompletion> findByUserIdAndThemeOrderByCompletedAtDesc(String userId, String theme);
    
    /**
     * 테마별 완료 기록 개수 조회
     */
    long countByThemeAndPuzzleDate(String theme, LocalDate puzzleDate);
    
    /**
     * 사용자별 완료 기록 개수 조회
     */
    long countByUserIdAndTheme(String userId, String theme);
    
    /**
     * 테마별 평균 완료 시간 조회
     */
    @Query("SELECT AVG(u.completionTime) FROM UserPuzzleCompletion u WHERE u.theme = :theme AND u.puzzleDate = :puzzleDate")
    Double findAverageCompletionTimeByThemeAndDate(@Param("theme") String theme, @Param("puzzleDate") LocalDate puzzleDate);
    
    /**
     * 테마별 최고 기록 조회
     */
    @Query("SELECT MIN(u.completionTime) FROM UserPuzzleCompletion u WHERE u.theme = :theme AND u.puzzleDate = :puzzleDate")
    Integer findBestCompletionTimeByThemeAndDate(@Param("theme") String theme, @Param("puzzleDate") LocalDate puzzleDate);
    
    /**
     * 사용자별 테마 완료 기록 통계
     */
    @Query("SELECT COUNT(u), AVG(u.completionTime), SUM(u.hintsUsed), SUM(u.wrongAttempts) " +
           "FROM UserPuzzleCompletion u WHERE u.userId = :userId AND u.theme = :theme")
    Object[] findUserStatsByTheme(@Param("userId") String userId, @Param("theme") String theme);
}
