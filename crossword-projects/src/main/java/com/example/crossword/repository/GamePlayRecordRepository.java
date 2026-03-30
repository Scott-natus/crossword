package com.example.crossword.repository;

import com.example.crossword.entity.GamePlayRecord;
import com.example.crossword.entity.GameSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 게임 플레이 기록을 관리하는 Repository
 * Laravel의 GamePlayRecord 모델과 동일한 기능을 제공
 */
@Repository
public interface GamePlayRecordRepository extends JpaRepository<GamePlayRecord, Long> {
    
    /**
     * 특정 게임 세션의 모든 플레이 기록 조회
     */
    List<GamePlayRecord> findByGameSession(GameSession gameSession);
    
    /**
     * 특정 게임 세션의 모든 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByGameSession(GameSession gameSession, Pageable pageable);
    
    /**
     * 특정 게임 세션 ID의 모든 플레이 기록 조회
     */
    List<GamePlayRecord> findByGameSessionId(Long gameSessionId);
    
    /**
     * 특정 게임 세션 ID의 모든 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByGameSessionId(Long gameSessionId, Pageable pageable);
    
    /**
     * 특정 게임 세션의 플레이 기록을 단계 순으로 조회
     */
    List<GamePlayRecord> findByGameSessionOrderByStepNumberAsc(GameSession gameSession);
    
    /**
     * 특정 게임 세션 ID의 플레이 기록을 단계 순으로 조회
     */
    List<GamePlayRecord> findByGameSessionIdOrderByStepNumberAsc(Long gameSessionId);
    
    /**
     * 특정 게임 세션의 플레이 기록을 시간 순으로 조회
     */
    List<GamePlayRecord> findByGameSessionOrderByCreatedAtAsc(GameSession gameSession);
    
    /**
     * 특정 게임 세션 ID의 플레이 기록을 시간 순으로 조회
     */
    List<GamePlayRecord> findByGameSessionIdOrderByCreatedAtAsc(Long gameSessionId);
    
    /**
     * 특정 액션 타입의 플레이 기록 조회
     */
    List<GamePlayRecord> findByActionType(String actionType);
    
    /**
     * 특정 액션 타입의 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByActionType(String actionType, Pageable pageable);
    
    /**
     * 특정 게임 세션의 특정 액션 타입 플레이 기록 조회
     */
    List<GamePlayRecord> findByGameSessionAndActionType(GameSession gameSession, String actionType);
    
    /**
     * 특정 게임 세션 ID의 특정 액션 타입 플레이 기록 조회
     */
    List<GamePlayRecord> findByGameSessionIdAndActionType(Long gameSessionId, String actionType);
    
    /**
     * 정답인 플레이 기록 조회
     */
    List<GamePlayRecord> findByIsCorrectTrue();
    
    /**
     * 정답인 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByIsCorrectTrue(Pageable pageable);
    
    /**
     * 오답인 플레이 기록 조회
     */
    List<GamePlayRecord> findByIsCorrectFalse();
    
    /**
     * 오답인 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByIsCorrectFalse(Pageable pageable);
    
    /**
     * 특정 게임 세션의 정답 플레이 기록 조회
     */
    List<GamePlayRecord> findByGameSessionAndIsCorrectTrue(GameSession gameSession);
    
    /**
     * 특정 게임 세션 ID의 정답 플레이 기록 조회
     */
    List<GamePlayRecord> findByGameSessionIdAndIsCorrectTrue(Long gameSessionId);
    
    /**
     * 특정 게임 세션의 오답 플레이 기록 조회
     */
    List<GamePlayRecord> findByGameSessionAndIsCorrectFalse(GameSession gameSession);
    
    /**
     * 특정 게임 세션 ID의 오답 플레이 기록 조회
     */
    List<GamePlayRecord> findByGameSessionIdAndIsCorrectFalse(Long gameSessionId);
    
    /**
     * 특정 단계 번호의 플레이 기록 조회
     */
    List<GamePlayRecord> findByStepNumber(Integer stepNumber);
    
