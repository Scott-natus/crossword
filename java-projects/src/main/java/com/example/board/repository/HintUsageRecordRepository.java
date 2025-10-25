package com.example.board.repository;

import com.example.board.entity.GameSession;
import com.example.board.entity.Hint;
import com.example.board.entity.HintUsageRecord;
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
 * 힌트 사용 기록을 관리하는 Repository
 * Laravel의 HintUsageRecord 모델과 동일한 기능을 제공
 */
@Repository
public interface HintUsageRecordRepository extends JpaRepository<HintUsageRecord, Long> {
    
    /**
     * 특정 게임 세션의 모든 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByGameSession(GameSession gameSession);
    
    /**
     * 특정 게임 세션의 모든 힌트 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByGameSession(GameSession gameSession, Pageable pageable);
    
    /**
     * 특정 게임 세션 ID의 모든 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByGameSessionId(Long gameSessionId);
    
    /**
     * 특정 게임 세션 ID의 모든 힌트 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByGameSessionId(Long gameSessionId, Pageable pageable);
    
    /**
     * 특정 게임 세션의 힌트 사용 기록을 사용 시간 순으로 조회
     */
    List<HintUsageRecord> findByGameSessionOrderByUsedAtAsc(GameSession gameSession);
    
    /**
     * 특정 게임 세션 ID의 힌트 사용 기록을 사용 시간 순으로 조회
     */
    List<HintUsageRecord> findByGameSessionIdOrderByUsedAtAsc(Long gameSessionId);
    
    /**
     * 특정 게임 세션의 힌트 사용 기록을 힌트 번호 순으로 조회
     */
    List<HintUsageRecord> findByGameSessionOrderByHintNumberAsc(GameSession gameSession);
    
    /**
     * 특정 게임 세션 ID의 힌트 사용 기록을 힌트 번호 순으로 조회
     */
    List<HintUsageRecord> findByGameSessionIdOrderByHintNumberAsc(Long gameSessionId);
    
    /**
     * 특정 힌트의 모든 사용 기록 조회
     */
    List<HintUsageRecord> findByHint(Hint hint);
    
    /**
     * 특정 힌트의 모든 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByHint(Hint hint, Pageable pageable);
    
    /**
     * 특정 힌트 ID의 모든 사용 기록 조회
     */
    List<HintUsageRecord> findByHintId(Integer hintId);
    
    /**
     * 특정 힌트 ID의 모든 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByHintId(Integer hintId, Pageable pageable);
    
    /**
     * 특정 힌트의 사용 기록을 사용 시간 순으로 조회
     */
    List<HintUsageRecord> findByHintOrderByUsedAtDesc(Hint hint);
    
    /**
     * 특정 힌트 ID의 사용 기록을 사용 시간 순으로 조회
     */
    List<HintUsageRecord> findByHintIdOrderByUsedAtDesc(Integer hintId);
    
    /**
     * 특정 힌트 번호의 사용 기록 조회
     */
    List<HintUsageRecord> findByHintNumber(Integer hintNumber);
    
    /**
     * 특정 힌트 번호의 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByHintNumber(Integer hintNumber, Pageable pageable);
    
    /**
     * 특정 게임 세션의 특정 힌트 번호 사용 기록 조회
     */
    List<HintUsageRecord> findByGameSessionAndHintNumber(GameSession gameSession, Integer hintNumber);
    
    /**
     * 특정 게임 세션 ID의 특정 힌트 번호 사용 기록 조회
     */
    List<HintUsageRecord> findByGameSessionIdAndHintNumber(Long gameSessionId, Integer hintNumber);
    
    /**
     * 도움이 된 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByIsHelpfulTrue();
    
    /**
     * 도움이 된 힌트 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByIsHelpfulTrue(Pageable pageable);
    
    /**
     * 도움이 되지 않은 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByIsHelpfulFalse();
    
    /**
     * 도움이 되지 않은 힌트 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByIsHelpfulFalse(Pageable pageable);
    
    /**
     * 특정 게임 세션의 도움이 된 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByGameSessionAndIsHelpfulTrue(GameSession gameSession);
    
    /**
     * 특정 게임 세션 ID의 도움이 된 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByGameSessionIdAndIsHelpfulTrue(Long gameSessionId);
    
    /**
     * 특정 게임 세션의 도움이 되지 않은 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByGameSessionAndIsHelpfulFalse(GameSession gameSession);
    
    /**
     * 특정 게임 세션 ID의 도움이 되지 않은 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByGameSessionIdAndIsHelpfulFalse(Long gameSessionId);
    
    /**
     * 특정 힌트의 도움이 된 사용 기록 조회
     */
    List<HintUsageRecord> findByHintAndIsHelpfulTrue(Hint hint);
    
