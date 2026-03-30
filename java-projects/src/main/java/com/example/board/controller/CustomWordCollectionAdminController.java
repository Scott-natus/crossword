package com.example.board.controller;

import com.example.board.entity.TmpForMakeWords;
import com.example.board.repository.TmpForMakeWordsRepository;
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

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class CustomWordCollectionAdminController {

    private final TmpForMakeWordsRepository tmpForMakeWordsRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String geminiApiUrl;

    @GetMapping({"/custom-word-collection", "/custom-word-collection/"})
    public String index(Model model) {
        model.addAttribute("title", "커스텀 단어 수집");
        return "admin/custom-word-collection/index";
    }

    @PostMapping("/api/custom-word-collection/test")
    @ResponseBody
    public Map<String, Object> testPrompt(@RequestParam String category,
                                          @RequestParam String promptTemplate) {
        Map<String, Object> resp = new HashMap<>();
        try {
            List<String> words = callGemini(promptTemplate);
            resp.put("success", true);
            resp.put("category", category);
            resp.put("generatedPrompt", promptTemplate);
            resp.put("generatedWords", words);
            resp.put("wordCount", words.size());
            resp.put("message", "테스트 완료 - " + words.size() + "개 단어 생성");
        } catch (Exception e) {
            log.error("커스텀 프롬프트 테스트 중 오류", e);
            resp.put("success", false);
            resp.put("message", "테스트 중 오류 발생: " + e.getMessage());
        }
        return resp;
    }

    @PostMapping("/api/custom-word-collection/execute")
    @ResponseBody
    public Map<String, Object> executePrompt(@RequestParam String category,
                                             @RequestParam String promptTemplate) {
        Map<String, Object> resp = new HashMap<>();
        try {
            List<String> words = callGemini(promptTemplate);
            int saved = saveWords(category, words);
            resp.put("success", true);
            resp.put("category", category);
            resp.put("generatedWords", words);
            resp.put("totalGenerated", words.size());
            resp.put("savedCount", saved);
            resp.put("duplicateCount", Math.max(0, words.size() - saved));
            resp.put("message", "수집 완료 - " + saved + "개 단어 저장");
        } catch (Exception e) {
            log.error("커스텀 프롬프트 실행 중 오류", e);
            resp.put("success", false);
            resp.put("message", "실행 중 오류 발생: " + e.getMessage());
        }
        return resp;
    }

    @GetMapping("/api/custom-word-collection/recent-words")
    @ResponseBody
    public Map<String, Object> recentWords(@RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> resp = new HashMap<>();
        try {
            var list = tmpForMakeWordsRepository.findTopNByOrderByRegdtDesc(Math.max(1, Math.min(limit, 500)));
            resp.put("success", true);
            resp.put("words", list);
        } catch (Exception e) {
            log.error("최근 단어 조회 중 오류", e);
            resp.put("success", false);
            resp.put("message", "최근 단어 조회 중 오류: " + e.getMessage());
        }
        return resp;
    }

    private List<String> callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            // 키가 없을 경우 안전하게 빈 결과 반환 (UI는 메시지로 안내)
            log.warn("Gemini API 키가 설정되어 있지 않습니다.");
            return Collections.emptyList();
        }

        Map<String, Object> body = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        body.put("contents", contents);

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 2048);
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = geminiApiUrl + "?key=" + geminiApiKey;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("Gemini API 호출 실패: {}", response.getStatusCode());
            return Collections.emptyList();
        }

        return parseWords(response.getBody());
    }

    private List<String> parseWords(String responseBody) {
        List<String> words = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        String text = parts.get(0).get("text").asText("");
                        String[] lines = text.split("\n");
                        for (String line : lines) {
                            String w = line.trim();
                            if (!w.isEmpty() && w.matches("[가-힣]+") && w.length() >= 2 && w.length() <= 10) {
                                words.add(w);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("응답 파싱 중 오류", e);
        }
        return words;
    }

    private int saveWords(String category, List<String> words) {
        int saved = 0;
        for (String w : words) {
            if (!tmpForMakeWordsRepository.existsByWords(w)) {
                TmpForMakeWords entity = TmpForMakeWords.builder()
                        .category(category)
                        .words(w)
                        .hintYn(false)
                        .regdt(LocalDateTime.now())
                        .build();
                tmpForMakeWordsRepository.save(entity);
                saved++;
            }
        }
        return saved;
    }
}



