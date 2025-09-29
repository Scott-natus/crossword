package com.example.crossword.service;

import com.example.crossword.entity.GameSession;
import com.example.crossword.entity.Word;
import com.example.crossword.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 게임 세션 관리 서비스
 * 게임 세션 조회, 생성, 업데이트, 통계 등의 비즈니스 로직을 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GameSessionService {
    
    private final GameSessionRepository gameSessionRepository;
    
    /**
     * ID로 게임 세션 조회
     */
    public Optional<GameSession> getGameSessionById(Long id) {
        log.debug("ID로 게임 세션 조회: {}", id);
        return gameSessionRepository.findById(id);
    }
    
    /**
     * 특정 사용자의 모든 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByUserId(Long userId) {
        log.debug("사용자별 게임 세션 조회: {}", userId);
        return gameSessionRepository.findByUserId(userId);
    }
    
    /**
     * 특정 사용자의 모든 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsByUserId(Long userId, Pageable pageable) {
        log.debug("사용자별 게임 세션 페이징 조회: {}, {}", userId, pageable);
        return gameSessionRepository.findByUserId(userId, pageable);
    }
    
    /**
     * 특정 사용자의 게임 세션을 최신순으로 조회
     */
    public List<GameSession> getGameSessionsByUserIdOrdered(Long userId) {
        log.debug("사용자별 게임 세션 최신순 조회: {}", userId);
        return gameSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 특정 사용자의 게임 세션을 최신순으로 페이징 조회
     */
    public Page<GameSession> getGameSessionsByUserIdOrdered(Long userId, Pageable pageable) {
        log.debug("사용자별 게임 세션 최신순 페이징 조회: {}, {}", userId, pageable);
        return gameSessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 특정 단어의 모든 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByWord(Word word) {
        log.debug("단어별 게임 세션 조회: {}", word.getWord());
        return gameSessionRepository.findByWord(word);
    }
    
    /**
     * 특정 단어의 모든 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsByWord(Word word, Pageable pageable) {
        log.debug("단어별 게임 세션 페이징 조회: {}, {}", word.getWord(), pageable);
        return gameSessionRepository.findByWord(word, pageable);
    }
    
    /**
     * 특정 단어 ID의 모든 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByWordId(Integer wordId) {
        log.debug("단어 ID별 게임 세션 조회: {}", wordId);
        return gameSessionRepository.findByWordId(wordId);
    }
    
    /**
     * 특정 단어 ID의 모든 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsByWordId(Integer wordId, Pageable pageable) {
        log.debug("단어 ID별 게임 세션 페이징 조회: {}, {}", wordId, pageable);
        return gameSessionRepository.findByWordId(wordId, pageable);
    }
    
    /**
     * 특정 사용자와 특정 단어의 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByUserIdAndWord(Long userId, Word word) {
        log.debug("사용자와 단어별 게임 세션 조회: {}, {}", userId, word.getWord());
        return gameSessionRepository.findByUserIdAndWord(userId, word);
    }
    
    /**
     * 특정 사용자와 특정 단어 ID의 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByUserIdAndWordId(Long userId, Integer wordId) {
        log.debug("사용자와 단어 ID별 게임 세션 조회: {}, {}", userId, wordId);
        return gameSessionRepository.findByUserIdAndWordId(userId, wordId);
    }
    
    /**
     * 완료된 게임 세션 조회
     */
    public List<GameSession> getCompletedGameSessions() {
        log.debug("완료된 게임 세션 조회");
        return gameSessionRepository.findByIsCompletedTrue();
    }
    
    /**
     * 완료된 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getCompletedGameSessions(Pageable pageable) {
        log.debug("완료된 게임 세션 페이징 조회: {}", pageable);
        return gameSessionRepository.findByIsCompletedTrue(pageable);
    }
    
    /**
     * 미완료 게임 세션 조회
     */
    public List<GameSession> getIncompleteGameSessions() {
        log.debug("미완료 게임 세션 조회");
        return gameSessionRepository.findByIsCompletedFalse();
    }
    
    /**
     * 미완료 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getIncompleteGameSessions(Pageable pageable) {
        log.debug("미완료 게임 세션 페이징 조회: {}", pageable);
        return gameSessionRepository.findByIsCompletedFalse(pageable);
    }
    
    /**
     * 특정 사용자의 완료된 게임 세션 조회
     */
    public List<GameSession> getCompletedGameSessionsByUserId(Long userId) {
        log.debug("사용자별 완료된 게임 세션 조회: {}", userId);
        return gameSessionRepository.findByUserIdAndIsCompletedTrue(userId);
    }
    
    /**
     * 특정 사용자의 완료된 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getCompletedGameSessionsByUserId(Long userId, Pageable pageable) {
        log.debug("사용자별 완료된 게임 세션 페이징 조회: {}, {}", userId, pageable);
        return gameSessionRepository.findByUserIdAndIsCompletedTrue(userId, pageable);
    }
    
    /**
     * 특정 사용자의 미완료 게임 세션 조회
     */
    public List<GameSession> getIncompleteGameSessionsByUserId(Long userId) {
        log.debug("사용자별 미완료 게임 세션 조회: {}", userId);
        return gameSessionRepository.findByUserIdAndIsCompletedFalse(userId);
    }
    
    /**
     * 특정 사용자의 미완료 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getIncompleteGameSessionsByUserId(Long userId, Pageable pageable) {
        log.debug("사용자별 미완료 게임 세션 페이징 조회: {}, {}", userId, pageable);
        return gameSessionRepository.findByUserIdAndIsCompletedFalse(userId, pageable);
    }
    
    /**
     * 특정 정확도 이상의 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByMinAccuracy(BigDecimal accuracyRate) {
        log.debug("최소 정확도별 게임 세션 조회: {}", accuracyRate);
        return gameSessionRepository.findByAccuracyRateGreaterThanEqual(accuracyRate);
    }
    
    /**
     * 특정 정확도 이상의 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsByMinAccuracy(BigDecimal accuracyRate, Pageable pageable) {
        log.debug("최소 정확도별 게임 세션 페이징 조회: {}, {}", accuracyRate, pageable);
        return gameSessionRepository.findByAccuracyRateGreaterThanEqual(accuracyRate, pageable);
    }
    
    /**
     * 특정 정확도 이하의 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByMaxAccuracy(BigDecimal accuracyRate) {
        log.debug("최대 정확도별 게임 세션 조회: {}", accuracyRate);
        return gameSessionRepository.findByAccuracyRateLessThanEqual(accuracyRate);
    }
    
    /**
     * 특정 정확도 이하의 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsByMaxAccuracy(BigDecimal accuracyRate, Pageable pageable) {
        log.debug("최대 정확도별 게임 세션 페이징 조회: {}, {}", accuracyRate, pageable);
        return gameSessionRepository.findByAccuracyRateLessThanEqual(accuracyRate, pageable);
    }
    
    /**
     * 특정 날짜 이후의 게임 세션 조회
     */
    public List<GameSession> getGameSessionsAfterDate(LocalDateTime date) {
        log.debug("날짜 이후 게임 세션 조회: {}", date);
        return gameSessionRepository.findByCreatedAtAfter(date);
    }
    
    /**
     * 특정 날짜 이후의 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsAfterDate(LocalDateTime date, Pageable pageable) {
        log.debug("날짜 이후 게임 세션 페이징 조회: {}, {}", date, pageable);
        return gameSessionRepository.findByCreatedAtAfter(date, pageable);
    }
    
    /**
     * 특정 날짜 이전의 게임 세션 조회
     */
    public List<GameSession> getGameSessionsBeforeDate(LocalDateTime date) {
        log.debug("날짜 이전 게임 세션 조회: {}", date);
        return gameSessionRepository.findByCreatedAtBefore(date);
    }
    
    /**
     * 특정 날짜 이전의 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsBeforeDate(LocalDateTime date, Pageable pageable) {
        log.debug("날짜 이전 게임 세션 페이징 조회: {}, {}", date, pageable);
        return gameSessionRepository.findByCreatedAtBefore(date, pageable);
    }
    
    /**
     * 특정 날짜 범위의 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("날짜 범위별 게임 세션 조회: {} - {}", startDate, endDate);
        return gameSessionRepository.findByCreatedAtBetween(startDate, endDate);
    }
    
    /**
     * 특정 날짜 범위의 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("날짜 범위별 게임 세션 페이징 조회: {} - {}, {}", startDate, endDate, pageable);
        return gameSessionRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }
    
    /**
     * 특정 사용자의 특정 날짜 범위 게임 세션 조회
     */
    public List<GameSession> getGameSessionsByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("사용자와 날짜 범위별 게임 세션 조회: {}, {} - {}", userId, startDate, endDate);
        return gameSessionRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate);
    }
    
    /**
     * 특정 사용자의 특정 날짜 범위 게임 세션을 페이징으로 조회
     */
    public Page<GameSession> getGameSessionsByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("사용자와 날짜 범위별 게임 세션 페이징 조회: {}, {} - {}, {}", userId, startDate, endDate, pageable);
        return gameSessionRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate, pageable);
    }
    
    /**
     * 특정 사용자의 평균 정확도 조회
     */
    public Optional<BigDecimal> getAverageAccuracyByUserId(Long userId) {
        log.debug("사용자별 평균 정확도 조회: {}", userId);
        return gameSessionRepository.findAverageAccuracyByUserId(userId);
    }
    
    /**
     * 특정 사용자의 평균 플레이 시간 조회
     */
    public Optional<Double> getAveragePlayTimeByUserId(Long userId) {
        log.debug("사용자별 평균 플레이 시간 조회: {}", userId);
        return gameSessionRepository.findAveragePlayTimeByUserId(userId);
    }
    
    /**
     * 특정 사용자의 평균 힌트 사용 횟수 조회
     */
    public Optional<Double> getAverageHintsUsedByUserId(Long userId) {
        log.debug("사용자별 평균 힌트 사용 횟수 조회: {}", userId);
        return gameSessionRepository.findAverageHintsUsedByUserId(userId);
    }
    
    /**
     * 특정 사용자의 최고 정확도 조회
     */
    public Optional<BigDecimal> getMaxAccuracyByUserId(Long userId) {
        log.debug("사용자별 최고 정확도 조회: {}", userId);
        return gameSessionRepository.findMaxAccuracyByUserId(userId);
    }
    
    /**
     * 특정 사용자의 최저 정확도 조회
     */
    public Optional<BigDecimal> getMinAccuracyByUserId(Long userId) {
        log.debug("사용자별 최저 정확도 조회: {}", userId);
        return gameSessionRepository.findMinAccuracyByUserId(userId);
    }
    
    /**
     * 특정 사용자의 총 게임 세션 수 조회
     */
    public long getGameSessionCountByUserId(Long userId) {
        log.debug("사용자별 총 게임 세션 수 조회: {}", userId);
        return gameSessionRepository.countByUserId(userId);
    }
    
    /**
     * 특정 사용자의 완료된 게임 세션 수 조회
     */
    public long getCompletedGameSessionCountByUserId(Long userId) {
        log.debug("사용자별 완료된 게임 세션 수 조회: {}", userId);
        return gameSessionRepository.countByUserIdAndIsCompletedTrue(userId);
    }
    
    /**
     * 특정 사용자의 미완료 게임 세션 수 조회
     */
    public long getIncompleteGameSessionCountByUserId(Long userId) {
        log.debug("사용자별 미완료 게임 세션 수 조회: {}", userId);
        return gameSessionRepository.countByUserIdAndIsCompletedFalse(userId);
    }
    
    /**
     * 특정 단어의 총 게임 세션 수 조회
     */
    public long getGameSessionCountByWord(Word word) {
        log.debug("단어별 총 게임 세션 수 조회: {}", word.getWord());
        return gameSessionRepository.countByWord(word);
    }
    
    /**
     * 특정 단어 ID의 총 게임 세션 수 조회
     */
    public long getGameSessionCountByWordId(Integer wordId) {
        log.debug("단어 ID별 총 게임 세션 수 조회: {}", wordId);
        return gameSessionRepository.countByWordId(wordId);
    }
    
    /**
     * 특정 단어의 완료된 게임 세션 수 조회
     */
    public long getCompletedGameSessionCountByWord(Word word) {
        log.debug("단어별 완료된 게임 세션 수 조회: {}", word.getWord());
        return gameSessionRepository.countByWordAndIsCompletedTrue(word);
    }
    
    /**
     * 특정 단어 ID의 완료된 게임 세션 수 조회
     */
    public long getCompletedGameSessionCountByWordId(Integer wordId) {
        log.debug("단어 ID별 완료된 게임 세션 수 조회: {}", wordId);
        return gameSessionRepository.countByWordIdAndIsCompletedTrue(wordId);
    }
    
    /**
     * 특정 날짜 이후의 게임 세션 수 조회
     */
    public long getGameSessionCountAfterDate(LocalDateTime date) {
        log.debug("날짜 이후 게임 세션 수 조회: {}", date);
        return gameSessionRepository.countByCreatedAtAfter(date);
    }
    
    /**
     * 특정 날짜 이전의 게임 세션 수 조회
     */
    public long getGameSessionCountBeforeDate(LocalDateTime date) {
        log.debug("날짜 이전 게임 세션 수 조회: {}", date);
        return gameSessionRepository.countByCreatedAtBefore(date);
    }
    
    /**
     * 특정 날짜 범위의 게임 세션 수 조회
     */
    public long getGameSessionCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("날짜 범위별 게임 세션 수 조회: {} - {}", startDate, endDate);
        return gameSessionRepository.countByCreatedAtBetween(startDate, endDate);
    }
    
    /**
     * 특정 사용자의 특정 날짜 범위 게임 세션 수 조회
     */
    public long getGameSessionCountByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("사용자와 날짜 범위별 게임 세션 수 조회: {}, {} - {}", userId, startDate, endDate);
        return gameSessionRepository.countByUserIdAndCreatedAtBetween(userId, startDate, endDate);
    }
    
    /**
     * 완료 상태별 게임 세션 수 조회
     */
    public List<Object[]> getGameSessionCountByCompletionStatus() {
        log.debug("완료 상태별 게임 세션 수 조회");
        return gameSessionRepository.countSessionsByCompletionStatus();
    }
    
    /**
     * 사용자별 게임 세션 수 조회 (상위 N명)
     */
    public List<Object[]> getGameSessionCountByUser() {
        log.debug("사용자별 게임 세션 수 조회");
        return gameSessionRepository.countSessionsByUser();
    }
    
    /**
     * 단어별 게임 세션 수 조회 (상위 N개)
     */
    public List<Object[]> getGameSessionCountByWord() {
        log.debug("단어별 게임 세션 수 조회");
        return gameSessionRepository.countSessionsByWord();
    }
    
    /**
     * 게임 세션 저장
     */
    @Transactional
    public GameSession saveGameSession(GameSession gameSession) {
        log.debug("게임 세션 저장: 사용자 {}, 단어 {}", gameSession.getUserId(), gameSession.getWord().getWord());
        return gameSessionRepository.save(gameSession);
    }
    
    /**
     * 게임 세션 업데이트
     */
    @Transactional
    public GameSession updateGameSession(GameSession gameSession) {
        log.debug("게임 세션 업데이트: {}", gameSession.getId());
        return gameSessionRepository.save(gameSession);
    }
    
    /**
     * 게임 세션 삭제
     */
    @Transactional
    public void deleteGameSession(Long id) {
        log.debug("게임 세션 삭제: {}", id);
        gameSessionRepository.deleteById(id);
    }
    
    /**
     * 게임 세션 완료 처리
     */
    @Transactional
    public void completeGameSession(Long id) {
        log.debug("게임 세션 완료 처리: {}", id);
        gameSessionRepository.findById(id).ifPresent(gameSession -> {
            gameSession.completeGame();
            gameSessionRepository.save(gameSession);
        });
    }
    
    /**
     * 게임 세션 정답 추가
     */
    @Transactional
    public void addCorrectAnswer(Long id) {
        log.debug("게임 세션 정답 추가: {}", id);
        gameSessionRepository.findById(id).ifPresent(gameSession -> {
            gameSession.addCorrectAnswer();
            gameSessionRepository.save(gameSession);
        });
    }
    
    /**
     * 게임 세션 오답 추가
     */
    @Transactional
    public void addWrongAnswer(Long id) {
        log.debug("게임 세션 오답 추가: {}", id);
        gameSessionRepository.findById(id).ifPresent(gameSession -> {
            gameSession.addWrongAnswer();
            gameSessionRepository.save(gameSession);
        });
    }
    
    /**
     * 게임 세션 힌트 사용 추가
     */
    @Transactional
    public void addHintUsed(Long id) {
        log.debug("게임 세션 힌트 사용 추가: {}", id);
        gameSessionRepository.findById(id).ifPresent(gameSession -> {
            gameSession.addHintUsed();
            gameSessionRepository.save(gameSession);
        });
    }
    
    /**
     * 게임 세션 플레이 시간 업데이트
     */
    @Transactional
    public void updatePlayTime(Long id, int playTimeSeconds) {
        log.debug("게임 세션 플레이 시간 업데이트: {}, {}초", id, playTimeSeconds);
        gameSessionRepository.findById(id).ifPresent(gameSession -> {
            gameSession.updatePlayTime(playTimeSeconds);
            gameSessionRepository.save(gameSession);
        });
    }
    
    /**
     * 게임 세션 존재 여부 확인
     */
    public boolean existsGameSession(Long id) {
        log.debug("게임 세션 존재 여부 확인: {}", id);
        return gameSessionRepository.existsById(id);
    }
    
    /**
     * 새로운 게임 세션 생성
     */
    @Transactional
    public GameSession createGameSession(Long userId, Word word) {
        log.debug("새로운 게임 세션 생성: 사용자 {}, 단어 {}", userId, word.getWord());
        
        GameSession gameSession = GameSession.builder()
                .userId(userId)
                .word(word)
                .sessionStartedAt(LocalDateTime.now())
                .totalPlayTime(0)
                .accuracyRate(BigDecimal.ZERO)
                .totalCorrectAnswers(0)
                .totalWrongAnswers(0)
                .hintsUsedCount(0)
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return gameSessionRepository.save(gameSession);
    }
}
