package com.example.board.service;

import com.example.board.entity.PzWord;
import com.example.board.entity.PzHint;
import com.example.board.repository.PzWordRepository;
import com.example.board.repository.PzHintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 테마별 퍼즐 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThemePuzzleGeneratorService {
    
    private final PzWordRepository pzWordRepository;
    private final PzHintRepository pzHintRepository;
    
    /**
     * 테마별 퍼즐 생성
     */
    @Transactional
    public Map<String, Object> generateThemePuzzle(String theme) {
        try {
            log.info("테마별 퍼즐 생성 시작: {}", theme);
            
            // 테마별 단어 조회
            List<PzWord> themeWords = pzWordRepository.findByCat2AndIsApproved(theme, true);
            
            if (themeWords.isEmpty()) {
                log.warn("{} 테마의 승인된 단어가 없습니다.", theme);
                return Map.of(
                    "success", false,
                    "message", "테마별 단어가 부족합니다."
                );
            }
            
            // 퍼즐에 사용할 단어 선택 (난이도별)
            List<PzWord> selectedWords = selectWordsForPuzzle(themeWords);
            
            if (selectedWords.isEmpty()) {
                log.warn("{} 테마의 적합한 단어를 찾을 수 없습니다.", theme);
                return Map.of(
                    "success", false,
                    "message", "적합한 단어를 찾을 수 없습니다."
                );
            }
            
            // 퍼즐 그리드 생성
            Map<String, Object> gridData = generatePuzzleGrid(selectedWords);
            
            // 힌트 조회
            List<Map<String, Object>> hints = getHintsForWords(selectedWords);
            
            // 퍼즐 데이터 구성
            Map<String, Object> puzzleData = Map.of(
                "success", true,
                "puzzleId", generatePuzzleId(),
                "theme", theme,
                "words", selectedWords.stream()
                    .map(word -> Map.of(
                        "id", word.getId(),
                        "word", word.getWord(),
                        "difficulty", word.getDifficulty()
                    ))
                    .collect(Collectors.toList()),
                "hints", hints,
                "grid", gridData,
                "generatedAt", new Date()
            );
            
            log.info("테마별 퍼즐 생성 완료: {} - 단어 {}개", theme, selectedWords.size());
            
            return puzzleData;
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 생성 중 오류 발생: {} - {}", theme, e.getMessage());
            return Map.of(
                "success", false,
                "message", "퍼즐 생성에 실패했습니다."
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
     * 퍼즐에 사용할 단어 선택
     */
    private List<PzWord> selectWordsForPuzzle(List<PzWord> themeWords) {
        try {
            log.info("퍼즐용 단어 선택 시작: {}개 중에서", themeWords.size());
            
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
            
            log.info("퍼즐용 단어 선택 완료: {}개", selectedWords.size());
            
            return selectedWords;
            
        } catch (Exception e) {
            log.error("퍼즐용 단어 선택 중 오류 발생: {}", e.getMessage());
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
     * 단어들의 힌트 조회
     */
    private List<Map<String, Object>> getHintsForWords(List<PzWord> words) {
        try {
            log.info("단어별 힌트 조회 시작: {}개 단어", words.size());
            
            List<Map<String, Object>> hints = new ArrayList<>();
            
            for (PzWord word : words) {
                List<PzHint> wordHints = pzHintRepository.findByWordIdAndLanguageCode(word.getId(), "ko");
                
                for (PzHint hint : wordHints) {
                    Map<String, Object> hintData = Map.of(
                        "wordId", word.getId(),
                        "word", word.getWord(),
                        "hintText", hint.getHintText(),
                        "hintType", hint.getHintType(),
                        "difficulty", hint.getDifficulty()
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
