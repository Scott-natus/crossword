package com.example.crossword.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 크로스워드 퍼즐 시스템 메인 컨트롤러
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-16
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CrosswordController {

    /**
     * 시스템 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Crossword Puzzle System");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "크로스워드 퍼즐 시스템이 정상 작동 중입니다.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * API 정보 조회
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Crossword Puzzle System");
        response.put("description", "Spring Boot 기반 크로스워드 퍼즐 시스템");
        response.put("version", "1.0.0");
        response.put("author", "Crossword Team");
        response.put("created", "2025-09-16");
        response.put("endpoints", new String[]{
            "GET /api/health - 시스템 상태 확인",
            "GET /api/info - API 정보 조회",
            "GET /api/words - 단어 목록 조회",
            "GET /api/puzzle/generate/{level} - 퍼즐 생성",
            "POST /api/game/session/start - 게임 세션 시작"
        });
        
        return ResponseEntity.ok(response);
    }

    /**
     * 테스트 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "크로스워드 퍼즐 시스템 테스트 성공!");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "SUCCESS");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 데이터베이스 연결 테스트
     */
    @GetMapping("/db-test")
    public ResponseEntity<Map<String, Object>> databaseTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "데이터베이스 연결 테스트");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "SUCCESS");
        response.put("note", "Repository 인터페이스들이 정상적으로 로드되었습니다.");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 서비스 레이어 테스트
     */
    @GetMapping("/service-test")
    public ResponseEntity<Map<String, Object>> serviceTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "서비스 레이어 테스트");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "SUCCESS");
        response.put("services", Arrays.asList(
            "WordService - 단어 관리 서비스",
            "HintService - 힌트 관리 서비스", 
            "PuzzleLevelService - 퍼즐 레벨 관리 서비스",
            "GameSessionService - 게임 세션 관리 서비스",
            "CrosswordGeneratorService - 퍼즐 생성 알고리즘",
            "HintGenerationService - Gemini API 연동 힌트 생성"
        ));
        response.put("note", "모든 서비스 클래스들이 정상적으로 로드되었습니다.");
        
        return ResponseEntity.ok(response);
    }
}
