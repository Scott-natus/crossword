package com.example.crossword.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@lombok.RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    /**
     * 단어에 대한 힌트 생성 (세 가지 난이도를 한 번에)
     */
    public Map<String, Object> generateHint(String word, String category) {
        log.info("Gemini API Request URL: {}", baseUrl);
        try {
            String prompt = buildPrompt(word, category);

            Map<String, Object> requestData = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();
            parts.put("text", prompt);
            contents.put("parts", Collections.singletonList(parts));
            requestData.put("contents", Collections.singletonList(contents));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.8);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 1024);
            requestData.put("generationConfig", generationConfig);

            String url = baseUrl + "?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<Integer, Map<String, Object>> hints = extractMultipleHintsFromResponse(response.getBody());

                Integer frequency = null;
                if (hints.containsKey(1) && hints.get(1).containsKey("frequency")) {
                    frequency = (Integer) hints.get(1).get("frequency");
                }

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("hints", hints);
                result.put("word", word);
                result.put("category", category);
                result.put("frequency", frequency);
                return result;
            } else {
                return getFailureResponse("API 요청 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Gemini API error for word {}: {}", word, e.getMessage());
            return getFailureResponse("서비스 오류: " + e.getMessage());
        }
    }

    private String buildPrompt(String word, String category) {
        return "당신은 한글 십자낱말 퍼즐을 위한 힌트를 만드는 전문가입니다.\n\n" +
                "단어 '" + word + "' (" + category + " 카테고리)에 대한 힌트를 3가지 난이도로 만들어주세요.\n\n" +
                "**힌트 작성 규칙:**\n" +
                "1. 정답 단어를 직접 언급하지 마세요\n" +
                "2. 30자 내외로 연상되기 쉽게 설명해 주세요\n" +
                "3. 초등학생도 이해할 수 있게 작성하세요\n" +
                "4. 너무 어렵거나 추상적인 표현은 피하세요\n\n" +
                "**응답 형식 (다른 설명 없이):**\n\n" +
                "'" + word + "' 의 사용빈도 : [1~5 숫자]\n\n" +
                "쉬움: [매우 쉬운 힌트]\n" +
                "보통: [보통 난이도 힌트]\n" +
                "어려움: [조금 어려운 힌트]";
    }

    private Map<Integer, Map<String, Object>> extractMultipleHintsFromResponse(Map response) {
        Map<Integer, String> difficulties = new HashMap<>();
        difficulties.put(1, "쉬움");
        difficulties.put(2, "보통");
        difficulties.put(3, "어려움");

        Map<Integer, Map<String, Object>> hints = new HashMap<>();
        Integer frequency = null;

        try {
            List candidates = (List) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map candidate = (Map) candidates.get(0);
                Map content = (Map) candidate.get("content");
                List parts = (List) content.get("parts");
                String text = (String) ((Map) parts.get(0)).get("text");

                // 사용빈도 추출
                Pattern freqPattern = Pattern.compile("의 사용빈도\\s*:\\s*(\\d+)");
                Matcher freqMatcher = freqPattern.matcher(text);
                if (freqMatcher.find()) {
                    frequency = Integer.parseInt(freqMatcher.group(1));
                }

                for (Map.Entry<Integer, String> entry : difficulties.entrySet()) {
                    Integer level = entry.getKey();
                    String diffText = entry.getValue();
                    
                    Pattern pattern = Pattern.compile(diffText + "\\s*:\\s*([^\\n\\[\\]]+)");
                    Matcher matcher = pattern.matcher(text);
                    
                    String hintText = matcher.find() ? matcher.group(1).trim() : "힌트 추출 실패";
                    boolean isSuccess = matcher.reset().find();

                    Map<String, Object> hintData = new HashMap<>();
                    hintData.put("difficulty", level);
                    hintData.put("difficulty_text", diffText);
                    hintData.put("hint", hintText);
                    hintData.put("success", isSuccess);
                    hints.put(level, hintData);
                }

                if (frequency != null && hints.containsKey(1)) {
                    hints.get(1).put("frequency", frequency);
                }

                return hints;
            }
            throw new RuntimeException("Invalid response structure");
        } catch (Exception e) {
            log.error("Multiple Hint extraction error", e);
            return (Map) getFailureResponse("힌트 추출 중 오류 발생").get("hints");
        }
    }

    /**
     * 단어 추출 (재미나이 API 활용)
     */
    public Map<String, Object> generateWords(String prompt) {
        try {
            Map<String, Object> requestData = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();
            parts.put("text", prompt);
            contents.put("parts", Collections.singletonList(parts));
            requestData.put("contents", Collections.singletonList(contents));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 1024);
            requestData.put("generationConfig", generationConfig);

            String url = baseUrl + "?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> words = extractWordsFromResponse(response.getBody());
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("words", words);
                result.put("prompt", prompt);
                return result;
            } else {
                return Map.of("success", false, "error", "API 요청 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Gemini word generation error", e);
            return Map.of("success", false, "error", "서비스 오류: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> extractWordsFromResponse(Map response) {
        List<Map<String, Object>> words = new ArrayList<>();
        try {
            List candidates = (List) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map candidate = (Map) candidates.get(0);
                Map content = (Map) candidate.get("content");
                List partsList = (List) content.get("parts");
                String text = (String) ((Map) partsList.get(0)).get("text");

                String[] lines = text.split("\\r?\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // [카테고리,단어,난이도] 형식 파싱
                    Pattern pattern = Pattern.compile("\\[([^,]+),([^,]+),(\\d+)\\]");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        Map<String, Object> wordMap = new HashMap<>();
                        wordMap.put("category", matcher.group(1).trim());
                        wordMap.put("word", matcher.group(2).trim());
                        wordMap.put("difficulty", Integer.parseInt(matcher.group(3)));
                        words.add(wordMap);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Word extraction error", e);
        }
        return words;
    }

    private Map<String, Object> getFailureResponse(String errorMessage) {
        Map<Integer, String> difficulties = new HashMap<>();
        difficulties.put(1, "쉬움");
        difficulties.put(2, "보통");
        difficulties.put(3, "어려움");

        Map<Integer, Map<String, Object>> hints = new HashMap<>();
        for (Map.Entry<Integer, String> entry : difficulties.entrySet()) {
            Map<String, Object> hintData = new HashMap<>();
            hintData.put("difficulty", entry.getKey());
            hintData.put("difficulty_text", entry.getValue());
            hintData.put("hint", "힌트 생성 실패");
            hintData.put("success", false);
            hintData.put("error", errorMessage);
            hints.put(entry.getKey(), hintData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("hints", hints);
        result.put("error", errorMessage);
        return result;
    }
}
