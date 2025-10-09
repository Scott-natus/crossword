package com.example.crossword.service;

import com.example.crossword.entity.TmpForMakeWords;
import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.repository.TmpForMakeWordsRepository;
import com.example.crossword.repository.PzWordRepository;
import com.example.crossword.repository.PzHintRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * tmp_for_make_words 테이블의 단어들을 이용한 힌트 생성 서비스
 * 라라벨의 GenerateHintsFromTempWords.php를 참고하여 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HintGenerationFromTempService {
    
    private final TmpForMakeWordsRepository tmpForMakeWordsRepository;
    private final PzWordRepository pzWordRepository;
    private final PzHintRepository pzHintRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    
    /**
     * 힌트가 생성되지 않은 단어들에 대해 힌트 생성 및 최종 테이블 저장
     * @param limit 처리할 단어 개수
     * @return 처리 결과 통계
     */
    @Transactional
    public Map<String, Object> generateHintsFromTempWords(int limit) {
        log.info("🚀 힌트 생성 스케줄러 시작 - 처리 개수: {}개", limit);
        
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        int skipCount = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // 힌트가 생성되지 않은 단어들 조회
            List<TmpForMakeWords> wordsWithoutHints = tmpForMakeWordsRepository
                    .findWordsWithoutHints()
                    .stream()
                    .limit(limit)
                    .toList();
            
            if (wordsWithoutHints.isEmpty()) {
                log.info("✅ 처리할 단어가 없습니다. 모든 단어에 힌트가 생성되어 있습니다.");
                result.put("success", true);
                result.put("message", "처리할 단어가 없습니다.");
                result.put("successCount", 0);
                result.put("errorCount", 0);
                result.put("skipCount", 0);
                return result;
            }
            
            log.info("📝 힌트가 없는 단어: {}개 발견", wordsWithoutHints.size());
            
            for (TmpForMakeWords tempWord : wordsWithoutHints) {
                try {
                    log.info("처리 중: {} (카테고리: {})", tempWord.getWords(), tempWord.getCategory());
                    
                    // 1. 재미나이 API로 힌트 생성
                    Map<String, Object> hintResult = generateHintsForWord(tempWord.getWords(), tempWord.getCategory());
                    
                    if ((Boolean) hintResult.get("success")) {
                        // 2. [단어, 카테고리] 조합으로 중복 체크
                        Optional<PzWord> existingWord = pzWordRepository.findByWordAndCategory(
                                tempWord.getWords(), tempWord.getCategory());
                        
                        if (existingWord.isPresent()) {
                            log.warn("⚠️ 스킵: {} (카테고리 '{}'로 이미 존재)", 
                                    tempWord.getWords(), tempWord.getCategory());
                            
                            // 임시테이블 마킹 (중복으로 처리됨)
                            tempWord.setHintYn(true);
                            tmpForMakeWordsRepository.save(tempWord);
                            
                            skipCount++;
                            continue;
                        }
                        
                        // 3. pz_words 테이블에 단어 저장
                        PzWord newWord = new PzWord();
                        newWord.setWord(tempWord.getWords());
                        newWord.setCategory(tempWord.getCategory());
                        newWord.setDifficulty(2); // 기본 난이도 2 (보통)
                        newWord.setIsActive(true);
                        
                        PzWord savedWord = pzWordRepository.save(newWord);
                        
                        // 4. pz_hints 테이블에 힌트들 저장
                        int hintCount = 0;
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> hints = (List<Map<String, Object>>) hintResult.get("hints");
                        
                        for (Map<String, Object> hintData : hints) {
                            if ((Boolean) hintData.get("success")) {
                                PzHint hint = new PzHint();
                                hint.setWord(savedWord);
                                hint.setHintText((String) hintData.get("hint"));
                                hint.setHintType("text");
                                hint.setDifficulty((Integer) hintData.get("difficulty"));
                                hint.setIsPrimary((Integer) hintData.get("difficulty") == 2); // 기본 난이도가 2이므로
                                
                                pzHintRepository.save(hint);
                                hintCount++;
                            }
                        }
                        
                        // 5. 임시테이블 마킹 (성공으로 처리됨)
                        tempWord.setHintYn(true);
                        tmpForMakeWordsRepository.save(tempWord);
                        
                        successCount++;
                        log.info("✓ 성공: {} (카테고리: {}, 힌트: {}개)", 
                                tempWord.getWords(), tempWord.getCategory(), hintCount);
                        
                    } else {
                        errorCount++;
                        String errorMsg = String.format("✗ 실패: %s - %s", 
                                tempWord.getWords(), hintResult.get("error"));
                        errors.add(errorMsg);
                        log.error(errorMsg);
                    }
                    
                    // API 호출 간격 조절 (3초 대기)
                    Thread.sleep(3000);
                    
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = String.format("✗ 오류: %s - %s", 
                            tempWord.getWords(), e.getMessage());
                    errors.add(errorMsg);
                    log.error("힌트 생성 오류 - 단어: {}", tempWord.getWords(), e);
                }
            }
            
            // 결과 정리
            result.put("success", true);
            result.put("message", String.format("처리 완료: 성공 %d개, 실패 %d개, 스킵 %d개", 
                    successCount, errorCount, skipCount));
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("skipCount", skipCount);
            result.put("errors", errors);
            
            log.info("📊 힌트 생성 완료! 성공: {}개, 실패: {}개, 스킵: {}개", 
                    successCount, errorCount, skipCount);
            
        } catch (Exception e) {
            log.error("힌트 생성 스케줄러 실행 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "힌트 생성 스케줄러 실행 중 오류가 발생했습니다: " + e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 재미나이 API를 이용하여 특정 단어의 힌트 생성
     * @param word 단어
     * @param category 카테고리
     * @return 힌트 생성 결과
     */
    private Map<String, Object> generateHintsForWord(String word, String category) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String prompt = createHintGenerationPrompt(word, category);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);
            
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "topK", 40,
                            "topP", 0.95,
                            "maxOutputTokens", 2048
                    ),
                    "safetySettings", List.of(
                            Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                            Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                            Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                            Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE")
                    )
            );
            
            String jsonRequestBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonRequestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(geminiApiUrl, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    List<Map<String, Object>> hints = parseHintResponse(text);
                    
                    result.put("success", true);
                    result.put("hints", hints);
                } else {
                    result.put("success", false);
                    result.put("error", "API 응답에서 힌트를 찾을 수 없습니다.");
                }
            } else {
                result.put("success", false);
                result.put("error", "재미나이 API 호출 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("힌트 생성 중 오류 발생 - 단어: {}", word, e);
            result.put("success", false);
            result.put("error", "힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 힌트 생성 프롬프트 생성
     * @param word 단어
     * @param category 카테고리
     * @return 프롬프트 문자열
     */
    private String createHintGenerationPrompt(String word, String category) {
        return String.format("""
            당신은 한글 십자낱말 퍼즐을 위한 힌트를 만드는 전문가입니다.
            
            단어: '%s'
            카테고리: '%s'
            
            위 단어에 대해 3가지 난이도의 힌트를 생성해주세요:
            
            **힌트 생성 규칙:**
            1. 쉬운 힌트 (난이도 1): 직접적이고 명확한 힌트
            2. 보통 힌트 (난이도 2): 적당한 수준의 힌트
            3. 어려운 힌트 (난이도 3): 간접적이고 추상적인 힌트
            
            **응답 형식:**
            쉬운힌트: [힌트 내용]
            보통힌트: [힌트 내용]
            어려운힌트: [힌트 내용]
            
            (다른 설명 없이 위 형식으로만 응답해주세요)
            """, word, category);
    }
    
    /**
     * 재미나이 API 응답에서 힌트 파싱
     * @param responseText API 응답 텍스트
     * @return 파싱된 힌트 목록
     */
    private List<Map<String, Object>> parseHintResponse(String responseText) {
        List<Map<String, Object>> hints = new ArrayList<>();
        
        try {
            String[] lines = responseText.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // 정규식을 사용하여 더 정확한 파싱
                if (line.matches("^쉬운힌트\\s*:.*")) {
                    String hint = line.replaceFirst("^쉬운힌트\\s*:", "").trim();
                    hints.add(Map.of(
                            "difficulty", 1,
                            "hint", hint,
                            "success", true
                    ));
                } else if (line.matches("^보통힌트\\s*:.*")) {
                    String hint = line.replaceFirst("^보통힌트\\s*:", "").trim();
                    hints.add(Map.of(
                            "difficulty", 2,
                            "hint", hint,
                            "success", true
                    ));
                } else if (line.matches("^어려운힌트\\s*:.*")) {
                    String hint = line.replaceFirst("^어려운힌트\\s*:", "").trim();
                    hints.add(Map.of(
                            "difficulty", 3,
                            "hint", hint,
                            "success", true
                    ));
                }
            }
            
            // 최소 3개의 힌트가 생성되었는지 확인
            if (hints.size() < 3) {
                log.warn("생성된 힌트가 3개 미만입니다: {}개", hints.size());
            }
            
        } catch (Exception e) {
            log.error("힌트 응답 파싱 중 오류 발생", e);
        }
        
        return hints;
    }
    
    /**
     * 임시 테이블 통계 조회
     * @return 통계 정보
     */
    public Map<String, Object> getTempWordsStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalWords = tmpForMakeWordsRepository.count();
        long wordsWithoutHints = tmpForMakeWordsRepository.countWordsWithoutHints();
        long wordsWithHints = totalWords - wordsWithoutHints;
        
        stats.put("totalWords", totalWords);
        stats.put("wordsWithoutHints", wordsWithoutHints);
        stats.put("wordsWithHints", wordsWithHints);
        stats.put("completionRate", totalWords > 0 ? (double) wordsWithHints / totalWords * 100 : 0);
        
        return stats;
    }
}
