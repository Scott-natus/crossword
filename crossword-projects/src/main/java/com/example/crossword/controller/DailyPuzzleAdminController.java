package com.example.crossword.controller;

import com.example.crossword.service.DailyPuzzleSchedulerService;
import com.example.crossword.service.DailyPuzzleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 매일 갱신 퍼즐 관리 컨트롤러
 * 관리자용 퍼즐 생성 및 관리 API
 */
@RestController
@RequestMapping("/K-CrossWord/admin/daily-puzzles")
@RequiredArgsConstructor
@Slf4j
public class DailyPuzzleAdminController {
    
    private final DailyPuzzleSchedulerService dailyPuzzleSchedulerService;
    private final DailyPuzzleService dailyPuzzleService;
    
    /**
     * 모든 테마의 오늘 퍼즐 생성
     */
    @PostMapping("/generate-all")
    public ResponseEntity<Map<String, Object>> generateAllPuzzles() {
        try {
            log.info("관리자 퍼즐 생성 요청");
            
            dailyPuzzleSchedulerService.generateAllThemePuzzles();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "모든 테마의 퍼즐이 성공적으로 생성되었습니다.",
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("관리자 퍼즐 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "퍼즐 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 특정 테마의 오늘 퍼즐 생성
     */
    @PostMapping("/generate/{theme}")
    public ResponseEntity<Map<String, Object>> generateThemePuzzle(@PathVariable String theme) {
        try {
            log.info("관리자 테마별 퍼즐 생성 요청: {}", theme);
            
            LocalDate today = LocalDate.now();
            dailyPuzzleSchedulerService.generateThemePuzzle(theme, today);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", theme + " 테마의 퍼즐이 성공적으로 생성되었습니다.",
                "theme", theme,
                "date", today,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("관리자 테마별 퍼즐 생성 중 오류 발생: {} - {}", theme, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "테마별 퍼즐 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 특정 날짜의 퍼즐 생성
     */
    @PostMapping("/generate-for-date")
    public ResponseEntity<Map<String, Object>> generatePuzzlesForDate(@RequestParam String date) {
        try {
            log.info("관리자 특정 날짜 퍼즐 생성 요청: {}", date);
            
            LocalDate targetDate = LocalDate.parse(date);
            dailyPuzzleSchedulerService.generatePuzzlesForDate(targetDate);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "지정된 날짜의 퍼즐이 성공적으로 생성되었습니다.",
                "date", targetDate,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("관리자 특정 날짜 퍼즐 생성 중 오류 발생: {} - {}", date, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "특정 날짜 퍼즐 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 퍼즐 생성 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPuzzleStatus() {
        try {
            log.info("퍼즐 생성 상태 확인 요청");
            
            Map<String, Object> stats = dailyPuzzleSchedulerService.getPuzzleGenerationStats();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats,
                "timestamp", java.time.LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("퍼즐 생성 상태 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "퍼즐 상태 확인에 실패했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 특정 테마의 퍼즐 상태 확인
     */
    @GetMapping("/status/{theme}")
    public ResponseEntity<Map<String, Object>> getThemePuzzleStatus(@PathVariable String theme) {
        try {
            log.info("테마별 퍼즐 상태 확인 요청: {}", theme);
            
            LocalDate today = LocalDate.now();
            boolean isReady = dailyPuzzleSchedulerService.isThemePuzzleReady(theme, today);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "theme", theme,
                "date", today,
                "isReady", isReady,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 상태 확인 중 오류 발생: {} - {}", theme, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "테마별 퍼즐 상태 확인에 실패했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 준비되지 않은 테마 목록 조회
     */
    @GetMapping("/unready-themes")
    public ResponseEntity<Map<String, Object>> getUnreadyThemes() {
        try {
            log.info("준비되지 않은 테마 목록 조회 요청");
            
            LocalDate today = LocalDate.now();
            java.util.List<String> unreadyThemes = dailyPuzzleSchedulerService.getUnreadyThemes(today);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "date", today,
                "unreadyThemes", unreadyThemes,
                "count", unreadyThemes.size(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("준비되지 않은 테마 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "준비되지 않은 테마 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 테마별 퍼즐 통계 조회
     */
    @GetMapping("/stats/{theme}")
    public ResponseEntity<Map<String, Object>> getThemePuzzleStats(@PathVariable String theme) {
        try {
            log.info("테마별 퍼즐 통계 조회 요청: {}", theme);
            
            Map<String, Object> stats = dailyPuzzleService.getThemePuzzleStats(theme);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats,
                "timestamp", java.time.LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 통계 조회 중 오류 발생: {} - {}", theme, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "테마별 퍼즐 통계 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }
}
