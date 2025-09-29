package com.example.crossword.controller;

import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.service.HintGeneratorManagementService;
import com.example.crossword.service.WordManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 힌트생성 관리 컨트롤러
 * 라라벨의 PzHintGeneratorController와 동일한 기능 제공
 * 경로: /K-CrossWord/admin/hint-generator
 */
@RestController
@RequestMapping("/admin/api/hint-generator")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class HintGeneratorManagementController {
    
    private final HintGeneratorManagementService hintGeneratorService;
    private final WordManagementService wordManagementService;
    
    /**
     * 테스트 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "HintGeneratorManagementController is working!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 단어 데이터 조회 (DataTables용)
     * 라라벨의 getWordsAjax와 동일한 기능
     */
    @GetMapping("/words-ajax")
    public ResponseEntity<Map<String, Object>> getWordsAjax(
            @RequestParam(defaultValue = "1") int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "30") int length,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int orderColumn,
            @RequestParam(defaultValue = "desc") String orderDir,
            @RequestParam(required = false) String status) {
        
        try {
            Map<String, Object> data = hintGeneratorService.getWordsData(
                draw, start, length, search, orderColumn, orderDir, status);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("단어 데이터 조회 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "데이터 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 단일 단어에 대한 힌트 생성
     * 라라벨의 generateForWord와 동일한 기능
     */
    @PostMapping("/word/{wordId}")
    public ResponseEntity<Map<String, Object>> generateForWord(@PathVariable Integer wordId) {
        log.debug("단일 단어 힌트 생성: {}", wordId);
        
        try {
            Map<String, Object> result = hintGeneratorService.generateForWord(wordId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("단일 힌트 생성 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 여러 단어에 대한 힌트 일괄 생성
     * 라라벨의 generateBatch와 동일한 기능
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> generateBatch(
            @RequestParam List<Integer> wordIds,
            @RequestParam(defaultValue = "false") Boolean overwrite) {
        
        log.debug("일괄 힌트 생성: {}개 단어, 덮어쓰기: {}", wordIds.size(), overwrite);
        
        try {
            Map<String, Object> result = hintGeneratorService.generateBatch(wordIds, overwrite);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("일괄 힌트 생성 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "일괄 힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 카테고리별 힌트 생성
     * 라라벨의 generateByCategory와 동일한 기능
     */
    @PostMapping("/category")
    public ResponseEntity<Map<String, Object>> generateByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "false") Boolean overwrite) {
        
        log.debug("카테고리별 힌트 생성: {}, 덮어쓰기: {}", category, overwrite);
        
        try {
            Map<String, Object> result = hintGeneratorService.generateByCategory(category, overwrite);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("카테고리별 힌트 생성 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "카테고리별 힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 기존 힌트 수정 (재생성)
     * 라라벨의 regenerateHints와 동일한 기능
     */
    @PostMapping("/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateHints(
            @RequestParam List<Integer> hintIds) {
        
        log.debug("힌트 재생성: {}개 힌트", hintIds.size());
        
        try {
            Map<String, Object> result = hintGeneratorService.regenerateHints(hintIds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("힌트 재생성 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "힌트 재생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 보정이 필요한 힌트 목록 조회
     * 라라벨의 getHintsForCorrection과 동일한 기능
     */
    @GetMapping("/correction-hints")
    public ResponseEntity<Map<String, Object>> getHintsForCorrection(
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Map<String, Object> result = hintGeneratorService.getHintsForCorrection(
                difficulty, category, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("보정 힌트 목록 조회 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "보정 힌트 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * API 연결 테스트
     * 라라벨의 testConnection과 동일한 기능
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            Map<String, Object> result = hintGeneratorService.testConnection();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("API 연결 테스트 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "API 연결 테스트 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 힌트 생성 통계
     * 라라벨의 getStats와 동일한 기능
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = hintGeneratorService.getStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("통계 조회 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "통계 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 단어의 힌트 데이터 가져오기
     * 라라벨의 getWordHints와 동일한 기능
     */
    @GetMapping("/word/{wordId}/hints")
    public ResponseEntity<Map<String, Object>> getWordHints(@PathVariable Integer wordId) {
        try {
            Map<String, Object> result = hintGeneratorService.getWordHints(wordId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("단어 힌트 조회 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "힌트 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        try {
            List<String> categories = wordManagementService.getCategories();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("categories", categories);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리 목록 조회 실패: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "카테고리 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 개별 힌트 삭제
     */
    @DeleteMapping("/hint/{hintId}")
    public ResponseEntity<Map<String, Object>> deleteHint(@PathVariable Integer hintId) {
        try {
            Map<String, Object> result = hintGeneratorService.deleteHint(hintId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("힌트 삭제 실패: hintId={}, error={}", hintId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
