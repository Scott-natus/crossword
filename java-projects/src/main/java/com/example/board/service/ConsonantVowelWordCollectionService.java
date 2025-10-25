package com.example.board.service;

import com.example.board.entity.TmpForMakeWords;
import com.example.board.repository.TmpForMakeWordsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 초성+중성 조합별 단어 수집 서비스
 * 136개 초성+중성 조합에 대해 각각 단어를 수집
 */
// @Service  // 임시 비활성화
@RequiredArgsConstructor
@Slf4j
public class ConsonantVowelWordCollectionService {

    private final TmpForMakeWordsRepository tmpForMakeWordsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WordCollectionPromptService promptService;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    // 현재 진행 상태를 저장하는 Map
    private final Map<String, Object> batchStatus = new HashMap<>();

    // 한글 패턴 (2-6글자)
    private static final Pattern KOREAN_PATTERN = Pattern.compile("^[가-힣]{2,6}$");

    /**
     * 초성+중성 조합별 단어 수집 배치 실행
     * @return 배치 실행 결과
     */
    @Async
    public CompletableFuture<Map<String, Object>> runWordCollectionBatch() {
        Map<String, Object> batchResult = new HashMap<>();
        
        // 136개 초성+중성 조합 목록 조회
        List<String> consonantVowelCombinations = getConsonantVowelCombinations();
        int totalCombinations = consonantVowelCombinations.size();
        int processedCount = 0;
        int successCount = 0;
        int failureCount = 0;
        int totalWordsCollected = 0;
        
        // 배치 상태 초기화
        batchStatus.put("isRunning", true);
        batchStatus.put("totalCombinations", totalCombinations);
        batchStatus.put("processedCount", 0);
        batchStatus.put("successCount", 0);
        batchStatus.put("failureCount", 0);
        batchStatus.put("totalWordsCollected", 0);
        batchStatus.put("currentCombination", "");
        batchStatus.put("startTime", System.currentTimeMillis());
        
        log.info("🎯 초성+중성 조합별 단어 수집 배치 시작: 총 {}개 조합 (30초 간격)", totalCombinations);
        
        try {
            for (String combination : consonantVowelCombinations) {
                if (!(Boolean) batchStatus.get("isRunning")) {
                    log.info("⏹️ 배치 중단됨");
                    break;
                }
                
                batchStatus.put("currentCombination", combination);
                
                log.info("📍 진행상황: {}/{} - 조합 '{}'", 
                    processedCount + 1, totalCombinations, combination);
                
                // 조합별 단어 수집
                Map<String, Object> result = collectWordsForCombination(combination);
                
                processedCount++;
                batchStatus.put("processedCount", processedCount);
                
                if ((Boolean) result.get("success")) {
                    successCount++;
                    batchStatus.put("successCount", successCount);
                    totalWordsCollected += (Integer) result.get("wordsCollected");
                    batchStatus.put("totalWordsCollected", totalWordsCollected);
                    
                    log.info("✅ 성공: '{}' 조합에서 {}개 단어 수집", 
                        combination, result.get("wordsCollected"));
                } else {
                    failureCount++;
                    batchStatus.put("failureCount", failureCount);
                    
                    log.warn("❌ 실패: '{}' 조합 - {}", combination, result.get("message"));
                }
                
                // 30초 대기 (마지막 조합이 아닌 경우)
                if (processedCount < totalCombinations) {
                    log.info("⏳ 30초 대기 중... (다음: {})", 
                        processedCount < totalCombinations ? "계속" : "완료");
                    Thread.sleep(30000); // 30초
                }
            }
            
        } catch (InterruptedException e) {
            log.error("❌ 배치 실행 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ 배치 실행 중 오류 발생", e);
        } finally {
            // 배치 상태 완료 처리
            batchStatus.put("isRunning", false);
            batchStatus.put("endTime", System.currentTimeMillis());
            
            long duration = (Long) batchStatus.get("endTime") - (Long) batchStatus.get("startTime");
            long durationMinutes = duration / (1000 * 60);
            
            log.info("🏁 초성+중성 조합별 단어 수집 완료: 총 {}개 조합 처리 (성공: {}, 실패: {}) - 총 {}개 단어 수집 - 소요시간: {}분", 
                processedCount, successCount, failureCount, totalWordsCollected, durationMinutes);
        }
        
        batchResult.put("totalCombinations", totalCombinations);
        batchResult.put("processedCount", processedCount);
        batchResult.put("successCount", successCount);
        batchResult.put("failureCount", failureCount);
        batchResult.put("totalWordsCollected", totalWordsCollected);
        batchResult.put("duration", batchStatus.get("endTime"));
        batchResult.put("message", "초성+중성 조합별 단어 수집 완료");
        
        return CompletableFuture.completedFuture(batchResult);
    }

