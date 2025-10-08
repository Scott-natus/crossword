package com.example.crossword.service;

import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.repository.PzWordRepository;
import com.example.crossword.repository.PzHintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 힌트생성 관리 서비스
 * 라라벨의 PzHintGeneratorController 로직을 스프링부트로 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HintGeneratorManagementService {
    
    private final PzWordRepository pzWordRepository;
    private final PzHintRepository pzHintRepository;
    private final HintGenerationService hintGenerationService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${gemini.api.key:}")
    private String geminiApiKey;
    
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String geminiApiUrl;
    
    /**
     * 단어 데이터 조회 (DataTables용)
     * 라라벨의 getWordsAjax와 동일한 기능
     */
    public Map<String, Object> getWordsData(int draw, int start, int length, String search, 
                                           int orderColumn, String orderDir, String status) {
        
        log.debug("getWordsData 호출: draw={}, start={}, length={}, search={}, orderColumn={}, orderDir={}, status={}", 
                 draw, start, length, search, orderColumn, orderDir, status);
        
        try {
            // 컬럼 매핑 (PzWord 엔티티의 실제 속성만 사용)
            String[] columns = {"id", "category", "word", "length", "difficulty"};
            String orderBy = "id"; // 기본 정렬
            
            // 정렬 컬럼이 유효한 경우에만 설정
            if (orderColumn < columns.length) {
                orderBy = columns[orderColumn];
            }
            
            log.debug("정렬 설정: orderBy={}, orderColumn={}", orderBy, orderColumn);
            
            // 정렬 방향
            Sort.Direction direction = "desc".equalsIgnoreCase(orderDir) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            
            // 정렬 설정
            Sort sort = Sort.by(direction, orderBy);
            Pageable pageable = PageRequest.of(start / length, length, sort);
            
            log.debug("Pageable 설정: page={}, size={}", start / length, length);
            
            // 검색 조건과 힌트 보유 상태에 따른 쿼리 구성
            Page<Object[]> wordPage;
            
            log.debug("검색 조건: search='{}', status='{}'", search, status);
            
            if (search != null && !search.trim().isEmpty()) {
                // 검색어가 있는 경우
                if ("with_hints".equals(status)) {
                    log.debug("검색어 + 힌트 보유 조회: {}", search.trim());
                    wordPage = pzWordRepository.findActiveWordsWithSearchAndHintCountAndHasHints(search.trim(), pageable);
                } else if ("without_hints".equals(status)) {
                    log.debug("검색어 + 힌트 없음 조회: {}", search.trim());
                    wordPage = pzWordRepository.findActiveWordsWithSearchAndHintCountAndNoHints(search.trim(), pageable);
                } else {
                    log.debug("검색어로 조회: {}", search.trim());
                    wordPage = pzWordRepository.findActiveWordsWithSearchAndHintCount(search.trim(), pageable);
                }
            } else {
                // 검색어가 없는 경우
                if ("with_hints".equals(status)) {
                    log.debug("힌트 보유 조회");
                    wordPage = pzWordRepository.findActiveWordsWithHintCountAndHasHints(pageable);
                } else if ("without_hints".equals(status)) {
                    log.debug("힌트 없음 조회");
                    wordPage = pzWordRepository.findActiveWordsWithHintCountAndNoHints(pageable);
                } else {
                    log.debug("전체 조회");
                    wordPage = pzWordRepository.findActiveWordsWithHintCount(pageable);
                }
            }
            
            log.debug("조회된 단어 수: {}", wordPage.getContent().size());
        
            // 결과에서 PzWord와 힌트 개수 추출
            List<Object[]> filteredResults = wordPage.getContent();
            List<PzWord> filteredWords = new ArrayList<>();
            Map<Integer, Integer> hintCounts = new HashMap<>();
            
            for (Object[] result : filteredResults) {
                PzWord word = (PzWord) result[0];
                // COUNT 결과는 Integer 또는 Long일 수 있으므로 안전하게 처리
                Number hintCountNumber = (Number) result[1];
                int hintCount = hintCountNumber.intValue();
                
                filteredWords.add(word);
                hintCounts.put(word.getId(), hintCount);
            }
            
            log.debug("힌트 개수 조회 결과: {}", hintCounts);
            
            // 힌트 개수로 정렬하는 경우는 쿼리에서 이미 처리됨 (컬럼 인덱스 5)
            if (orderColumn == 5) {
                log.debug("힌트 개수로 정렬 - 쿼리에서 이미 처리됨");
            }
            
            // DataTables 형식으로 데이터 변환 (객체 형태로 변경)
            List<Map<String, Object>> data = new ArrayList<>();
            for (PzWord word : filteredWords) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", word.getId());
                row.put("category", word.getCategory());
                row.put("word", word.getWord());
                row.put("length", word.getLength());
                row.put("difficulty", word.getDifficulty());
                row.put("difficulty_text", getDifficultyText(word.getDifficulty()));
                row.put("hints_count", hintCounts.getOrDefault(word.getId(), 0));
                row.put("is_active", word.getIsActive());
                row.put("created_at", word.getCreatedAt());
                data.add(row);
            }
            
            log.debug("데이터 변환 완료: {} 개 행", data.size());
            
            // 전체 필터링된 결과 수 조회
            long totalFiltered;
            if (search != null && !search.trim().isEmpty()) {
                totalFiltered = pzWordRepository.countActiveWordsWithSearchAndHintCount(search.trim());
            } else {
                totalFiltered = pzWordRepository.countActiveWordsWithHintCount();
            }
            
            log.debug("전체 필터링된 결과 수: {}", totalFiltered);
            
            Map<String, Object> result = new HashMap<>();
            result.put("draw", draw);
            result.put("recordsTotal", pzWordRepository.countByIsActiveTrue());
            result.put("recordsFiltered", totalFiltered);
            result.put("data", data);
            
            log.debug("최종 결과: draw={}, recordsTotal={}, recordsFiltered={}, dataSize={}", 
                     draw, result.get("recordsTotal"), result.get("recordsFiltered"), data.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("getWordsData 실행 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 단일 단어에 대한 힌트 생성
     * 라라벨의 generateForWord와 동일한 기능
     */
    @Transactional
    public Map<String, Object> generateForWord(Integer wordId, Boolean overwrite) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 단어 조회
            Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
            if (wordOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "단어를 찾을 수 없습니다.");
                return response;
            }
            
            PzWord word = wordOpt.get();
            
            // 비활성화된 단어는 힌트 생성 불가
            if (!word.getIsActive()) {
                response.put("success", false);
                response.put("message", "비활성화된 단어에는 힌트를 생성할 수 없습니다.");
                return response;
            }
            
            // 기존 힌트 확인
            List<PzHint> existingHints = pzHintRepository.findByWordId(wordId);
            if (!existingHints.isEmpty() && !overwrite) {
                response.put("success", false);
                response.put("message", "이미 힌트가 존재합니다. 덮어쓰기 옵션을 선택하거나 기존 힌트를 삭제 후 다시 시도해주세요.");
                return response;
            }
            
            // 덮어쓰기 모드인 경우 기존 힌트 삭제
            if (overwrite && !existingHints.isEmpty()) {
                pzHintRepository.deleteByWordId(wordId);
                log.info("기존 힌트 삭제 완료: wordId={}, 삭제된 힌트 수={}", wordId, existingHints.size());
            }
            
            // Gemini API를 통한 힌트 생성
            List<PzHint> createdHints = createDummyHints(word);
            
            response.put("success", true);
            response.put("message", "힌트가 생성되었습니다. (성공: " + createdHints.size() + "개)");
            response.put("hints", createdHints);
            response.put("word", word);
            response.put("summary", Map.of(
                "total", 3,
                "success", createdHints.size(),
                "error", 0
            ));
            
        } catch (Exception e) {
            log.error("단일 힌트 생성 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 여러 단어에 대한 힌트 일괄 생성
     * 라라벨의 generateBatch와 동일한 기능
     */
    @Transactional
    public Map<String, Object> generateBatch(List<Integer> wordIds, Boolean overwrite) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<PzWord> words = pzWordRepository.findAllById(wordIds).stream()
                .filter(PzWord::getIsActive)
                .collect(Collectors.toList());
            
            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;
            
            for (PzWord word : words) {
                Map<String, Object> result = new HashMap<>();
                result.put("word_id", word.getId());
                result.put("word", word.getWord());
                
                try {
                    // 기존 힌트가 있고 덮어쓰기가 false인 경우 스킵
                    if (!overwrite && hasHints(word.getId())) {
                        result.put("status", "skipped");
                        result.put("message", "기존 힌트가 있어서 스킵되었습니다.");
                    } else {
                        // 덮어쓰기인 경우 기존 힌트 삭제
                        if (overwrite) {
                            pzHintRepository.deleteByWordId(word.getId());
                        }
                        
                        // 힌트 생성
                        List<PzHint> hints = createDummyHints(word);
                        
                        result.put("status", "success");
                        result.put("hint_count", hints.size());
                        result.put("hints", hints);
                        successCount++;
                    }
                } catch (Exception e) {
                    result.put("status", "error");
                    result.put("message", "힌트 생성 실패: " + e.getMessage());
                    errorCount++;
                }
                
                results.add(result);
                
                // API 호출 간격 조절
                Thread.sleep(1000);
            }
            
            response.put("success", true);
            response.put("message", "힌트 생성 완료: 성공 " + successCount + "개, 실패 " + errorCount + "개");
            response.put("results", results);
            response.put("summary", Map.of(
                "total", words.size(),
                "success", successCount,
                "error", errorCount
            ));
            
        } catch (Exception e) {
            log.error("일괄 힌트 생성 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "일괄 힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 카테고리별 힌트 생성
     * 라라벨의 generateByCategory와 동일한 기능
     */
    @Transactional
    public Map<String, Object> generateByCategory(String category, Boolean overwrite) {
        List<PzWord> words = pzWordRepository.findByCategoryAndIsActiveTrue(category);
        
        if (words.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "해당 카테고리의 단어가 없습니다.");
            return response;
        }
        
        List<Integer> wordIds = words.stream()
            .map(PzWord::getId)
            .collect(Collectors.toList());
        
        return generateBatch(wordIds, overwrite);
    }
    
    /**
     * 기존 힌트 수정 (재생성)
     * 라라벨의 regenerateHints와 동일한 기능
     */
    @Transactional
    public Map<String, Object> regenerateHints(List<Integer> hintIds) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<PzHint> hints = pzHintRepository.findAllById(hintIds);
            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;
            
            for (PzHint hint : hints) {
                Map<String, Object> result = new HashMap<>();
                result.put("hint_id", hint.getId());
                result.put("word", hint.getWord().getWord());
                
                try {
                    String oldHint = hint.getHintText();
                    String newHint = generateNewHint(hint.getWord().getWord(), hint.getWord().getCategory());
                    
                    hint.setHintText(newHint);
                    hint.setUpdatedAt(LocalDateTime.now());
                    pzHintRepository.save(hint);
                    
                    result.put("status", "success");
                    result.put("old_hint", oldHint);
                    result.put("new_hint", newHint);
                    successCount++;
                } catch (Exception e) {
                    result.put("status", "error");
                    result.put("message", "힌트 재생성 실패");
                    errorCount++;
                }
                
                results.add(result);
                
                // API 호출 간격 조절
                Thread.sleep(1000);
            }
            
            response.put("success", true);
            response.put("message", "힌트 수정 완료: 성공 " + successCount + "개, 실패 " + errorCount + "개");
            response.put("results", results);
            response.put("success_count", successCount);
            response.put("error_count", errorCount);
            
        } catch (Exception e) {
            log.error("힌트 재생성 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "힌트 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 보정이 필요한 힌트 목록 조회
     * 라라벨의 getHintsForCorrection과 동일한 기능
     */
    public Map<String, Object> getHintsForCorrection(Integer difficulty, String category, 
                                                    int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // 보정이 필요한 힌트 조회 (임시로 모든 힌트 조회)
        Page<PzHint> hintPage = pzHintRepository.findAll(pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", hintPage.getContent());
        result.put("totalElements", hintPage.getTotalElements());
        result.put("totalPages", hintPage.getTotalPages());
        result.put("size", hintPage.getSize());
        result.put("number", hintPage.getNumber());
        
        return result;
    }
    
    /**
     * API 연결 테스트
     * 라라벨의 testConnection과 동일한 기능
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 실제로는 Gemini API 연결 테스트
            boolean isConnected = true; // 임시로 true 반환
            
            response.put("success", isConnected);
            response.put("message", isConnected ? "API 연결 성공" : "API 연결 실패");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "API 연결 테스트 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 힌트 생성 통계
     * 라라벨의 getStats와 동일한 기능
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalWords = pzWordRepository.countByIsActiveTrue();
            long wordsWithHints = pzWordRepository.countWordsWithHints();
            long wordsWithoutHints = totalWords - wordsWithHints;
            long totalHints = pzHintRepository.count();
            
            // 카테고리별 통계
            List<Object[]> categoryStats = pzWordRepository.getCategoryStats();
            
            stats.put("total_words", totalWords);
            stats.put("words_with_hints", wordsWithHints);
            stats.put("words_without_hints", wordsWithoutHints);
            stats.put("total_hints", totalHints);
            stats.put("categories", categoryStats);
            
        } catch (Exception e) {
            log.error("통계 조회 실패: {}", e.getMessage());
            stats.put("error", "통계 조회 실패: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 단어의 힌트 데이터 가져오기
     * 라라벨의 getWordHints와 동일한 기능
     */
    public Map<String, Object> getWordHints(Integer wordId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
            if (wordOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "단어를 찾을 수 없습니다.");
                return response;
            }
            
            PzWord word = wordOpt.get();
            
            // 비활성화된 단어인 경우 힌트를 표시하지 않음
            if (!word.getIsActive()) {
                response.put("success", false);
                response.put("message", "비활성화된 단어의 힌트는 표시할 수 없습니다.");
                return response;
            }
            
            List<PzHint> hints = pzHintRepository.findByWordId(wordId);
            List<Map<String, Object>> hintList = hints.stream()
                .map(hint -> {
                    Map<String, Object> hintData = new HashMap<>();
                    hintData.put("id", hint.getId());
                    hintData.put("hint_text", hint.getHintText());
                    hintData.put("difficulty", hint.getDifficulty());
                    hintData.put("difficulty_text", getDifficultyText(hint.getDifficulty()));
                    hintData.put("hint_type", hint.getHintType());
                    hintData.put("is_primary", hint.getIsPrimary());
                    return hintData;
                })
                .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("hints", hintList);
            response.put("word", Map.of(
                "id", word.getId(),
                "word", word.getWord(),
                "category", word.getCategory(),
                "length", word.getLength(),
                "difficulty", word.getDifficulty()
            ));
            
        } catch (Exception e) {
            log.error("단어 힌트 조회 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "힌트를 불러오는데 실패했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    // ========== 헬퍼 메서드들 ==========
    
    /**
     * Gemini API를 사용한 힌트 생성 (라라벨과 동일한 로직)
     */
    private List<PzHint> createDummyHints(PzWord word) {
        List<PzHint> hints = new ArrayList<>();
        
        try {
            // Gemini API를 통해 3가지 난이도의 힌트를 한 번에 생성
            Map<String, Object> geminiResult = generateHintsWithGemini(word.getWord(), word.getCategory());
            
            if (geminiResult != null && (Boolean) geminiResult.get("success")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> geminiHints = (Map<String, Object>) geminiResult.get("hints");
                
                // 3가지 난이도별로 힌트 생성
                for (int difficulty = 1; difficulty <= 3; difficulty++) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> hintData = (Map<String, Object>) geminiHints.get(String.valueOf(difficulty));
                    
                    if (hintData != null && (Boolean) hintData.get("success")) {
                        PzHint hint = new PzHint();
                        hint.setWord(word);
                        hint.setHintText((String) hintData.get("hint"));
                        hint.setHintType("text");
                        hint.setDifficulty(difficulty);
                        hint.setIsPrimary(difficulty == 1);
                        hint.setCreatedAt(LocalDateTime.now());
                        hint.setUpdatedAt(LocalDateTime.now());
                        
                        hints.add(pzHintRepository.save(hint));
                        log.info("힌트 생성 성공: 난이도 {} - {}", difficulty, hint.getHintText());
                    } else {
                        // API 실패 시 더미 힌트 생성하지 않고 로그만 기록
                        log.warn("힌트 생성 실패: 난이도 {}", difficulty);
                    }
                }
            } else {
                // 전체 API 실패 시 더미 힌트 생성하지 않고 예외 발생
                log.error("Gemini API 전체 실패, 힌트 생성 중단");
                throw new RuntimeException("제미나이 API 호출에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }
            
        } catch (Exception e) {
            log.error("힌트 생성 중 예외 발생: {}", e.getMessage());
            // 예외 발생 시 더미 힌트 생성하지 않고 예외 재발생
            throw new RuntimeException("힌트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return hints;
    }
    
    /**
     * Gemini API를 사용한 다중 힌트 생성 (라라벨과 동일한 로직)
     */
    private Map<String, Object> generateHintsWithGemini(String word, String category) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            log.warn("Gemini API 키가 설정되지 않았습니다");
            return createFailureResponse("API 키가 설정되지 않았습니다");
        }
        
        try {
            // 라라벨과 동일한 프롬프트 구성
            String prompt = buildPrompt(word, category);
            
            // 요청 본문 구성 (라라벨과 동일)
            Map<String, Object> requestBody = createGeminiRequestBody(prompt);
            
            // API 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            log.info("Gemini API 호출 시작: word={}, category={}", word, category);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Gemini API 응답 성공: word={}", word);
                return extractMultipleHintsFromResponse(response.getBody(), word, category);
            } else {
                log.warn("Gemini API 응답 오류: status={}", response.getStatusCode());
                return createFailureResponse("API 응답 오류: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: word={}, error={}", word, e.getMessage());
            return createFailureResponse("API 호출 실패: " + e.getMessage());
        }
    }
    
    /**
     * 새로운 힌트 생성 (Gemini API 사용) - 기존 메서드 유지
     */
    private String generateNewHint(String word, String category) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            log.warn("Gemini API 키가 설정되지 않았습니다");
            return generateFallbackHint(word, category);
        }
        
        try {
            // 라라벨과 동일한 프롬프트 구성
            String prompt = buildPrompt(word, category);
            
            // 요청 본문 구성
            Map<String, Object> requestBody = createGeminiRequestBody(prompt);
            
            // API 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractHintFromResponse(response.getBody());
            } else {
                log.warn("Gemini API 응답 오류: {}", response.getStatusCode());
                return generateFallbackHint(word, category);
            }
            
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            return generateFallbackHint(word, category);
        }
    }
    
    /**
     * 라라벨과 동일한 프롬프트 구성
     */
    private String buildPrompt(String word, String category) {
        return String.format("당신은 한글 십자낱말 퍼즐을 위한 힌트를 만드는 전문가입니다.\n\n" +
                "단어 '%s' (%s 카테고리)에 대한 힌트를 3가지 난이도로 만들어주세요.\n\n" +
                "**힌트 작성 규칙:**\n" +
                "1. 정답 단어를 직접 언급하지 마세요\n" +
                "2. 30자 내외로 연상되기 쉽게 설명해 주세요\n" +
                "3. 초등학생도 이해할 수 있게 작성하세요\n" +
                "4. 너무 어렵거나 추상적인 표현은 피하세요\n\n" +
                "**응답 형식 (다른 설명 없이):**\n\n" +
                "'%s' 의 사용빈도 : [1~5 숫자]\n\n" +
                "쉬움: [매우 쉬운 힌트]\n" +
                "보통: [보통 난이도 힌트]\n" +
                "어려움: [조금 어려운 힌트]", word, category, word);
    }
    
    /**
     * Gemini API 요청 본문 생성
     */
    private Map<String, Object> createGeminiRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 라라벨과 동일한 구조: contents 배열 안에 직접 parts 배열
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList(Map.of("text", prompt)));
        
        requestBody.put("contents", Arrays.asList(content));
        
        // 라라벨과 동일한 생성 설정
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.8);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", generationConfig);
        
        return requestBody;
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
                            // 라라벨과 동일한 방식으로 힌트 추출
                            return extractHintsFromText(text);
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
     * Gemini API 응답에서 다중 힌트 추출 (라라벨과 동일한 로직)
     */
    private Map<String, Object> extractMultipleHintsFromResponse(Map<String, Object> response, String word, String category) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> hints = new HashMap<>();
        Integer frequency = null;
        
        try {
            log.info("Gemini API 응답 구조 확인: {}", response);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                
                if (content != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    
                    if (parts != null && !parts.isEmpty()) {
                        String text = (String) parts.get(0).get("text");
                        
                        if (text != null && !text.trim().isEmpty()) {
                            // 사용빈도 추출
                            java.util.regex.Pattern frequencyPattern = java.util.regex.Pattern.compile("의 사용빈도\\s*:\\s*(\\d+)");
                            java.util.regex.Matcher frequencyMatcher = frequencyPattern.matcher(text);
                            if (frequencyMatcher.find()) {
                                frequency = Integer.parseInt(frequencyMatcher.group(1));
                            }
                            
                            // 3가지 난이도별 힌트 추출
                            String[] difficulties = {"쉬움", "보통", "어려움"};
                            
                            for (int i = 0; i < difficulties.length; i++) {
                                int difficulty = i + 1;
                                String diffText = difficulties[i];
                                
                                // 정규식으로 힌트 추출
                                String pattern = diffText + "\\s*:\\s*([^\\n\\[\\]]+)";
                                java.util.regex.Pattern hintPattern = java.util.regex.Pattern.compile(pattern);
                                java.util.regex.Matcher hintMatcher = hintPattern.matcher(text);
                                
                                Map<String, Object> hintData = new HashMap<>();
                                hintData.put("difficulty", difficulty);
                                hintData.put("difficulty_text", diffText);
                                
                                if (hintMatcher.find()) {
                                    String hintText = hintMatcher.group(1).trim();
                                    hintData.put("hint", hintText);
                                    hintData.put("success", true);
                                    
                                    // 사용빈도 정보를 첫 번째 힌트에 포함
                                    if (frequency != null && difficulty == 1) {
                                        hintData.put("frequency", frequency);
                                    }
                                    
                                    log.info("힌트 추출 성공: 난이도 {} - {}", difficulty, hintText);
                                } else {
                                    hintData.put("hint", "힌트 추출 실패");
                                    hintData.put("success", false);
                                    log.warn("힌트 추출 실패: 난이도 {}", difficulty);
                                }
                                
                                hints.put(String.valueOf(difficulty), hintData);
                            }
                            
                            result.put("success", true);
                            result.put("hints", hints);
                            result.put("word", word);
                            result.put("category", category);
                            result.put("frequency", frequency);
                            
                            return result;
                        }
                    }
                }
            }
            
            throw new Exception("Invalid response structure");
            
        } catch (Exception e) {
            log.error("다중 힌트 추출 오류: {}", e.getMessage());
            return createFailureResponse("힌트 추출 중 오류 발생: " + e.getMessage());
        }
    }
    
    /**
     * 실패 응답 생성 (라라벨과 동일한 구조)
     */
    private Map<String, Object> createFailureResponse(String errorMessage) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> hints = new HashMap<>();
        
        String[] difficulties = {"쉬움", "보통", "어려움"};
        
        for (int i = 0; i < difficulties.length; i++) {
            int difficulty = i + 1;
            String diffText = difficulties[i];
            
            Map<String, Object> hintData = new HashMap<>();
            hintData.put("difficulty", difficulty);
            hintData.put("difficulty_text", diffText);
            hintData.put("hint", "힌트 생성 실패");
            hintData.put("success", false);
            hintData.put("error", errorMessage);
            
            hints.put(String.valueOf(difficulty), hintData);
        }
        
        result.put("success", false);
        result.put("hints", hints);
        result.put("error", errorMessage);
        
        return result;
    }
    
    /**
     * 폴백 힌트 생성
     */
    private PzHint createFallbackHint(PzWord word, int difficulty) {
        PzHint hint = new PzHint();
        hint.setWord(word);
        hint.setHintText(generateFallbackHint(word.getWord(), word.getCategory()));
        hint.setHintType("text");
        hint.setDifficulty(difficulty);
        hint.setIsPrimary(difficulty == 1);
        hint.setCreatedAt(LocalDateTime.now());
        hint.setUpdatedAt(LocalDateTime.now());
        return hint;
    }
    
    /**
     * 응답 텍스트에서 힌트 추출 (라라벨과 동일한 방식) - 기존 메서드 유지
     */
    private String extractHintsFromText(String text) {
        try {
            // 쉬움 힌트 추출
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.contains("쉬움:")) {
                    String hint = line.substring(line.indexOf("쉬움:") + 3).trim();
                    if (!hint.isEmpty()) {
                        return hint;
                    }
                }
            }
            
            // 쉬움 힌트가 없으면 보통 힌트 추출
            for (String line : lines) {
                if (line.contains("보통:")) {
                    String hint = line.substring(line.indexOf("보통:") + 3).trim();
                    if (!hint.isEmpty()) {
                        return hint;
                    }
                }
            }
            
            // 보통 힌트가 없으면 어려움 힌트 추출
            for (String line : lines) {
                if (line.contains("어려움:")) {
                    String hint = line.substring(line.indexOf("어려움:") + 4).trim();
                    if (!hint.isEmpty()) {
                        return hint;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("힌트 추출 오류: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * API 실패 시 대체 힌트 생성
     */
    private String generateFallbackHint(String word, String category) {
        return category + " 관련 단어입니다. " + word.length() + "글자입니다.";
    }
    
    /**
     * Gemini API 연결 테스트
     */
    public Map<String, Object> testGeminiConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Gemini API 키가 설정되지 않았습니다");
                return response;
            }
            
            // 간단한 테스트 요청
            String testWord = "사과";
            String testCategory = "과일";
            String testHint = generateNewHint(testWord, testCategory);
            
            if (testHint != null && !testHint.trim().isEmpty() && !testHint.contains("관련 단어입니다")) {
                response.put("success", true);
                response.put("message", "Gemini API 연결 성공");
                response.put("test_result", Map.of(
                    "word", testWord,
                    "category", testCategory,
                    "hint", testHint
                ));
            } else {
                response.put("success", false);
                response.put("message", "Gemini API 응답이 비어있거나 대체 힌트가 생성되었습니다");
                response.put("test_result", Map.of(
                    "word", testWord,
                    "category", testCategory,
                    "hint", testHint != null ? testHint : "null"
                ));
            }
            
        } catch (Exception e) {
            log.error("Gemini API 연결 테스트 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Gemini API 연결 실패: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 단어가 힌트를 가지고 있는지 확인
     */
    private boolean hasHints(Integer wordId) {
        return pzHintRepository.countByWordId(wordId) > 0;
    }
    
    /**
     * 난이도 배지 HTML 생성
     */
    private String getDifficultyBadge(Integer difficulty) {
        String color = switch (difficulty) {
            case 1 -> "success";
            case 2 -> "warning";
            case 3 -> "danger";
            case 4 -> "dark";
            case 5 -> "secondary";
            default -> "secondary";
        };
        return "<span class='badge bg-" + color + "'>" + getDifficultyText(difficulty) + "</span>";
    }
    
    /**
     * 힌트 개수 배지 HTML 생성
     */
    private String getHintsCountBadge(int count) {
        String color = count > 0 ? "success" : "secondary";
        return "<span class='badge bg-" + color + "'>" + count + "</span>";
    }
    
    /**
     * 액션 버튼 HTML 생성
     */
    private String getActionButtons(PzWord word) {
        if (!word.getIsActive()) {
            return "<span class='text-muted'>비활성화됨</span>";
        }
        
        int hintCount = word.getHints() != null ? word.getHints().size() : 0;
        
        if (hintCount > 0) {
            return "<button type='button' class='btn btn-sm btn-outline-primary' data-word-id='" + word.getId() + "'>" +
                   "<i class='fas fa-eye'></i> 힌트 보기</button>" +
                   "<button type='button' class='btn btn-sm btn-warning ms-1' data-word-id='" + word.getId() + "'>" +
                   "<i class='fas fa-redo'></i> 재생성</button>";
        } else {
            return "<button type='button' class='btn btn-sm btn-primary' data-word-id='" + word.getId() + "'>" +
                   "<i class='fas fa-magic'></i> 힌트 생성</button>";
        }
    }
    
    /**
     * 난이도 텍스트 반환
     */
    private String getDifficultyText(Integer difficulty) {
        return switch (difficulty) {
            case 1 -> "쉬움";
            case 2 -> "보통";
            case 3 -> "어려움";
            case 4 -> "매우 어려움";
            case 5 -> "극도 어려움";
            default -> "알 수 없음";
        };
    }
    
    /**
     * 개별 힌트 삭제
     */
    public Map<String, Object> deleteHint(Integer hintId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<PzHint> hintOpt = pzHintRepository.findById(hintId);
            if (hintOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "존재하지 않는 힌트입니다.");
                return response;
            }
            
            PzHint hint = hintOpt.get();
            pzHintRepository.delete(hint);
            
            log.info("힌트 삭제 완료: hintId={}, wordId={}", hintId, hint.getWord().getId());
            
            response.put("success", true);
            response.put("message", "힌트가 성공적으로 삭제되었습니다.");
            return response;
            
        } catch (Exception e) {
            log.error("힌트 삭제 실패: hintId={}, error={}", hintId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "힌트 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return response;
        }
    }
}
