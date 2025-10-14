package com.example.crossword.controller;

import com.example.crossword.service.ConsonantVowelWordCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 초성+중성 조합별 단어 수집 배치 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/consonant-vowel-word-collection")
@RequiredArgsConstructor
@Slf4j
public class ConsonantVowelWordCollectionController {

    private final ConsonantVowelWordCollectionService consonantVowelWordCollectionService;

    /**
     * 초성+중성 조합별 단어 수집 배치 실행
     * @return 배치 실행 결과
     */
    @PostMapping("/run-batch")
    public ResponseEntity<Map<String, Object>> runBatch() {
        try {
            log.info("🚀 초성+중성 조합별 단어 수집 배치 실행 요청");
            
            consonantVowelWordCollectionService.runWordCollectionBatch();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "초성+중성 조합별 단어 수집 배치가 시작되었습니다. 상태는 /status 엔드포인트에서 확인하세요.");
            response.put("estimatedDuration", "약 68분 (136개 조합 × 30초)");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 배치 실행 요청 처리 중 오류", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "배치 실행 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 배치 상태 조회
     * @return 현재 배치 상태
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBatchStatus() {
        try {
            Map<String, Object> status = consonantVowelWordCollectionService.getBatchStatus();
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("❌ 배치 상태 조회 중 오류", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "상태 조회 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 배치 중단
     * @return 중단 결과
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopBatch() {
        try {
            consonantVowelWordCollectionService.stopBatch();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "배치 중단 요청이 전송되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 배치 중단 요청 처리 중 오류", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "배치 중단 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 조합 처리 (테스트용)
     * @param combination 초성+중성 조합
     * @return 처리 결과
     */
    @PostMapping("/process-combination")
    public ResponseEntity<Map<String, Object>> processCombination(@RequestParam String combination) {
        try {
            log.info("🔍 단일 조합 처리 요청: '{}'", combination);
            
            Map<String, Object> result = consonantVowelWordCollectionService.collectWordsForCombination(combination);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 조합 처리 중 오류: '{}'", combination, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "조합 처리 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 배치 도움말
     * @return API 사용법
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp() {
        Map<String, Object> help = new HashMap<>();
        
        help.put("title", "초성+중성 조합별 단어 수집 배치 API");
        help.put("description", "136개 초성+중성 조합을 30초 간격으로 처리하여 단어를 수집합니다.");
        
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("POST /run-batch", "전체 배치 실행 (136개 조합)");
        endpoints.put("GET /status", "현재 배치 상태 조회");
        endpoints.put("POST /stop", "배치 중단");
        endpoints.put("POST /process-combination?combination=가", "단일 조합 처리 (테스트용)");
        endpoints.put("GET /help", "API 사용법");
        
        help.put("endpoints", endpoints);
        
        Map<String, Object> info = new HashMap<>();
        info.put("totalCombinations", 136);
        info.put("description", "초성+중성 조합 (종성 제거)");
        info.put("interval", "30초");
        info.put("estimatedDuration", "약 68분");
        info.put("targetTable", "tmp_for_make_words");
        info.put("categoryPrefix", "초성중성:");
        
        help.put("batchInfo", info);
        
        return ResponseEntity.ok(help);
    }
}
