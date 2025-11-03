package com.example.board.service;

import com.example.board.entity.PuzzleGridTemplate;
import com.example.board.entity.PuzzleLevel;
import com.example.board.entity.PzWord;
import com.example.board.entity.PzHint;
import com.example.board.repository.PuzzleGridTemplateRepository;
import com.example.board.repository.PuzzleLevelRepository;
import com.example.board.repository.PzWordRepository;
import com.example.board.repository.PzHintRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Arrays;

@Service
public class PuzzleGridTemplateService {
    
    @Autowired
    private PuzzleGridTemplateRepository templateRepository;
    
    @Autowired
    private PuzzleLevelRepository levelRepository;
    
    @Autowired
    private PzWordRepository wordRepository;
    
    @Autowired
    private PzHintRepository hintRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private FailedWordExtractionService failedWordExtractionService;
    
    private static final Logger log = LoggerFactory.getLogger(PuzzleGridTemplateService.class);

    /**
     * 테마별 단어 추출 (cat2 조건 추가)
     * 레벨 1 템플릿 기반으로 테마별 퍼즐 생성
     */
    public Map<String, Object> extractWordsWithTheme(Integer initialTemplateId, String theme) {
        try {
            log.info("=== [2단계 상세] 테마별 단어 추출 시작 ===");
            log.info("[2-0] 초기 템플릿 ID: {}, 테마: {}", initialTemplateId, theme);
            
            // 최대 5회 재시도 (매 시도마다 새로운 템플릿 선택)
            int maxRetries = 5;
            int retryCount = 0;
            List<Map<String, Object>> extractedWords = new ArrayList<>();
            Map<Integer, String> confirmedWords = new HashMap<>();
            boolean extractionFailed = false;
            
            // 레벨 1 템플릿 목록 조회 (매 시도마다 랜덤 선택)
            List<PuzzleGridTemplate> availableTemplates = templateRepository.findByLevelIdAndIsActiveTrue(1);
            if (availableTemplates.isEmpty()) {
                log.error("[2-1 실패] 레벨 1 활성 템플릿이 없습니다.");
                return Map.of("success", false, "message", "레벨 1 템플릿이 없습니다.");
            }
            
            PuzzleGridTemplate template = null;
            PuzzleLevel level = null;
            List<Map<String, Object>> wordPositions = null;
            
            while (retryCount < maxRetries) {
                retryCount++;
                extractedWords.clear();
                confirmedWords.clear();
                extractionFailed = false;
                
                // 매 시도마다 새로운 템플릿 랜덤 선택
                template = availableTemplates.get(new Random().nextInt(availableTemplates.size()));
                log.info("[2-4] === 시도 #{} 시작 (최대 {}회) ===", retryCount, maxRetries);
                log.info("[2-4-{}] 새 템플릿 선택 - 템플릿 ID: {}, 테마: {}", retryCount, template.getId(), theme);
                
                // 레벨 정보 조회
                final PuzzleGridTemplate finalTemplate = template;
                level = levelRepository.findByLevel(finalTemplate.getLevelId())
                    .orElseThrow(() -> new RuntimeException("레벨 정보를 찾을 수 없습니다: " + finalTemplate.getLevelId()));
                log.info("[2-4-{}] 레벨 정보 - 레벨: {}, 단어 난이도: {}, 힌트 난이도: {}, 교차점 수: {}", 
                    retryCount, level.getLevel(), level.getWordDifficulty(), level.getHintDifficulty(), level.getIntersectionCount());
                
                // 단어 위치 정보 정렬
                wordPositions = new ArrayList<>(template.getWordPositions());
                wordPositions.sort((a, b) -> {
                    Integer idA = (Integer) a.get("id");
                    Integer idB = (Integer) b.get("id");
                    return idA.compareTo(idB);
                });
                
                final List<Map<String, Object>> finalWordPositions = wordPositions;
                log.info("[2-4-{}] 단어 위치 정보 - 총 단어 수: {}, 단어 ID 목록: {}", 
                    retryCount, finalWordPositions.size(), finalWordPositions.stream().map(w -> w.get("id")).collect(java.util.stream.Collectors.toList()));
                
                for (Map<String, Object> word : wordPositions) {
                    Integer wordId = (Integer) word.get("id");
                    Integer length = (Integer) word.get("length");
                    String direction = (String) word.get("direction");
                    
                    // 이미 확정된 단어는 건너뛰기
                    if (confirmedWords.containsKey(wordId)) {
                        log.debug("[2-4-{}] 단어 ID {}는 이미 확정되어 건너뜀", retryCount, wordId);
                        continue;
                    }
                    
                    log.info("[2-4-{}] 단어 ID {} 처리 시작 - 길이: {}, 방향: {}, 위치: ({}, {})", 
                        retryCount, wordId, length, direction, word.get("start_x"), word.get("start_y"));
                    
                    // 교차점 찾기
                    log.info("[2-4-{}] 단어 ID {} - 교차점 탐색 시작...", retryCount, wordId);
                    List<Map<String, Object>> intersections = findIntersectionsWithConfirmedWords(
                        word, confirmedWords, wordPositions);
                    
                    if (intersections.isEmpty()) {
                        // 2-1. 독립 단어 추출 (교차점 없음, 테마 조건 추가)
                        log.info("[2-4-{}] 단어 ID {} - 독립 단어 추출 시도 (교차점 없음, 테마: {})", retryCount, wordId, theme);
                        Map<String, Object> extractedWord = extractIndependentWord(word, level, confirmedWords, theme);
                        
                        if ("추출 실패".equals(extractedWord.get("word"))) {
                            log.error("[2-4-{}] 단어 ID {} - 독립 단어 추출 실패", retryCount, wordId);
                            log.error("  - 길이: {}, 위치: ({}, {})", 
                                word.get("length"), word.get("start_x"), word.get("start_y"));
                            log.error("  - 레벨: {}, 단어 난이도: {}, 힌트 난이도: {}", 
                                level.getLevel(), level.getWordDifficulty(), level.getHintDifficulty());
                            log.error("  - 시도 횟수: {}/{}", retryCount, maxRetries);
                            
                            // 독립 단어 추출 실패 정보 저장
                            try {
                                String failureReason = String.format("독립 단어 추출 실패 - 길이: %s, 난이도: %s", 
                                    word.get("length"), level.getWordDifficulty());
                                
                                failedWordExtractionService.saveIndependentWordFailure(
                                    template.getId(), level.getLevel(), level.getWordDifficulty(),
                                    level.getHintDifficulty(), level.getIntersectionCount(),
                                    wordId, word, confirmedWords, retryCount);
                                log.info("[2-4-{}] 독립 단어 추출 실패 로그 저장 완료", retryCount);
                            } catch (Exception e) {
                                log.error("[2-4-{}] 독립 단어 추출 실패 로그 저장 중 오류", retryCount, e);
                            }
                            
                            extractionFailed = true;
                            continue;
                        }
                        
                        String extractedWordStr = (String) extractedWord.get("word");
                        log.info("[2-4-{}] 단어 ID {} - 독립 단어 추출 성공: '{}' (마스킹: '{}')", 
                            retryCount, wordId, extractedWordStr, maskWord(extractedWordStr));
                        // Map.of()는 null을 허용하지 않으므로 HashMap 사용
                        Map<String, Object> wordData = new HashMap<>();
                        wordData.put("word_id", wordId);
                        wordData.put("pz_word_id", extractedWord.get("pz_word_id"));
                        wordData.put("hint_id", extractedWord.get("hint_id")); // null 허용
                        wordData.put("position", word);
                        wordData.put("type", "no_intersection");
                        wordData.put("extracted_word", maskWord(extractedWordStr));
                        wordData.put("hint", extractedWord.get("hint"));
                        extractedWords.add(wordData);
                        confirmedWords.put(wordId, extractedWordStr);
                        log.info("[2-4-{}] 단어 ID {} - 확정 완료 (현재 확정 단어 수: {}/{})", 
                            retryCount, wordId, confirmedWords.size(), wordPositions.size());
                    } else {
                        // 2-2. 교차점이 있는 경우: 교차점 음절 추출
                        log.info("[2-4-{}] 단어 ID {} - 교차점 발견: {}개", retryCount, wordId, intersections.size());
                        List<Map<String, Object>> confirmedIntersectionSyllables = new ArrayList<>();
                        
                        for (Map<String, Object> intersection : intersections) {
                            Integer connectedWordId = (Integer) intersection.get("connected_word_id");
                            String connectedWord = confirmedWords.get(connectedWordId);
                            Map<String, Object> connectedWordPosition = findWordById(connectedWordId, wordPositions);
                            
                            Integer connectedSyllablePos = getSyllablePosition(connectedWordPosition, intersection);
                            String connectedSyllable = connectedWord.substring(connectedSyllablePos - 1, connectedSyllablePos);
                            
                            Integer currentSyllablePos = getSyllablePosition(word, intersection);
                            
                            log.info("[2-4-{}] 단어 ID {} - 교차점 음절 계산: 연결된 단어 ID {} ('{}'), 음절 위치 {}, 음절 '{}', 현재 단어 음절 위치 {}", 
                                retryCount, wordId, connectedWordId, connectedWord, connectedSyllablePos, connectedSyllable, currentSyllablePos);
                            
                            confirmedIntersectionSyllables.add(Map.of(
                                "syllable", connectedSyllable,
                                "position", currentSyllablePos
                            ));
                        }
                        
                        log.info("[2-4-{}] 단어 ID {} - 확정된 교차점 음절: {}개", retryCount, wordId, confirmedIntersectionSyllables.size());
                        
                        // 2-2. 확정된 음절들과 매칭되는 단어 추출 (테마 조건 추가)
                        log.info("[2-4-{}] 단어 ID {} - 교차점 단어 추출 시도 (확정 음절 {}개, 테마: {})", retryCount, wordId, confirmedIntersectionSyllables.size(), theme);
                        Map<String, Object> extractedWord = extractWordWithConfirmedSyllables(
                            word, level, confirmedIntersectionSyllables, confirmedWords, theme);
                        
                        if (!Boolean.TRUE.equals(extractedWord.get("success"))) {
                            log.error("[2-4-{}] 단어 ID {} - 교차점 단어 추출 실패", retryCount, wordId);
                            log.error("  - 길이: {}, 위치: ({}, {})", 
                                word.get("length"), word.get("start_x"), word.get("start_y"));
                            log.error("  - 레벨: {}, 단어 난이도: {}, 힌트 난이도: {}", 
                                level.getLevel(), level.getWordDifficulty(), level.getHintDifficulty());
                            log.error("  - 교차점 개수: {}, 확정된 교차점 음절: {}", intersections.size(), confirmedIntersectionSyllables);
                            log.error("  - 시도 횟수: {}/{}", retryCount, maxRetries);
                            
                            // 교차점 단어 추출 실패 정보 저장
                            try {
                                String failureReason = String.format("교차점 단어 추출 실패 - 길이: %s, 난이도: %s, 교차점 개수: %d", 
                                    word.get("length"), level.getWordDifficulty(), intersections.size());
                                
                                failedWordExtractionService.saveIntersectionWordFailure(
                                    template.getId(), level.getLevel(), level.getWordDifficulty(),
                                    level.getHintDifficulty(), level.getIntersectionCount(),
                                    wordId, word, failureReason, confirmedWords,
                                    confirmedIntersectionSyllables, retryCount);
                                log.info("[2-4-{}] 교차점 단어 추출 실패 로그 저장 완료", retryCount);
                            } catch (Exception e) {
                                log.error("[2-4-{}] 교차점 단어 추출 실패 로그 저장 중 오류", retryCount, e);
                            }
                            
                            extractionFailed = true;
                            continue;
                        }
                        
                        String extractedWordStr = (String) extractedWord.get("word");
                        log.info("[2-4-{}] 단어 ID {} - 교차점 단어 추출 성공: '{}' (마스킹: '{}')", 
                            retryCount, wordId, extractedWordStr, maskWord(extractedWordStr));
                        // Map.of()는 null을 허용하지 않으므로 HashMap 사용
                        Map<String, Object> wordData = new HashMap<>();
                        wordData.put("word_id", wordId);
                        wordData.put("pz_word_id", extractedWord.get("pz_word_id"));
                        wordData.put("hint_id", extractedWord.get("hint_id")); // null 허용
                        wordData.put("position", word);
                        wordData.put("type", "intersection_connected");
                        wordData.put("extracted_word", maskWord(extractedWordStr));
                        wordData.put("hint", extractedWord.get("hint"));
                        extractedWords.add(wordData);
                        confirmedWords.put(wordId, extractedWordStr);
                        log.info("[2-4-{}] 단어 ID {} - 확정 완료 (현재 확정 단어 수: {}/{})", 
                            retryCount, wordId, confirmedWords.size(), wordPositions.size());
                    }
                }
                
                if (!extractionFailed && extractedWords.size() == wordPositions.size()) {
                    log.info("[2-4-{}] 테마별 단어 추출 성공: 템플릿 ID={}, 테마={}, 단어 수={}", 
                        retryCount, template.getId(), theme, extractedWords.size());
                    break;
                } else {
                    log.warn("[2-4-{}] 테마별 단어 추출 실패 (재시도 {}): 템플릿 ID={}, 추출된 단어={}/{}", 
                        retryCount, retryCount, template.getId(), extractedWords.size(), wordPositions.size());
                }
            }
            
            if (extractionFailed || extractedWords.size() != wordPositions.size()) {
                return Map.of("success", false, "message", 
                    "단어 추출에 실패했습니다. (추출된 단어: " + extractedWords.size() + "/" + wordPositions.size() + ")");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("template", Map.of(
                "id", template.getId(),
                "grid_pattern", template.getGridPattern(),
                "words", extractedWords
            ));
            result.put("extracted_words", extractedWords);
            
            return result;
            
        } catch (Exception e) {
            log.error("테마별 단어 추출 중 오류 발생: {}", e.getMessage(), e);
            return Map.of("success", false, "message", "단어 추출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 라라벨의 extractWords 메서드와 동일한 로직
     */
    public Map<String, Object> extractWordsFromTemplate(
            Integer wordDifficulty, 
            Integer hintDifficulty, 
            Integer wordCount, 
            Integer intersectionCount) {
        
        try {
            // 1. 레벨에 맞는 템플릿 찾기 (레벨 기반으로 변경)
            List<PuzzleGridTemplate> templates = templateRepository.findByLevelIdAndIsActiveTrue(
                wordCount); // wordCount를 levelId로 사용
            
            if (templates.isEmpty()) {
                return Map.of("success", false, "message", "조건에 맞는 템플릿을 찾을 수 없습니다.");
            }
            
            // 3. 최대 5회 재시도 (매번 새로운 템플릿 선택)
            int maxRetries = 5;
            int retryCount = 0;
            List<Map<String, Object>> extractedWords = new ArrayList<>();
            Map<Integer, String> confirmedWords = new HashMap<>(); // word_id => word
            boolean extractionFailed = false;
            
            // 변수들을 루프 밖에서 선언
            PuzzleGridTemplate template = null;
            PuzzleLevel level = null;
            List<Map<String, Object>> wordPositions = null;
            
            while (retryCount < maxRetries) {
                retryCount++;
                extractedWords.clear();
                confirmedWords.clear();
                extractionFailed = false;
                
                // 1. 랜덤하게 템플릿 선택 (매번 새로운 템플릿)
                template = templates.get(new Random().nextInt(templates.size()));
                
                // 레벨 정보에서 난이도 가져오기
                level = levelRepository.findByLevel(template.getLevelId()).orElse(null);
                if (level == null) {
                    return Map.of("success", false, "message", "레벨 정보를 찾을 수 없습니다.");
                }
                
                // 2. word_positions를 id 순서대로 정렬 (라라벨과 동일)
                wordPositions = new ArrayList<>(template.getWordPositions());
                wordPositions.sort((a, b) -> {
                    Integer idA = (Integer) a.get("id");
                    Integer idB = (Integer) b.get("id");
                    return idA.compareTo(idB);
                });
                
                System.out.println("템플릿 로드 시도 #" + retryCount + " 시작 - 템플릿 ID: " + template.getId());
                System.out.println("총 단어 개수: " + wordPositions.size());
                
                for (Map<String, Object> word : wordPositions) {
                    Integer wordId = (Integer) word.get("id");
                    System.out.println("단어 ID " + wordId + " 처리 시작");
                    
                    // 이미 확정된 단어는 건너뛰기
                    if (confirmedWords.containsKey(wordId)) {
                        System.out.println("단어 ID " + wordId + " 이미 확정됨, 건너뛰기");
                        continue;
                    }
                    
                    // 4. 현재 단어와 이미 확정된 단어들 사이의 교차점 찾기
                    List<Map<String, Object>> intersections = findIntersectionsWithConfirmedWords(
                        word, confirmedWords, wordPositions);
                    
                    if (intersections.isEmpty()) {
                        // 5. 교차점이 없으면 독립적으로 단어 추출
                        System.out.println("단어 ID " + wordId + " 교차점 없음, 독립 추출");
                        Map<String, Object> extractedWord = extractIndependentWord(word, level, confirmedWords);
                        if ("추출 실패".equals(extractedWord.get("word"))) {
                            log.error("=== 독립 단어 추출 실패 ===");
                            log.error("단어 ID: {}, 길이: {}, 위치: {}", wordId, word.get("length"), word.get("position"));
                            log.error("레벨: {}, 단어 난이도: {}, 힌트 난이도: {}", 
                                level.getLevel(), level.getWordDifficulty(), level.getHintDifficulty());
                            log.error("시도 횟수: {}", retryCount);
                            
                            // 실패한 단어 추출 정보 저장
                            try {
                                failedWordExtractionService.saveIndependentWordFailure(
                                    template.getId(), level.getLevel(), level.getWordDifficulty(),
                                    level.getHintDifficulty(), level.getIntersectionCount(),
                                    wordId, word, confirmedWords, retryCount);
                                log.info("독립 단어 추출 실패 로그 저장 완료");
                            } catch (Exception e) {
                                log.error("독립 단어 추출 실패 로그 저장 중 오류", e);
                            }
                            
                            extractionFailed = true;
                            continue;
                        }
                        
                        extractedWords.add(Map.of(
                            "word_id", wordId,
                            "pz_word_id", extractedWord.get("pz_word_id"),
                            "hint_id", extractedWord.get("hint_id"),
                            "position", word,
                            "type", "no_intersection",
                            "extracted_word", maskWord((String) extractedWord.get("word")),
                            "hint", extractedWord.get("hint")
                        ));
                        confirmedWords.put(wordId, (String) extractedWord.get("word"));
                        
                    } else {
                        // 6. 교차점이 있으면 확정된 단어들의 교차점 음절 추출
                        List<Map<String, Object>> confirmedIntersectionSyllables = new ArrayList<>();
                        
                        for (Map<String, Object> intersection : intersections) {
                            Integer connectedWordId = (Integer) intersection.get("connected_word_id");
                            String connectedWord = confirmedWords.get(connectedWordId);
                            Map<String, Object> connectedWordPosition = findWordById(connectedWordId, wordPositions);
                            
                            Integer connectedSyllablePos = getSyllablePosition(connectedWordPosition, intersection);
                            String connectedSyllable = connectedWord.substring(connectedSyllablePos - 1, connectedSyllablePos);
                            
                            Integer currentSyllablePos = getSyllablePosition(word, intersection);
                            
                            System.out.println("교차점 음절 계산 - 연결된 단어: " + connectedWord + 
                                ", 연결된 음절 위치: " + connectedSyllablePos + 
                                ", 연결된 음절: " + connectedSyllable + 
                                ", 현재 음절 위치: " + currentSyllablePos);
                            
                            confirmedIntersectionSyllables.add(Map.of(
                                "syllable", connectedSyllable,
                                "position", currentSyllablePos
                            ));
                        }
                        
                        // 7. 확정된 음절들과 매칭되는 단어 추출
                        Map<String, Object> extractedWord = extractWordWithConfirmedSyllables(
                            word, level, confirmedIntersectionSyllables, confirmedWords);
                        
                        if ((Boolean) extractedWord.get("success")) {
                            String extractedWordStr = (String) extractedWord.get("word");
                            log.info("[2-4-{}] 단어 ID {} - 교차점 단어 추출 성공: '{}' (마스킹: '{}')", 
                                retryCount, wordId, extractedWordStr, maskWord(extractedWordStr));
                            extractedWords.add(Map.of(
                                "word_id", wordId,
                                "pz_word_id", extractedWord.get("pz_word_id"),
                                "hint_id", extractedWord.get("hint_id"),
                                "position", word,
                                "type", "intersection_connected",
                                "extracted_word", maskWord(extractedWordStr),
                                "hint", extractedWord.get("hint")
                            ));
                            confirmedWords.put(wordId, extractedWordStr);
                            log.info("[2-4-{}] 단어 ID {} - 확정 완료 (현재 확정 단어 수: {}/{})", 
                                retryCount, wordId, confirmedWords.size(), wordPositions.size());
                        } else {
                            log.error("[2-4-{}] 단어 ID {} - 교차점 단어 추출 실패", retryCount, wordId);
                            log.error("  - 길이: {}, 위치: ({}, {})", 
                                word.get("length"), word.get("start_x"), word.get("start_y"));
                            log.error("  - 레벨: {}, 단어 난이도: {}, 힌트 난이도: {}", 
                                level.getLevel(), level.getWordDifficulty(), level.getHintDifficulty());
                            log.error("  - 교차점 개수: {}, 확정된 교차점 음절: {}", intersections.size(), confirmedIntersectionSyllables);
                            log.error("  - 시도 횟수: {}/{}", retryCount, maxRetries);
                            
                            // 교차점 단어 추출 실패 정보 저장
                            try {
                                String failureReason = String.format("교차점 단어 추출 실패 - 길이: %s, 난이도: %s, 교차점 개수: %d", 
                                    word.get("length"), level.getWordDifficulty(), intersections.size());
                                
                                failedWordExtractionService.saveIntersectionWordFailure(
                                    template.getId(), level.getLevel(), level.getWordDifficulty(),
                                    level.getHintDifficulty(), level.getIntersectionCount(),
                                    wordId, word, failureReason, confirmedWords,
                                    confirmedIntersectionSyllables, retryCount);
                                log.info("교차점 단어 추출 실패 로그 저장 완료");
                            } catch (Exception e) {
                                log.error("교차점 단어 추출 실패 로그 저장 중 오류", e);
                            }
                            
                            extractionFailed = true;
                            continue;
                        }
                    }
                }
                
                // 모든 단어가 성공적으로 추출되었으면 루프 종료
                if (!extractionFailed) {
                    log.info("[2-4-{}] === 단어 추출 성공 ===", retryCount);
                    log.info("[2-4-{}] 추출된 단어 수: {}/{}, 시도 횟수: {}", retryCount, extractedWords.size(), wordPositions.size(), retryCount);
                    break;
                } else {
                    log.warn("[2-4-{}] === 단어 추출 실패 - 재시도 예정 ===", retryCount);
                    log.warn("[2-4-{}] 추출된 단어 수: {}/{}, 시도 횟수: {}/{}", 
                        retryCount, extractedWords.size(), wordPositions.size(), retryCount, maxRetries);
                }
            }
            
            // 5회 시도 후에도 실패한 경우 (라라벨과 동일한 방어 로직)
            if (extractionFailed || extractedWords.size() != wordPositions.size()) {
                log.error("=== 퍼즐 템플릿 로드 실패 ===");
                log.error("템플릿 ID: {}", template.getId());
                log.error("레벨: {}", level.getLevel());
                log.error("단어 난이도: {}, 힌트 난이도: {}, 교차점 수: {}", 
                    level.getWordDifficulty(), level.getHintDifficulty(), level.getIntersectionCount());
                log.error("추출된 단어: {}/{}", extractedWords.size(), wordPositions.size());
                log.error("확정된 단어: {}", confirmedWords.keySet());
                log.error("시도 횟수: {}/5", retryCount);
                
                // 실패한 단어들 상세 로깅
                List<Integer> failedWordIds = new ArrayList<>();
                for (Map<String, Object> word : wordPositions) {
                    Integer wordId = (Integer) word.get("id");
                    if (!confirmedWords.containsKey(wordId)) {
                        failedWordIds.add(wordId);
                        log.error("실패한 단어 ID: {}, 위치: {}, 길이: {}", 
                            wordId, word.get("position"), word.get("length"));
                    }
                }
                log.error("실패한 단어 ID 목록: {}", failedWordIds);
                
                // 데이터베이스에서 사용 가능한 단어 수 확인
                try {
                    long availableWordsCount = wordRepository.countByDifficultyAndIsActiveTrue(level.getWordDifficulty());
                    log.error("사용 가능한 단어 수 (난이도 {}): {}", level.getWordDifficulty(), availableWordsCount);
                } catch (Exception e) {
                    log.error("사용 가능한 단어 수 조회 실패", e);
                }
                
                // 최종 실패 정보 저장 (마지막 시도에서 실패한 단어들)
                try {
                    for (Map<String, Object> word : wordPositions) {
                        Integer wordId = (Integer) word.get("id");
                        if (!confirmedWords.containsKey(wordId)) {
                            // 아직 확정되지 않은 단어들에 대해 실패 로그 저장
                            String failureReason = String.format("5회 시도 후 최종 실패 - 추출된 단어: %d/%d, 실패한 단어 ID: %s", 
                                extractedWords.size(), wordPositions.size(), failedWordIds);
                            
                            log.info("실패 로그 저장 시도 - 단어 ID: {}, 이유: {}", wordId, failureReason);
                            
                            failedWordExtractionService.saveFailedExtraction(
                                template.getId(), level.getLevel(), level.getWordDifficulty(),
                                level.getHintDifficulty(), level.getIntersectionCount(),
                                wordId, word, failureReason, confirmedWords,
                                null, maxRetries);
                        }
                    }
                    log.info("실패 로그 저장 완료");
                } catch (Exception e) {
                    log.error("실패 로그 저장 중 오류 발생", e);
                }
                
                return Map.of("success", false, "message", 
                    "단어 추출에 실패했습니다. 모든 단어를 추출할 수 없습니다. (추출된 단어: " + 
                    extractedWords.size() + "/" + wordPositions.size() + ", 실패한 단어 ID: " + failedWordIds + ")");
            }
            
            // 8. 결과 반환
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("template", Map.of(
                "id", template.getId(),
                "grid_pattern", template.getGridPattern(),
                "words", extractedWords
            ));
            result.put("extracted_words", extractedWords);
        
        return result;
            
        } catch (Exception e) {
            System.err.println("=== PuzzleGridTemplateService.extractWordsFromTemplate 오류 ===");
            System.err.println("오류 메시지: " + e.getMessage());
            System.err.println("오류 클래스: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return Map.of("success", false, "message", "단어 추출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 라라벨의 findIntersectionsWithConfirmedWords와 동일한 로직
     */
    private List<Map<String, Object>> findIntersectionsWithConfirmedWords(
            Map<String, Object> currentWord, 
            Map<Integer, String> confirmedWords, 
            List<Map<String, Object>> wordPositions) {
        
        List<Map<String, Object>> intersections = new ArrayList<>();
        
            for (Map.Entry<Integer, String> entry : confirmedWords.entrySet()) {
                Integer confirmedWordId = entry.getKey();
                Map<String, Object> confirmedWordPosition = findWordById(confirmedWordId, wordPositions);
            
            if (confirmedWordPosition != null) {
                Map<String, Object> intersection = findIntersection(currentWord, confirmedWordPosition);
                if (intersection != null) {
                    intersection.put("connected_word_id", confirmedWordId);
                    intersection.put("connected_word", entry.getValue());
                    intersections.add(intersection);
                }
            }
        }
        
        return intersections;
    }
    
    /**
     * 라라벨의 findIntersection과 동일한 로직
     */
    private Map<String, Object> findIntersection(Map<String, Object> word1, Map<String, Object> word2) {
        String direction1 = (String) word1.get("direction");
        String direction2 = (String) word2.get("direction");
        
        // 같은 방향이면 연결점 찾기
        if (direction1.equals(direction2)) {
            return findConnectionPoint(word1, word2);
        }
        
        // 다른 방향이면 교차점 찾기
        Integer startX1 = (Integer) word1.get("start_x");
        Integer startY1 = (Integer) word1.get("start_y");
        Integer endX1 = (Integer) word1.get("end_x");
        Integer endY1 = (Integer) word1.get("end_y");
        
        Integer startX2 = (Integer) word2.get("start_x");
        Integer startY2 = (Integer) word2.get("start_y");
        Integer endX2 = (Integer) word2.get("end_x");
        Integer endY2 = (Integer) word2.get("end_y");
        
        // 가로-세로 교차점 찾기
        if (direction1.equals("horizontal") && direction2.equals("vertical")) {
            // word1이 가로, word2가 세로
            Integer intersectX = startX2; // 세로 단어의 x 좌표
            Integer intersectY = startY1; // 가로 단어의 y 좌표
            
            // 교차점이 두 단어 모두의 범위 내에 있는지 확인
            if (intersectX >= startX1 && intersectX <= endX1 && 
                intersectY >= startY2 && intersectY <= endY2) {
                
                Map<String, Object> intersection = new HashMap<>();
                intersection.put("x", intersectX);
                intersection.put("y", intersectY);
                intersection.put("horizontal_word_id", (Integer) word1.get("id"));
                intersection.put("vertical_word_id", (Integer) word2.get("id"));
                return intersection;
            }
        } else if (direction1.equals("vertical") && direction2.equals("horizontal")) {
            // word1이 세로, word2가 가로
            Integer intersectX = startX1; // 세로 단어의 x 좌표
            Integer intersectY = startY2; // 가로 단어의 y 좌표
            
            // 교차점이 두 단어 모두의 범위 내에 있는지 확인
            if (intersectX >= startX2 && intersectX <= endX2 && 
                intersectY >= startY1 && intersectY <= endY1) {
            
            Map<String, Object> intersection = new HashMap<>();
            intersection.put("x", intersectX);
            intersection.put("y", intersectY);
            intersection.put("horizontal_word_id", (Integer) word2.get("id"));
            intersection.put("vertical_word_id", (Integer) word1.get("id"));
            return intersection;
            }
        }
        
        return null;
    }
    
    /**
     * 라라벨의 findConnectionPoint와 동일한 로직
     */
    private Map<String, Object> findConnectionPoint(Map<String, Object> word1, Map<String, Object> word2) {
        String direction = (String) word1.get("direction");
        
        if ("horizontal".equals(direction)) {
            // 가로 단어들 간의 연결점 찾기
            Integer startX1 = (Integer) word1.get("start_x");
            Integer endX1 = (Integer) word1.get("end_x");
            Integer startX2 = (Integer) word2.get("start_x");
            Integer endX2 = (Integer) word2.get("end_x");
            Integer y1 = (Integer) word1.get("start_y");
            Integer y2 = (Integer) word2.get("start_y");
            
            // 같은 y 좌표이고 x 좌표가 연결되는지 확인
            if (y1.equals(y2) && (endX1 + 1 == startX2 || endX2 + 1 == startX1)) {
                Integer connectX = endX1 + 1 == startX2 ? endX1 + 1 : endX2 + 1;
                return Map.of("x", connectX, "y", y1);
            }
        } else if ("vertical".equals(direction)) {
            // 세로 단어들 간의 연결점 찾기
            Integer startY1 = (Integer) word1.get("start_y");
            Integer endY1 = (Integer) word1.get("end_y");
            Integer startY2 = (Integer) word2.get("start_y");
            Integer endY2 = (Integer) word2.get("end_y");
            Integer x1 = (Integer) word1.get("start_x");
            Integer x2 = (Integer) word2.get("start_x");
            
            // 같은 x 좌표이고 y 좌표가 연결되는지 확인
            if (x1.equals(x2) && (endY1 + 1 == startY2 || endY2 + 1 == startY1)) {
                Integer connectY = endY1 + 1 == startY2 ? endY1 + 1 : endY2 + 1;
                return Map.of("x", x1, "y", connectY);
            }
        }
        
        return null;
    }
    
    /**
     * 라라벨의 extractIndependentWord와 동일한 로직 (테마 조건 추가 가능)
     */
    private Map<String, Object> extractIndependentWord(Map<String, Object> word, PuzzleLevel level, Map<Integer, String> confirmedWords) {
        return extractIndependentWord(word, level, confirmedWords, null);
    }
    
    /**
     * 테마별 독립 단어 추출 (cat2 조건 추가)
     */
    private Map<String, Object> extractIndependentWord(Map<String, Object> word, PuzzleLevel level, Map<Integer, String> confirmedWords, String theme) {
        Integer length = (Integer) word.get("length");
        try {
            // 이미 사용된 단어들 목록 생성
            List<String> usedWords = new ArrayList<>(confirmedWords.values());
            
            // 새로운 난이도 규칙 적용
            List<Integer> allowedDifficulties = getAllowedDifficulties(level.getWordDifficulty());
            
            // 조건에 맞는 단어 찾기 (기존 퍼즐 로직과 동일, WHERE 조건에 cat2만 추가)
            List<PzWord> words;
            if (theme != null && !theme.isEmpty()) {
                // 테마별 단어 추출 (일시적으로 cat2 LIKE 'K-%' 조건 사용)
                if (usedWords.isEmpty()) {
                    words = wordRepository.findForThemePuzzleGenerationByDifficultyInAndLength(allowedDifficulties, length);
                } else {
                    words = wordRepository.findForThemePuzzleGenerationByDifficultyInAndLengthExcludingUsed(allowedDifficulties, length, usedWords);
                }
                log.info("[DEBUG] 테마별 단어 조회 결과 - 길이: {}, 난이도: {}, 조회된 단어 수: {}, 사용된 단어 수: {}", 
                    length, allowedDifficulties, words.size(), usedWords.size());
            } else {
                // 기존 방식 (테마 조건 없음)
                if (usedWords.isEmpty()) {
                    words = wordRepository.findForPuzzleGenerationByDifficultyInAndLength(allowedDifficulties, length);
                } else {
                    words = wordRepository.findForPuzzleGenerationByDifficultyInAndLengthExcludingUsed(allowedDifficulties, length, usedWords);
                }
            }
            
            if (words.isEmpty()) {
                log.error("[DEBUG] 단어 추출 실패 상세 - 길이: {}, 난이도: {}, 테마: {}, 사용된 단어: {}", 
                    length, allowedDifficulties, theme, usedWords);
                return Map.of("word", "추출 실패");
            }
            
            try {
                // 랜덤하게 단어 선택
                int randomIndex = new Random().nextInt(words.size());
                log.info("[DEBUG] 단어 선택 시도 - 총 단어 수: {}, 선택 인덱스: {}", words.size(), randomIndex);
                PzWord selectedWord = words.get(randomIndex);
                log.info("[DEBUG] 단어 선택 성공 - 단어: {}, ID: {}", selectedWord.getWord(), selectedWord.getId());
                
                // 힌트 찾기 (게임 중 클릭 시에만 사용되므로 실패해도 계속 진행)
                try {
                    List<PzHint> hints = hintRepository.findByWordIdAndDifficulty(selectedWord.getId(), level.getHintDifficulty());
                    String hint = hints.isEmpty() ? "힌트가 없습니다." : hints.get(0).getHintText();
                    Integer hintId = hints.isEmpty() ? null : hints.get(0).getId();
                    
                    log.info("[DEBUG] 힌트 조회 완료 - 힌트 존재: {}, 힌트 ID: {}", !hints.isEmpty(), hintId);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("word", selectedWord.getWord());
                    result.put("pz_word_id", selectedWord.getId());
                    result.put("hint_id", hintId);
                    result.put("hint", hint);
                    return result;
                } catch (Exception hintException) {
                    log.warn("[DEBUG] 힌트 조회 실패 (계속 진행) - 단어 ID: {}, 오류: {}", selectedWord.getId(), hintException.getMessage());
                    // 힌트 조회 실패해도 단어는 반환
                    Map<String, Object> result = new HashMap<>();
                    result.put("word", selectedWord.getWord());
                    result.put("pz_word_id", selectedWord.getId());
                    result.put("hint_id", null);
                    result.put("hint", "힌트 조회 중 오류 발생");
                    return result;
                }
            } catch (Exception selectionException) {
                log.error("[DEBUG] 단어 선택 중 예외 발생 - 길이: {}, 단어 수: {}, 오류: {}", 
                    length, words.size(), selectionException.getMessage(), selectionException);
                return Map.of("word", "추출 실패");
            }
            
        } catch (Exception e) {
            log.error("[DEBUG] extractIndependentWord 전체 예외 - 길이: {}, 테마: {}, 오류: {}", 
                length, theme, e.getMessage(), e);
            return Map.of("word", "추출 실패");
        }
    }

    /**
     * 라라벨의 extractWordWithConfirmedSyllables와 동일한 로직 (테마 조건 추가 가능)
     */
    private Map<String, Object> extractWordWithConfirmedSyllables(
            Map<String, Object> word, 
            PuzzleLevel level, 
            List<Map<String, Object>> confirmedIntersectionSyllables,
            Map<Integer, String> confirmedWords) {
        return extractWordWithConfirmedSyllables(word, level, confirmedIntersectionSyllables, confirmedWords, null);
    }
    
    /**
     * 테마별 교차점 단어 추출 (cat2 조건 추가)
     */
    private Map<String, Object> extractWordWithConfirmedSyllables(
            Map<String, Object> word, 
            PuzzleLevel level, 
            List<Map<String, Object>> confirmedIntersectionSyllables,
            Map<Integer, String> confirmedWords,
            String theme) {
        
        try {
            Integer length = (Integer) word.get("length");
            
            // 이미 사용된 단어들 목록 생성
            List<String> usedWords = new ArrayList<>(confirmedWords.values());
            
            // 새로운 난이도 규칙 적용
            List<Integer> allowedDifficulties = getAllowedDifficulties(level.getWordDifficulty());
            
            // 조건에 맞는 단어들 찾기 (기존 퍼즐 로직과 동일, WHERE 조건에 cat2만 추가)
            List<PzWord> words;
            if (theme != null && !theme.isEmpty()) {
                // 테마별 단어 추출 (일시적으로 cat2 LIKE 'K-%' 조건 사용)
                if (usedWords.isEmpty()) {
                    words = wordRepository.findForThemePuzzleGenerationByDifficultyInAndLength(allowedDifficulties, length);
                } else {
                    words = wordRepository.findForThemePuzzleGenerationByDifficultyInAndLengthExcludingUsed(allowedDifficulties, length, usedWords);
                }
            } else {
                // 기존 방식 (테마 조건 없음)
                if (usedWords.isEmpty()) {
                    words = wordRepository.findForPuzzleGenerationByDifficultyInAndLength(allowedDifficulties, length);
                } else {
                    words = wordRepository.findForPuzzleGenerationByDifficultyInAndLengthExcludingUsed(allowedDifficulties, length, usedWords);
                }
            }
            
            for (PzWord candidateWord : words) {
                // 이미 사용된 단어인지 확인
                if (usedWords.contains(candidateWord.getWord())) {
                    continue;
                }
                
                boolean matchesAllSyllables = true;
                
                // 각 교차점 음절이 매칭되는지 확인
                for (Map<String, Object> syllableInfo : confirmedIntersectionSyllables) {
                    String requiredSyllable = (String) syllableInfo.get("syllable");
                    Integer position = (Integer) syllableInfo.get("position");
                    
                    if (position > candidateWord.getWord().length()) {
                        matchesAllSyllables = false;
                        break;
                    }
                    
                    String actualSyllable = candidateWord.getWord().substring(position - 1, position);
                    if (!requiredSyllable.equals(actualSyllable)) {
                        matchesAllSyllables = false;
                        break;
                    }
                }
                
                if (matchesAllSyllables) {
                    // 힌트 찾기
                    List<PzHint> hints = hintRepository.findByWordIdAndDifficulty(candidateWord.getId(), level.getHintDifficulty());
                    String hint = hints.isEmpty() ? "힌트가 없습니다." : hints.get(0).getHintText();
                    
                    return Map.of(
                        "success", true,
                        "word", candidateWord.getWord(),
                        "pz_word_id", candidateWord.getId(),
                        "hint_id", hints.isEmpty() ? null : hints.get(0).getId(),
                        "hint", hint
                    );
                }
            }
            
            return Map.of("success", false, "message", "조건에 맞는 단어를 찾을 수 없습니다.");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "단어 추출 중 오류가 발생했습니다.");
        }
    }

    /**
     * 라라벨의 getSyllablePosition과 동일한 로직
     */
    private Integer getSyllablePosition(Map<String, Object> word, Map<String, Object> intersectionPosition) {
        String direction = (String) word.get("direction");
        Integer startX = (Integer) word.get("start_x");
        Integer startY = (Integer) word.get("start_y");
        Integer intersectX = (Integer) intersectionPosition.get("x");
        Integer intersectY = (Integer) intersectionPosition.get("y");
        
        if (direction.equals("horizontal")) {
            return intersectX - startX + 1;
        } else if (direction.equals("vertical")) {
            return intersectY - startY + 1;
        }
        
        return 1;
    }

    /**
     * 라라벨의 findWordById와 동일한 로직
     */
    private Map<String, Object> findWordById(Integer wordId, List<Map<String, Object>> wordPositions) {
        return wordPositions.stream()
            .filter(word -> wordId.equals(word.get("id")))
            .findFirst()
            .orElse(null);
    }

    /**
     * 단어 마스킹 (정답 숨기기)
     */
    private String maskWord(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        return "●".repeat(word.length());
    }

    /**
     * 새로운 난이도 규칙에 따른 허용 난이도 반환
     */
    private List<Integer> getAllowedDifficulties(Integer levelDifficulty) {
        return switch (levelDifficulty) {
            case 1 -> Arrays.asList(1, 2); // 레벨 1: 난이도 1,2
            case 2 -> Arrays.asList(1, 2, 3); // 레벨 2: 난이도 1,2,3
            case 3 -> Arrays.asList(2, 3, 4); // 레벨 3: 난이도 2,3,4
            case 4 -> Arrays.asList(3, 4, 5); // 레벨 4: 난이도 3,4,5
            case 5 -> Arrays.asList(4, 5); // 레벨 5: 난이도 4,5
            default -> Arrays.asList(1, 2, 3, 4, 5); // 기본값: 모든 난이도
        };
    }

}