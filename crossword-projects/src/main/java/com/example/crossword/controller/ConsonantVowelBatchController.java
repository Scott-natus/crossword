package com.example.crossword.controller;

import com.example.crossword.service.ConsonantVowelBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 자음-모음 조합 단어 수집 배치 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/consonant-vowel-batch")
@RequiredArgsConstructor
@Slf4j
public class ConsonantVowelBatchController {

    private final ConsonantVowelBatchService consonantVowelBatchService;

    /**
     * 전체 배치 실행 (196개 조합)
     * @return 배치 실행 결과
     */
    @PostMapping("/run-full-batch")
    public ResponseEntity<Map<String, Object>> runFullBatch() {
        try {
            log.info("🚀 전체 배치 실행 요청");
            
            consonantVowelBatchService.runFullBatch();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "배치 실행이 시작되었습니다. 상태는 /status 엔드포인트에서 확인하세요.");
            response.put("totalCombinations", 196);
            response.put("estimatedDuration", "약 98분 (30초 × 196개 조합)");
            
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
     * 실패한 조합들만 재처리하는 배치 실행
     * @return 배치 실행 결과
     */
    @PostMapping("/run-failed-batch")
    public ResponseEntity<Map<String, Object>> runFailedBatch() {
        try {
            log.info("🚀 실패한 조합 재처리 배치 실행 요청");
            
            consonantVowelBatchService.runFailedCombinationsBatch();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "실패한 조합 재처리 배치가 시작되었습니다. 상태는 /status 엔드포인트에서 확인하세요.");
            response.put("estimatedDuration", "약 30분 (실패한 조합들만 처리)");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 실패한 조합 재처리 배치 실행 요청 처리 중 오류", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "실패한 조합 재처리 배치 실행 중 오류 발생: " + e.getMessage());
            
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
            Map<String, Object> status = consonantVowelBatchService.getBatchStatus();
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
            consonantVowelBatchService.stopBatch();
            
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
     * @param consonant 자음
     * @param vowel 모음
     * @return 처리 결과
     */
    @PostMapping("/process-combination")
    public ResponseEntity<Map<String, Object>> processCombination(
            @RequestParam String consonant,
            @RequestParam String vowel) {
        try {
            log.info("🔍 단일 조합 처리 요청: 초성 '{}', 중성 '{}'", consonant, vowel);
            
            Map<String, Object> result = consonantVowelBatchService.processConsonantVowelCombination(consonant, vowel);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 조합 처리 중 오류: 초성 '{}', 중성 '{}'", consonant, vowel, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "조합 처리 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 조합부터 배치 재시작
     * @param startConsonant 시작 자음
     * @param startVowel 시작 모음
     * @return 재시작 결과
     */
    @PostMapping("/resume-from")
    public ResponseEntity<Map<String, Object>> resumeBatchFrom(
            @RequestParam String startConsonant,
            @RequestParam String startVowel) {
        try {
            log.info("🔄 배치 재시작 요청: 초성 '{}', 중성 '{}'부터", startConsonant, startVowel);
            
            consonantVowelBatchService.resumeBatchFrom(startConsonant, startVowel);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("배치가 초성 '%s', 중성 '%s'부터 재시작되었습니다.", 
                startConsonant, startVowel));
            response.put("startFrom", startConsonant + startVowel);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 배치 재시작 중 오류: 초성 '{}', 중성 '{}'", startConsonant, startVowel, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "배치 재시작 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 자음-모음 조합 목록 조회
     * @return 전체 조합 목록
     */
    @GetMapping("/combinations")
    public ResponseEntity<Map<String, Object>> getCombinations() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // 자음 배열
            String[] consonants = {"ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"};
            // 모음 배열
            String[] vowels = {"ㅏ", "ㅑ", "ㅓ", "ㅕ", "ㅗ", "ㅛ", "ㅜ", "ㅠ", "ㅡ", "ㅣ", "ㅢ", "ㅘ", "ㅙ", "ㅞ"};
            
            response.put("consonants", consonants);
            response.put("vowels", vowels);
            response.put("totalCombinations", consonants.length * vowels.length);
            response.put("estimatedDuration", "약 98분 (30초 × 196개 조합)");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 조합 목록 조회 중 오류", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "조합 목록 조회 중 오류 발생: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Gemini API 간단 테스트
     * @return 테스트 결과
     */
    @GetMapping("/test-gemini")
    public ResponseEntity<Map<String, Object>> testGemini() {
        try {
            log.info("🧪 Gemini API 간단 테스트 시작");
            
            // 간단한 프롬프트로 테스트
            List<String> words = consonantVowelBatchService.generateWordsFromGemini("ㄱ", "ㅏ");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Gemini API 테스트 완료");
            response.put("generatedWords", words);
            response.put("wordCount", words.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Gemini API 테스트 중 오류", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Gemini API 테스트 실패: " + e.getMessage());
            
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
        
        help.put("title", "자음-모음 조합 단어 수집 배치 API");
        help.put("description", "196개 자음-모음 조합을 30초 간격으로 처리하여 단어를 수집합니다.");
        
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("POST /run-full-batch", "전체 배치 실행 (196개 조합)");
        endpoints.put("GET /status", "현재 배치 상태 조회");
        endpoints.put("POST /stop", "배치 중단");
        endpoints.put("POST /process-combination?consonant=ㄱ&vowel=ㅏ", "단일 조합 처리 (테스트용)");
        endpoints.put("POST /resume-from?startConsonant=ㄱ&startVowel=ㅏ", "특정 조합부터 재시작");
        endpoints.put("GET /combinations", "전체 조합 목록 조회");
        endpoints.put("GET /help", "API 사용법");
        
        help.put("endpoints", endpoints);
        
        Map<String, Object> info = new HashMap<>();
        info.put("totalCombinations", 196);
        info.put("consonants", "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ (14개)");
        info.put("vowels", "ㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣㅢㅘㅙㅞ (14개)");
        info.put("interval", "30초");
        info.put("estimatedDuration", "약 98분");
        info.put("targetTable", "tmp_for_make_words");
        
        help.put("batchInfo", info);
        
        return ResponseEntity.ok(help);
    }
}
