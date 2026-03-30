package com.example.board.service;

import com.example.board.entity.ThemeDailyPuzzle;
import com.example.board.repository.ThemeDailyPuzzleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 테마별 퍼즐 초기화 서비스
 * 시스템 시작 시 또는 관리자 요청 시 최소 3일치 퍼즐을 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThemePuzzleInitializerService {
    
    private final ThemeDailyPuzzleRepository themeDailyPuzzleRepository;
    private final DailyPuzzleService dailyPuzzleService;
    
    // 지원하는 테마 목록 (테마별 게임용)
    private static final List<String> THEME_PUZZLE_THEMES = Arrays.asList(
        "K-DRAMA", "K-POP", "K-MOVIE", "K-CULTURE"
    );
    
    /**
     * 기본 3일치 퍼즐 초기화
     * 오늘부터 3일 후까지 각 테마별로 퍼즐 생성
     */
    @Transactional
    public Map<String, Object> initializePuzzles(int days) {
        try {
            log.info("퍼즐 초기화 시작: {}일치 생성", days);
            
            LocalDate today = LocalDate.now();
            Map<String, Object> result = new HashMap<>();
            int totalCreated = 0;
            int totalSkipped = 0;
            int totalFailed = 0;
            
            for (String theme : THEME_PUZZLE_THEMES) {
                Map<String, Object> themeResult = new HashMap<>();
                int created = 0;
                int skipped = 0;
                int failed = 0;
                
                try {
                    // 지정된 일수만큼 확인 및 생성
                    for (int i = 0; i < days; i++) {
                        LocalDate targetDate = today.plusDays(i);
                        
                        try {
                            // 이미 생성되어 있는지 확인 (isActive 무관)
                            Optional<ThemeDailyPuzzle> existing = themeDailyPuzzleRepository
                                .findByThemeAndPuzzleDate(theme, targetDate);
                            
                            if (!existing.isPresent() || !existing.get().getIsActive()) {
                                // 생성되지 않았거나 비활성화된 경우 생성
                                log.info("퍼즐 생성: {} - {}", theme, targetDate);
                                dailyPuzzleService.generateTodayPuzzle(theme, targetDate);
                                created++;
                                totalCreated++;
                            } else {
                                // 이미 활성화된 퍼즐이 있으면 스킵
                                log.debug("퍼즐 이미 존재: {} - {} (스킵)", theme, targetDate);
                                skipped++;
                                totalSkipped++;
                            }
                        } catch (Exception e) {
                            log.error("퍼즐 생성 실패: {} - {} - {}", theme, targetDate, e.getMessage(), e);
                            failed++;
                            totalFailed++;
                        }
                    }
                    
                    themeResult.put("created", created);
                    themeResult.put("skipped", skipped);
                    themeResult.put("failed", failed);
                    result.put(theme, themeResult);
                    
                    log.info("테마별 퍼즐 초기화 완료: {} - 생성: {}, 스킵: {}, 실패: {}", 
                        theme, created, skipped, failed);
                    
                } catch (Exception e) {
                    log.error("테마별 퍼즐 초기화 실패: {} - {}", theme, e.getMessage(), e);
                    themeResult.put("error", e.getMessage());
                    result.put(theme, themeResult);
                    totalFailed += days;
                }
            }
            
            result.put("totalCreated", totalCreated);
            result.put("totalSkipped", totalSkipped);
            result.put("totalFailed", totalFailed);
            result.put("totalThemes", THEME_PUZZLE_THEMES.size());
            result.put("days", days);
            result.put("timestamp", LocalDateTime.now());
            
            log.info("퍼즐 초기화 완료: 총 생성: {}, 스킵: {}, 실패: {}", 
                totalCreated, totalSkipped, totalFailed);
            
            return result;
            
        } catch (Exception e) {
            log.error("퍼즐 초기화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("퍼즐 초기화에 실패했습니다.", e);
        }
    }
    
    /**
     * 기본 3일치 퍼즐 초기화 (기본값 사용)
     */
    @Transactional
    public Map<String, Object> initializePuzzles() {
        return initializePuzzles(3);
    }
    
    /**
     * 특정 테마의 퍼즐 초기화
     */
    @Transactional
    public Map<String, Object> initializeThemePuzzles(String theme, int days) {
        try {
            log.info("특정 테마 퍼즐 초기화 시작: {} - {}일치", theme, days);
            
            if (!THEME_PUZZLE_THEMES.contains(theme)) {
                throw new IllegalArgumentException("지원하지 않는 테마입니다: " + theme);
            }
            
            LocalDate today = LocalDate.now();
            Map<String, Object> result = new HashMap<>();
            int created = 0;
            int skipped = 0;
            int failed = 0;
            
            // 지정된 일수만큼 확인 및 생성
            for (int i = 0; i < days; i++) {
                LocalDate targetDate = today.plusDays(i);
                
                try {
                    // 이미 생성되어 있는지 확인 (isActive 무관)
                    Optional<ThemeDailyPuzzle> existing = themeDailyPuzzleRepository
                        .findByThemeAndPuzzleDate(theme, targetDate);
                    
                    if (!existing.isPresent() || !existing.get().getIsActive()) {
                        // 생성되지 않았거나 비활성화된 경우 생성
                        log.info("퍼즐 생성: {} - {}", theme, targetDate);
                        dailyPuzzleService.generateTodayPuzzle(theme, targetDate);
                        created++;
                    } else {
                        // 이미 활성화된 퍼즐이 있으면 스킵
                        log.debug("퍼즐 이미 존재: {} - {} (스킵)", theme, targetDate);
                        skipped++;
                    }
                } catch (Exception e) {
                    log.error("퍼즐 생성 실패: {} - {} - {}", theme, targetDate, e.getMessage(), e);
                    failed++;
                }
            }
            
            result.put("theme", theme);
            result.put("created", created);
            result.put("skipped", skipped);
            result.put("failed", failed);
            result.put("days", days);
            result.put("timestamp", LocalDateTime.now());
            
            log.info("특정 테마 퍼즐 초기화 완료: {} - 생성: {}, 스킵: {}, 실패: {}", 
                theme, created, skipped, failed);
            
            return result;
            
        } catch (Exception e) {
            log.error("특정 테마 퍼즐 초기화 중 오류 발생: {} - {}", theme, e.getMessage(), e);
            throw new RuntimeException("특정 테마 퍼즐 초기화에 실패했습니다.", e);
        }
    }
    
    /**
     * 퍼즐 초기화 상태 확인
     */
    public Map<String, Object> getInitializationStatus(int days) {
        try {
            log.info("퍼즐 초기화 상태 확인: {}일치", days);
            
            LocalDate today = LocalDate.now();
            Map<String, Object> result = new HashMap<>();
            int totalReady = 0;
            int totalMissing = 0;
            
            for (String theme : THEME_PUZZLE_THEMES) {
                Map<String, Object> themeStatus = new HashMap<>();
                int ready = 0;
                int missing = 0;
                List<LocalDate> missingDates = new java.util.ArrayList<>();
                
                // 지정된 일수만큼 확인
                for (int i = 0; i < days; i++) {
                    LocalDate targetDate = today.plusDays(i);
                    
                    Optional<ThemeDailyPuzzle> existing = themeDailyPuzzleRepository
                        .findByThemeAndPuzzleDate(theme, targetDate);
                    
                    if (existing.isPresent() && existing.get().getIsActive()) {
                        ready++;
                        totalReady++;
                    } else {
                        missing++;
                        totalMissing++;
                        missingDates.add(targetDate);
                    }
                }
                
                themeStatus.put("ready", ready);
                themeStatus.put("missing", missing);
                themeStatus.put("missingDates", missingDates);
                result.put(theme, themeStatus);
            }
            
            result.put("totalReady", totalReady);
            result.put("totalMissing", totalMissing);
            result.put("totalThemes", THEME_PUZZLE_THEMES.size());
            result.put("days", days);
            result.put("timestamp", LocalDateTime.now());
            
            log.info("퍼즐 초기화 상태 확인 완료: 준비됨: {}, 누락: {}", totalReady, totalMissing);
            
            return result;
            
        } catch (Exception e) {
            log.error("퍼즐 초기화 상태 확인 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("퍼즐 초기화 상태 확인에 실패했습니다.", e);
        }
    }
}

