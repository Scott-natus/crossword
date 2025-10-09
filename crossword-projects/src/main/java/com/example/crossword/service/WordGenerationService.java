package com.example.crossword.service;

import com.example.crossword.entity.TmpForMakeWords;
import com.example.crossword.repository.TmpForMakeWordsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.springframework.core.io.ClassPathResource;

/**
 * 재미나이 API를 이용한 단어 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordGenerationService {
    
    private final TmpForMakeWordsRepository tmpForMakeWordsRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    
    /**
     * 재미나이 API를 이용하여 단어 생성
     * @param category 카테고리
     * @param count 생성할 단어 개수
     * @return 생성된 단어 목록
     */
    public List<String> generateWords(String category, int count) {
        try {
            log.info("재미나이 API를 이용한 단어 생성 시작 - 카테고리: {}, 개수: {}", category, count);
            
            // 프롬프트 생성 (사용자가 수정 가능하도록 별도 메서드로 분리)
            String prompt = createWordGenerationPrompt(category, count);
            
            // API 요청 생성
            Map<String, Object> requestBody = createGeminiRequest(prompt);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // API 호출
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseWordGenerationResponse(response.getBody());
            } else {
                log.error("재미나이 API 호출 실패 - 상태코드: {}", response.getStatusCode());
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            log.error("단어 생성 중 오류 발생", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 단어 생성 프롬프트 생성 (파일 기반, 사용자 수정 가능)
     * @param category 카테고리
     * @param count 생성할 단어 개수
     * @return 프롬프트 문자열
     */
    private String createWordGenerationPrompt(String category, int count) {
        try {
            // 파일에서 프롬프트 템플릿 읽기
            ClassPathResource resource = new ClassPathResource("prompts/word-generation-prompt.txt");
            String template = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
            
            // 플레이스홀더 치환
            return template.replace("{category}", category)
                          .replace("{count}", String.valueOf(count));
            
        } catch (IOException e) {
            log.warn("프롬프트 파일을 읽을 수 없어 기본 프롬프트를 사용합니다: {}", e.getMessage());
            
            // 기본 프롬프트 반환
            return String.format("""
                당신은 한글 십자낱말 퍼즐을 위한 단어를 만드는 전문가입니다.
                
                '%s' 카테고리에서 십자낱말 퍼즐에 적합한 단어 %d개를 생성해주세요.
                
                **단어 생성 규칙:**
                1. 한글 단어만 생성 (영어, 숫자, 특수문자 제외)
                2. 2글자 이상 10글자 이하의 단어
                3. 일반인이 알 수 있는 단어 (너무 전문적이거나 생소한 단어 제외)
                4. 십자낱말 퍼즐에 적합한 단어 (너무 추상적이지 않은 구체적인 단어)
                5. 중복되지 않는 다양한 단어
                
                **응답 형식:**
                단어1
                단어2
                단어3
                ...
                
                (다른 설명 없이 단어만 한 줄씩 나열해주세요)
                """, category, count);
        }
    }
    
    /**
     * 재미나이 API 요청 본문 생성
     * @param prompt 프롬프트
     * @return API 요청 본문
     */
    private Map<String, Object> createGeminiRequest(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // contents 배열
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);
        
        // generationConfig 설정
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    /**
     * 재미나이 API 응답에서 단어 목록 파싱
     * @param responseBody API 응답 본문
     * @return 파싱된 단어 목록
     */
    private List<String> parseWordGenerationResponse(String responseBody) {
        List<String> words = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidates = rootNode.get("candidates");
            
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);
                JsonNode content = candidate.get("content");
                
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        String text = parts.get(0).get("text").asText();
                        
                        // 텍스트를 줄 단위로 분리하여 단어 추출
                        String[] lines = text.split("\n");
                        for (String line : lines) {
                            String word = line.trim();
                            if (!word.isEmpty() && !word.startsWith("**") && !word.contains(":")) {
                                // 한글만 포함된 단어인지 확인
                                if (word.matches("[가-힣]+") && word.length() >= 2 && word.length() <= 10) {
                                    words.add(word);
                                }
                            }
                        }
                    }
                }
            }
            
            log.info("파싱된 단어 개수: {}", words.size());
            return words;
            
        } catch (Exception e) {
            log.error("API 응답 파싱 중 오류 발생", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 생성된 단어들을 임시 테이블에 저장
     * @param category 카테고리
     * @param words 생성된 단어 목록
     * @return 저장된 단어 개수
     */
    public int saveGeneratedWords(String category, List<String> words) {
        int savedCount = 0;
        
        for (String word : words) {
            try {
                // 중복 확인
                if (!tmpForMakeWordsRepository.existsByWords(word)) {
                    TmpForMakeWords tmpWord = TmpForMakeWords.builder()
                            .category(category)
                            .words(word)
                            .hintYn(false)
                            .build();
                    
                    tmpForMakeWordsRepository.save(tmpWord);
                    savedCount++;
                    log.info("단어 저장 완료: {} ({})", word, category);
                } else {
                    log.info("중복 단어 건너뛰기: {} ({})", word, category);
                }
            } catch (Exception e) {
                log.error("단어 저장 중 오류 발생: {}", word, e);
            }
        }
        
        log.info("총 {}개 단어 저장 완료", savedCount);
        return savedCount;
    }
    
    /**
     * 단어 생성 및 저장을 한 번에 처리
     * @param category 카테고리
     * @param count 생성할 단어 개수
     * @return 저장된 단어 개수
     */
    public int generateAndSaveWords(String category, int count) {
        List<String> generatedWords = generateWords(category, count);
        if (!generatedWords.isEmpty()) {
            return saveGeneratedWords(category, generatedWords);
        }
        return 0;
    }
    
    /**
     * 임시 테이블의 통계 정보 조회
     * @return 통계 정보
     */
    public Map<String, Object> getTmpWordsStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalWords = tmpForMakeWordsRepository.count();
        long wordsWithoutHints = tmpForMakeWordsRepository.countWordsWithoutHints();
        List<Object[]> categoryStats = tmpForMakeWordsRepository.countWordsByCategory();
        
        stats.put("totalWords", totalWords);
        stats.put("wordsWithoutHints", wordsWithoutHints);
        stats.put("wordsWithHints", totalWords - wordsWithoutHints);
        stats.put("categoryStats", categoryStats);
        
        return stats;
    }
}
