package com.example.board.controller;

import com.example.board.entity.PzHint;
import com.example.board.entity.PzWord;
import com.example.board.service.HintGeneratorManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI 힌트 생성 관리 컨트롤러
 * 
 * @author Board Team
 * @version 1.0.0
 * @since 2025-10-26
 */
@RestController
@RequestMapping("/admin/api/hint-generator")
@CrossOrigin(origins = "*")
@Slf4j
public class HintGeneratorManagementController {
    
    @Autowired
    private HintGeneratorManagementService hintGeneratorManagementService;
    
    /**
     * 힌트 생성 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = hintGeneratorManagementService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("힌트 생성 통계 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 단어 목록 조회 (DataTables용)
     */
    @GetMapping("/words-ajax")
    public ResponseEntity<Map<String, Object>> getWordsAjax(
            @RequestParam(defaultValue = "0") int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(defaultValue = "0") int orderColumn,
            @RequestParam(defaultValue = "asc") String orderDir,
            @RequestParam(required = false) String search) {
        
        try {
            int page = start / length;
            
            // 컬럼 인덱스를 실제 필드명으로 변환
            String sortField = getSortField(orderColumn);
            
            Page<PzWord> wordPage = hintGeneratorManagementService.getWordsForHintGeneration(
                search, page, length, sortField, orderDir);
            
            // 단어 데이터를 Map으로 변환
            List<Map<String, Object>> wordData = wordPage.getContent().stream()
                .map(hintGeneratorManagementService::mapWordToMap)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", wordPage.getTotalElements());
            response.put("recordsFiltered", wordPage.getTotalElements());
            response.put("data", wordData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 목록 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("draw", draw);
            errorResponse.put("recordsTotal", 0);
            errorResponse.put("recordsFiltered", 0);
            errorResponse.put("data", List.of());
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * DataTables 컬럼 인덱스를 실제 필드명으로 변환
     */
    private String getSortField(int columnIndex) {
        switch (columnIndex) {
            case 0: return "id";           // 체크박스
            case 1: return "category";     // 카테고리
            case 2: return "word";         // 단어
            case 3: return "length";       // 길이
            case 4: return "difficulty";   // 난이도
            case 5: return "id";           // 힌트 개수 (정렬 불가능하므로 id로 대체)
            case 6: return "id";           // 액션 (정렬 불가능하므로 id로 대체)
            default: return "id";
        }
    }
    
    /**
     * API 연결 테스트
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            boolean isConnected = hintGeneratorManagementService.testApiConnection();
            Map<String, Object> response = new HashMap<>();
            response.put("success", isConnected);
            response.put("message", isConnected ? "API 연결 성공" : "API 연결 실패");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("API 연결 테스트 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 단어의 힌트 목록 조회
     */
    @GetMapping("/word/{wordId}/hints")
    public ResponseEntity<Map<String, Object>> getWordHints(@PathVariable Integer wordId) {
        try {
            Map<String, Object> result = hintGeneratorManagementService.getWordHints(wordId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("단어 힌트 조회 중 오류 발생: {}", wordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 단어에 대한 힌트 생성
     */
    @PostMapping("/word/{wordId}")
    public ResponseEntity<Map<String, Object>> generateHintsForWord(@PathVariable Integer wordId) {
        try {
            Map<String, Object> result = hintGeneratorManagementService.generateForWord(wordId, true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("힌트 생성 중 오류 발생: {}", wordId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 힌트 삭제
     */
    @DeleteMapping("/hint/{hintId}")
    public ResponseEntity<Map<String, Object>> deleteHint(@PathVariable Long hintId) {
        try {
            boolean success = hintGeneratorManagementService.deleteHint(hintId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "힌트가 삭제되었습니다." : "힌트 삭제에 실패했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("힌트 삭제 중 오류 발생: {}", hintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
