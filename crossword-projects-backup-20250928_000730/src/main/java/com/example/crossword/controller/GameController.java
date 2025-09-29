package com.example.crossword.controller;

import com.example.crossword.entity.GameSession;
import com.example.crossword.entity.Word;
import com.example.crossword.service.GameSessionService;
import com.example.crossword.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 게임 세션 관리 API 컨트롤러
 * 게임 세션 생성, 조회, 업데이트, 통계 등의 REST API를 제공
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class GameController {
    
    private final GameSessionService gameSessionService;
    private final WordService wordService;
    
    /**
     * 새로운 게임 세션 생성
     */
    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createGameSession(
            @RequestParam Long userId,
            @RequestParam Integer wordId) {
        
        log.debug("새로운 게임 세션 생성: 사용자 {}, 단어 {}", userId, wordId);
        
        try {
            // 단어 존재 여부 확인
            Optional<Word> wordOpt = wordService.getWordById(wordId);
            if (wordOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "존재하지 않는 단어입니다: " + wordId);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 게임 세션 생성
            GameSession gameSession = gameSessionService.createGameSession(userId, wordOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", gameSession);
            response.put("message", "게임 세션을 성공적으로 생성했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("게임 세션 생성 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "게임 세션 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * ID로 게임 세션 조회
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<Map<String, Object>> getGameSessionById(@PathVariable Long id) {
        log.debug("ID로 게임 세션 조회: {}", id);
        
        try {
            Optional<GameSession> gameSession = gameSessionService.getGameSessionById(id);
            
            if (gameSession.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", gameSession.get());
                response.put("message", "게임 세션을 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("게임 세션 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "게임 세션 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 사용자의 게임 세션 목록 조회
     */
    @GetMapping("/sessions/user/{userId}")
    public ResponseEntity<Map<String, Object>> getGameSessionsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.debug("사용자별 게임 세션 조회: {}, page={}, size={}, sortBy={}, sortDir={}", 
                 userId, page, size, sortBy, sortDir);
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<GameSession> gameSessions = gameSessionService.getGameSessionsByUserIdOrdered(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", gameSessions.getContent());
            response.put("pagination", Map.of(
                "currentPage", gameSessions.getNumber(),
                "totalPages", gameSessions.getTotalPages(),
                "totalElements", gameSessions.getTotalElements(),
                "size", gameSessions.getSize(),
                "hasNext", gameSessions.hasNext(),
                "hasPrevious", gameSessions.hasPrevious()
            ));
            response.put("message", "사용자별 게임 세션 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자별 게임 세션 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "사용자별 게임 세션 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 사용자의 완료된 게임 세션 조회
     */
    @GetMapping("/sessions/user/{userId}/completed")
    public ResponseEntity<Map<String, Object>> getCompletedGameSessionsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("사용자별 완료된 게임 세션 조회: {}, page={}, size={}", userId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<GameSession> gameSessions = gameSessionService.getCompletedGameSessionsByUserId(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", gameSessions.getContent());
            response.put("pagination", Map.of(
                "currentPage", gameSessions.getNumber(),
                "totalPages", gameSessions.getTotalPages(),
                "totalElements", gameSessions.getTotalElements(),
                "size", gameSessions.getSize(),
                "hasNext", gameSessions.hasNext(),
                "hasPrevious", gameSessions.hasPrevious()
            ));
            response.put("message", "사용자별 완료된 게임 세션 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자별 완료된 게임 세션 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "사용자별 완료된 게임 세션 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 사용자의 미완료 게임 세션 조회
     */
    @GetMapping("/sessions/user/{userId}/incomplete")
    public ResponseEntity<Map<String, Object>> getIncompleteGameSessionsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("사용자별 미완료 게임 세션 조회: {}, page={}, size={}", userId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<GameSession> gameSessions = gameSessionService.getIncompleteGameSessionsByUserId(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", gameSessions.getContent());
            response.put("pagination", Map.of(
                "currentPage", gameSessions.getNumber(),
                "totalPages", gameSessions.getTotalPages(),
                "totalElements", gameSessions.getTotalElements(),
                "size", gameSessions.getSize(),
                "hasNext", gameSessions.hasNext(),
                "hasPrevious", gameSessions.hasPrevious()
            ));
            response.put("message", "사용자별 미완료 게임 세션 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자별 미완료 게임 세션 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "사용자별 미완료 게임 세션 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 단어의 게임 세션 조회
     */
    @GetMapping("/sessions/word/{wordId}")
    public ResponseEntity<Map<String, Object>> getGameSessionsByWordId(
            @PathVariable Integer wordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("단어별 게임 세션 조회: {}, page={}, size={}", wordId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<GameSession> gameSessions = gameSessionService.getGameSessionsByWordId(wordId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", gameSessions.getContent());
            response.put("pagination", Map.of(
                "currentPage", gameSessions.getNumber(),
                "totalPages", gameSessions.getTotalPages(),
                "totalElements", gameSessions.getTotalElements(),
                "size", gameSessions.getSize(),
                "hasNext", gameSessions.hasNext(),
                "hasPrevious", gameSessions.hasPrevious()
            ));
            response.put("message", "단어별 게임 세션 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어별 게임 세션 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어별 게임 세션 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 날짜 범위의 게임 세션 조회
     */
    @GetMapping("/sessions/date-range")
    public ResponseEntity<Map<String, Object>> getGameSessionsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("날짜 범위별 게임 세션 조회: {} - {}, page={}, size={}", startDate, endDate, page, size);
        
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<GameSession> gameSessions = gameSessionService.getGameSessionsByDateRange(start, end, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", gameSessions.getContent());
            response.put("pagination", Map.of(
                "currentPage", gameSessions.getNumber(),
                "totalPages", gameSessions.getTotalPages(),
                "totalElements", gameSessions.getTotalElements(),
                "size", gameSessions.getSize(),
                "hasNext", gameSessions.hasNext(),
                "hasPrevious", gameSessions.hasPrevious()
            ));
            response.put("message", "날짜 범위별 게임 세션 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("날짜 범위별 게임 세션 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "날짜 범위별 게임 세션 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 사용자의 특정 날짜 범위 게임 세션 조회
     */
    @GetMapping("/sessions/user/{userId}/date-range")
    public ResponseEntity<Map<String, Object>> getGameSessionsByUserIdAndDateRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("사용자와 날짜 범위별 게임 세션 조회: {}, {} - {}, page={}, size={}", 
                 userId, startDate, endDate, page, size);
        
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<GameSession> gameSessions = gameSessionService.getGameSessionsByUserIdAndDateRange(userId, start, end, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", gameSessions.getContent());
            response.put("pagination", Map.of(
                "currentPage", gameSessions.getNumber(),
                "totalPages", gameSessions.getTotalPages(),
                "totalElements", gameSessions.getTotalElements(),
                "size", gameSessions.getSize(),
                "hasNext", gameSessions.hasNext(),
                "hasPrevious", gameSessions.hasPrevious()
            ));
            response.put("message", "사용자와 날짜 범위별 게임 세션 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자와 날짜 범위별 게임 세션 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "사용자와 날짜 범위별 게임 세션 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게임 세션 완료 처리
     */
    @PutMapping("/sessions/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeGameSession(@PathVariable Long id) {
        log.debug("게임 세션 완료 처리: {}", id);
        
        try {
            gameSessionService.completeGameSession(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "게임 세션을 성공적으로 완료 처리했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("게임 세션 완료 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "게임 세션 완료 처리 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게임 세션에 정답 추가
     */
    @PutMapping("/sessions/{id}/correct-answer")
    public ResponseEntity<Map<String, Object>> addCorrectAnswer(@PathVariable Long id) {
        log.debug("게임 세션에 정답 추가: {}", id);
        
        try {
            gameSessionService.addCorrectAnswer(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "정답을 성공적으로 추가했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("정답 추가 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "정답 추가 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게임 세션에 오답 추가
     */
    @PutMapping("/sessions/{id}/wrong-answer")
    public ResponseEntity<Map<String, Object>> addWrongAnswer(@PathVariable Long id) {
        log.debug("게임 세션에 오답 추가: {}", id);
        
        try {
            gameSessionService.addWrongAnswer(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "오답을 성공적으로 추가했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("오답 추가 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "오답 추가 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게임 세션에 힌트 사용 추가
     */
    @PutMapping("/sessions/{id}/hint-used")
    public ResponseEntity<Map<String, Object>> addHintUsed(@PathVariable Long id) {
        log.debug("게임 세션에 힌트 사용 추가: {}", id);
        
        try {
            gameSessionService.addHintUsed(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "힌트 사용을 성공적으로 추가했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 사용 추가 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "힌트 사용 추가 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게임 세션 플레이 시간 업데이트
     */
    @PutMapping("/sessions/{id}/play-time")
    public ResponseEntity<Map<String, Object>> updatePlayTime(
            @PathVariable Long id,
            @RequestParam int playTimeSeconds) {
        
        log.debug("게임 세션 플레이 시간 업데이트: {}, {}초", id, playTimeSeconds);
        
        try {
            gameSessionService.updatePlayTime(id, playTimeSeconds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "플레이 시간을 성공적으로 업데이트했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("플레이 시간 업데이트 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "플레이 시간 업데이트 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 사용자별 게임 통계 조회
     */
    @GetMapping("/stats/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserGameStats(@PathVariable Long userId) {
        log.debug("사용자별 게임 통계 조회: {}", userId);
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 기본 통계
            stats.put("totalSessions", gameSessionService.getGameSessionCountByUserId(userId));
            stats.put("completedSessions", gameSessionService.getCompletedGameSessionCountByUserId(userId));
            stats.put("incompleteSessions", gameSessionService.getIncompleteGameSessionCountByUserId(userId));
            
            // 성능 통계
            stats.put("averageAccuracy", gameSessionService.getAverageAccuracyByUserId(userId).orElse(BigDecimal.ZERO));
            stats.put("averagePlayTime", gameSessionService.getAveragePlayTimeByUserId(userId).orElse(0.0));
            stats.put("averageHintsUsed", gameSessionService.getAverageHintsUsedByUserId(userId).orElse(0.0));
            stats.put("maxAccuracy", gameSessionService.getMaxAccuracyByUserId(userId).orElse(BigDecimal.ZERO));
            stats.put("minAccuracy", gameSessionService.getMinAccuracyByUserId(userId).orElse(BigDecimal.ZERO));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userId", userId);
            response.put("message", "사용자별 게임 통계를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자별 게임 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "사용자별 게임 통계 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 전체 게임 통계 조회
     */
    @GetMapping("/stats/overall")
    public ResponseEntity<Map<String, Object>> getOverallGameStats() {
        log.debug("전체 게임 통계 조회");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 완료 상태별 통계
            stats.put("sessionsByCompletionStatus", gameSessionService.getGameSessionCountByCompletionStatus());
            
            // 사용자별 통계
            stats.put("sessionsByUser", gameSessionService.getGameSessionCountByUser());
            
            // 단어별 통계
            stats.put("sessionsByWord", gameSessionService.getGameSessionCountByWord());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "전체 게임 통계를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("전체 게임 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "전체 게임 통계 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게임 세션 존재 여부 확인
     */
    @GetMapping("/sessions/exists/{id}")
    public ResponseEntity<Map<String, Object>> checkGameSessionExists(@PathVariable Long id) {
        log.debug("게임 세션 존재 여부 확인: {}", id);
        
        try {
            boolean exists = gameSessionService.existsGameSession(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exists", exists);
            response.put("sessionId", id);
            response.put("message", exists ? "게임 세션이 존재합니다." : "게임 세션이 존재하지 않습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("게임 세션 존재 여부 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "게임 세션 존재 여부 확인 중 오류가 발생했습니다."));
        }
    }
}
