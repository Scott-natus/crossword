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
import java.util.Optional;

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
    private final com.example.board.service.ThemePuzzleInitializerService themePuzzleInitializerService;
    private final com.example.board.service.ThemePuzzleEditService themePuzzleEditService;
    private final com.example.board.service.DailyPuzzleSchedulerService dailyPuzzleSchedulerService;
    private final com.example.board.repository.UserPuzzleCompletionRepository userPuzzleCompletionRepository;
    private final com.example.board.repository.PzWordRepository pzWordRepository;
    private final com.example.board.repository.PzHintRepository pzHintRepository;
    private final com.example.board.service.HintGeneratorManagementService hintGeneratorManagementService;
    
    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String geminiApiKey;
    
    @org.springframework.beans.factory.annotation.Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent}")
    private String geminiApiUrl;
    
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
     * 전체 테이블 동기화 실행
     */
    @PostMapping("/db-sync/sync-all")
    public ResponseEntity<Map<String, Object>> syncAllTables() {
        try {
            log.info("전체 테이블 동기화 요청");
            Map<String, Object> result = databaseSyncService.syncAllTables();
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("전체 테이블 동기화 중 오류 발생: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "전체 동기화 중 오류가 발생했습니다: " + e.getMessage());
            
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
     * 테마별 퍼즐 목록 조회 (테마별 그룹화, 각 테마별 최신 5개만)
     */
    @GetMapping("/theme-puzzles")
    public ResponseEntity<Map<String, Object>> getThemePuzzles(
            @RequestParam(required = false) String theme) {
        try {
            log.info("테마별 퍼즐 목록 조회 요청 - 테마: {} (각 테마별 최신 5개)", theme);
            
            java.util.List<String> themes = java.util.Arrays.asList("K-POP", "K-DRAMA", "K-MOVIE", "K-CULTURE");
            java.util.Map<String, Object> result = new HashMap<>();
            java.util.List<java.util.Map<String, Object>> themePuzzleList = new java.util.ArrayList<>();
            
            for (String t : themes) {
                if (theme != null && !theme.equals(t)) {
                    continue;
                }
                
                // 각 테마별로 최신 5개만 조회
                java.util.List<com.example.board.entity.ThemeDailyPuzzle> puzzles = themeDailyPuzzleRepository
                    .findTop5ByThemeOrderByPuzzleDateDesc(t);
                
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
                themeData.put("puzzleCount", puzzleList.size());
                themeData.put("puzzles", puzzleList);
                themePuzzleList.add(themeData);
            }
            
            result.put("success", true);
            result.put("data", themePuzzleList);
            
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
    
    /**
     * 테마별 퍼즐 초기화 (3일치 미리 생성)
     */
    @PostMapping("/theme-puzzles/initialize")
    public ResponseEntity<Map<String, Object>> initializeThemePuzzles(
            @RequestParam(required = false) Integer days) {
        try {
            log.info("테마별 퍼즐 초기화 요청 - 일수: {}", days);
            
            int targetDays = (days != null && days > 0) ? days : 3; // 기본값 3일
            
            Map<String, Object> result = themePuzzleInitializerService.initializePuzzles(targetDays);
            result.put("success", true);
            result.put("message", "퍼즐 초기화가 완료되었습니다.");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 초기화 중 오류 발생: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 초기화 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 특정 테마의 퍼즐 초기화
     */
    @PostMapping("/theme-puzzles/{theme}/initialize")
    public ResponseEntity<Map<String, Object>> initializeThemePuzzle(
            @PathVariable String theme,
            @RequestParam(required = false) Integer days) {
        try {
            log.info("특정 테마 퍼즐 초기화 요청 - 테마: {}, 일수: {}", theme, days);
            
            int targetDays = (days != null && days > 0) ? days : 3; // 기본값 3일
            
            Map<String, Object> result = themePuzzleInitializerService.initializeThemePuzzles(theme, targetDays);
            result.put("success", true);
            result.put("message", "퍼즐 초기화가 완료되었습니다.");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("특정 테마 퍼즐 초기화 중 오류 발생: 테마={}, error={}", theme, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 초기화 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 퍼즐 초기화 상태 확인
     */
    @GetMapping("/theme-puzzles/initialize/status")
    public ResponseEntity<Map<String, Object>> getInitializationStatus(
            @RequestParam(required = false) Integer days) {
        try {
            log.info("퍼즐 초기화 상태 확인 요청 - 일수: {}", days);
            
            int targetDays = (days != null && days > 0) ? days : 3; // 기본값 3일
            
            Map<String, Object> result = themePuzzleInitializerService.getInitializationStatus(targetDays);
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("퍼즐 초기화 상태 확인 중 오류 발생: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 초기화 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 퍼즐 상세 정보 조회
     */
    @GetMapping("/theme-puzzles/{id}/detail")
    public ResponseEntity<Map<String, Object>> getPuzzleDetail(@PathVariable Integer id) {
        try {
            log.info("퍼즐 상세 정보 조회 요청 - ID: {}", id);
            
            Map<String, Object> detail = themePuzzleEditService.getPuzzleDetail(id);
            detail.put("success", true);
            
            return ResponseEntity.ok(detail);
            
        } catch (Exception e) {
            log.error("퍼즐 상세 정보 조회 중 오류 발생: ID={}, error={}", id, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 템플릿 수정
     */
    @PutMapping("/theme-puzzles/{id}/template")
    public ResponseEntity<Map<String, Object>> updateTemplate(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("템플릿 수정 요청 - 퍼즐 ID: {}, 요청: {}", id, request);
            
            Integer templateId = (Integer) request.get("templateId");
            Boolean regenerateWords = request.containsKey("regenerateWords") 
                ? (Boolean) request.get("regenerateWords") 
                : false;
            
            if (templateId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "템플릿 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = themePuzzleEditService.updateTemplate(id, templateId, regenerateWords);
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("템플릿 수정 중 오류 발생: 퍼즐 ID={}, error={}", id, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "템플릿 수정 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 단어 수정
     */
    @PutMapping("/theme-puzzles/{id}/word/{wordIndex}")
    public ResponseEntity<Map<String, Object>> updateWord(
            @PathVariable Integer id,
            @PathVariable Integer wordIndex,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("단어 수정 요청 - 퍼즐 ID: {}, 단어 인덱스: {}, 요청: {}", id, wordIndex, request);
            
            String newWord = (String) request.get("word");
            String newHint = request.containsKey("hint") ? (String) request.get("hint") : null;
            
            if (newWord == null || newWord.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "단어가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = themePuzzleEditService.updateWord(id, wordIndex, newWord, newHint);
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("단어 수정 중 오류 발생: 퍼즐 ID={}, 단어 인덱스={}, error={}", id, wordIndex, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "단어 수정 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 단어별 힌트 조회 (pz_hints 테이블에서 조회)
     */
    @GetMapping("/theme-puzzles/{id}/word-hints")
    public ResponseEntity<Map<String, Object>> getWordHints(
            @PathVariable Integer id,
            @RequestParam Integer wordId) {
        try {
            log.info("단어별 힌트 조회 요청 - 퍼즐 ID: {}, 단어 ID: {}", id, wordId);
            
            List<com.example.board.entity.PzHint> hints = pzHintService.getHintsByWordId(wordId);
            
            List<Map<String, Object>> hintsList = new java.util.ArrayList<>();
            for (com.example.board.entity.PzHint hint : hints) {
                Map<String, Object> hintMap = new HashMap<>();
                hintMap.put("id", hint.getId());
                hintMap.put("hint_text", hint.getHintText());
                hintMap.put("difficulty", hint.getDifficulty());
                hintMap.put("is_primary", hint.getIsPrimary());
                hintsList.add(hintMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hints", hintsList);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어별 힌트 조회 중 오류 발생: 퍼즐 ID={}, 단어 ID={}, error={}", id, wordId, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 힌트 수정 (여러 힌트를 한 번에 수정)
     */
    @PutMapping("/theme-puzzles/{id}/hints")
    public ResponseEntity<Map<String, Object>> updateHints(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("힌트 수정 요청 - 퍼즐 ID: {}, 요청: {}", id, request);
            
            String word = (String) request.get("word");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hints = (List<Map<String, Object>>) request.get("hints");
            
            if (word == null || word.isEmpty() || hints == null || hints.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "단어와 힌트 목록이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = themePuzzleEditService.updateHints(id, word, hints);
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("힌트 수정 중 오류 발생: 퍼즐 ID={}, error={}", id, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 수정 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 힌트 수정 (단일 힌트 - 기존 호환성 유지)
     */
    @PutMapping("/theme-puzzles/{id}/hint")
    public ResponseEntity<Map<String, Object>> updateHint(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("힌트 수정 요청 - 퍼즐 ID: {}, 요청: {}", id, request);
            
            String word = (String) request.get("word");
            String newHint = (String) request.get("hint");
            
            if (word == null || word.isEmpty() || newHint == null || newHint.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "단어와 힌트가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = themePuzzleEditService.updateHint(id, word, newHint);
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("힌트 수정 중 오류 발생: 퍼즐 ID={}, error={}", id, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 수정 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 테마별 퍼즐 통계 조회
     */
    @GetMapping("/theme-puzzles/{id}/stats")
    public ResponseEntity<Map<String, Object>> getPuzzleStats(@PathVariable Integer id) {
        try {
            log.info("퍼즐 통계 조회 요청 - 퍼즐 ID: {}", id);
            
            Optional<com.example.board.entity.ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository.findById(id);
            if (puzzleOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "퍼즐을 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(response);
            }
            
            com.example.board.entity.ThemeDailyPuzzle puzzle = puzzleOpt.get();
            String theme = puzzle.getTheme();
            java.time.LocalDate puzzleDate = puzzle.getPuzzleDate();
            
            // 완료 기록 통계
            long participantCount = userPuzzleCompletionRepository.countByThemeAndPuzzleDate(theme, puzzleDate);
            Double avgCompletionTime = userPuzzleCompletionRepository.findAverageCompletionTimeByThemeAndDate(theme, puzzleDate);
            Integer bestTime = userPuzzleCompletionRepository.findBestCompletionTimeByThemeAndDate(theme, puzzleDate);
            
            // 완료 기록 목록 조회 (평균 힌트 사용, 평균 오답 횟수 계산용)
            List<com.example.board.entity.UserPuzzleCompletion> completions = 
                userPuzzleCompletionRepository.findByThemeAndPuzzleDateOrderByCompletionTimeAsc(theme, puzzleDate);
            
            double avgHintsUsed = 0;
            double avgWrongAttempts = 0;
            if (!completions.isEmpty()) {
                int totalHints = completions.stream().mapToInt(c -> c.getHintsUsed() != null ? c.getHintsUsed() : 0).sum();
                int totalWrong = completions.stream().mapToInt(c -> c.getWrongAttempts() != null ? c.getWrongAttempts() : 0).sum();
                avgHintsUsed = (double) totalHints / completions.size();
                avgWrongAttempts = (double) totalWrong / completions.size();
            }
            
            // 통계 데이터 구성
            Map<String, Object> stats = new HashMap<>();
            stats.put("theme", theme);
            stats.put("puzzleDate", puzzleDate.toString());
            stats.put("participantCount", participantCount);
            stats.put("avgCompletionTime", avgCompletionTime != null ? Math.round(avgCompletionTime) : 0);
            stats.put("bestTime", bestTime != null ? bestTime : 0);
            stats.put("avgHintsUsed", Math.round(avgHintsUsed * 10) / 10.0);
            stats.put("avgWrongAttempts", Math.round(avgWrongAttempts * 10) / 10.0);
            stats.put("completionRate", 0); // 완료율은 전체 접근자 수가 없어서 일단 0으로 표시
            
            // SNS 공유 통계 (현재는 로그만 있으므로 0으로 표시)
            stats.put("shareCount", 0);
            stats.put("facebookShares", 0);
            stats.put("twitterShares", 0);
            stats.put("kakaoShares", 0);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 통계 조회 중 오류 발생: 퍼즐 ID={}, error={}", id, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 퍼즐 생성 상태 모니터링 (3일치)
     */
    @GetMapping("/theme-puzzles/monitor")
    public ResponseEntity<Map<String, Object>> getPuzzleGenerationMonitor() {
        try {
            log.info("퍼즐 생성 상태 모니터링 요청");
            
            Map<String, Object> result = dailyPuzzleSchedulerService.getThreeDayStatus();
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("퍼즐 생성 상태 모니터링 중 오류 발생: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 생성 상태 모니터링 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 단어 검색 API
     */
    @GetMapping("/words/search")
    public ResponseEntity<Map<String, Object>> searchWords(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("단어 검색 요청 - query: {}, page: {}, size: {}", query, page, size);
            
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            org.springframework.data.domain.Page<com.example.board.entity.PzWord> wordPage;
            
            if (query != null && !query.trim().isEmpty()) {
                wordPage = pzWordRepository.findByWordContainingIgnoreCaseAndIsActive(
                    query.trim(), true, pageable);
            } else {
                List<com.example.board.entity.PzWord> words = pzWordRepository.findByIsActiveTrue();
                // List를 Page로 변환
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), words.size());
                List<com.example.board.entity.PzWord> pageContent = words.subList(start, end);
                wordPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, words.size());
            }
            
            List<Map<String, Object>> wordsList = new java.util.ArrayList<>();
            for (com.example.board.entity.PzWord word : wordPage.getContent()) {
                Map<String, Object> wordMap = new HashMap<>();
                wordMap.put("id", word.getId());
                wordMap.put("word", word.getWord());
                wordMap.put("length", word.getLength());
                wordMap.put("category", word.getCategory());
                wordMap.put("difficulty", word.getDifficulty());
                wordMap.put("isActive", word.getIsActive());
                wordsList.add(wordMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("words", wordsList);
            response.put("totalElements", wordPage.getTotalElements());
            response.put("totalPages", wordPage.getTotalPages());
            response.put("currentPage", wordPage.getNumber());
            response.put("pageSize", wordPage.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 검색 중 오류 발생: query={}, error={}", query, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "단어 검색 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 재미나이 API로 단어+힌트 생성 (프롬프트 기반)
     */
    @PostMapping("/words/generate-from-prompt")
    public ResponseEntity<Map<String, Object>> generateWordFromPrompt(
            @RequestBody Map<String, Object> request) {
        try {
            log.info("재미나이 API로 단어+힌트 생성 요청 - prompt: {}", request.get("prompt"));
            
            String prompt = (String) request.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "프롬프트가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 재미나이 API 호출하여 단어 추출
            
            // 단어 추출 프롬프트 구성
            String wordExtractionPrompt = "다음 프롬프트에서 십자낱말 퍼즐에 적합한 한글 단어 하나를 추출해주세요.\n\n" +
                "프롬프트: " + prompt + "\n\n" +
                "**응답 형식 (다른 설명 없이):**\n" +
                "단어: [추출된 한글 단어]\n" +
                "카테고리: [단어의 카테고리]\n" +
                "난이도: [1-5 사이의 숫자]\n\n" +
                "쉬움: [매우 쉬운 힌트]\n" +
                "보통: [보통 난이도 힌트]\n" +
                "어려움: [조금 어려운 힌트]";
            
            // Gemini API 호출
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", wordExtractionPrompt);
            content.put("parts", java.util.List.of(part));
            body.put("contents", java.util.List.of(content));
            
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 1024);
            body.put("generationConfig", generationConfig);
            
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> req = 
                new org.springframework.http.HttpEntity<>(body, headers);
            
            org.springframework.web.client.RestTemplate restTemplate = 
                new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<String> resp;
            try {
                resp = restTemplate.postForEntity(url, req, String.class);
            } catch (Exception ex) {
                log.error("Gemini API 호출 예외: url={}, error={}", url, ex.getMessage(), ex);
                throw new RuntimeException("Gemini API 호출 예외: " + ex.getMessage());
            }
            
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Gemini API 호출 실패: status={}, body={}", resp.getStatusCode(), resp.getBody());
                throw new RuntimeException("Gemini API 호출 실패: " + resp.getStatusCode());
            }
            
            // 응답 파싱
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root;
            try {
                root = objectMapper.readTree(resp.getBody());
            } catch (Exception e) {
                log.error("Gemini API 응답 파싱 실패: body={}, error={}", resp.getBody(), e.getMessage(), e);
                throw new RuntimeException("Gemini API 응답 파싱 실패: " + e.getMessage());
            }
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            
            // 단어, 카테고리, 난이도, 힌트 추출
            String word = extractWordFromResponse(text);
            String category = extractCategoryFromResponse(text);
            Integer difficulty = extractDifficultyFromResponse(text);
            Map<Integer, String> hints = extractHintsFromResponse(text);
            
            if (word == null || word.trim().isEmpty()) {
                throw new RuntimeException("단어를 추출할 수 없습니다.");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("word", word.trim());
            response.put("category", category != null ? category.trim() : "기타");
            response.put("difficulty", difficulty != null ? difficulty : 2);
            response.put("hints", hints);
            response.put("rawResponse", text);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("재미나이 API로 단어+힌트 생성 중 오류 발생: error={}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "단어+힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 재미나이 API 응답에서 단어 추출
     */
    private String extractWordFromResponse(String text) {
        if (text == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("단어\\s*:\\s*([^\\n]+)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim().replaceAll("^\\[|\\]$", "");
        }
        return null;
    }
    
    /**
     * 재미나이 API 응답에서 카테고리 추출
     */
    private String extractCategoryFromResponse(String text) {
        if (text == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("카테고리\\s*:\\s*([^\\n]+)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim().replaceAll("^\\[|\\]$", "");
        }
        return null;
    }
    
    /**
     * 재미나이 API 응답에서 난이도 추출
     */
    private Integer extractDifficultyFromResponse(String text) {
        if (text == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("난이도\\s*:\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 재미나이 API 응답에서 힌트 추출
     */
    private Map<Integer, String> extractHintsFromResponse(String text) {
        Map<Integer, String> hints = new HashMap<>();
        if (text == null) return hints;
        
        hints.put(1, extractHintByLabel(text, "쉬움"));
        hints.put(2, extractHintByLabel(text, "보통"));
        hints.put(3, extractHintByLabel(text, "어려움"));
        
        return hints;
    }
    
    /**
     * 재미나이 API 응답에서 라벨별 힌트 추출
     */
    private String extractHintByLabel(String text, String label) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(label + "\\s*:\\s*([^\\n\\[\\]]+)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
    
    /**
     * 재미나이 API로 생성된 단어+힌트를 DB에 저장
     */
    @PostMapping("/words/save-generated")
    public ResponseEntity<Map<String, Object>> saveGeneratedWord(
            @RequestBody Map<String, Object> request) {
        try {
            log.info("생성된 단어+힌트 저장 요청 - word: {}", request.get("word"));
            
            String word = (String) request.get("word");
            String category = (String) request.get("category");
            Integer difficulty = request.get("difficulty") != null ? 
                ((Number) request.get("difficulty")).intValue() : 2;
            @SuppressWarnings("unchecked")
            Map<Integer, String> hints = (Map<Integer, String>) request.get("hints");
            
            if (word == null || word.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "단어가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 단어가 이미 존재하는지 확인
            Optional<com.example.board.entity.PzWord> existingWordOpt = 
                pzWordRepository.findByWordAndIsActiveTrue(word.trim());
            
            com.example.board.entity.PzWord pzWord;
            if (existingWordOpt.isPresent()) {
                pzWord = existingWordOpt.get();
                log.info("기존 단어 사용: wordId={}, word={}", pzWord.getId(), pzWord.getWord());
            } else {
                // 새 단어 생성
                pzWord = new com.example.board.entity.PzWord();
                pzWord.setWord(word.trim());
                pzWord.setLength(word.trim().length());
                pzWord.setCategory(category != null ? category.trim() : "기타");
                pzWord.setDifficulty(difficulty);
                pzWord.setIsActive(true);
                pzWord.setConfYn("N");
                pzWord.setCreatedAt(java.time.LocalDateTime.now());
                pzWord.setUpdatedAt(java.time.LocalDateTime.now());
                pzWord = pzWordRepository.save(pzWord);
                log.info("새 단어 생성: wordId={}, word={}", pzWord.getId(), pzWord.getWord());
            }
            
            // 힌트 저장
            if (hints != null && !hints.isEmpty()) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                
                for (Map.Entry<Integer, String> entry : hints.entrySet()) {
                    Integer level = entry.getKey();
                    String hintText = entry.getValue();
                    
                    if (hintText == null || hintText.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 기존 힌트 확인 (같은 단어, 같은 난이도)
                    List<com.example.board.entity.PzHint> existingHints = 
                        pzHintRepository.findByWordIdAndDifficulty(pzWord.getId(), level);
                    
                    if (existingHints.isEmpty()) {
                        // 새 힌트 생성
                        com.example.board.entity.PzHint hint = new com.example.board.entity.PzHint();
                        hint.setWord(pzWord);
                        hint.setHintText(hintText.trim());
                        hint.setDifficulty(level);
                        hint.setIsPrimary(level == 2);
                        hint.setHintType("TEXT");
                        hint.setLanguageCode("ko");
                        hint.setCreatedAt(now);
                        hint.setUpdatedAt(now);
                        pzHintRepository.save(hint);
                        log.info("새 힌트 생성: wordId={}, level={}, hint={}", 
                            pzWord.getId(), level, hintText.trim());
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어와 힌트가 성공적으로 저장되었습니다.");
            response.put("wordId", pzWord.getId());
            response.put("word", pzWord.getWord());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("생성된 단어+힌트 저장 중 오류 발생: error={}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "단어+힌트 저장 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
