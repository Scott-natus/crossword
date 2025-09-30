package com.example.crossword.service;

import com.example.crossword.entity.PuzzleGameRecord;
import com.example.crossword.entity.PuzzleLevel;
import com.example.crossword.repository.PuzzleGameRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 퍼즐 게임 기록 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PuzzleGameRecordService {
    
    private final PuzzleGameRecordRepository puzzleGameRecordRepository;
    
    /**
     * 레벨 클리어 기록 저장
     */
    @Transactional
    public PuzzleGameRecord recordLevelClear(Long userId, Integer level, Map<String, Object> gameData) {
        log.debug("레벨 클리어 기록 저장: userId={}, level={}", userId, level);
        
        PuzzleGameRecord record = PuzzleGameRecord.builder()
                .userId(userId)
                .levelPlayed(level)
                .gameStatus("completed")
                .score((Integer) gameData.getOrDefault("score", 0))
                .playTime((Integer) gameData.getOrDefault("play_time", 0))
                .hintsUsed((Integer) gameData.getOrDefault("hints_used", 0))
                .wordsFound((Integer) gameData.getOrDefault("words_found", 0))
                .totalWords((Integer) gameData.getOrDefault("total_words", 0))
                .accuracy((Double) gameData.getOrDefault("accuracy", 0.0))
                .levelBefore(level)
                .levelAfter(level + 1)
                .levelUp(true)
                .gameData(convertGameDataToJson(gameData))
                .build();
        
        return puzzleGameRecordRepository.save(record);
    }
    
    /**
     * 특정 사용자의 특정 레벨 클리어 횟수 조회
     */
    public Long getClearCountByUserAndLevel(Long userId, Integer level) {
        log.debug("클리어 횟수 조회: userId={}, level={}", userId, level);
        return puzzleGameRecordRepository.countCompletedGamesByUserAndLevel(userId, level);
    }
    
    /**
     * 레벨 클리어 조건 확인
     */
    public boolean checkLevelClearCondition(Long userId, Integer level, PuzzleLevel puzzleLevel) {
        if (puzzleLevel == null || puzzleLevel.getClearCondition() == null || puzzleLevel.getClearCondition() <= 0) {
            return true; // 클리어 조건이 없으면 자유롭게 진행
        }
        
        Long clearCount = getClearCountByUserAndLevel(userId, level);
        boolean canAdvance = clearCount >= puzzleLevel.getClearCondition();
        
        log.debug("클리어 조건 확인: userId={}, level={}, clearCount={}, required={}, canAdvance={}", 
                userId, level, clearCount, puzzleLevel.getClearCondition(), canAdvance);
        
        return canAdvance;
    }
    
    /**
     * 레벨 클리어 조건 메시지 반환
     */
    public String getLevelClearConditionMessage(Long userId, Integer level, PuzzleLevel puzzleLevel) {
        if (puzzleLevel == null || puzzleLevel.getClearCondition() == null || puzzleLevel.getClearCondition() <= 0) {
            return null;
        }
        
        Long clearCount = getClearCountByUserAndLevel(userId, level);
        int remaining = Math.max(0, puzzleLevel.getClearCondition() - clearCount.intValue());
        
        if (remaining <= 0) {
            return String.format("레벨 %d 클리어 조건을 만족했습니다!", level);
        } else {
            return String.format("레벨 %d을 %d회 클리어해야 합니다. (현재: %d회, 남은 횟수: %d회)", 
                    level, puzzleLevel.getClearCondition(), clearCount, remaining);
        }
    }
    
    /**
     * 특정 사용자의 특정 레벨 게임 기록 조회
     */
    public List<PuzzleGameRecord> getGameRecordsByUserAndLevel(Long userId, Integer level) {
        return puzzleGameRecordRepository.findByUserIdAndLevelPlayedOrderByCreatedAtDesc(userId, level);
    }
    
    /**
     * 특정 사용자의 모든 게임 기록 조회
     */
    public List<PuzzleGameRecord> getAllGameRecordsByUser(Long userId) {
        return puzzleGameRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 게임 데이터를 JSON 문자열로 변환
     */
    private String convertGameDataToJson(Map<String, Object> gameData) {
        // 간단한 JSON 변환 (실제로는 ObjectMapper 사용 권장)
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : gameData.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else {
                json.append(entry.getValue());
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}
