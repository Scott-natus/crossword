package com.example.board.service;

import com.example.board.entity.FailedWordExtraction;
import com.example.board.repository.FailedWordExtractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FailedWordExtractionService {
    
    private static final Logger log = LoggerFactory.getLogger(FailedWordExtractionService.class);
    
    @Autowired
    private FailedWordExtractionRepository failedWordExtractionRepository;
    
    /**
     * 실패한 단어 추출 정보를 저장
     */
    public FailedWordExtraction saveFailedExtraction(
            Integer templateId, Integer level, Integer wordDifficulty, 
            Integer hintDifficulty, Integer intersectionCount,
            Integer failedWordId, Map<String, Object> failedWordPosition,
            String failureReason, Map<Integer, String> confirmedWords,
            Object intersectionRequirements, Integer retryCount) {
        
        try {
            FailedWordExtraction failedExtraction = new FailedWordExtraction(
                templateId, level, wordDifficulty, hintDifficulty, intersectionCount,
                failedWordId, failedWordPosition, failureReason, confirmedWords,
                intersectionRequirements, retryCount
            );
            
            FailedWordExtraction saved = failedWordExtractionRepository.save(failedExtraction);
            log.info("실패한 단어 추출 정보 저장 완료 - ID: {}, 레벨: {}, 실패 단어 ID: {}, 실패 이유: {}", 
                saved.getId(), level, failedWordId, failureReason);
            
            return saved;
        } catch (Exception e) {
            log.error("실패한 단어 추출 정보 저장 실패", e);
            return null;
        }
    }
    
    /**
     * 독립 단어 추출 실패 정보 저장
     */
    public void saveIndependentWordFailure(
            Integer templateId, Integer level, Integer wordDifficulty, 
            Integer hintDifficulty, Integer intersectionCount,
            Integer failedWordId, Map<String, Object> failedWordPosition,
            Map<Integer, String> confirmedWords, Integer retryCount) {
        
        String failureReason = String.format("독립 단어 추출 실패 - 길이: %s, 난이도: %s", 
            failedWordPosition.get("length"), wordDifficulty);
        
        saveFailedExtraction(templateId, level, wordDifficulty, hintDifficulty, 
            intersectionCount, failedWordId, failedWordPosition, failureReason, 
            confirmedWords, null, retryCount);
    }
    
    /**
     * 교차점 단어 추출 실패 정보 저장
     */
    public void saveIntersectionWordFailure(
            Integer templateId, Integer level, Integer wordDifficulty, 
            Integer hintDifficulty, Integer intersectionCount,
            Integer failedWordId, Map<String, Object> failedWordPosition,
            String failureReason, Map<Integer, String> confirmedWords,
            List<Map<String, Object>> intersectionRequirements, Integer retryCount) {
        
        saveFailedExtraction(templateId, level, wordDifficulty, hintDifficulty, 
            intersectionCount, failedWordId, failedWordPosition, failureReason, 
            confirmedWords, intersectionRequirements, retryCount);
    }
    
    /**
     * 특정 레벨의 실패 기록 조회
     */
    @Transactional(readOnly = true)
    public List<FailedWordExtraction> getFailuresByLevel(Integer level) {
        return failedWordExtractionRepository.findByLevelOrderByCreatedAtDesc(level);
    }
    
    /**
     * 특정 템플릿의 실패 기록 조회
     */
    @Transactional(readOnly = true)
    public List<FailedWordExtraction> getFailuresByTemplate(Integer templateId) {
        return failedWordExtractionRepository.findByTemplateIdOrderByCreatedAtDesc(templateId);
    }
    
    /**
     * 특정 기간의 실패 기록 조회
     */
    @Transactional(readOnly = true)
    public List<FailedWordExtraction> getFailuresByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return failedWordExtractionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
    }
    
    /**
     * 특정 실패 이유로 실패한 기록 조회
     */
    @Transactional(readOnly = true)
    public List<FailedWordExtraction> getFailuresByReason(String failureReason) {
        return failedWordExtractionRepository.findByFailureReasonContainingOrderByCreatedAtDesc(failureReason);
    }
    
    /**
     * 특정 단어 ID로 실패한 기록 조회
     */
    @Transactional(readOnly = true)
    public List<FailedWordExtraction> getFailuresByWordId(Integer wordId) {
        return failedWordExtractionRepository.findByFailedWordIdOrderByCreatedAtDesc(wordId);
    }
    
    /**
     * 최근 N개의 실패 기록 조회
     */
    @Transactional(readOnly = true)
    public List<FailedWordExtraction> getRecentFailures(int limit) {
        return failedWordExtractionRepository.findTopNByOrderByCreatedAtDesc(limit);
    }
    
    /**
     * 레벨별 실패 횟수 통계
     */
    @Transactional(readOnly = true)
    public List<Object[]> getFailureCountByLevel() {
        return failedWordExtractionRepository.getFailureCountByLevel();
    }
    
    /**
     * 실패 이유별 통계
     */
    @Transactional(readOnly = true)
    public List<Object[]> getFailureCountByReason() {
        return failedWordExtractionRepository.getFailureCountByReason();
    }
    
    /**
     * 특정 레벨에서 가장 많이 실패한 단어 ID 조회
     */
    @Transactional(readOnly = true)
    public List<Object[]> getMostFailedWordsByLevel(Integer level) {
        return failedWordExtractionRepository.getMostFailedWordsByLevel(level);
    }
    
    /**
     * 실패 통계 요약 정보
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFailureSummary() {
        List<Object[]> levelStats = getFailureCountByLevel();
        List<Object[]> reasonStats = getFailureCountByReason();
        List<FailedWordExtraction> recentFailures = getRecentFailures(10);
        
        return Map.of(
            "levelStats", levelStats,
            "reasonStats", reasonStats,
            "recentFailures", recentFailures,
            "totalFailures", failedWordExtractionRepository.count()
        );
    }
}
