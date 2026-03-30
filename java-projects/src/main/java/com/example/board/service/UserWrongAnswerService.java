package com.example.board.service;

import com.example.board.entity.UserWrongAnswer;
import com.example.board.repository.UserWrongAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 오답 기록 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserWrongAnswerService {
    
    private final UserWrongAnswerRepository userWrongAnswerRepository;
    
    /**
     * 오답 기록 저장
     */
    @Transactional
    public UserWrongAnswer saveWrongAnswer(Long userId, Long wordId, String userAnswer, 
                                         String correctAnswer, String category, Integer level) {
        try {
            UserWrongAnswer wrongAnswer = UserWrongAnswer.builder()
                    .userId(userId)
                    .wordId(wordId)
                    .userAnswer(userAnswer)
                    .correctAnswer(correctAnswer)
                    .category(category)
                    .level(level)
                    .build();
            
            UserWrongAnswer saved = userWrongAnswerRepository.save(wrongAnswer);
            
            log.info("오답 기록 저장 완료: userId={}, wordId={}, userAnswer='{}', correctAnswer='{}', category='{}', level={}", 
                    userId, wordId, userAnswer, correctAnswer, category, level);
            
            return saved;
            
        } catch (Exception e) {
            log.error("오답 기록 저장 실패: userId={}, wordId={}, userAnswer='{}', error={}", 
                    userId, wordId, userAnswer, e.getMessage());
            throw new RuntimeException("오답 기록 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 특정 사용자의 오답 기록 조회
     */
    public List<UserWrongAnswer> getUserWrongAnswers(Long userId) {
        return userWrongAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 특정 사용자의 오답 기록을 페이징으로 조회
     */
    public Page<UserWrongAnswer> getUserWrongAnswers(Long userId, Pageable pageable) {
        return userWrongAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 특정 단어의 오답 기록 조회
     */
    public List<UserWrongAnswer> getWordWrongAnswers(Long wordId) {
        return userWrongAnswerRepository.findByWordIdOrderByCreatedAtDesc(wordId);
    }
    
    /**
     * 특정 카테고리의 오답 기록 조회
     */
    public List<UserWrongAnswer> getCategoryWrongAnswers(String category) {
        return userWrongAnswerRepository.findByCategoryOrderByCreatedAtDesc(category);
    }
    
    /**
     * 특정 레벨의 오답 기록 조회
     */
    public List<UserWrongAnswer> getLevelWrongAnswers(Integer level) {
        return userWrongAnswerRepository.findByLevelOrderByCreatedAtDesc(level);
    }
    
    /**
     * 특정 기간의 오답 기록 조회
     */
    public List<UserWrongAnswer> getWrongAnswersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return userWrongAnswerRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
    }
    
    /**
     * 오답으로 자주 등록된 단어들 조회 (상위 N개)
     */
    public List<Object[]> getMostWrongAnswers(Pageable pageable) {
        return userWrongAnswerRepository.findMostWrongAnswers(pageable);
    }
    
    /**
     * 카테고리별 오답 통계 조회
     */
    public List<Object[]> getWrongAnswerStatisticsByCategory() {
        return userWrongAnswerRepository.findWrongAnswerStatisticsByCategory();
    }
    
    /**
     * 특정 사용자의 총 오답 수 조회
     */
    public long getUserWrongAnswerCount(Long userId) {
        return userWrongAnswerRepository.countByUserId(userId);
    }
    
    /**
     * 특정 단어의 총 오답 수 조회
     */
    public long getWordWrongAnswerCount(Long wordId) {
        return userWrongAnswerRepository.countByWordId(wordId);
    }
    
    /**
     * 특정 카테고리의 총 오답 수 조회
     */
    public long getCategoryWrongAnswerCount(String category) {
        return userWrongAnswerRepository.countByCategory(category);
    }
    
    /**
     * 특정 레벨의 총 오답 수 조회
     */
    public long getLevelWrongAnswerCount(Integer level) {
        return userWrongAnswerRepository.countByLevel(level);
    }
    
    /**
     * 특정 사용자의 특정 단어 오답 기록 조회
     */
    public List<UserWrongAnswer> getUserWordWrongAnswers(Long userId, Long wordId) {
        return userWrongAnswerRepository.findByUserIdAndWordIdOrderByCreatedAtDesc(userId, wordId);
    }
    
    /**
     * 특정 사용자의 특정 카테고리 오답 기록 조회
     */
    public List<UserWrongAnswer> getUserCategoryWrongAnswers(Long userId, String category) {
        return userWrongAnswerRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category);
    }
    
    /**
     * 특정 사용자의 특정 레벨 오답 기록 조회
     */
    public List<UserWrongAnswer> getUserLevelWrongAnswers(Long userId, Integer level) {
        return userWrongAnswerRepository.findByUserIdAndLevelOrderByCreatedAtDesc(userId, level);
    }
    
    /**
     * 오답 통계 정보 조회
     */
    public Map<String, Object> getWrongAnswerStatistics() {
        long totalWrongAnswers = userWrongAnswerRepository.count();
        List<Object[]> categoryStats = getWrongAnswerStatisticsByCategory();
        List<Object[]> mostWrongAnswers = getMostWrongAnswers(org.springframework.data.domain.PageRequest.of(0, 10));
        
        return Map.of(
                "totalWrongAnswers", totalWrongAnswers,
                "categoryStatistics", categoryStats,
                "mostWrongAnswers", mostWrongAnswers
        );
    }
}



