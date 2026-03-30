package com.example.crossword.repository;

import com.example.crossword.entity.PuzzleGameRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 퍼즐 게임 기록 Repository
 */
@Repository
public interface PuzzleGameRecordRepository extends JpaRepository<PuzzleGameRecord, Long> {
    
    /**
     * 특정 사용자의 특정 레벨 클리어 횟수 조회
     */
    @Query("SELECT COUNT(p) FROM PuzzleGameRecord p WHERE p.userId = :userId AND p.levelPlayed = :level AND p.gameStatus = 'completed'")
    Long countCompletedGamesByUserAndLevel(@Param("userId") Long userId, @Param("level") Integer level);
    
    /**
     * 특정 사용자의 특정 레벨 게임 기록 조회
     */
    List<PuzzleGameRecord> findByUserIdAndLevelPlayedOrderByCreatedAtDesc(Long userId, Integer level);
    
    /**
     * 특정 사용자의 모든 게임 기록 조회
     */
    List<PuzzleGameRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 특정 사용자의 특정 레벨 완료 기록만 조회
     */
    List<PuzzleGameRecord> findByUserIdAndLevelPlayedAndGameStatusOrderByCreatedAtDesc(Long userId, Integer level, String gameStatus);
}