    /**
     * 특정 힌트 ID의 도움이 된 사용 기록 조회
     */
    List<HintUsageRecord> findByHintIdAndIsHelpfulTrue(Integer hintId);
    
    /**
     * 특정 힌트의 도움이 되지 않은 사용 기록 조회
     */
    List<HintUsageRecord> findByHintAndIsHelpfulFalse(Hint hint);
    
    /**
     * 특정 힌트 ID의 도움이 되지 않은 사용 기록 조회
     */
    List<HintUsageRecord> findByHintIdAndIsHelpfulFalse(Integer hintId);
    
    /**
     * 특정 날짜 이후의 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByUsedAtAfter(LocalDateTime usedAt);
    
    /**
     * 특정 날짜 이후의 힌트 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByUsedAtAfter(LocalDateTime usedAt, Pageable pageable);
    
    /**
     * 특정 날짜 이전의 힌트 사용 기록 조회
     */
    List<HintUsageRecord> findByUsedAtBefore(LocalDateTime usedAt);
    
    /**
     * 특정 날짜 이전의 힌트 사용 기록을 페이징으로 조회
     */
    Page<HintUsageRecord> findByUsedAtBefore(LocalDateTime usedAt, Pageable pageable);
    
    /**
     * 특정 날짜 범위의 힌트 사용 기록 조회
     */
    @Query("SELECT hur FROM HintUsageRecord hur WHERE hur.usedAt BETWEEN :startDate AND :endDate ORDER BY hur.usedAt DESC")
    List<HintUsageRecord> findByUsedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 날짜 범위의 힌트 사용 기록을 페이징으로 조회
     */
    @Query("SELECT hur FROM HintUsageRecord hur WHERE hur.usedAt BETWEEN :startDate AND :endDate ORDER BY hur.usedAt DESC")
    Page<HintUsageRecord> findByUsedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate, 
                                            Pageable pageable);
    
    /**
     * 특정 게임 세션의 특정 날짜 범위 힌트 사용 기록 조회
     */
    @Query("SELECT hur FROM HintUsageRecord hur WHERE hur.gameSession.id = :gameSessionId AND hur.usedAt BETWEEN :startDate AND :endDate ORDER BY hur.usedAt ASC")
    List<HintUsageRecord> findByGameSessionIdAndUsedAtBetween(@Param("gameSessionId") Long gameSessionId, 
                                                             @Param("startDate") LocalDateTime startDate, 
                                                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 게임 세션의 특정 날짜 범위 힌트 사용 기록을 페이징으로 조회
     */
    @Query("SELECT hur FROM HintUsageRecord hur WHERE hur.gameSession.id = :gameSessionId AND hur.usedAt BETWEEN :startDate AND :endDate ORDER BY hur.usedAt ASC")
    Page<HintUsageRecord> findByGameSessionIdAndUsedAtBetween(@Param("gameSessionId") Long gameSessionId, 
                                                             @Param("startDate") LocalDateTime startDate, 
                                                             @Param("endDate") LocalDateTime endDate, 
                                                             Pageable pageable);
    
    /**
     * 특정 힌트의 특정 날짜 범위 사용 기록 조회
     */
    @Query("SELECT hur FROM HintUsageRecord hur WHERE hur.hint.id = :hintId AND hur.usedAt BETWEEN :startDate AND :endDate ORDER BY hur.usedAt DESC")
    List<HintUsageRecord> findByHintIdAndUsedAtBetween(@Param("hintId") Integer hintId, 
                                                      @Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 힌트의 특정 날짜 범위 사용 기록을 페이징으로 조회
     */
    @Query("SELECT hur FROM HintUsageRecord hur WHERE hur.hint.id = :hintId AND hur.usedAt BETWEEN :startDate AND :endDate ORDER BY hur.usedAt DESC")
    Page<HintUsageRecord> findByHintIdAndUsedAtBetween(@Param("hintId") Integer hintId, 
                                                      @Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate, 
                                                      Pageable pageable);
    
    /**
     * 특정 게임 세션의 최대 힌트 번호 조회
     */
    @Query("SELECT MAX(hur.hintNumber) FROM HintUsageRecord hur WHERE hur.gameSession.id = :gameSessionId")
    Optional<Integer> findMaxHintNumberByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 최소 힌트 번호 조회
     */
    @Query("SELECT MIN(hur.hintNumber) FROM HintUsageRecord hur WHERE hur.gameSession.id = :gameSessionId")
    Optional<Integer> findMinHintNumberByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 힌트 사용 횟수 조회
     */
    @Query("SELECT COUNT(hur) FROM HintUsageRecord hur WHERE hur.gameSession.id = :gameSessionId")
    long countByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 도움이 된 힌트 사용 횟수 조회
     */
    @Query("SELECT COUNT(hur) FROM HintUsageRecord hur WHERE hur.gameSession.id = :gameSessionId AND hur.isHelpful = true")
    long countHelpfulHintsByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 게임 세션의 도움이 되지 않은 힌트 사용 횟수 조회
     */
    @Query("SELECT COUNT(hur) FROM HintUsageRecord hur WHERE hur.gameSession.id = :gameSessionId AND hur.isHelpful = false")
    long countUnhelpfulHintsByGameSessionId(@Param("gameSessionId") Long gameSessionId);
    
    /**
     * 특정 힌트의 사용 횟수 조회
     */
    long countByHintId(Integer hintId);
    
    /**
     * 특정 힌트의 도움이 된 사용 횟수 조회
     */
    @Query("SELECT COUNT(hur) FROM HintUsageRecord hur WHERE hur.hint.id = :hintId AND hur.isHelpful = true")
    long countHelpfulUsagesByHintId(@Param("hintId") Integer hintId);
    
    /**
     * 특정 힌트의 도움이 되지 않은 사용 횟수 조회
     */
    @Query("SELECT COUNT(hur) FROM HintUsageRecord hur WHERE hur.hint.id = :hintId AND hur.isHelpful = false")
    long countUnhelpfulUsagesByHintId(@Param("hintId") Integer hintId);
    
    /**
     * 특정 힌트 번호의 사용 횟수 조회
     */
    long countByHintNumber(Integer hintNumber);
    
    /**
     * 특정 날짜 이후의 힌트 사용 횟수 조회
     */
    long countByUsedAtAfter(LocalDateTime usedAt);
    
    /**
     * 특정 날짜 이전의 힌트 사용 횟수 조회
     */
    long countByUsedAtBefore(LocalDateTime usedAt);
    
    /**
     * 특정 날짜 범위의 힌트 사용 횟수 조회
     */
    @Query("SELECT COUNT(hur) FROM HintUsageRecord hur WHERE hur.usedAt BETWEEN :startDate AND :endDate")
    long countByUsedAtBetween(@Param("startDate") LocalDateTime startDate, 
                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * 도움이 된 힌트 사용 횟수 조회
     */
    long countByIsHelpfulTrue();
    
    /**
     * 도움이 되지 않은 힌트 사용 횟수 조회
     */
    long countByIsHelpfulFalse();
    
    /**
     * 힌트 번호별 사용 횟수 조회
     */
    @Query("SELECT hur.hintNumber, COUNT(hur) FROM HintUsageRecord hur GROUP BY hur.hintNumber ORDER BY hur.hintNumber")
    List<Object[]> countUsagesByHintNumber();
    
    /**
     * 도움 여부별 힌트 사용 횟수 조회
     */
    @Query("SELECT hur.isHelpful, COUNT(hur) FROM HintUsageRecord hur GROUP BY hur.isHelpful ORDER BY hur.isHelpful")
    List<Object[]> countUsagesByHelpfulness();
    
    /**
     * 게임 세션별 힌트 사용 횟수 조회 (상위 N개)
     */
    @Query("SELECT hur.gameSession.id, COUNT(hur) FROM HintUsageRecord hur GROUP BY hur.gameSession.id ORDER BY COUNT(hur) DESC")
    List<Object[]> countUsagesByGameSession();
    
    /**
     * 힌트별 사용 횟수 조회 (상위 N개)
     */
    @Query("SELECT hur.hint.id, COUNT(hur) FROM HintUsageRecord hur GROUP BY hur.hint.id ORDER BY COUNT(hur) DESC")
    List<Object[]> countUsagesByHint();
    
    /**
     * 특정 힌트의 도움 비율 조회
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN hur.isHelpful = true THEN 1 END) * 100.0 / COUNT(hur) " +
           "FROM HintUsageRecord hur WHERE hur.hint.id = :hintId")
    Optional<Double> findHelpfulnessRatioByHintId(@Param("hintId") Integer hintId);
    
    /**
     * 특정 게임 세션의 힌트 도움 비율 조회
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN hur.isHelpful = true THEN 1 END) * 100.0 / COUNT(hur) " +
           "FROM HintUsageRecord hur WHERE hur.gameSession.id = :gameSessionId")
    Optional<Double> findHelpfulnessRatioByGameSessionId(@Param("gameSessionId") Long gameSessionId);
}
