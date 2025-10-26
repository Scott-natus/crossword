package com.example.board.controller;

import com.example.board.service.PzWordService;
import com.example.board.service.PzHintService;
import com.example.board.service.PuzzleLevelService;
import com.example.board.service.GameSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
            long refinedWords = pzWordService.getWordsWithHintsCount();
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
            stats.put("refinedWords", pzWordService.getWordsWithHintsCount());
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
}
