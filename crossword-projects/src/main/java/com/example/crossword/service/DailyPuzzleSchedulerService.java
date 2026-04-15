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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 매일 갱신 퍼즐 스케줄러 서비스
 * 매일 자정에 새로운 퍼즐을 생성하고 기존 퍼즐을 비활성화
 * <p>
 * 로직은 java-projects(8080) {@code DailyPuzzleSchedulerService}와 동일하게 유지한다.
 * 스케줄 실행은 본 서비스(8081)에서만 담당한다.
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
     * 항상 최소 3일치 퍼즐이 생성되어 있도록 보장
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void generateDailyPuzzles() {
        try {
            log.info("매일 갱신 퍼즐 생성 시작: {}", LocalDateTime.now());
            
            LocalDate today = LocalDate.now();
            
            // 모든 테마에 대해 오늘부터 3일 후까지 확인 및 생성
            for (String theme : SUPPORTED_THEMES) {
                try {
                    // 오늘부터 3일 후까지 확인 (총 3일치)
                    for (int i = 0; i <= 2; i++) {
                        LocalDate targetDate = today.plusDays(i);
                        
                        // 이미 생성되어 있는지 확인 (isActive 무관)
                        Optional<ThemeDailyPuzzle> existing = themeDailyPuzzleRepository
                            .findByThemeAndPuzzleDate(theme, targetDate);
                        
                        if (!existing.isPresent() || !existing.get().getIsActive()) {
                            // 생성되지 않았거나 비활성화된 경우 생성
                            log.info("퍼즐 생성 필요: {} - {} (없음 또는 비활성화)", theme, targetDate);
                            generateThemePuzzle(theme, targetDate);
                        } else {
                            // 이미 활성화된 퍼즐이 있으면 스킵
                            log.debug("퍼즐 이미 존재: {} - {} (스킵)", theme, targetDate);
                        }
                    }
                    
                    log.info("테마별 퍼즐 생성 완료: {} (3일치 확인)", theme);
                } catch (Exception e) {
                    log.error("테마별 퍼즐 생성 실패: {} - {}", theme, e.getMessage(), e);
                }
            }
            
            log.info("매일 갱신 퍼즐 생성 완료: {}개 테마 (각 3일치 확인)", SUPPORTED_THEMES.size());
            
        } catch (Exception e) {
            log.error("매일 갱신 퍼즐 생성 중 오류 발생: {}", e.getMessage(), e);
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
     * 3일치 퍼즐 생성 상태 확인 (모니터링용)
     */
    public Map<String, Object> getThreeDayStatus() {
        try {
            log.info("3일치 퍼즐 생성 상태 확인");
            
            LocalDate today = LocalDate.now();
            Map<String, Object> result = new HashMap<>();
            int totalReady = 0;
            int totalMissing = 0;
            List<Map<String, Object>> themeStatusList = new ArrayList<>();
            
            for (String theme : SUPPORTED_THEMES) {
                Map<String, Object> themeStatus = new HashMap<>();
                int ready = 0;
                int missing = 0;
                List<LocalDate> missingDates = new ArrayList<>();
                List<LocalDate> readyDates = new ArrayList<>();
                
                // 오늘부터 3일 후까지 확인
                for (int i = 0; i <= 2; i++) {
                    LocalDate targetDate = today.plusDays(i);
                    
                    Optional<ThemeDailyPuzzle> existing = themeDailyPuzzleRepository
                        .findByThemeAndPuzzleDate(theme, targetDate);
                    
                    if (existing.isPresent() && existing.get().getIsActive()) {
                        ready++;
                        totalReady++;
                        readyDates.add(targetDate);
                    } else {
                        missing++;
                        totalMissing++;
                        missingDates.add(targetDate);
                    }
                }
                
                themeStatus.put("theme", theme);
                themeStatus.put("ready", ready);
                themeStatus.put("missing", missing);
                themeStatus.put("readyDates", readyDates);
                themeStatus.put("missingDates", missingDates);
                themeStatus.put("status", missing == 0 ? "OK" : "WARNING");
                
                themeStatusList.add(themeStatus);
            }
            
            result.put("themes", themeStatusList);
            result.put("totalReady", totalReady);
            result.put("totalMissing", totalMissing);
            result.put("totalExpected", SUPPORTED_THEMES.size() * 3);
            result.put("date", today);
            result.put("timestamp", LocalDateTime.now());
            
            log.info("3일치 퍼즐 생성 상태 확인 완료: 준비됨: {}, 누락: {}", totalReady, totalMissing);
            
            return result;
            
        } catch (Exception e) {
            log.error("3일치 퍼즐 생성 상태 확인 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("퍼즐 생성 상태 확인에 실패했습니다.", e);
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
