package com.example.crossword.service;

import com.example.crossword.entity.ThemeDailyPuzzle;
import com.example.crossword.repository.ThemeDailyPuzzleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * 일일 퍼즐 서비스
 * 매일 갱신되는 테마별 퍼즐 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyPuzzleService {
    
    private final ThemeDailyPuzzleRepository themeDailyPuzzleRepository;
    private final ThemePuzzleGeneratorService themePuzzleGeneratorService;
    
    /**
     * 오늘의 테마별 퍼즐 조회
     */
    public Map<String, Object> getTodayPuzzle(String theme, LocalDate date) {
        try {
            log.info("오늘의 테마별 퍼즐 조회: {} - {}", theme, date);
            
            // 오늘 날짜의 퍼즐 조회
            Optional<ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository
                .findByThemeAndPuzzleDateAndIsActive(theme, date, true);
            
            if (puzzleOpt.isEmpty()) {
                log.warn("오늘 날짜의 {} 테마 퍼즐이 없습니다. 새로 생성합니다.", theme);
                return generateTodayPuzzle(theme, date);
            }
            
            ThemeDailyPuzzle puzzle = puzzleOpt.get();
            log.info("오늘의 테마별 퍼즐 조회 완료: {} - 퍼즐 ID: {}", theme, puzzle.getPuzzleId());
            
            // 퍼즐 데이터 조회 (퍼즐 ID를 기반으로 실제 퍼즐 데이터 조회)
            return themePuzzleGeneratorService.getPuzzleData(puzzle.getPuzzleId());
            
        } catch (Exception e) {
            log.error("오늘의 테마별 퍼즐 조회 중 오류 발생: {} - {}", theme, e.getMessage());
            throw new RuntimeException("퍼즐 조회에 실패했습니다.", e);
        }
    }
    
    /**
     * 오늘의 테마별 퍼즐 생성
     */
    @Transactional
    public Map<String, Object> generateTodayPuzzle(String theme, LocalDate date) {
        try {
            log.info("오늘의 테마별 퍼즐 생성: {} - {}", theme, date);
            
            // 기존 퍼즐 비활성화
            themeDailyPuzzleRepository.deactivateByThemeAndDate(theme, date);
            
            // 새 퍼즐 생성
            Map<String, Object> puzzleData = themePuzzleGeneratorService.generateThemePuzzle(theme);
            
            if (puzzleData == null || !(Boolean) puzzleData.get("success")) {
                throw new RuntimeException("퍼즐 생성에 실패했습니다.");
            }
            
            // 일일 퍼즐 기록 생성
            ThemeDailyPuzzle dailyPuzzle = new ThemeDailyPuzzle();
            dailyPuzzle.setTheme(theme);
            dailyPuzzle.setPuzzleDate(date);
            dailyPuzzle.setPuzzleId((Integer) puzzleData.get("puzzleId"));
            dailyPuzzle.setIsActive(true);
            
            themeDailyPuzzleRepository.save(dailyPuzzle);
            
            log.info("오늘의 테마별 퍼즐 생성 완료: {} - 퍼즐 ID: {}", theme, dailyPuzzle.getPuzzleId());
            
            return puzzleData;
            
        } catch (Exception e) {
            log.error("오늘의 테마별 퍼즐 생성 중 오류 발생: {} - {}", theme, e.getMessage());
            throw new RuntimeException("퍼즐 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 모든 테마의 오늘 퍼즐 생성
     */
    @Transactional
    public void generateAllTodayPuzzles() {
        try {
            log.info("모든 테마의 오늘 퍼즐 생성 시작");
            
            String[] themes = {"K-POP", "K-DRAMA", "K-MOVIE", "K-CULTURE", "Korean"};
            LocalDate today = LocalDate.now();
            
            for (String theme : themes) {
                try {
                    generateTodayPuzzle(theme, today);
                    log.info("테마별 퍼즐 생성 완료: {}", theme);
                } catch (Exception e) {
                    log.error("테마별 퍼즐 생성 실패: {} - {}", theme, e.getMessage());
                }
            }
            
            log.info("모든 테마의 오늘 퍼즐 생성 완료");
            
        } catch (Exception e) {
            log.error("모든 테마의 오늘 퍼즐 생성 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("전체 퍼즐 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 특정 테마의 퍼즐 상태 확인
     */
    public boolean isPuzzleAvailable(String theme, LocalDate date) {
        try {
            Optional<ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository
                .findByThemeAndPuzzleDateAndIsActive(theme, date, true);
            
            return puzzleOpt.isPresent();
            
        } catch (Exception e) {
            log.error("퍼즐 상태 확인 중 오류 발생: {} - {}", theme, e.getMessage());
            return false;
        }
    }
    
    /**
     * 테마별 퍼즐 통계 조회
     */
    public Map<String, Object> getThemePuzzleStats(String theme) {
        try {
            log.info("테마별 퍼즐 통계 조회: {}", theme);
            
            // 최근 7일간의 퍼즐 생성 현황
            LocalDate startDate = LocalDate.now().minusDays(7);
            long totalPuzzles = themeDailyPuzzleRepository.countByThemeAndPuzzleDateAfter(theme, startDate);
            long activePuzzles = themeDailyPuzzleRepository.countByThemeAndPuzzleDateAfterAndIsActive(theme, startDate, true);
            
            Map<String, Object> stats = Map.of(
                "theme", theme,
                "totalPuzzles", totalPuzzles,
                "activePuzzles", activePuzzles,
                "period", "7일"
            );
            
            log.info("테마별 퍼즐 통계 조회 완료: {} - 총 {}개, 활성 {}개", theme, totalPuzzles, activePuzzles);
            
            return stats;
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 통계 조회 중 오류 발생: {} - {}", theme, e.getMessage());
            throw new RuntimeException("퍼즐 통계 조회에 실패했습니다.", e);
        }
    }
}