    /**
     * 특정 단계 번호의 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByStepNumber(Integer stepNumber, Pageable pageable);
    
    /**
     * 특정 게임 세션의 특정 단계 번호 플레이 기록 조회
     */
    Optional<GamePlayRecord> findByGameSessionAndStepNumber(GameSession gameSession, Integer stepNumber);
    
    /**
     * 특정 게임 세션 ID의 특정 단계 번호 플레이 기록 조회
     */
    Optional<GamePlayRecord> findByGameSessionIdAndStepNumber(Long gameSessionId, Integer stepNumber);
    
    /**
     * 특정 소요 시간 이상의 플레이 기록 조회
     */
    List<GamePlayRecord> findByTimeSpentSecondsGreaterThanEqual(Integer timeSpentSeconds);
    
    /**
     * 특정 소요 시간 이상의 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByTimeSpentSecondsGreaterThanEqual(Integer timeSpentSeconds, Pageable pageable);
    
    /**
     * 특정 소요 시간 이하의 플레이 기록 조회
     */
    List<GamePlayRecord> findByTimeSpentSecondsLessThanEqual(Integer timeSpentSeconds);
    
    /**
     * 특정 소요 시간 이하의 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByTimeSpentSecondsLessThanEqual(Integer timeSpentSeconds, Pageable pageable);
    
    /**
     * 특정 소요 시간 범위의 플레이 기록 조회
     */
    @Query("SELECT gpr FROM GamePlayRecord gpr WHERE gpr.timeSpentSeconds BETWEEN :minTime AND :maxTime ORDER BY gpr.createdAt DESC")
    List<GamePlayRecord> findByTimeSpentSecondsRange(@Param("minTime") Integer minTime, 
                                                    @Param("maxTime") Integer maxTime);
    
    /**
     * 특정 소요 시간 범위의 플레이 기록을 페이징으로 조회
     */
    @Query("SELECT gpr FROM GamePlayRecord gpr WHERE gpr.timeSpentSeconds BETWEEN :minTime AND :maxTime ORDER BY gpr.createdAt DESC")
    Page<GamePlayRecord> findByTimeSpentSecondsRange(@Param("minTime") Integer minTime, 
                                                    @Param("maxTime") Integer maxTime, 
                                                    Pageable pageable);
    
    /**
     * 특정 날짜 이후의 플레이 기록 조회
     */
    List<GamePlayRecord> findByCreatedAtAfter(LocalDateTime createdAt);
    
    /**
     * 특정 날짜 이후의 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByCreatedAtAfter(LocalDateTime createdAt, Pageable pageable);
    
    /**
     * 특정 날짜 이전의 플레이 기록 조회
     */
    List<GamePlayRecord> findByCreatedAtBefore(LocalDateTime createdAt);
    
    /**
     * 특정 날짜 이전의 플레이 기록을 페이징으로 조회
     */
    Page<GamePlayRecord> findByCreatedAtBefore(LocalDateTime createdAt, Pageable pageable);
    
