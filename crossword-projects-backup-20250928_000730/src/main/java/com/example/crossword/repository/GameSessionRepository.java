package com.example.crossword.repository;

import com.example.crossword.entity.GameSession;
import com.example.crossword.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 게임 세션 정보를 관리하는 Repository
 * Laravel의 GameSession 모델과 동일한 기능을 제공
 */
@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    
    /**
     * 특정 사용자의 모든 게임 세션 조회
     */
    List<GameSession> findByUserId(Long userId);
    
    /**
     * 특정 사용자의 모든 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 특정 사용자의 게임 세션을 최신순으로 조회
     */
    List<GameSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 특정 사용자의 게임 세션을 최신순으로 페이징 조회
     */
    Page<GameSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 단어의 모든 게임 세션 조회
     */
    List<GameSession> findByWord(Word word);
    
    /**
     * 특정 단어의 모든 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByWord(Word word, Pageable pageable);
    
    /**
     * 특정 단어 ID의 모든 게임 세션 조회
     */
    List<GameSession> findByWordId(Integer wordId);
    
    /**
     * 특정 단어 ID의 모든 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByWordId(Integer wordId, Pageable pageable);
    
    /**
     * 특정 사용자와 특정 단어의 게임 세션 조회
     */
    List<GameSession> findByUserIdAndWord(Long userId, Word word);
    
    /**
     * 특정 사용자와 특정 단어 ID의 게임 세션 조회
     */
    List<GameSession> findByUserIdAndWordId(Long userId, Integer wordId);
    
    /**
     * 완료된 게임 세션 조회
     */
    List<GameSession> findByIsCompletedTrue();
    
    /**
     * 완료된 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByIsCompletedTrue(Pageable pageable);
    
    /**
     * 미완료 게임 세션 조회
     */
    List<GameSession> findByIsCompletedFalse();
    
    /**
     * 미완료 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByIsCompletedFalse(Pageable pageable);
    
    /**
     * 특정 사용자의 완료된 게임 세션 조회
     */
    List<GameSession> findByUserIdAndIsCompletedTrue(Long userId);
    
    /**
     * 특정 사용자의 완료된 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByUserIdAndIsCompletedTrue(Long userId, Pageable pageable);
    
    /**
     * 특정 사용자의 미완료 게임 세션 조회
     */
    List<GameSession> findByUserIdAndIsCompletedFalse(Long userId);
    
    /**
     * 특정 사용자의 미완료 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByUserIdAndIsCompletedFalse(Long userId, Pageable pageable);
    
    /**
     * 특정 정확도 이상의 게임 세션 조회
     */
    List<GameSession> findByAccuracyRateGreaterThanEqual(BigDecimal accuracyRate);
    
    /**
     * 특정 정확도 이상의 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByAccuracyRateGreaterThanEqual(BigDecimal accuracyRate, Pageable pageable);
    
    /**
     * 특정 정확도 이하의 게임 세션 조회
     */
    List<GameSession> findByAccuracyRateLessThanEqual(BigDecimal accuracyRate);
    
    /**
     * 특정 정확도 이하의 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByAccuracyRateLessThanEqual(BigDecimal accuracyRate, Pageable pageable);
    
    /**
     * 특정 정확도 범위의 게임 세션 조회
     */
    @Query("SELECT gs FROM GameSession gs WHERE gs.accuracyRate BETWEEN :minAccuracy AND :maxAccuracy ORDER BY gs.createdAt DESC")
    List<GameSession> findByAccuracyRateRange(@Param("minAccuracy") BigDecimal minAccuracy, 
                                             @Param("maxAccuracy") BigDecimal maxAccuracy);
    
    /**
     * 특정 정확도 범위의 게임 세션을 페이징으로 조회
     */
    @Query("SELECT gs FROM GameSession gs WHERE gs.accuracyRate BETWEEN :minAccuracy AND :maxAccuracy ORDER BY gs.createdAt DESC")
    Page<GameSession> findByAccuracyRateRange(@Param("minAccuracy") BigDecimal minAccuracy, 
                                             @Param("maxAccuracy") BigDecimal maxAccuracy, 
                                             Pageable pageable);
    
    /**
     * 특정 플레이 시간 이상의 게임 세션 조회
     */
    List<GameSession> findByTotalPlayTimeGreaterThanEqual(Integer totalPlayTime);
    
    /**
     * 특정 플레이 시간 이상의 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByTotalPlayTimeGreaterThanEqual(Integer totalPlayTime, Pageable pageable);
    
    /**
     * 특정 플레이 시간 이하의 게임 세션 조회
     */
    List<GameSession> findByTotalPlayTimeLessThanEqual(Integer totalPlayTime);
    
    /**
     * 특정 플레이 시간 이하의 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByTotalPlayTimeLessThanEqual(Integer totalPlayTime, Pageable pageable);
    
    /**
     * 특정 힌트 사용 횟수 이상의 게임 세션 조회
     */
    List<GameSession> findByHintsUsedCountGreaterThanEqual(Integer hintsUsedCount);
    
    /**
     * 특정 힌트 사용 횟수 이상의 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByHintsUsedCountGreaterThanEqual(Integer hintsUsedCount, Pageable pageable);
    
    /**
     * 특정 힌트 사용 횟수 이하의 게임 세션 조회
     */
    List<GameSession> findByHintsUsedCountLessThanEqual(Integer hintsUsedCount);
    
    /**
     * 특정 힌트 사용 횟수 이하의 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByHintsUsedCountLessThanEqual(Integer hintsUsedCount, Pageable pageable);
    
    /**
     * 특정 날짜 이후의 게임 세션 조회
     */
    List<GameSession> findByCreatedAtAfter(LocalDateTime createdAt);
    
    /**
     * 특정 날짜 이후의 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByCreatedAtAfter(LocalDateTime createdAt, Pageable pageable);
    
    /**
     * 특정 날짜 이전의 게임 세션 조회
     */
    List<GameSession> findByCreatedAtBefore(LocalDateTime createdAt);
    
    /**
     * 특정 날짜 이전의 게임 세션을 페이징으로 조회
     */
    Page<GameSession> findByCreatedAtBefore(LocalDateTime createdAt, Pageable pageable);
    
    /**
     * 특정 날짜 범위의 게임 세션 조회
     */
    @Query("SELECT gs FROM GameSession gs WHERE gs.createdAt BETWEEN :startDate AND :endDate ORDER BY gs.createdAt DESC")
    List<GameSession> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 날짜 범위의 게임 세션을 페이징으로 조회
     */
    @Query("SELECT gs FROM GameSession gs WHERE gs.createdAt BETWEEN :startDate AND :endDate ORDER BY gs.createdAt DESC")
    Page<GameSession> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           Pageable pageable);
    
    /**
     * 특정 사용자의 특정 날짜 범위 게임 세션 조회
     */
    @Query("SELECT gs FROM GameSession gs WHERE gs.userId = :userId AND gs.createdAt BETWEEN :startDate AND :endDate ORDER BY gs.createdAt DESC")
    List<GameSession> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId, 
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 사용자의 특정 날짜 범위 게임 세션을 페이징으로 조회
     */
    @Query("SELECT gs FROM GameSession gs WHERE gs.userId = :userId AND gs.createdAt BETWEEN :startDate AND :endDate ORDER BY gs.createdAt DESC")
    Page<GameSession> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId, 
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate, 
                                                     Pageable pageable);
    
    /**
     * 특정 사용자의 평균 정확도 조회
     */
    @Query("SELECT AVG(gs.accuracyRate) FROM GameSession gs WHERE gs.userId = :userId AND gs.isCompleted = true")
    Optional<BigDecimal> findAverageAccuracyByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 평균 플레이 시간 조회
     */
    @Query("SELECT AVG(gs.totalPlayTime) FROM GameSession gs WHERE gs.userId = :userId AND gs.isCompleted = true")
    Optional<Double> findAveragePlayTimeByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 평균 힌트 사용 횟수 조회
     */
    @Query("SELECT AVG(gs.hintsUsedCount) FROM GameSession gs WHERE gs.userId = :userId AND gs.isCompleted = true")
    Optional<Double> findAverageHintsUsedByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 최고 정확도 조회
     */
    @Query("SELECT MAX(gs.accuracyRate) FROM GameSession gs WHERE gs.userId = :userId AND gs.isCompleted = true")
    Optional<BigDecimal> findMaxAccuracyByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 최저 정확도 조회
     */
    @Query("SELECT MIN(gs.accuracyRate) FROM GameSession gs WHERE gs.userId = :userId AND gs.isCompleted = true")
    Optional<BigDecimal> findMinAccuracyByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 총 게임 세션 수 조회
     */
    long countByUserId(Long userId);
    
    /**
     * 특정 사용자의 완료된 게임 세션 수 조회
     */
    long countByUserIdAndIsCompletedTrue(Long userId);
    
    /**
     * 특정 사용자의 미완료 게임 세션 수 조회
     */
    long countByUserIdAndIsCompletedFalse(Long userId);
    
    /**
     * 특정 단어의 총 게임 세션 수 조회
     */
    long countByWord(Word word);
    
    /**
     * 특정 단어 ID의 총 게임 세션 수 조회
     */
    long countByWordId(Integer wordId);
    
    /**
     * 특정 단어의 완료된 게임 세션 수 조회
     */
    long countByWordAndIsCompletedTrue(Word word);
    
    /**
     * 특정 단어 ID의 완료된 게임 세션 수 조회
     */
    long countByWordIdAndIsCompletedTrue(Integer wordId);
    
    /**
     * 특정 날짜 이후의 게임 세션 수 조회
     */
    long countByCreatedAtAfter(LocalDateTime createdAt);
    
    /**
     * 특정 날짜 이전의 게임 세션 수 조회
     */
    long countByCreatedAtBefore(LocalDateTime createdAt);
    
    /**
     * 특정 날짜 범위의 게임 세션 수 조회
     */
    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 사용자의 특정 날짜 범위 게임 세션 수 조회
     */
    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.userId = :userId AND gs.createdAt BETWEEN :startDate AND :endDate")
    long countByUserIdAndCreatedAtBetween(@Param("userId") Long userId, 
                                         @Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * 완료 상태별 게임 세션 수 조회
     */
    @Query("SELECT gs.isCompleted, COUNT(gs) FROM GameSession gs GROUP BY gs.isCompleted ORDER BY gs.isCompleted")
    List<Object[]> countSessionsByCompletionStatus();
    
    /**
     * 사용자별 게임 세션 수 조회 (상위 N명)
     */
    @Query("SELECT gs.userId, COUNT(gs) FROM GameSession gs GROUP BY gs.userId ORDER BY COUNT(gs) DESC")
    List<Object[]> countSessionsByUser();
    
    /**
     * 단어별 게임 세션 수 조회 (상위 N개)
     */
    @Query("SELECT gs.word.id, COUNT(gs) FROM GameSession gs GROUP BY gs.word.id ORDER BY COUNT(gs) DESC")
    List<Object[]> countSessionsByWord();
}
