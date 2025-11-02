package com.example.board.service;

import com.example.board.entity.PzWord;
import com.example.board.entity.PzHint;
import com.example.board.entity.PuzzleGridTemplate;
import com.example.board.repository.PzWordRepository;
import com.example.board.repository.PzHintRepository;
import com.example.board.repository.PuzzleGridTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 테마별 퍼즐 생성 서비스
 * Phase 2: 레벨 1 템플릿 기반 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThemePuzzleGeneratorService {
    
    private final PzWordRepository pzWordRepository;
    private final PzHintRepository pzHintRepository;
    private final PuzzleGridTemplateRepository puzzleGridTemplateRepository;
    private final PuzzleGridTemplateService puzzleGridTemplateService;
    
    /**
     * 테마별 퍼즐 생성 (레벨 1 템플릿 기반)
     */
    @Transactional
    public Map<String, Object> generateThemePuzzle(String theme) {
        try {
            log.info("테마별 퍼즐 생성 시작: {} (레벨 1 템플릿 기반)", theme);
            
            // 1. 레벨 1 템플릿 랜덤 선택
            List<PuzzleGridTemplate> templates = puzzleGridTemplateRepository
                .findRandomByLevelIdAndIsActiveTrue(1);
            
            if (templates.isEmpty()) {
                log.error("레벨 1 활성 템플릿이 없습니다.");
                return Map.of(
                    "success", false,
                    "message", "레벨 1 템플릿이 없습니다."
                );
            }
            
            // 첫 번째 템플릿 선택 (이미 RANDOM()으로 정렬됨)
            PuzzleGridTemplate selectedTemplate = templates.get(0);
            log.info("선택된 템플릿 ID: {}", selectedTemplate.getId());
            
            // 2. 템플릿 기반 단어 추출 (cat2 조건 추가)
            Map<String, Object> extractResult = puzzleGridTemplateService
                .extractWordsWithTheme(selectedTemplate.getId(), theme);
            
            if (!Boolean.TRUE.equals(extractResult.get("success"))) {
                log.error("테마별 단어 추출 실패: {}", extractResult.get("message"));
                return Map.of(
                    "success", false,
                    "message", extractResult.get("message") != null 
                        ? extractResult.get("message").toString() 
                        : "단어 추출에 실패했습니다."
                );
            }
            
            // 3. 추출된 단어와 템플릿 정보로 퍼즐 데이터 구성
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> extractedWords = (List<Map<String, Object>>) extractResult.get("extracted_words");
            
            if (extractedWords == null || extractedWords.isEmpty()) {
                log.error("추출된 단어가 없습니다.");
                return Map.of(
                    "success", false,
                    "message", "추출된 단어가 없습니다."
                );
            }
            
            // 힌트 조회
            List<Map<String, Object>> hints = getHintsForExtractedWords(extractedWords);
            
            // 퍼즐 데이터 구성
            Map<String, Object> puzzleData = new HashMap<>();
            puzzleData.put("success", true);
            puzzleData.put("puzzleId", generatePuzzleId());
            puzzleData.put("theme", theme);
            puzzleData.put("templateId", selectedTemplate.getId());
            puzzleData.put("words", extractedWords);
            puzzleData.put("hints", hints);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> templateInfo = (Map<String, Object>) extractResult.get("template");
            if (templateInfo != null) {
                puzzleData.put("grid", templateInfo.get("grid_pattern"));
            }
            
            puzzleData.put("generatedAt", new Date());
            
            log.info("테마별 퍼즐 생성 완료: {} - 템플릿 ID: {}, 단어 {}개", 
                theme, selectedTemplate.getId(), extractedWords.size());
            
            return puzzleData;
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 생성 중 오류 발생: {} - {}", theme, e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "퍼즐 생성에 실패했습니다: " + e.getMessage()
            );
        }
    }
    
    /**
     * 퍼즐 ID로 퍼즐 데이터 조회
     */
    public Map<String, Object> getPuzzleData(Integer puzzleId) {
        try {
            log.info("퍼즐 데이터 조회: {}", puzzleId);
            
            // 퍼즐 ID를 기반으로 퍼즐 데이터 조회
            // 실제 구현에서는 퍼즐 데이터를 저장하고 조회하는 로직이 필요
            
            // 임시로 기본 퍼즐 데이터 반환
            Map<String, Object> puzzleData = Map.of(
                "success", true,
                "puzzleId", puzzleId,
                "words", new ArrayList<>(),
                "hints", new ArrayList<>(),
                "grid", Map.of(
                    "width", 15,
                    "height", 15,
                    "grid", new String[15][15]
                )
            );
            
            log.info("퍼즐 데이터 조회 완료: {}", puzzleId);
            
            return puzzleData;
            
        } catch (Exception e) {
            log.error("퍼즐 데이터 조회 중 오류 발생: {} - {}", puzzleId, e.getMessage());
            return Map.of(
                "success", false,
                "message", "퍼즐 데이터 조회에 실패했습니다."
            );
        }
    }
    
    /**
     * 퍼즐에 사용할 단어 선택 (힌트가 있는 단어만 선택)
     */
    private List<PzWord> selectWordsForPuzzle(List<PzWord> themeWords) {
        try {
            log.info("퍼즐용 단어 선택 시작: {}개 중에서", themeWords.size());
            
            // 테마별 단어 조회 시 이미 힌트가 있는 단어만 조회됨 (findByCat2AndIsActiveTrueAndHasHints)
            // 별도 필터링 불필요
            
            log.info("힌트가 있는 단어: {}개", themeWords.size());
            
            if (themeWords.isEmpty()) {
                log.warn("힌트가 있는 단어가 없습니다.");
                return new ArrayList<>();
            }
            
            // 난이도별 단어 분류
            Map<Integer, List<PzWord>> wordsByDifficulty = themeWords.stream()
                .collect(Collectors.groupingBy(PzWord::getDifficulty));
            
            List<PzWord> selectedWords = new ArrayList<>();
            
            // 각 난이도에서 2-3개씩 선택
            for (int difficulty = 1; difficulty <= 5; difficulty++) {
                List<PzWord> difficultyWords = wordsByDifficulty.getOrDefault(difficulty, new ArrayList<>());
                if (!difficultyWords.isEmpty()) {
                    Collections.shuffle(difficultyWords);
                    int selectCount = Math.min(3, difficultyWords.size());
                    selectedWords.addAll(difficultyWords.subList(0, selectCount));
                }
            }
            
            // 최대 15개 단어로 제한
            if (selectedWords.size() > 15) {
                selectedWords = selectedWords.subList(0, 15);
            }
            
            log.info("퍼즐용 단어 선택 완료: {}개 (힌트 보유 단어 중에서)", selectedWords.size());
            
            return selectedWords;
            
        } catch (Exception e) {
            log.error("퍼즐용 단어 선택 중 오류 발생: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 퍼즐 그리드 생성
     */
    private Map<String, Object> generatePuzzleGrid(List<PzWord> words) {
        try {
            log.info("퍼즐 그리드 생성 시작: {}개 단어", words.size());
            
            // 기본 그리드 크기
            int gridSize = 15;
            String[][] grid = new String[gridSize][gridSize];
            
            // 그리드 초기화
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    grid[i][j] = "";
                }
            }
            
            // 단어 배치 (간단한 구현)
            List<Map<String, Object>> placedWords = new ArrayList<>();
            int wordIndex = 0;
            
            for (PzWord word : words) {
                if (wordIndex >= 10) break; // 최대 10개 단어만 배치
                
                Map<String, Object> placedWord = Map.of(
                    "word", word.getWord(),
                    "startRow", wordIndex * 2,
                    "startCol", 0,
                    "direction", "horizontal",
                    "length", word.getWord().length()
                );
                
                placedWords.add(placedWord);
                wordIndex++;
            }
            
            Map<String, Object> gridData = Map.of(
                "width", gridSize,
                "height", gridSize,
                "grid", grid,
                "placedWords", placedWords,
                "wordCount", placedWords.size()
            );
            
            log.info("퍼즐 그리드 생성 완료: {}개 단어 배치", placedWords.size());
            
            return gridData;
            
        } catch (Exception e) {
            log.error("퍼즐 그리드 생성 중 오류 발생: {}", e.getMessage());
            return Map.of(
                "width", 15,
                "height", 15,
                "grid", new String[15][15],
                "placedWords", new ArrayList<>(),
                "wordCount", 0
            );
        }
    }
    
    /**
     * 추출된 단어들의 힌트 조회 (템플릿 기반)
     */
    private List<Map<String, Object>> getHintsForExtractedWords(List<Map<String, Object>> extractedWords) {
        try {
            log.info("추출된 단어별 힌트 조회 시작: {}개 단어", extractedWords.size());
            
            List<Map<String, Object>> hints = new ArrayList<>();
            
            for (Map<String, Object> extractedWord : extractedWords) {
                Integer pzWordId = (Integer) extractedWord.get("pz_word_id");
                Integer hintId = extractedWord.get("hint_id") != null ? (Integer) extractedWord.get("hint_id") : null;
                
                if (pzWordId == null) {
                    log.warn("pz_word_id가 null입니다. 건너뜁니다.");
                    continue;
                }
                
                // 힌트가 이미 추출 결과에 포함되어 있는 경우
                String hintText = extractedWord.get("hint") != null ? extractedWord.get("hint").toString() : null;
                
                if (hintText == null || hintText.isEmpty()) {
                    // 힌트가 없으면 조회
                    List<PzHint> wordHints = pzHintRepository.findByWordId(pzWordId);
                    if (!wordHints.isEmpty()) {
                        hintText = wordHints.get(0).getHintText();
                    }
                }
                
                if (hintText != null && !hintText.isEmpty()) {
                    hints.add(Map.of(
                        "wordId", pzWordId,
                        "hintId", hintId != null ? hintId : "",
                        "hintText", hintText,
                        "wordPositionId", extractedWord.get("word_id")
                    ));
                }
            }
            
            log.info("추출된 단어별 힌트 조회 완료: {}개 힌트", hints.size());
            
            return hints;
            
        } catch (Exception e) {
            log.error("추출된 단어별 힌트 조회 중 오류 발생: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 단어들의 힌트 조회 (기존 메서드 - 하위 호환성)
     */
    private List<Map<String, Object>> getHintsForWords(List<PzWord> words) {
        try {
            log.info("단어별 힌트 조회 시작: {}개 단어", words.size());
            
            List<Map<String, Object>> hints = new ArrayList<>();
            
            for (PzWord word : words) {
                List<PzHint> wordHints = pzHintRepository.findByWordIdAndLanguageCode(word.getId(), "ko");
                
                if (wordHints.isEmpty()) {
                    log.warn("단어 {} (ID: {})의 힌트가 없습니다.", word.getWord(), word.getId());
                    continue;
                }
                
                for (PzHint hint : wordHints) {
                    // null 체크 추가 (Map.of는 null을 허용하지 않음)
                    String hintText = hint.getHintText() != null ? hint.getHintText() : "";
                    String hintType = hint.getHintType() != null ? hint.getHintType() : "TEXT";
                    Integer difficulty = hint.getDifficulty() != null ? hint.getDifficulty() : 1;
                    
                    Map<String, Object> hintData = Map.of(
                        "wordId", word.getId(),
                        "word", word.getWord() != null ? word.getWord() : "",
                        "hintText", hintText,
                        "hintType", hintType,
                        "difficulty", difficulty
                    );
                    hints.add(hintData);
                }
            }
            
            log.info("단어별 힌트 조회 완료: {}개 힌트", hints.size());
            
            return hints;
            
        } catch (Exception e) {
            log.error("단어별 힌트 조회 중 오류 발생: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 퍼즐 ID 생성
     */
    private Integer generatePuzzleId() {
        return (int) (System.currentTimeMillis() % 1000000);
    }
}
