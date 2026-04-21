package com.example.crossword.controller;

import com.example.crossword.entity.TmpForMakeWords;
import com.example.crossword.repository.TmpForMakeWordsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 커스텀 단어 수집 컨트롤러
 * 사용자가 직접 카테고리와 프롬프트를 입력하여 단어를 수집할 수 있는 웹 인터페이스 제공
 */
@Controller
@RequestMapping("/admin/custom-word-collection")
@RequiredArgsConstructor
@Slf4j
public class CustomWordCollectionController {
    
    private final TmpForMakeWordsRepository tmpForMakeWordsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    
    /**
     * 커스텀 단어 수집 메인 페이지
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "커스텀 단어 수집");
        return "admin/custom-word-collection/index";
    }
    
    /**
     * 커스텀 프롬프트로 단어 생성 테스트
     */
    @PostMapping("/test")
    @ResponseBody
    public Map<String, Object> testCustomPrompt(
            @RequestParam String category,
            @RequestParam String promptTemplate) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("커스텀 프롬프트 테스트 - 카테고리: {}, URL: {}", category, geminiApiUrl);
            
            List<String> generatedWords = callGeminiAPI(promptTemplate, "테스트");
            log.info("테스트 완료 - 생성된 단어 수: {}", generatedWords.size());
            
            response.put("success", true);
            response.put("category", category);
            response.put("generatedPrompt", promptTemplate);
            response.put("generatedWords", generatedWords);
            response.put("wordCount", generatedWords.size());
            response.put("message", "테스트 완료 - " + generatedWords.size() + "개 단어 생성");
            
        } catch (Exception e) {
            log.error("커스텀 프롬프트 테스트 중 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "테스트 중 오류 발생: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 커스텀 프롬프트로 단어 수집 실행
     */
    @PostMapping("/execute")
    @ResponseBody
    public Map<String, Object> executeCustomPrompt(
            @RequestParam String category,
            @RequestParam String promptTemplate) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("=== 커스텀 단어 수집 실행 시작 ===");
            log.info("카테고리: {}", category);
            log.info("프롬프트: {}", promptTemplate);
            
            // Gemini API 호출 (프롬프트 그대로 사용)
            log.info("Gemini API 호출 시작...");
            List<String> generatedWords = callGeminiAPI(promptTemplate, "실행");
            log.info("Gemini API 응답 완료 - 생성된 단어 수: {}", generatedWords.size());
            log.info("생성된 단어 목록: {}", generatedWords);
            
            // 단어 저장
            log.info("단어 저장 시작...");
            int savedCount = saveWords(category, generatedWords);
            log.info("단어 저장 완료 - 저장된 단어 수: {}", savedCount);
            
            response.put("success", true);
            response.put("category", category);
            response.put("generatedWords", generatedWords);
            response.put("totalGenerated", generatedWords.size());
            response.put("savedCount", savedCount);
            response.put("duplicateCount", generatedWords.size() - savedCount);
            response.put("message", "수집 완료 - " + savedCount + "개 단어 저장 (" + 
                (generatedWords.size() - savedCount) + "개 중복 제외)");
            
        } catch (Exception e) {
            log.error("=== 커스텀 단어 수집 실행 중 오류 발생 ===", e);
            log.error("오류 메시지: {}", e.getMessage());
            log.error("오류 스택: ", e);
            response.put("success", false);
            response.put("message", "수집 중 오류 발생: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Gemini API 호출
     */
    private List<String> callGeminiAPI(String prompt, String combination) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        
        part.put("text", prompt);
        content.put("parts", Arrays.asList(part));
        requestBody.put("contents", Arrays.asList(content));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        log.info("Gemini API 호출 - 조합: {}", combination);
        ResponseEntity<String> apiResponse = restTemplate.exchange(
            geminiApiUrl, HttpMethod.POST, request, String.class);
        
        if (apiResponse.getStatusCode().is2xxSuccessful()) {
            return parseGeminiResponse(apiResponse.getBody(), combination);
        } else {
            throw new RuntimeException("Gemini API 호출 실패: " + apiResponse.getStatusCode());
        }
    }
    
    /**
     * Gemini API 응답 파싱
     */
    private List<String> parseGeminiResponse(String responseBody, String combination) throws Exception {
        List<String> words = new ArrayList<>();
        
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode textNode = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text");
        
        if (textNode.isTextual()) {
            String[] lines = textNode.asText().split("\\n");
            for (String line : lines) {
                String cleaned = cleanWord(line);
                if (cleaned != null && !cleaned.isEmpty() && isValidWord(cleaned, combination)) {
                    words.add(cleaned);
                }
            }
        }
        
        return words;
    }
    
    /**
     * Gemini 응답 라인에서 번호·기호를 제거하고 한글 단어만 추출
     */
    private String cleanWord(String line) {
        if (line == null) return null;
        String s = line.trim();
        // "1. 단어", "1) 단어", "- 단어", "* 단어" 등의 접두사 제거
        s = s.replaceAll("^[\\d]+[.)\\-:\\s]+", "");
        s = s.replaceAll("^[-*•·\\s]+", "");
        // 괄호 이후 설명 제거: "단어 (설명)" → "단어"
        s = s.replaceAll("[\\s]*[\\(（].*$", "");
        // 남은 공백/특수문자 제거 후 한글만 추출
        s = s.replaceAll("[^가-힣]", "");
        return s.trim();
    }

    /**
     * 단어 유효성 검증
     */
    private boolean isValidWord(String word, String combination) {
        if (word.length() < 2 || word.length() > 10) {
            return false;
        }
        if (!word.matches("^[가-힣]+$")) {
            return false;
        }
        return true;
    }
    
    /**
     * 단어 저장 (중복 제외)
     */
    private int saveWords(String category, List<String> words) {
        int savedCount = 0;
        String fullCategory = category;
        
        for (String word : words) {
            try {
                // 중복 확인
                if (!tmpForMakeWordsRepository.existsByWords(word)) {
                    TmpForMakeWords tmpWord = TmpForMakeWords.builder()
                            .category(fullCategory)
                            .words(word)
                            .hintYn(false)
                            .regdt(LocalDateTime.now())
                            .build();
                    
                    tmpForMakeWordsRepository.save(tmpWord);
                    savedCount++;
                    log.info("단어 저장: {} (카테고리: {})", word, fullCategory);
                } else {
                    log.debug("중복 단어 제외: {}", word);
                }
            } catch (Exception e) {
                log.error("단어 저장 중 오류: {}", word, e);
            }
        }
        
        return savedCount;
    }
    
    /**
     * 최근 수집된 단어 조회
     */
    @GetMapping("/recent-words")
    @ResponseBody
    public Map<String, Object> getRecentWords(@RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TmpForMakeWords> recentWords = tmpForMakeWordsRepository.findTopNByOrderByRegdtDesc(limit);
            
            response.put("success", true);
            response.put("words", recentWords);
            response.put("count", recentWords.size());
            
        } catch (Exception e) {
            log.error("최근 단어 조회 중 오류", e);
            response.put("success", false);
            response.put("message", "최근 단어 조회 중 오류 발생: " + e.getMessage());
        }
        
        return response;
    }
}
