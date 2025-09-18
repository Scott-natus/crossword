package com.example.crossword.service;

import com.example.crossword.entity.Hint;
import com.example.crossword.entity.Word;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 힌트 생성 서비스
 * Gemini API를 연동하여 자동으로 힌트를 생성하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HintGenerationService {
    
    private final WordService wordService;
    private final HintService hintService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${gemini.api.key:}")
    private String geminiApiKey;
    
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent}")
    private String geminiApiUrl;
    
    /**
     * 특정 단어에 대한 힌트 생성
     */
    @Transactional
    public Hint generateHintForWord(Word word, String hintType) {
        log.debug("단어에 대한 힌트 생성: {}, 타입: {}", word.getWord(), hintType);
        
        try {
            // 기존 힌트 확인
            List<Hint> existingHints = hintService.getHintsByWordIdAndType(word.getId(), hintType);
            if (!existingHints.isEmpty()) {
                log.debug("이미 존재하는 힌트가 있습니다: {}", word.getWord());
                return existingHints.get(0);
            }
            
            // Gemini API 호출
            String hintText = callGeminiApi(word, hintType);
            if (hintText == null || hintText.trim().isEmpty()) {
                log.warn("힌트 생성 실패: {}", word.getWord());
                return null;
            }
            
            // 힌트 저장
            Hint hint = Hint.builder()
                    .word(word)
                    .hintText(hintText.trim())
                    .hintType(hintType)
                    .isPrimary(false)
                    .difficulty(word.getDifficulty())
                    .correctionStatus("n")
                    .createdAt(java.time.LocalDateTime.now())
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();
            
            Hint savedHint = hintService.saveHint(hint);
            log.info("힌트 생성 완료: {} - {}", word.getWord(), hintText);
            
            return savedHint;
            
        } catch (Exception e) {
            log.error("힌트 생성 중 오류 발생: {}, 오류: {}", word.getWord(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 특정 단어에 대한 모든 타입의 힌트 생성
     */
    @Transactional
    public List<Hint> generateAllHintsForWord(Word word) {
        log.debug("단어에 대한 모든 힌트 생성: {}", word.getWord());
        
        List<Hint> generatedHints = new ArrayList<>();
        String[] hintTypes = {"TEXT", "IMAGE", "AUDIO"};
        
        for (String hintType : hintTypes) {
            try {
                Hint hint = generateHintForWord(word, hintType);
                if (hint != null) {
                    generatedHints.add(hint);
                }
            } catch (Exception e) {
                log.error("힌트 타입 {} 생성 실패: {}, 오류: {}", hintType, word.getWord(), e.getMessage());
            }
        }
        
        // 첫 번째 생성된 힌트를 주 힌트로 설정
        if (!generatedHints.isEmpty()) {
            hintService.setAsPrimaryHint(generatedHints.get(0).getId());
        }
        
        log.info("단어 {}에 대한 힌트 생성 완료: {}개", word.getWord(), generatedHints.size());
        return generatedHints;
    }
    
    /**
     * 여러 단어에 대한 힌트 일괄 생성
     */
    @Transactional
    public Map<String, List<Hint>> generateHintsForWords(List<Word> words) {
        log.debug("여러 단어에 대한 힌트 일괄 생성: {}개", words.size());
        
        Map<String, List<Hint>> results = new HashMap<>();
        
        for (Word word : words) {
            try {
                List<Hint> hints = generateAllHintsForWord(word);
                results.put(word.getWord(), hints);
                
                // API 호출 제한을 위한 대기
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("단어 {} 힌트 생성 실패: {}", word.getWord(), e.getMessage());
                results.put(word.getWord(), new ArrayList<>());
            }
        }
        
        log.info("일괄 힌트 생성 완료: {}개 단어 처리", words.size());
        return results;
    }
    
    /**
     * 난이도별 단어에 대한 힌트 생성
     */
    @Transactional
    public Map<String, List<Hint>> generateHintsByDifficulty(Integer difficulty, int limit) {
        log.debug("난이도별 힌트 생성: 난이도 {}, 제한 {}", difficulty, limit);
        
        List<Word> words = wordService.getRandomWordsByDifficulty(difficulty, limit);
        return generateHintsForWords(words);
    }
    
    /**
     * 카테고리별 단어에 대한 힌트 생성
     */
    @Transactional
    public Map<String, List<Hint>> generateHintsByCategory(String category, int limit) {
        log.debug("카테고리별 힌트 생성: 카테고리 {}, 제한 {}", category, limit);
        
        List<Word> words = wordService.getRandomWordsByCategory(category, limit);
        return generateHintsForWords(words);
    }
    
    /**
     * Gemini API 호출
     */
    private String callGeminiApi(Word word, String hintType) {
        log.debug("Gemini API 호출: {}, 타입: {}", word.getWord(), hintType);
        
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            log.warn("Gemini API 키가 설정되지 않았습니다");
            return generateFallbackHint(word, hintType);
        }
        
        try {
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 요청 본문 구성
            Map<String, Object> requestBody = createGeminiRequestBody(word, hintType);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // API 호출
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractHintFromResponse(response.getBody());
            } else {
                log.warn("Gemini API 응답 오류: {}", response.getStatusCode());
                return generateFallbackHint(word, hintType);
            }
            
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            return generateFallbackHint(word, hintType);
        }
    }
    
    /**
     * Gemini API 요청 본문 생성
     */
    private Map<String, Object> createGeminiRequestBody(Word word, String hintType) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 프롬프트 구성
        String prompt = createPrompt(word, hintType);
        
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList(Map.of("text", prompt)));
        
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("content", content);
        
        requestBody.put("contents", Arrays.asList(candidate));
        
        // 생성 설정
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 1024);
        requestBody.put("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    /**
     * 힌트 생성 프롬프트 생성
     */
    private String createPrompt(Word word, String hintType) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("다음 단어에 대한 크로스워드 퍼즐 힌트를 생성해주세요.\n\n");
        prompt.append("단어: ").append(word.getWord()).append("\n");
        prompt.append("길이: ").append(word.getLength()).append("글자\n");
        prompt.append("카테고리: ").append(word.getCategory()).append("\n");
        prompt.append("난이도: ").append(word.getDifficulty()).append("\n\n");
        
        switch (hintType) {
            case "TEXT":
                prompt.append("텍스트 힌트를 생성해주세요. 다음 조건을 만족해야 합니다:\n");
                prompt.append("- 단어의 의미나 정의를 설명\n");
                prompt.append("- 너무 직접적이지 않게 암시적으로 표현\n");
                prompt.append("- 한국어로 작성\n");
                prompt.append("- 1-2문장으로 간결하게\n");
                prompt.append("- 답을 직접적으로 말하지 않도록 주의\n\n");
                prompt.append("힌트: ");
                break;
                
            case "IMAGE":
                prompt.append("이미지 힌트를 생성해주세요. 다음 조건을 만족해야 합니다:\n");
                prompt.append("- 단어와 관련된 이미지 설명\n");
                prompt.append("- 구체적인 시각적 요소 설명\n");
                prompt.append("- 한국어로 작성\n");
                prompt.append("- 1-2문장으로 간결하게\n\n");
                prompt.append("이미지 설명: ");
                break;
                
            case "AUDIO":
                prompt.append("오디오 힌트를 생성해주세요. 다음 조건을 만족해야 합니다:\n");
                prompt.append("- 단어와 관련된 소리나 음향 효과 설명\n");
                prompt.append("- 구체적인 청각적 요소 설명\n");
                prompt.append("- 한국어로 작성\n");
                prompt.append("- 1-2문장으로 간결하게\n\n");
                prompt.append("소리 설명: ");
                break;
                
            default:
                prompt.append("텍스트 힌트를 생성해주세요.\n\n");
                prompt.append("힌트: ");
                break;
        }
        
        return prompt.toString();
    }
    
    /**
     * Gemini API 응답에서 힌트 추출
     */
    private String extractHintFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                if (content != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        String text = (String) parts.get(0).get("text");
                        if (text != null && !text.trim().isEmpty()) {
                            return text.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("응답 파싱 오류: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * API 실패 시 대체 힌트 생성
     */
    private String generateFallbackHint(Word word, String hintType) {
        log.debug("대체 힌트 생성: {}, 타입: {}", word.getWord(), hintType);
        
        switch (hintType) {
            case "TEXT":
                return word.getCategory() + " 관련 단어입니다. " + word.getLength() + "글자입니다.";
            case "IMAGE":
                return word.getCategory() + "와 관련된 이미지를 상상해보세요.";
            case "AUDIO":
                return word.getCategory() + "와 관련된 소리를 들어보세요.";
            default:
                return word.getCategory() + " 관련 단어입니다.";
        }
    }
    
    /**
     * 힌트 품질 평가
     */
    public int evaluateHintQuality(Hint hint) {
        log.debug("힌트 품질 평가: {}", hint.getHintText());
        
        String hintText = hint.getHintText().toLowerCase();
        String wordText = hint.getWord().getWord().toLowerCase();
        
        int score = 0;
        
        // 1. 길이 점수 (적절한 길이)
        if (hintText.length() >= 10 && hintText.length() <= 100) {
            score += 2;
        }
        
        // 2. 직접성 점수 (너무 직접적이지 않음)
        if (!hintText.contains(wordText)) {
            score += 3;
        }
        
        // 3. 의미 관련성 점수
        if (hintText.contains(hint.getWord().getCategory().toLowerCase())) {
            score += 2;
        }
        
        // 4. 문장 구조 점수
        if (hintText.contains(".") || hintText.contains("!") || hintText.contains("?")) {
            score += 1;
        }
        
        // 5. 힌트 타입별 추가 점수
        switch (hint.getHintType()) {
            case "TEXT":
                if (hintText.contains("의미") || hintText.contains("정의") || hintText.contains("설명")) {
                    score += 1;
                }
                break;
            case "IMAGE":
                if (hintText.contains("이미지") || hintText.contains("보기") || hintText.contains("모습")) {
                    score += 1;
                }
                break;
            case "AUDIO":
                if (hintText.contains("소리") || hintText.contains("음향") || hintText.contains("들어")) {
                    score += 1;
                }
                break;
        }
        
        // 점수를 1-5 범위로 정규화
        int normalizedScore = Math.min(5, Math.max(1, score));
        
        log.debug("힌트 품질 점수: {} (원점수: {})", normalizedScore, score);
        return normalizedScore;
    }
    
    /**
     * 힌트 품질 업데이트
     */
    @Transactional
    public void updateHintQuality(Hint hint) {
        int quality = evaluateHintQuality(hint);
        hint.setDifficulty(quality);
        hintService.updateHint(hint);
        
        log.debug("힌트 품질 업데이트: {} -> {}", hint.getHintText(), quality);
    }
    
    /**
     * 힌트 생성 통계 조회
     */
    public Map<String, Object> getHintGenerationStats() {
        log.debug("힌트 생성 통계 조회");
        
        Map<String, Object> stats = new HashMap<>();
        
        // 힌트 타입별 개수
        List<Object[]> typeStats = hintService.getHintCountByType();
        Map<String, Long> typeCounts = new HashMap<>();
        for (Object[] stat : typeStats) {
            typeCounts.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("hintTypes", typeCounts);
        
        // 난이도별 개수
        List<Object[]> difficultyStats = hintService.getHintCountByDifficulty();
        Map<Integer, Long> difficultyCounts = new HashMap<>();
        for (Object[] stat : difficultyStats) {
            difficultyCounts.put((Integer) stat[0], (Long) stat[1]);
        }
        stats.put("difficulties", difficultyCounts);
        
        // 수정 상태별 개수
        List<Object[]> correctionStats = hintService.getHintCountByCorrectionStatus();
        Map<String, Long> correctionCounts = new HashMap<>();
        for (Object[] stat : correctionStats) {
            correctionCounts.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("correctionStatus", correctionCounts);
        
        return stats;
    }
}
