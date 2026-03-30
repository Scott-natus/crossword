package com.example.crossword.service;

import com.example.crossword.entity.ThemeDailyPuzzle;
import com.example.crossword.repository.ThemeDailyPuzzleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/**
 * 매일 갱신 퍼즐 스케줄러 서비스
 * 매일 자정에 새로운 퍼즐을 생성하고 기존 퍼즐을 비활성화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyPuzzleSchedulerService {
    
    private final ThemeDailyPuzzleRepository themeDailyPuzzleRepository;
    private final DailyPuzzleService dailyPuzzleService;
    
    // 지원하는 테마 목록
    private static final List<String> SUPPORTED_THEMES = Arrays.asList(
        "K-POP", "K-DRAMA", "K-MOVIE", "K-CULTURE", "Korean"
    );
    
    /**
     * 매일 자정에 실행되는 퍼즐 갱신 스케줄러
     * 한국 시간 기준 자정 (UTC+9)
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void generateDailyPuzzles() {
        try {
            log.info("매일 갱신 퍼즐 생성 시작: {}", LocalDateTime.now());
            
            LocalDate today = LocalDate.now();
            
            // 모든 테마에 대해 오늘의 퍼즐 생성
            for (String theme : SUPPORTED_THEMES) {
                try {
                    generateThemePuzzle(theme, today);
                    log.info("테마별 퍼즐 생성 완료: {}", theme);
                } catch (Exception e) {
                    log.error("테마별 퍼즐 생성 실패: {} - {}", theme, e.getMessage());
                }
            }
            
            log.info("매일 갱신 퍼즐 생성 완료: {}개 테마", SUPPORTED_THEMES.size());
            
        } catch (Exception e) {
            log.error("매일 갱신 퍼즐 생성 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * 특정 테마의 오늘 퍼즐 생성
     */
    @Transactional
    public void generateThemePuzzle(String theme, LocalDate date) {
        try {
            log.info("테마별 퍼즐 생성 시작: {} - {}", theme, date);
            
            // 기존 퍼즐 비활성화
            themeDailyPuzzleRepository.deactivateByThemeAndDate(theme, date);
            
            // 새 퍼즐 생성
            dailyPuzzleService.generateTodayPuzzle(theme, date);
            
            log.info("테마별 퍼즐 생성 완료: {} - {}", theme, date);
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 생성 중 오류 발생: {} - {} - {}", theme, date, e.getMessage());
            throw new RuntimeException("테마별 퍼즐 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 수동으로 모든 테마의 퍼즐 생성 (관리자용)
     */
    @Transactional
    public void generateAllThemePuzzles() {
        try {
            log.info("수동 퍼즐 생성 시작: {}", LocalDateTime.now());
            
            LocalDate today = LocalDate.now();
            
            for (String theme : SUPPORTED_THEMES) {
                try {
                    generateThemePuzzle(theme, today);
                    log.info("수동 테마별 퍼즐 생성 완료: {}", theme);
                } catch (Exception e) {
                    log.error("수동 테마별 퍼즐 생성 실패: {} - {}", theme, e.getMessage());
                }
            }
            
            log.info("수동 퍼즐 생성 완료: {}개 테마", SUPPORTED_THEMES.size());
            
        } catch (Exception e) {
            log.error("수동 퍼즐 생성 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("수동 퍼즐 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 특정 테마의 퍼즐 상태 확인
     */
    public boolean isThemePuzzleReady(String theme, LocalDate date) {
        try {
            return dailyPuzzleService.isPuzzleAvailable(theme, date);
        } catch (Exception e) {
            log.error("테마별 퍼즐 상태 확인 중 오류 발생: {} - {} - {}", theme, date, e.getMessage());
            return false;
        }
    }
    
    /**
     * 모든 테마의 퍼즐 상태 확인
     */
    public List<String> getUnreadyThemes(LocalDate date) {
        try {
            return SUPPORTED_THEMES.stream()
                .filter(theme -> !isThemePuzzleReady(theme, date))
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("테마별 퍼즐 상태 확인 중 오류 발생: {} - {}", date, e.getMessage());
            return SUPPORTED_THEMES;
        }
    }
    
    /**
     * 퍼즐 생성 통계 조회
     */
    public java.util.Map<String, Object> getPuzzleGenerationStats() {
        try {
            log.info("퍼즐 생성 통계 조회");
            
            LocalDate today = LocalDate.now();
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            
            for (String theme : SUPPORTED_THEMES) {
                boolean isReady = isThemePuzzleReady(theme, today);
                stats.put(theme, isReady);
            }
            
            stats.put("totalThemes", SUPPORTED_THEMES.size());
            stats.put("readyThemes", stats.values().stream().mapToInt(v -> (Boolean) v ? 1 : 0).sum());
            stats.put("date", today);
            
            log.info("퍼즐 생성 통계 조회 완료: {}", stats);
            
            return stats;
            
        } catch (Exception e) {
            log.error("퍼즐 생성 통계 조회 중 오류 발생: {}", e.getMessage());
            return java.util.Map.of("error", "통계 조회에 실패했습니다.");
        }
    }
    
    /**
     * 특정 날짜의 퍼즐 생성 (관리자용)
     */
    @Transactional
    public void generatePuzzlesForDate(LocalDate date) {
        try {
            log.info("특정 날짜 퍼즐 생성 시작: {}", date);
            
            for (String theme : SUPPORTED_THEMES) {
                try {
                    generateThemePuzzle(theme, date);
                    log.info("특정 날짜 테마별 퍼즐 생성 완료: {} - {}", theme, date);
                } catch (Exception e) {
                    log.error("특정 날짜 테마별 퍼즐 생성 실패: {} - {} - {}", theme, date, e.getMessage());
                }
            }
            
            log.info("특정 날짜 퍼즐 생성 완료: {} - {}개 테마", date, SUPPORTED_THEMES.size());
            
        } catch (Exception e) {
            log.error("특정 날짜 퍼즐 생성 중 오류 발생: {} - {}", date, e.getMessage());
            throw new RuntimeException("특정 날짜 퍼즐 생성에 실패했습니다.", e);
        }
    }
}