    /**
     * 특정 날짜 범위의 플레이 기록 조회
     */
    @Query("SELECT gpr FROM GamePlayRecord gpr WHERE gpr.createdAt BETWEEN :startDate AND :endDate ORDER BY gpr.createdAt DESC")
    List<GamePlayRecord> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 날짜 범위의 플레이 기록을 페이징으로 조회
     */
    @Query("SELECT gpr FROM GamePlayRecord gpr WHERE gpr.createdAt BETWEEN :startDate AND :endDate ORDER BY gpr.createdAt DESC")
    Page<GamePlayRecord> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate, 
                                              Pageable pageable);
    
    /**
     * 특정 게임 세션의 특정 날짜 범위 플레이 기록 조회
     */
    @Query("SELECT gpr FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId AND gpr.createdAt BETWEEN :startDate AND :endDate ORDER BY gpr.createdAt ASC")
    List<GamePlayRecord> findByGameSessionIdAndCreatedAtBetween(@Param("gameSessionId") Long gameSessionId, 
                                                               @Param("startDate") LocalDateTime startDate, 
                                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 게임 세션의 특정 날짜 범위 플레이 기록을 페이징으로 조회
     */
    @Query("SELECT gpr FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId AND gpr.createdAt BETWEEN :startDate AND :endDate ORDER BY gpr.createdAt ASC")
    Page<GamePlayRecord> findByGameSessionIdAndCreatedAtBetween(@Param("gameSessionId") Long gameSessionId, 
                                                               @Param("startDate") LocalDateTime startDate, 
                                                               @Param("endDate") LocalDateTime endDate, 
                                                               Pageable pageable);
    
    /**
     * 특정 게임 세션의 최대 단계 번호 조회
     */
    @Query("SELECT MAX(gpr.stepNumber) FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId")
    Optional<Integer> findMaxStepNumberByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 최소 단계 번호 조회
     */
    @Query("SELECT MIN(gpr.stepNumber) FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId")
    Optional<Integer> findMinStepNumberByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 평균 소요 시간 조회
     */
    @Query("SELECT AVG(gpr.timeSpentSeconds) FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId")
    Optional<Double> findAverageTimeSpentByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 총 소요 시간 조회
     */
    @Query("SELECT SUM(gpr.timeSpentSeconds) FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId")
    Optional<Long> findTotalTimeSpentByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 정답 개수 조회
     */
    @Query("SELECT COUNT(gpr) FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId AND gpr.isCorrect = true")
    long countCorrectAnswersByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 오답 개수 조회
     */
    @Query("SELECT COUNT(gpr) FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId AND gpr.isCorrect = false")
    long countWrongAnswersByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 특정 액션 타입 개수 조회
     */
    @Query("SELECT COUNT(gpr) FROM GamePlayRecord gpr WHERE gpr.gameSession.id = :gameSessionId AND gpr.actionType = :actionType")
    long countByGameSessionIdAndActionType(@Param("gameSessionId") Long gameSessionId, 
                                          @Param("actionType") String actionType);
    
    /**
     * 특정 게임 세션의 총 플레이 기록 개수 조회
     */
    long countByGameSessionId(Long gameSessionId);
    
    /**
     * 특정 액션 타입의 총 플레이 기록 개수 조회
     */
    long countByActionType(String actionType);
    
    /**
     * 정답 플레이 기록 개수 조회
     */
    long countByIsCorrectTrue();
    
    /**
     * 오답 플레이 기록 개수 조회
     */
    long countByIsCorrectFalse();
    
    /**
     * 특정 날짜 이후의 플레이 기록 개수 조회
     */
    long countByCreatedAtAfter(LocalDateTime createdAt);
    
    /**
     * 특정 날짜 이전의 플레이 기록 개수 조회
     */
    long countByCreatedAtBefore(LocalDateTime createdAt);
    
    /**
     * 특정 날짜 범위의 플레이 기록 개수 조회
     */
    @Query("SELECT COUNT(gpr) FROM GamePlayRecord gpr WHERE gpr.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * 액션 타입별 플레이 기록 개수 조회
     */
    @Query("SELECT gpr.actionType, COUNT(gpr) FROM GamePlayRecord gpr GROUP BY gpr.actionType ORDER BY COUNT(gpr) DESC")
    List<Object[]> countRecordsByActionType();
    
    /**
     * 정답/오답별 플레이 기록 개수 조회
     */
    @Query("SELECT gpr.isCorrect, COUNT(gpr) FROM GamePlayRecord gpr GROUP BY gpr.isCorrect ORDER BY gpr.isCorrect")
    List<Object[]> countRecordsByCorrectness();
    
    /**
     * 게임 세션별 플레이 기록 개수 조회 (상위 N개)
     */
    @Query("SELECT gpr.gameSession.id, COUNT(gpr) FROM GamePlayRecord gpr GROUP BY gpr.gameSession.id ORDER BY COUNT(gpr) DESC")
    List<Object[]> countRecordsByGameSession();
    
    /**
     * 단계 번호별 플레이 기록 개수 조회
     */
    @Query("SELECT gpr.stepNumber, COUNT(gpr) FROM GamePlayRecord gpr GROUP BY gpr.stepNumber ORDER BY gpr.stepNumber")
    List<Object[]> countRecordsByStepNumber();
}
