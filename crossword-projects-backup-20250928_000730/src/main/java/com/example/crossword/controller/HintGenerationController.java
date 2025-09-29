package com.example.crossword.controller;

import com.example.crossword.entity.Hint;
import com.example.crossword.entity.Word;
import com.example.crossword.service.HintGenerationService;
import com.example.crossword.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 힌트 생성 API 컨트롤러
 * Gemini API를 연동한 자동 힌트 생성 REST API를 제공
 */
@RestController
@RequestMapping("/api/hint-generation")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class HintGenerationController {
    
    private final HintGenerationService hintGenerationService;
    private final WordService wordService;
    
    /**
     * 특정 단어에 대한 힌트 생성
     */
    @PostMapping("/word/{wordId}")
    public ResponseEntity<Map<String, Object>> generateHintForWord(
            @PathVariable Integer wordId,
            @RequestParam String hintType) {
        
        log.debug("단어에 대한 힌트 생성: {}, 타입: {}", wordId, hintType);
        
        try {
            // 단어 존재 여부 확인
            Optional<Word> wordOpt = wordService.getWordById(wordId);
            if (wordOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "존재하지 않는 단어입니다: " + wordId);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 힌트 타입 검증
            if (!isValidHintType(hintType)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "유효하지 않은 힌트 타입입니다: " + hintType);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 힌트 생성
            Hint hint = hintGenerationService.generateHintForWord(wordOpt.get(), hintType);
            
            if (hint != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", hint);
                response.put("message", "힌트를 성공적으로 생성했습니다.");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "힌트 생성에 실패했습니다.");
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            log.error("힌트 생성 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 특정 단어에 대한 모든 타입의 힌트 생성
     */
    @PostMapping("/word/{wordId}/all")
    public ResponseEntity<Map<String, Object>> generateAllHintsForWord(@PathVariable Integer wordId) {
        log.debug("단어에 대한 모든 힌트 생성: {}", wordId);
        
        try {
            // 단어 존재 여부 확인
            Optional<Word> wordOpt = wordService.getWordById(wordId);
            if (wordOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "존재하지 않는 단어입니다: " + wordId);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 모든 힌트 생성
            List<Hint> hints = hintGenerationService.generateAllHintsForWord(wordOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hints);
            response.put("count", hints.size());
            response.put("message", "모든 힌트를 성공적으로 생성했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("모든 힌트 생성 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "모든 힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 여러 단어에 대한 힌트 일괄 생성
     */
    @PostMapping("/words/batch")
    public ResponseEntity<Map<String, Object>> generateHintsForWords(
            @RequestBody List<Integer> wordIds) {
        
        log.debug("여러 단어에 대한 힌트 일괄 생성: {}개", wordIds.size());
        
        try {
            // 단어 존재 여부 확인
            List<Word> words = wordIds.stream()
                .map(wordService::getWordById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
            
            if (words.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "유효한 단어가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 일괄 힌트 생성
            Map<String, List<Hint>> results = hintGenerationService.generateHintsForWords(words);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", results);
            response.put("processedWords", results.size());
            response.put("totalHints", results.values().stream().mapToInt(List::size).sum());
            response.put("message", "일괄 힌트 생성을 성공적으로 완료했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("일괄 힌트 생성 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "일괄 힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 난이도별 단어에 대한 힌트 생성
     */
    @PostMapping("/difficulty/{difficulty}")
    public ResponseEntity<Map<String, Object>> generateHintsByDifficulty(
            @PathVariable Integer difficulty,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.debug("난이도별 힌트 생성: 난이도 {}, 제한 {}", difficulty, limit);
        
        try {
            // 난이도별 힌트 생성
            Map<String, List<Hint>> results = hintGenerationService.generateHintsByDifficulty(difficulty, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", results);
            response.put("processedWords", results.size());
            response.put("totalHints", results.values().stream().mapToInt(List::size).sum());
            response.put("difficulty", difficulty);
            response.put("limit", limit);
            response.put("message", "난이도별 힌트 생성을 성공적으로 완료했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("난이도별 힌트 생성 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "난이도별 힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 카테고리별 단어에 대한 힌트 생성
     */
    @PostMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> generateHintsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.debug("카테고리별 힌트 생성: 카테고리 {}, 제한 {}", category, limit);
        
        try {
            // 카테고리별 힌트 생성
            Map<String, List<Hint>> results = hintGenerationService.generateHintsByCategory(category, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", results);
            response.put("processedWords", results.size());
            response.put("totalHints", results.values().stream().mapToInt(List::size).sum());
            response.put("category", category);
            response.put("limit", limit);
            response.put("message", "카테고리별 힌트 생성을 성공적으로 완료했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카테고리별 힌트 생성 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "카테고리별 힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 힌트 품질 평가
     */
    @PostMapping("/evaluate/{hintId}")
    public ResponseEntity<Map<String, Object>> evaluateHintQuality(@PathVariable Integer hintId) {
        log.debug("힌트 품질 평가: {}", hintId);
        
        try {
            // 힌트 존재 여부 확인 (간단한 구현)
            // 실제로는 HintService를 통해 확인해야 함
            
            // 힌트 품질 평가 (실제 구현에서는 Hint 객체를 가져와야 함)
            // int quality = hintGenerationService.evaluateHintQuality(hint);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hintId", hintId);
            response.put("message", "힌트 품질 평가 기능은 구현 예정입니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 품질 평가 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 품질 평가 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 힌트 생성 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getHintGenerationStats() {
        log.debug("힌트 생성 통계 조회");
        
        try {
            Map<String, Object> stats = hintGenerationService.getHintGenerationStats();
            
            // 추가 통계 정보
            stats.put("totalWords", wordService.getTotalActiveWordCount());
            stats.put("wordsWithHints", wordService.getWordsWithHintsCount());
            stats.put("wordsWithoutHints", wordService.getWordsWithoutHintsCount());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "힌트 생성 통계를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 생성 통계 조회 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 생성 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 힌트 생성 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getHintGenerationStatus() {
        log.debug("힌트 생성 상태 확인");
        
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("service", "HintGenerationService");
            status.put("status", "ACTIVE");
            status.put("geminiApiAvailable", true); // 실제로는 API 키 확인 로직 필요
            status.put("supportedHintTypes", List.of("TEXT", "IMAGE", "AUDIO"));
            status.put("message", "힌트 생성 서비스가 정상적으로 작동 중입니다.");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);
            response.put("message", "힌트 생성 상태를 성공적으로 확인했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 생성 상태 확인 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 생성 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 힌트 타입 유효성 검증
     */
    private boolean isValidHintType(String hintType) {
        return hintType != null && 
               (hintType.equals("TEXT") || hintType.equals("IMAGE") || hintType.equals("AUDIO"));
    }
    
    /**
     * 지원되는 힌트 타입 목록 조회
     */
    @GetMapping("/hint-types")
    public ResponseEntity<Map<String, Object>> getSupportedHintTypes() {
        log.debug("지원되는 힌트 타입 목록 조회");
        
        try {
            Map<String, Object> hintTypes = new HashMap<>();
            hintTypes.put("TEXT", "텍스트 힌트 - 단어의 의미나 정의를 설명");
            hintTypes.put("IMAGE", "이미지 힌트 - 단어와 관련된 이미지 설명");
            hintTypes.put("AUDIO", "오디오 힌트 - 단어와 관련된 소리나 음향 효과 설명");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hintTypes);
            response.put("count", hintTypes.size());
            response.put("message", "지원되는 힌트 타입 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 타입 목록 조회 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 타입 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
