package com.example.board.service;

import com.example.board.entity.ThemeDailyPuzzle;
import com.example.board.repository.ThemeDailyPuzzleRepository;
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
     * 미리 생성된 퍼즐을 조회하거나, 없으면 생성 (fallback)
     * TODO: Phase 2에서 퍼즐 데이터 완전한 저장/조회 로직 구현 예정
     */
    public Map<String, Object> getTodayPuzzle(String theme, LocalDate date) {
        try {
            log.info("오늘의 테마별 퍼즐 조회: {} - {}", theme, date);
            
            // 오늘 날짜의 퍼즐 조회 (미리 생성된 퍼즐)
            Optional<ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository
                .findByThemeAndPuzzleDateAndIsActive(theme, date, true);
            
            if (puzzleOpt.isPresent()) {
                // 이미 생성된 퍼즐이 있으면 조회
                ThemeDailyPuzzle puzzle = puzzleOpt.get();
                log.info("기존 퍼즐 조회: {} - 퍼즐 ID: {}", theme, puzzle.getPuzzleId());
                
                // TODO: Phase 2에서 실제 퍼즐 데이터 저장/조회 로직 구현
                // 현재는 puzzleId만 있고 실제 데이터가 없으므로 재생성
                // return themePuzzleGeneratorService.getPuzzleData(puzzle.getPuzzleId());
                
                // Phase 2 완성 전까지는 재생성 (실제로는 스케줄러가 미리 생성해야 함)
                log.warn("퍼즐 데이터 저장 로직 미완성으로 재생성: {}", theme);
                return generateTodayPuzzle(theme, date);
            }
            
            // 퍼즐이 없으면 생성 (fallback - 스케줄러가 실행되지 않았을 경우)
            log.info("퍼즐이 없어 새로 생성: {} - {}", theme, date);
            return generateTodayPuzzle(theme, date);
            
        } catch (Exception e) {
            log.error("오늘의 테마별 퍼즐 조회 중 오류 발생: {} - {}", theme, e.getMessage(), e);
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
            
            // 새 퍼즐 생성
            Map<String, Object> puzzleData = themePuzzleGeneratorService.generateThemePuzzle(theme);
            
            if (puzzleData == null || !(Boolean) puzzleData.get("success")) {
                throw new RuntimeException("퍼즐 생성에 실패했습니다.");
            }
            
            // 기존 퍼즐 조회 (UNIQUE 제약조건 때문에 theme + puzzle_date 조합은 하나만 존재)
            // isActive 여부와 관계없이 조회하여 UNIQUE 제약조건 위반 방지
            Optional<ThemeDailyPuzzle> existingPuzzleOpt = themeDailyPuzzleRepository
                .findByThemeAndPuzzleDate(theme, date);
            
            ThemeDailyPuzzle dailyPuzzle;
            if (existingPuzzleOpt.isPresent()) {
                // 기존 레코드 업데이트
                dailyPuzzle = existingPuzzleOpt.get();
                dailyPuzzle.setPuzzleId((Integer) puzzleData.get("puzzleId"));
                dailyPuzzle.setIsActive(true);
                log.info("기존 퍼즐 레코드 업데이트: {} - {}", theme, date);
            } else {
                // 새 레코드 생성
                dailyPuzzle = new ThemeDailyPuzzle();
                dailyPuzzle.setTheme(theme);
                dailyPuzzle.setPuzzleDate(date);
                dailyPuzzle.setPuzzleId((Integer) puzzleData.get("puzzleId"));
                dailyPuzzle.setIsActive(true);
                log.info("새 퍼즐 레코드 생성: {} - {}", theme, date);
            }
            
            themeDailyPuzzleRepository.save(dailyPuzzle);
            
            log.info("오늘의 테마별 퍼즐 생성 완료: {} - 퍼즐 ID: {}", theme, dailyPuzzle.getPuzzleId());
            
            return puzzleData;
            
        } catch (Exception e) {
            log.error("오늘의 테마별 퍼즐 생성 중 오류 발생: {} - {}", theme, e.getMessage(), e);
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