    /**
     * 특정 초성+중성 조합에 대한 단어 수집
     * @param combination 초성+중성 조합 (예: "가", "구", "소")
     * @return 수집 결과
     */
    public Map<String, Object> collectWordsForCombination(String combination) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 프롬프트 생성
            String prompt = generatePrompt(combination);
            
            // Gemini API 호출
            List<String> generatedWords = callGeminiAPI(prompt);
            
            if (generatedWords.isEmpty()) {
                result.put("success", false);
                result.put("message", "단어 생성 실패");
                result.put("wordsCollected", 0);
                return result;
            }
            
            // 단어 검증 및 저장
            int savedCount = saveWords(combination, generatedWords);
            
            result.put("success", true);
            result.put("message", String.format("'%s' 조합에서 %d개 단어 수집 완료", combination, savedCount));
            result.put("wordsCollected", savedCount);
            result.put("generatedCount", generatedWords.size());
            
        } catch (Exception e) {
            log.error("❌ 조합 '{}' 단어 수집 중 오류 발생", combination, e);
            result.put("success", false);
            result.put("message", "오류 발생: " + e.getMessage());
            result.put("wordsCollected", 0);
        }
        
        return result;
    }

    /**
     * 프롬프트 생성
     * @param combination 초성+중성 조합
     * @return 생성된 프롬프트
     */
    private String generatePrompt(String combination) {
        return promptService.generatePrompt(combination);
    }

    /**
     * Gemini API 호출
     * @param prompt 프롬프트
     * @return 생성된 단어 목록
     */
    private List<String> callGeminiAPI(String prompt) {
        try {
            // Gemini API 요청 구성
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            
            part.put("text", prompt);
            content.put("parts", Arrays.asList(part));
            requestBody.put("contents", Arrays.asList(content));
            
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("🔍 Gemini API 호출 시작");
            
            // API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                geminiApiUrl, HttpMethod.POST, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return parseGeminiResponse(response.getBody());
            } else {
                log.error("❌ Gemini API 호출 실패: {}", response.getStatusCode());
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            log.error("❌ Gemini API 호출 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    /**
     * Gemini API 응답 파싱
     * @param responseBody API 응답 본문
     * @return 파싱된 단어 목록
     */
    private List<String> parseGeminiResponse(String responseBody) {
        List<String> words = new ArrayList<>();
        
        try {
            // JSON 파싱
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            
            if (response.containsKey("candidates") && response.get("candidates") instanceof List) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                
                if (!candidates.isEmpty() && candidates.get(0).containsKey("content")) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    
                    if (content.containsKey("parts") && content.get("parts") instanceof List) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        
                        if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                            String text = (String) parts.get(0).get("text");
                            
                            // 텍스트를 줄 단위로 분리하여 단어 추출
                            String[] lines = text.split("\n");
                            for (String line : lines) {
                                String word = line.trim();
                                if (!word.isEmpty() && KOREAN_PATTERN.matcher(word).matches()) {
                                    words.add(word);
                                }
                            }
                        }
                    }
                }
            }
            
            log.info("🔍 Gemini API 응답 파싱 완료: {}개 단어 추출", words.size());
            
        } catch (Exception e) {
            log.error("❌ Gemini API 응답 파싱 중 오류 발생", e);
        }
        
        return words;
    }

    /**
     * 단어 저장
     * @param combination 초성+중성 조합
     * @param words 저장할 단어 목록
     * @return 저장된 단어 개수
     */
    private int saveWords(String combination, List<String> words) {
        int savedCount = 0;
        String promptType = promptService.getCurrentPromptType();
        String category = String.format("초성중성_%s:%s", promptType, combination);
        
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
                    log.debug("단어 저장 완료: {} ({})", word, category);
                } else {
                    log.debug("중복 단어 건너뛰기: {} ({})", word, category);
                }
            } catch (Exception e) {
                log.error("단어 저장 중 오류 발생: {}", word, e);
            }
        }
        
        log.info("총 {}개 단어 저장 완료 (카테고리: {})", savedCount, category);
        return savedCount;
    }

    /**
     * 데이터베이스에서 초성+중성 조합 목록 조회
     * @return 초성+중성 조합 목록
     */
    private List<String> getConsonantVowelCombinations() {
        List<String> combinations = new ArrayList<>();
        
        try {
            // 자음모음 카테고리의 모든 단어에서 초성+중성 조합 추출
            List<TmpForMakeWords> allWords = tmpForMakeWordsRepository.findAll();
            
            // 초성+중성 조합으로 변환 (종성 제거)
            Set<String> consonantVowelSet = new HashSet<>();
            for (TmpForMakeWords word : allWords) {
                if (word.getCategory() != null && word.getCategory().startsWith("자음모음:")) {
                    String wordText = word.getWords();
                    if (wordText != null && !wordText.isEmpty()) {
                        // 한글 첫글자에서 초성+중성만 추출
                        String consonantVowel = extractConsonantVowel(wordText);
                        if (consonantVowel != null) {
                            consonantVowelSet.add(consonantVowel);
                        }
                    }
                }
            }
            
            combinations = new ArrayList<>(consonantVowelSet);
            combinations.sort(String::compareTo);
            
            log.info("🔍 초성+중성 조합 조회 완료: {}개", combinations.size());
            log.info("🔍 조합 목록: {}", combinations.stream().limit(10).collect(Collectors.joining(", ")) + 
                (combinations.size() > 10 ? "..." : ""));
            
        } catch (Exception e) {
            log.error("❌ 초성+중성 조합 조회 중 오류 발생", e);
        }
        
        return combinations;
    }

    /**
     * 한글에서 초성+중성만 추출 (종성 제거)
     * @param korean 한글 문자열
     * @return 초성+중성 조합
     */
    private String extractConsonantVowel(String korean) {
        if (korean == null || korean.isEmpty()) {
            return null;
        }
        
        char firstChar = korean.charAt(0);
        
        // 한글 유니코드 범위 확인 (가-힣)
        if (firstChar >= 0xAC00 && firstChar <= 0xD7A3) {
            // 한글 유니코드에서 초성+중성만 추출하는 공식
            int unicode = firstChar - 0xAC00;
            int consonant = unicode / 588;
            int vowel = (unicode % 588) / 28;
            
            // 초성+중성만으로 새로운 유니코드 생성 (종성 = 0)
            int newUnicode = 0xAC00 + consonant * 588 + vowel * 28;
            
            return String.valueOf((char) newUnicode);
        }
        
        return String.valueOf(firstChar);
    }

    /**
     * 배치 상태 조회
     * @return 현재 배치 상태
     */
    public Map<String, Object> getBatchStatus() {
        Map<String, Object> status = new HashMap<>(batchStatus);
        
        // 진행률 계산
        int processed = (Integer) status.getOrDefault("processedCount", 0);
        int total = (Integer) status.getOrDefault("totalCombinations", 0);
        double progress = total > 0 ? (double) processed / total * 100 : 0.0;
        status.put("progress", Math.round(progress * 100.0) / 100.0);
        
        return status;
    }

    /**
     * 배치 중단
     */
    public void stopBatch() {
        batchStatus.put("isRunning", false);
        log.info("⏹️ 배치 중단 요청됨");
    }
}
