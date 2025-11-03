package com.example.board.controller;

import com.example.board.service.PzWordService;
import com.example.board.service.PzHintService;
import com.example.board.service.PuzzleLevelService;
import com.example.board.service.GameSessionService;
import com.example.board.service.LevelManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 API 컨트롤러
 * 관리자 페이지에서 사용하는 API 엔드포인트들
 * 인증 체크 없이 접근 가능
 */
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@Slf4j
public class AdminApiController {
    
    private final PzWordService pzWordService;
    private final PzHintService pzHintService;
    private final PuzzleLevelService puzzleLevelService;
    private final GameSessionService gameSessionService;
    private final LevelManagementService levelManagementService;
    private final com.example.board.service.DatabaseSyncService databaseSyncService;
    private final com.example.board.service.DailyPuzzleService dailyPuzzleService;
    private final com.example.board.repository.ThemeDailyPuzzleRepository themeDailyPuzzleRepository;
    
    /**
     * 시스템 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        try {
            log.info("시스템 통계 조회 요청");
            
            Map<String, Object> stats = new HashMap<>();
            
            // 단어 통계
            long totalWords = pzWordService.getTotalWordCount();
            long activeWords = pzWordService.getActiveWordCount();
            long refinedWords = pzWordService.getRefinedWordsCount();
            long wordsWithoutHints = pzWordService.getWordsWithoutHintsCount();
            
            // 힌트 통계
            long totalHints = pzHintService.getTotalHintCount();
            
            // 레벨 통계
            long totalLevels = puzzleLevelService.getTotalPuzzleLevelCount();
            
            // 게임 세션 통계
            long totalSessions = gameSessionService.getTotalSessionCount();
            long todaySessions = gameSessionService.getTodaySessionCount();
            
            stats.put("totalWords", totalWords);
            stats.put("activeWords", activeWords);
            stats.put("refinedWords", refinedWords);
            stats.put("wordsWithoutHints", wordsWithoutHints);
            stats.put("totalHints", totalHints);
            stats.put("totalLevels", totalLevels);
            stats.put("totalSessions", totalSessions);
            stats.put("todaySessions", todaySessions);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("시스템 통계 조회 중 오류 발생: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 단어 통계 조회
     */
    @GetMapping("/words/stats")
    public ResponseEntity<Map<String, Object>> getWordStats() {
        try {
            log.info("단어 통계 조회 요청");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalActiveWords", pzWordService.getActiveWordCount());
            stats.put("refinedWords", pzWordService.getRefinedWordsCount());
            stats.put("wordsWithoutHints", pzWordService.getWordsWithoutHintsCount());
            stats.put("totalHints", pzHintService.getTotalHintCount());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 통계 조회 중 오류 발생: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "단어 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 레벨 목록 조회 (DataTables 형식) - 실제 데이터베이스에서 조회
     */
    @GetMapping("/level-management/levels-ajax")
    public ResponseEntity<Map<String, Object>> getLevelsAjax(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "100") int length,
            @RequestParam(defaultValue = "level") String orderColumn,
            @RequestParam(defaultValue = "asc") String orderDir,
            @RequestParam(defaultValue = "1") int draw) {
        try {
            log.info("레벨 목록 조회 요청 (DataTables 형식)");
            
            // 실제 데이터베이스에서 레벨 데이터 조회
            int page = start / length;
            var levels = levelManagementService.getAllLevels(page, length, orderColumn, orderDir);
            
            // 레벨 데이터를 Map으로 변환
            var levelList = levels.getContent().stream()
                .map(level -> {
                    Map<String, Object> levelMap = new HashMap<>();
                    levelMap.put("id", level.getId());
                    levelMap.put("level", level.getLevel());
                    levelMap.put("levelName", level.getLevelName());
                    levelMap.put("wordDifficulty", level.getWordDifficulty());
                    levelMap.put("hintDifficulty", level.getHintDifficulty());
                    levelMap.put("intersectionCount", level.getIntersectionCount());
                    levelMap.put("wordCount", level.getWordCount());
                    levelMap.put("timeLimit", level.getTimeLimit());
                    levelMap.put("clearCondition", level.getClearCondition());
                    levelMap.put("createdAt", level.getCreatedAt());
                    levelMap.put("updatedAt", level.getUpdatedAt());
                    return levelMap;
                })
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", levels.getTotalElements());
            response.put("recordsFiltered", levels.getTotalElements());
            response.put("data", levelList);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("레벨 목록 조회 중 오류 발생: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", 0);
            response.put("recordsFiltered", 0);
            response.put("data", new Object[0]);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 특정 레벨 조회 (8081 형식) - 실제 데이터베이스에서 조회
     */
    @GetMapping("/level-management/level/{id}")
    public ResponseEntity<Map<String, Object>> getLevel(@PathVariable Integer id) {
        try {
            log.info("레벨 조회 요청: id={}", id);
            
            // 실제 데이터베이스에서 레벨 조회
            var levelOpt = levelManagementService.getLevelByLevelNumber(id);
            if (levelOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "레벨을 찾을 수 없습니다: " + id);
                return ResponseEntity.notFound().build();
            }
            
            var level = levelOpt.get();
            Map<String, Object> levelMap = new HashMap<>();
            levelMap.put("id", level.getId());
            levelMap.put("level", level.getLevel());
            levelMap.put("levelName", level.getLevelName());
            levelMap.put("wordDifficulty", level.getWordDifficulty());
            levelMap.put("hintDifficulty", level.getHintDifficulty());
            levelMap.put("intersectionCount", level.getIntersectionCount());
            levelMap.put("wordCount", level.getWordCount());
            levelMap.put("timeLimit", level.getTimeLimit());
            levelMap.put("clearCondition", level.getClearCondition());
            levelMap.put("createdAt", level.getCreatedAt());
            levelMap.put("updatedAt", level.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levelMap);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("레벨 조회 중 오류 발생: id={}, error={}", id, e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "레벨 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 로컬(개발 서버) 테이블 목록 조회
     */
    @GetMapping("/db-sync/local-tables")
    public ResponseEntity<Map<String, Object>> getLocalTables() {
        try {
            log.info("로컬 테이블 목록 조회 요청");
            List<Map<String, Object>> tables = databaseSyncService.getLocalTables();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tables);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("로컬 테이블 목록 조회 중 오류 발생: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "테이블 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 원격(운영 서버) 테이블 목록 조회
     */
    @GetMapping("/db-sync/remote-tables")
    public ResponseEntity<Map<String, Object>> getRemoteTables() {
        try {
            log.info("원격 테이블 목록 조회 요청");
            List<Map<String, Object>> tables = databaseSyncService.getRemoteTables();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tables);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("원격 테이블 목록 조회 중 오류 발생: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "테이블 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 테이블 동기화 실행
     */
    @PostMapping("/db-sync/sync/{tableName}")
    public ResponseEntity<Map<String, Object>> syncTable(@PathVariable String tableName) {
        try {
            log.info("테이블 동기화 요청: {}", tableName);
            Map<String, Object> result = databaseSyncService.syncTable(tableName);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("테이블 동기화 중 오류 발생 (테이블: {}): {}", tableName, e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "동기화 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 시스템 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "Board System Admin");
            health.put("version", "1.0.0");
            health.put("timestamp", System.currentTimeMillis());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", health);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("시스템 상태 확인 중 오류 발생: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "시스템 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 테마별 퍼즐 목록 조회 (테마별 그룹화)
     */
    @GetMapping("/theme-puzzles")
    public ResponseEntity<Map<String, Object>> getThemePuzzles(
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) Integer days) {
        try {
            log.info("테마별 퍼즐 목록 조회 요청 - 테마: {}, 일수: {}", theme, days);
            
            java.time.LocalDate startDate = java.time.LocalDate.now();
            if (days != null && days > 0) {
                startDate = startDate.minusDays(days);
            } else {
                startDate = startDate.minusDays(30); // 기본 30일
            }
            
            java.util.List<String> themes = java.util.Arrays.asList("K-POP", "K-DRAMA", "K-MOVIE", "K-CULTURE");
            java.util.Map<String, Object> result = new HashMap<>();
            java.util.List<java.util.Map<String, Object>> themePuzzleList = new java.util.ArrayList<>();
            
            for (String t : themes) {
                if (theme != null && !theme.equals(t)) {
                    continue;
                }
                
                java.util.List<com.example.board.entity.ThemeDailyPuzzle> puzzles = themeDailyPuzzleRepository
                    .findByThemeAndPuzzleDateAfterOrderByPuzzleDateDesc(t, startDate);
                
                java.util.List<java.util.Map<String, Object>> puzzleList = new java.util.ArrayList<>();
                for (com.example.board.entity.ThemeDailyPuzzle puzzle : puzzles) {
                    java.util.Map<String, Object> puzzleMap = new HashMap<>();
                    puzzleMap.put("id", puzzle.getId());
                    puzzleMap.put("theme", puzzle.getTheme());
                    puzzleMap.put("puzzleDate", puzzle.getPuzzleDate().toString());
                    puzzleMap.put("puzzleId", puzzle.getPuzzleId());
                    puzzleMap.put("isActive", puzzle.getIsActive());
                    puzzleMap.put("createdAt", puzzle.getCreatedAt());
                    puzzleMap.put("updatedAt", puzzle.getUpdatedAt());
                    
                    // 퍼즐 데이터에서 단어 수 추출
                    java.util.Map<String, Object> puzzleData = puzzle.getPuzzleDataAsMap();
                    if (puzzleData != null && puzzleData.containsKey("words")) {
                        @SuppressWarnings("unchecked")
                        java.util.List<java.util.Map<String, Object>> words = 
                            (java.util.List<java.util.Map<String, Object>>) puzzleData.get("words");
                        puzzleMap.put("wordCount", words != null ? words.size() : 0);
                    } else {
                        puzzleMap.put("wordCount", 0);
                    }
                    
                    puzzleList.add(puzzleMap);
                }
                
                java.util.Map<String, Object> themeData = new HashMap<>();
                themeData.put("theme", t);
                themeData.put("puzzleCount", puzzles.size());
                themeData.put("puzzles", puzzleList);
                themePuzzleList.add(themeData);
            }
            
            result.put("success", true);
            result.put("data", themePuzzleList);
            result.put("startDate", startDate.toString());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 테마별 퍼즐 재생성
     */
    @PostMapping("/theme-puzzles/{theme}/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateThemePuzzle(
            @PathVariable String theme,
            @RequestParam(required = false) String date) {
        try {
            log.info("테마별 퍼즐 재생성 요청 - 테마: {}, 날짜: {}", theme, date);
            
            java.time.LocalDate puzzleDate;
            if (date != null && !date.isEmpty()) {
                puzzleDate = java.time.LocalDate.parse(date);
            } else {
                puzzleDate = java.time.LocalDate.now();
            }
            
            // 퍼즐 재생성
            Map<String, Object> puzzleData = dailyPuzzleService.generateTodayPuzzle(theme, puzzleDate);
            
            if (puzzleData != null && Boolean.TRUE.equals(puzzleData.get("success"))) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "퍼즐이 성공적으로 재생성되었습니다.");
                response.put("theme", theme);
                response.put("puzzleDate", puzzleDate.toString());
                response.put("puzzleId", puzzleData.get("puzzleId"));
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "퍼즐 재생성에 실패했습니다.");
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 재생성 중 오류 발생: 테마={}, 날짜={}, error={}", theme, date, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 재생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
