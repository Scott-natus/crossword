package com.example.crossword.controller;

import com.example.crossword.service.WordGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 단어 생성 API 컨트롤러
 * 사용자 화면 없이 백엔드에서 직접 실행 가능한 API
 */
@RestController
@RequestMapping("/api/word-generation")
@RequiredArgsConstructor
@Slf4j
public class WordGenerationController {
    
    private final WordGenerationService wordGenerationService;
    
    /**
     * 단어 생성 및 저장 API
     * @param category 카테고리
     * @param count 생성할 단어 개수 (기본값: 10)
     * @return 생성 결과
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateWords(
            @RequestParam String category,
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("단어 생성 API 호출 - 카테고리: {}, 개수: {}", category, count);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 단어 생성 및 저장
            int savedCount = wordGenerationService.generateAndSaveWords(category, count);
            
            response.put("success", true);
            response.put("message", String.format("%s 카테고리에서 %d개 단어 생성 및 저장 완료", category, savedCount));
            response.put("category", category);
            response.put("requestedCount", count);
            response.put("savedCount", savedCount);
            
            log.info("단어 생성 완료 - 요청: {}개, 저장: {}개", count, savedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 생성 중 오류 발생", e);
            
            response.put("success", false);
            response.put("message", "단어 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 임시 테이블 통계 조회 API
     * @return 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("임시 테이블 통계 조회 API 호출");
        
        try {
            Map<String, Object> statistics = wordGenerationService.getTmpWordsStatistics();
            statistics.put("success", true);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("통계 조회 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 특정 카테고리의 단어 목록 조회 API
     * @param category 카테고리
     * @param hintYn 힌트 생성 여부 (기본값: null - 전체)
     * @return 단어 목록
     */
    @GetMapping("/words")
    public ResponseEntity<Map<String, Object>> getWords(
            @RequestParam String category,
            @RequestParam(required = false) Boolean hintYn) {
        
        log.info("단어 목록 조회 API 호출 - 카테고리: {}, 힌트여부: {}", category, hintYn);
        
        try {
            // TODO: TmpForMakeWordsRepository에 해당 메서드 추가 필요
            // List<TmpForMakeWords> words = tmpForMakeWordsRepository.findByCategoryAndHintYn(category, hintYn);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어 목록 조회 기능은 추후 구현 예정");
            response.put("category", category);
            response.put("hintYn", hintYn);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 목록 조회 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "단어 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * API 사용법 안내
     * @return API 사용법
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp() {
        Map<String, Object> help = new HashMap<>();
        
        help.put("title", "단어 생성 API 사용법");
        help.put("endpoints", Map.of(
            "POST /api/word-generation/generate", "단어 생성 및 저장 (category, count 파라미터 필요)",
            "GET /api/word-generation/statistics", "임시 테이블 통계 조회",
            "GET /api/word-generation/words", "특정 카테고리 단어 목록 조회 (category 파라미터 필요)",
            "GET /api/word-generation/help", "API 사용법 안내"
        ));
        help.put("examples", Map.of(
            "단어 생성", "POST /api/word-generation/generate?category=K-DRAMA&count=5",
            "통계 조회", "GET /api/word-generation/statistics",
            "단어 목록", "GET /api/word-generation/words?category=K-DRAMA"
        ));
        
        return ResponseEntity.ok(help);
    }
}
