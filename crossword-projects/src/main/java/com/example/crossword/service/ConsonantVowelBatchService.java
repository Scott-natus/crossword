package com.example.crossword.service;

import com.example.crossword.entity.TmpForMakeWords;
import com.example.crossword.repository.TmpForMakeWordsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 자음-모음 조합을 이용한 단어 수집 배치 서비스
 * 총 196개 조합 (14개 자음 × 14개 모음)을 30초 간격으로 실행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsonantVowelBatchService {

    private final TmpForMakeWordsRepository tmpForMakeWordsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    // 자음 배열 (14개)
    private static final String[] CONSONANTS = {
        "ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    // 모음 배열 (14개)
    private static final String[] VOWELS = {
        "ㅏ", "ㅑ", "ㅓ", "ㅕ", "ㅗ", "ㅛ", "ㅜ", "ㅠ", "ㅡ", "ㅣ", "ㅢ", "ㅘ", "ㅙ", "ㅞ"
    };

    // 현재 진행 상태를 저장하는 Map
    private final Map<String, Object> batchStatus = new HashMap<>();

    /**
     * 자음-모음 조합 프롬프트 생성
     * @param consonant 자음
     * @param vowel 모음
     * @return 생성된 프롬프트
     */
    public String generatePrompt(String consonant, String vowel) {
        return String.format("""
            당신은 한글 단어를 관리하는 관리자입니다. 
           
            초성 '%s' 과 
            중성 '%s' 로 이루어지는 글자로 시작하는 단어를 추천해줘

            - 한글로 이루어진 두글자 이상 6글자 미만의 단어를 최대한 다양하게 추천해줘
            - 숫자나 영어가 들어간 단어는 제외하고  명사와 대명사,고유명사로 추천해줘 ( 외래어 포함)
            - 설명없이 한줄에 한단어씩만 추천해줘
            - 일반적으로 잘 쓰이지 않은 단어는 필요 없어
            """, consonant, vowel);
    }

    /**
     * Gemini API를 호출하여 단어 생성
     * @param consonant 자음
     * @param vowel 모음
     * @return 생성된 단어 목록
     */
    public List<String> generateWordsFromGemini(String consonant, String vowel) {
        try {
            String prompt = generatePrompt(consonant, vowel);
            
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
            
            log.info("🔍 Gemini API 호출 시작: 초성 '{}', 중성 '{}'", consonant, vowel);
            log.info("🔍 API URL: {}", geminiApiUrl);
            log.info("🔍 API Key: {}...", geminiApiKey.substring(0, Math.min(10, geminiApiKey.length())));
            
            // API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                geminiApiUrl, HttpMethod.POST, request, String.class);
            
            log.info("🔍 API 응답 상태: {}", response.getStatusCode());
            log.info("🔍 API 응답 본문: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseGeminiResponse(response.getBody(), consonant, vowel);
            } else {
                log.error("❌ Gemini API 호출 실패: {}", response.getStatusCode());
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            log.error("❌ 단어 생성 중 오류 발생: 초성 '{}', 중성 '{}'", consonant, vowel, e);
            log.error("❌ 오류 상세: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gemini API 응답 파싱
     * @param responseBody API 응답 본문
     * @param consonant 자음
     * @param vowel 모음
     * @return 파싱된 단어 목록
     */
    private List<String> parseGeminiResponse(String responseBody, String consonant, String vowel) {
        List<String> words = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidates = rootNode.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    
                    // 텍스트를 줄 단위로 분리하여 단어 추출
                    String[] lines = text.split("\n");
                    for (String line : lines) {
                        String word = line.trim();
                        if (isValidWord(word)) {
                            words.add(word);
                        }
                    }
                }
            }
            
            log.info("✅ 단어 생성 완료: 초성 '{}', 중성 '{}' → {}개 단어", 
                consonant, vowel, words.size());
            
        } catch (Exception e) {
            log.error("❌ 응답 파싱 중 오류 발생: 초성 '{}', 중성 '{}'", consonant, vowel, e);
        }
        
        return words;
    }

    /**
     * 유효한 단어인지 검증
     * @param word 검증할 단어
     * @return 유효 여부
     */
    private boolean isValidWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        word = word.trim();
        
        // 2글자 이상 6글자 미만
        if (word.length() < 2 || word.length() >= 6) {
            return false;
        }
        
        // 한글만 포함 (숫자, 영어, 특수문자 제외)
        if (!word.matches("^[가-힣]+$")) {
            return false;
        }
        
        return true;
    }

    /**
     * 생성된 단어들을 임시 테이블에 저장
     * @param consonant 자음
     * @param vowel 모음
     * @param words 생성된 단어 목록
     * @return 저장된 단어 개수
     */
    public int saveWordsToTempTable(String consonant, String vowel, List<String> words) {
        if (words.isEmpty()) {
            return 0;
        }
        
        String category = String.format("자음모음:%s%s", consonant, vowel);
        
        try {
            // 1. 전체 단어 목록에서 중복 제거 (메모리에서)
            Set<String> uniqueWords = new HashSet<>(words);
            List<String> distinctWords = new ArrayList<>(uniqueWords);
            
            log.info("🔍 메모리 중복 제거: {}개 → {}개 단어", words.size(), distinctWords.size());
            
            // 2. DB에서 이미 존재하는 단어들 조회 (배치 쿼리)
            List<String> existingWords = tmpForMakeWordsRepository.findExistingWords(distinctWords);
            Set<String> existingWordsSet = new HashSet<>(existingWords);
            
            log.info("📋 DB 중복 체크: {}개 단어 중 {}개가 이미 존재", distinctWords.size(), existingWords.size());
            
            // 3. 중복되지 않는 단어들만 저장
            List<TmpForMakeWords> wordsToSave = new ArrayList<>();
            for (String word : distinctWords) {
                if (!existingWordsSet.contains(word)) {
                    TmpForMakeWords tmpWord = TmpForMakeWords.builder()
                            .category(category)
                            .words(word)
                            .hintYn(false)
                            .build();
                    wordsToSave.add(tmpWord);
                }
            }
            
            // 4. 배치 저장
            if (!wordsToSave.isEmpty()) {
                tmpForMakeWordsRepository.saveAll(wordsToSave);
                log.info("💾 배치 저장 완료: {}개 단어 저장됨 (카테고리: {})", wordsToSave.size(), category);
            } else {
                log.info("⏭️ 저장할 새로운 단어 없음 (모두 중복)");
            }
            
            return wordsToSave.size();
            
        } catch (Exception e) {
            log.error("❌ 단어 저장 중 오류 발생: 초성 '{}', 중성 '{}'", consonant, vowel, e);
            return 0;
        }
    }

    /**
     * 단일 자음-모음 조합 처리
     * @param consonant 자음
     * @param vowel 모음
     * @return 처리 결과
     */
    public Map<String, Object> processConsonantVowelCombination(String consonant, String vowel) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🚀 시작: 초성 '{}', 중성 '{}' 조합 처리", consonant, vowel);
            
            // 1. Gemini API로 단어 생성
            List<String> words = generateWordsFromGemini(consonant, vowel);
            
            if (words.isEmpty()) {
                result.put("success", false);
                result.put("message", "단어 생성 실패");
                return result;
            }
            
            // 2. 임시 테이블에 저장
            int savedCount = saveWordsToTempTable(consonant, vowel, words);
            
            result.put("success", true);
            result.put("consonant", consonant);
            result.put("vowel", vowel);
            result.put("generated_count", words.size());
            result.put("saved_count", savedCount);
            result.put("message", String.format("초성 '%s', 중성 '%s' 조합 처리 완료", consonant, vowel));
            
        } catch (Exception e) {
            log.error("❌ 조합 처리 중 오류: 초성 '{}', 중성 '{}'", consonant, vowel, e);
            result.put("success", false);
            result.put("message", "처리 중 오류 발생: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 전체 배치 실행 (196개 조합)
     * @return 배치 실행 결과
     */
    @Async
    public CompletableFuture<Map<String, Object>> runFullBatch() {
        Map<String, Object> batchResult = new HashMap<>();
        int totalCombinations = CONSONANTS.length * VOWELS.length;
        int processedCount = 0;
        int successCount = 0;
        int failureCount = 0;
        
        // 배치 상태 초기화
        batchStatus.put("isRunning", true);
        batchStatus.put("totalCombinations", totalCombinations);
        batchStatus.put("processedCount", 0);
        batchStatus.put("successCount", 0);
        batchStatus.put("failureCount", 0);
        batchStatus.put("currentCombination", "");
        batchStatus.put("startTime", System.currentTimeMillis());
        
        log.info("🎯 전체 배치 시작: 총 {}개 조합 (30초 간격)", totalCombinations);
        
        try {
            for (String consonant : CONSONANTS) {
                for (String vowel : VOWELS) {
                    if (!(Boolean) batchStatus.get("isRunning")) {
                        log.info("⏹️ 배치 중단됨");
                        break;
                    }
                    
                    String currentCombination = consonant + vowel;
                    batchStatus.put("currentCombination", currentCombination);
                    
                    log.info("📍 진행상황: {}/{} - 초성 '{}', 중성 '{}'", 
                        processedCount + 1, totalCombinations, consonant, vowel);
                    
                    // 조합 처리
                    Map<String, Object> result = processConsonantVowelCombination(consonant, vowel);
                    
                    processedCount++;
                    batchStatus.put("processedCount", processedCount);
                    
                    if ((Boolean) result.get("success")) {
                        successCount++;
                        batchStatus.put("successCount", successCount);
                    } else {
                        failureCount++;
                        batchStatus.put("failureCount", failureCount);
                    }
                    
                    // 30초 대기 (마지막 조합이 아닌 경우)
                    if (processedCount < totalCombinations) {
                        log.info("⏳ 30초 대기 중... (다음: {})", 
                            processedCount < totalCombinations ? "계속" : "완료");
                        Thread.sleep(30000); // 30초
                    }
                }
                
                if (!(Boolean) batchStatus.get("isRunning")) {
                    break;
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
            
            log.info("🏁 배치 완료: 총 {}개 조합 처리 (성공: {}, 실패: {}) - 소요시간: {}분", 
                processedCount, successCount, failureCount, durationMinutes);
        }
        
        batchResult.put("totalCombinations", totalCombinations);
        batchResult.put("processedCount", processedCount);
        batchResult.put("successCount", successCount);
        batchResult.put("failureCount", failureCount);
        batchResult.put("duration", batchStatus.get("endTime"));
        batchResult.put("message", "배치 실행 완료");
        
        return CompletableFuture.completedFuture(batchResult);
    }

    /**
     * 배치 상태 조회
     * @return 현재 배치 상태
     */
    public Map<String, Object> getBatchStatus() {
        Map<String, Object> status = new HashMap<>(batchStatus);
        
        if ((Boolean) status.getOrDefault("isRunning", false)) {
            int processed = (Integer) status.getOrDefault("processedCount", 0);
            int total = (Integer) status.getOrDefault("totalCombinations", 196);
            double progress = (double) processed / total * 100;
            status.put("progress", Math.round(progress * 100.0) / 100.0);
        }
        
        return status;
    }

    /**
     * 배치 중단
     */
    public void stopBatch() {
        batchStatus.put("isRunning", false);
        log.info("⏹️ 배치 중단 요청됨");
    }

    /**
     * 실패한 조합들만 처리하는 배치 실행
     * @return 배치 실행 결과
     */
    @Async
    public CompletableFuture<Map<String, Object>> runFailedCombinationsBatch() {
        Map<String, Object> batchResult = new HashMap<>();
        
        // 실패한 조합들 목록 (데이터베이스에서 조회)
        List<String[]> failedCombinations = getFailedCombinations();
        int totalFailed = failedCombinations.size();
        int processedCount = 0;
        int successCount = 0;
        int failureCount = 0;
        
        // 배치 상태 초기화
        batchStatus.put("isRunning", true);
        batchStatus.put("totalCombinations", totalFailed);
        batchStatus.put("processedCount", 0);
        batchStatus.put("successCount", 0);
        batchStatus.put("failureCount", 0);
        batchStatus.put("currentCombination", "");
        batchStatus.put("startTime", System.currentTimeMillis());
        
        log.info("🎯 실패한 조합 재처리 시작: 총 {}개 조합 (30초 간격)", totalFailed);
        
        try {
            for (String[] combination : failedCombinations) {
                if (!(Boolean) batchStatus.get("isRunning")) {
                    log.info("⏹️ 배치 중단됨");
                    break;
                }
                
                String consonant = combination[0];
                String vowel = combination[1];
                String currentCombination = consonant + vowel;
                batchStatus.put("currentCombination", currentCombination);
                
                log.info("📍 진행상황: {}/{} - 초성 '{}', 중성 '{}'", 
                    processedCount + 1, totalFailed, consonant, vowel);
                
                // 조합 처리
                Map<String, Object> result = processConsonantVowelCombination(consonant, vowel);
                
                processedCount++;
                batchStatus.put("processedCount", processedCount);
                
                if ((Boolean) result.get("success")) {
                    successCount++;
                    batchStatus.put("successCount", successCount);
                } else {
                    failureCount++;
                    batchStatus.put("failureCount", failureCount);
                }
                
                // 30초 대기 (마지막 조합이 아닌 경우)
                if (processedCount < totalFailed) {
                    log.info("⏳ 30초 대기 중... (다음: {})", 
                        processedCount < totalFailed ? "계속" : "완료");
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
            
            log.info("🏁 실패한 조합 재처리 완료: 총 {}개 조합 처리 (성공: {}, 실패: {}) - 소요시간: {}분", 
                processedCount, successCount, failureCount, durationMinutes);
        }
        
        batchResult.put("totalCombinations", totalFailed);
        batchResult.put("processedCount", processedCount);
        batchResult.put("successCount", successCount);
        batchResult.put("failureCount", failureCount);
        batchResult.put("duration", batchStatus.get("endTime"));
        batchResult.put("message", "실패한 조합 재처리 완료");
        
        return CompletableFuture.completedFuture(batchResult);
    }

    /**
     * 실패한 조합들을 데이터베이스에서 조회
     * @return 실패한 조합 목록
     */
    private List<String[]> getFailedCombinations() {
        List<String[]> failedCombinations = new ArrayList<>();
        
        try {
            // 모든 가능한 조합 생성
            List<String[]> allCombinations = new ArrayList<>();
            for (String consonant : CONSONANTS) {
                for (String vowel : VOWELS) {
                    allCombinations.add(new String[]{consonant, vowel});
                }
            }
            
            // 성공한 조합들 조회 (실시간으로 최신 데이터 조회)
            List<String> successfulCombinations = tmpForMakeWordsRepository.findSuccessfulCombinations();
            Set<String> successfulSet = new HashSet<>(successfulCombinations);
            
            log.info("🔍 성공한 조합 수: {}개", successfulSet.size());
            
            // 실패한 조합들 필터링
            for (String[] combination : allCombinations) {
                String combinationKey = combination[0] + combination[1];
                if (!successfulSet.contains(combinationKey)) {
                    failedCombinations.add(combination);
                }
            }
            
            log.info("🔍 실패한 조합 조회 완료: {}개", failedCombinations.size());
            log.info("🔍 실패한 조합 목록: {}", 
                failedCombinations.stream()
                    .map(combo -> combo[0] + combo[1])
                    .limit(10)
                    .collect(java.util.stream.Collectors.joining(", ")) + 
                (failedCombinations.size() > 10 ? "..." : ""));
            
        } catch (Exception e) {
            log.error("❌ 실패한 조합 조회 중 오류 발생", e);
        }
        
        return failedCombinations;
    }

    /**
     * 특정 조합부터 배치 재시작
     * @param startConsonant 시작 자음
     * @param startVowel 시작 모음
     * @return 배치 실행 결과
     */
    @Async
    public CompletableFuture<Map<String, Object>> resumeBatchFrom(String startConsonant, String startVowel) {
        // TODO: 특정 조합부터 재시작하는 로직 구현
        log.info("🔄 배치 재시작: 초성 '{}', 중성 '{}'부터", startConsonant, startVowel);
        return runFullBatch(); // 임시로 전체 배치 실행
    }
}
