package com.example.board.service;

import com.example.board.entity.PuzzleGridTemplate;
import com.example.board.entity.ThemeDailyPuzzle;
import com.example.board.entity.PzHint;
import com.example.board.repository.PuzzleGridTemplateRepository;
import com.example.board.repository.ThemeDailyPuzzleRepository;
import com.example.board.repository.PzWordRepository;
import com.example.board.repository.PzHintRepository;
import com.example.board.service.PzHintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDateTime;

/**
 * 테마별 퍼즐 수정 서비스
 * 생성된 퍼즐의 템플릿, 단어, 힌트 수정 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThemePuzzleEditService {
    
    private final ThemeDailyPuzzleRepository themeDailyPuzzleRepository;
    private final PuzzleGridTemplateRepository puzzleGridTemplateRepository;
    private final PuzzleGridTemplateService puzzleGridTemplateService;
    private final ThemePuzzleGeneratorService themePuzzleGeneratorService;
    private final PzWordRepository pzWordRepository;
    private final PzHintRepository pzHintRepository;
    private final PzHintService pzHintService;
    
    /**
     * 퍼즐 상세 정보 조회
     * 템플릿, 단어, 힌트 정보를 포함한 전체 퍼즐 데이터 반환
     */
    public Map<String, Object> getPuzzleDetail(Integer puzzleId) {
        try {
            log.info("퍼즐 상세 정보 조회: {}", puzzleId);
            
            Optional<ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository.findById(puzzleId);
            if (puzzleOpt.isEmpty()) {
                throw new RuntimeException("퍼즐을 찾을 수 없습니다: " + puzzleId);
            }
            
            ThemeDailyPuzzle puzzle = puzzleOpt.get();
            Map<String, Object> puzzleData = puzzle.getPuzzleDataAsMap();
            
            if (puzzleData == null || puzzleData.isEmpty()) {
                throw new RuntimeException("퍼즐 데이터가 없습니다: " + puzzleId);
            }
            
            // 상세 정보 구성
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", puzzle.getId());
            detail.put("theme", puzzle.getTheme());
            detail.put("puzzleDate", puzzle.getPuzzleDate().toString());
            detail.put("puzzleId", puzzle.getPuzzleId());
            detail.put("isActive", puzzle.getIsActive());
            detail.put("createdAt", puzzle.getCreatedAt());
            detail.put("updatedAt", puzzle.getUpdatedAt());
            
            // 템플릿 정보
            if (puzzleData.containsKey("templateId")) {
                Integer templateId = (Integer) puzzleData.get("templateId");
                Map<String, Object> templateInfo = new HashMap<>();
                templateInfo.put("id", templateId);
                
                // grid 또는 grid_pattern 필드 확인
                if (puzzleData.containsKey("grid")) {
                    templateInfo.put("grid_pattern", puzzleData.get("grid"));
                } else if (puzzleData.containsKey("grid_pattern")) {
                    templateInfo.put("grid_pattern", puzzleData.get("grid_pattern"));
                }
                
                templateInfo.put("word_positions", extractWordPositions(puzzleData));
                detail.put("template", templateInfo);
            }
            
            // 단어 정보 (실제 단어 텍스트 포함)
            if (puzzleData.containsKey("words")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> words = (List<Map<String, Object>>) puzzleData.get("words");
                
                // 각 단어에 실제 단어 텍스트 추가
                for (Map<String, Object> word : words) {
                    if (word.containsKey("pz_word_id") && word.get("pz_word_id") != null) {
                        Integer pzWordId = (Integer) word.get("pz_word_id");
                        try {
                            pzWordRepository.findById(pzWordId).ifPresent(pzWord -> {
                                word.put("word", pzWord.getWord());
                            });
                        } catch (Exception e) {
                            log.warn("단어 조회 실패: pz_word_id={}, error={}", pzWordId, e.getMessage());
                        }
                    }
                }
                
                detail.put("words", words);
            }
            
            // 힌트 정보 (단어 텍스트 포함)
            if (puzzleData.containsKey("hints")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> hints = (List<Map<String, Object>>) puzzleData.get("hints");
                
                // 각 힌트에 단어 텍스트 추가
                for (Map<String, Object> hint : hints) {
                    if (hint.containsKey("wordId") && hint.get("wordId") != null) {
                        Integer wordId = (Integer) hint.get("wordId");
                        try {
                            pzWordRepository.findById(wordId).ifPresent(pzWord -> {
                                hint.put("word", pzWord.getWord());
                            });
                        } catch (Exception e) {
                            log.warn("힌트의 단어 조회 실패: wordId={}, error={}", wordId, e.getMessage());
                        }
                    }
                    // hintText를 hint로도 매핑 (프론트엔드 호환성)
                    if (hint.containsKey("hintText") && !hint.containsKey("hint")) {
                        hint.put("hint", hint.get("hintText"));
                    }
                }
                
                detail.put("hints", hints);
            }
            
            log.info("퍼즐 상세 정보 조회 완료: {}", puzzleId);
            return detail;
            
        } catch (Exception e) {
            log.error("퍼즐 상세 정보 조회 중 오류 발생: {} - {}", puzzleId, e.getMessage(), e);
            throw new RuntimeException("퍼즐 상세 정보 조회에 실패했습니다.", e);
        }
    }
    
    /**
     * 템플릿 수정
     * @param puzzleId 퍼즐 ID
     * @param newTemplateId 새로운 템플릿 ID
     * @param regenerateWords 단어도 재생성할지 여부
     */
    @Transactional
    public Map<String, Object> updateTemplate(Integer puzzleId, Integer newTemplateId, Boolean regenerateWords) {
        try {
            log.info("템플릿 수정: 퍼즐 ID={}, 새 템플릿 ID={}, 단어 재생성={}", puzzleId, newTemplateId, regenerateWords);
            
            Optional<ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository.findById(puzzleId);
            if (puzzleOpt.isEmpty()) {
                throw new RuntimeException("퍼즐을 찾을 수 없습니다: " + puzzleId);
            }
            
            ThemeDailyPuzzle puzzle = puzzleOpt.get();
            Map<String, Object> puzzleData = puzzle.getPuzzleDataAsMap();
            
            if (puzzleData == null || puzzleData.isEmpty()) {
                throw new RuntimeException("퍼즐 데이터가 없습니다: " + puzzleId);
            }
            
            // 템플릿 ID 업데이트
            puzzleData.put("templateId", newTemplateId);
            
            // 새 템플릿 정보 조회
            Optional<PuzzleGridTemplate> templateOpt = puzzleGridTemplateRepository.findById(newTemplateId);
            if (templateOpt.isPresent()) {
                PuzzleGridTemplate template = templateOpt.get();
                puzzleData.put("grid", template.getGridPattern());
            }
            
            // 단어 재생성이 필요한 경우
            if (Boolean.TRUE.equals(regenerateWords)) {
                log.info("단어 재생성 시작: 템플릿 ID={}, 테마={}", newTemplateId, puzzle.getTheme());
                
                // 새 템플릿 기반으로 단어 추출
                Map<String, Object> extractResult = puzzleGridTemplateService.extractWordsWithTheme(
                    newTemplateId, puzzle.getTheme());
                
                if (extractResult != null && Boolean.TRUE.equals(extractResult.get("success"))) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> extractedWords = (List<Map<String, Object>>) extractResult.get("extracted_words");
                    
                    if (extractedWords != null && !extractedWords.isEmpty()) {
                        puzzleData.put("words", extractedWords);
                        
                        // 힌트도 재조회
                        List<Map<String, Object>> hints = themePuzzleGeneratorService.getHintsForExtractedWords(extractedWords);
                        puzzleData.put("hints", hints);
                        
                        log.info("단어 재생성 완료: {}개 단어, {}개 힌트", extractedWords.size(), hints.size());
                    }
                }
            }
            
            // 퍼즐 데이터 저장
            puzzle.setPuzzleDataFromMap(puzzleData);
            themeDailyPuzzleRepository.save(puzzle);
            
            log.info("템플릿 수정 완료: 퍼즐 ID={}, 새 템플릿 ID={}", puzzleId, newTemplateId);
            
            return Map.of(
                "success", true,
                "message", "템플릿이 성공적으로 수정되었습니다.",
                "puzzleId", puzzleId,
                "templateId", newTemplateId
            );
            
        } catch (Exception e) {
            log.error("템플릿 수정 중 오류 발생: {} - {}", puzzleId, e.getMessage(), e);
            throw new RuntimeException("템플릿 수정에 실패했습니다.", e);
        }
    }
    
    /**
     * 단어 수정
     * @param puzzleId 퍼즐 ID
     * @param wordIndex 단어 인덱스 (words 리스트 내 인덱스)
     * @param newWord 새로운 단어
     * @param newHint 새로운 힌트
     */
    @Transactional
    public Map<String, Object> updateWord(Integer puzzleId, Integer wordIndex, String newWord, String newHint) {
        try {
            log.info("단어 수정: 퍼즐 ID={}, 단어 인덱스={}, 새 단어={}", puzzleId, wordIndex, newWord);
            
            Optional<ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository.findById(puzzleId);
            if (puzzleOpt.isEmpty()) {
                throw new RuntimeException("퍼즐을 찾을 수 없습니다: " + puzzleId);
            }
            
            ThemeDailyPuzzle puzzle = puzzleOpt.get();
            Map<String, Object> puzzleData = puzzle.getPuzzleDataAsMap();
            
            if (puzzleData == null || puzzleData.isEmpty()) {
                throw new RuntimeException("퍼즐 데이터가 없습니다: " + puzzleId);
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> words = (List<Map<String, Object>>) puzzleData.get("words");
            
            if (words == null || wordIndex < 0 || wordIndex >= words.size()) {
                throw new RuntimeException("유효하지 않은 단어 인덱스입니다: " + wordIndex);
            }
            
            // 단어 수정
            Map<String, Object> wordToUpdate = words.get(wordIndex);
            String oldWord = (String) wordToUpdate.get("word");
            wordToUpdate.put("word", newWord);
            
            // 힌트 수정
            if (newHint != null && !newHint.isEmpty()) {
                wordToUpdate.put("hint", newHint);
                
                // hints 리스트도 업데이트
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> hints = (List<Map<String, Object>>) puzzleData.get("hints");
                if (hints != null) {
                    for (Map<String, Object> hint : hints) {
                        if (oldWord.equals(hint.get("word"))) {
                            hint.put("word", newWord);
                            hint.put("hint", newHint);
                            break;
                        }
                    }
                }
            }
            
            // 퍼즐 데이터 저장
            puzzle.setPuzzleDataFromMap(puzzleData);
            themeDailyPuzzleRepository.save(puzzle);
            
            log.info("단어 수정 완료: 퍼즐 ID={}, 단어 인덱스={}, 새 단어={}", puzzleId, wordIndex, newWord);
            
            return Map.of(
                "success", true,
                "message", "단어가 성공적으로 수정되었습니다.",
                "puzzleId", puzzleId,
                "wordIndex", wordIndex,
                "newWord", newWord
            );
            
        } catch (Exception e) {
            log.error("단어 수정 중 오류 발생: {} - {}", puzzleId, e.getMessage(), e);
            throw new RuntimeException("단어 수정에 실패했습니다.", e);
        }
    }
    
    /**
     * 힌트 수정
     * @param puzzleId 퍼즐 ID
     * @param word 단어
     * @param newHint 새로운 힌트
     */
    @Transactional
    public Map<String, Object> updateHint(Integer puzzleId, String word, String newHint) {
        try {
            log.info("힌트 수정: 퍼즐 ID={}, 단어={}, 새 힌트={}", puzzleId, word, newHint);
            
            Optional<ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository.findById(puzzleId);
            if (puzzleOpt.isEmpty()) {
                throw new RuntimeException("퍼즐을 찾을 수 없습니다: " + puzzleId);
            }
            
            ThemeDailyPuzzle puzzle = puzzleOpt.get();
            Map<String, Object> puzzleData = puzzle.getPuzzleDataAsMap();
            
            if (puzzleData == null || puzzleData.isEmpty()) {
                throw new RuntimeException("퍼즐 데이터가 없습니다: " + puzzleId);
            }
            
            // hints 리스트에서 해당 단어의 힌트 찾아서 수정
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hints = (List<Map<String, Object>>) puzzleData.get("hints");
            
            if (hints != null) {
                boolean found = false;
                for (Map<String, Object> hint : hints) {
                    if (word.equals(hint.get("word"))) {
                        hint.put("hint", newHint);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    throw new RuntimeException("해당 단어의 힌트를 찾을 수 없습니다: " + word);
                }
            } else {
                throw new RuntimeException("힌트 데이터가 없습니다.");
            }
            
            // words 리스트의 힌트도 업데이트
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> words = (List<Map<String, Object>>) puzzleData.get("words");
            if (words != null) {
                for (Map<String, Object> wordData : words) {
                    if (word.equals(wordData.get("word"))) {
                        wordData.put("hint", newHint);
                        break;
                    }
                }
            }
            
            // 퍼즐 데이터 저장
            puzzle.setPuzzleDataFromMap(puzzleData);
            themeDailyPuzzleRepository.save(puzzle);
            
            log.info("힌트 수정 완료: 퍼즐 ID={}, 단어={}, 새 힌트={}", puzzleId, word, newHint);
            
            return Map.of(
                "success", true,
                "message", "힌트가 성공적으로 수정되었습니다.",
                "puzzleId", puzzleId,
                "word", word,
                "newHint", newHint
            );
            
        } catch (Exception e) {
            log.error("힌트 수정 중 오류 발생: {} - {}", puzzleId, e.getMessage(), e);
            throw new RuntimeException("힌트 수정에 실패했습니다.", e);
        }
    }
    
    /**
     * 여러 힌트 수정 (pz_hints 테이블과 puzzle_data 모두 업데이트)
     * @param puzzleId 퍼즐 ID
     * @param word 단어
     * @param hints 힌트 목록 (id, hint_text, difficulty, is_primary 포함)
     */
    @Transactional
    public Map<String, Object> updateHints(Integer puzzleId, String word, List<Map<String, Object>> hints) {
        try {
            log.info("여러 힌트 수정: 퍼즐 ID={}, 단어={}, 힌트 개수={}", puzzleId, word, hints.size());
            
            // 퍼즐 데이터에서 단어 ID 찾기
            Optional<ThemeDailyPuzzle> puzzleOpt = themeDailyPuzzleRepository.findById(puzzleId);
            if (puzzleOpt.isEmpty()) {
                throw new RuntimeException("퍼즐을 찾을 수 없습니다: " + puzzleId);
            }
            
            ThemeDailyPuzzle puzzle = puzzleOpt.get();
            Map<String, Object> puzzleData = puzzle.getPuzzleDataAsMap();
            
            if (puzzleData == null || puzzleData.isEmpty()) {
                throw new RuntimeException("퍼즐 데이터가 없습니다: " + puzzleId);
            }
            
            // words 리스트에서 단어 ID 찾기
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> words = (List<Map<String, Object>>) puzzleData.get("words");
            Integer wordId = null;
            
            if (words != null) {
                for (Map<String, Object> wordData : words) {
                    if (word.equals(wordData.get("word"))) {
                        wordId = (Integer) wordData.get("pz_word_id");
                        if (wordId == null) {
                            wordId = (Integer) wordData.get("wordId");
                        }
                        break;
                    }
                }
            }
            
            if (wordId == null) {
                throw new RuntimeException("단어 ID를 찾을 수 없습니다: " + word);
            }
            
            // pz_hints 테이블의 힌트 업데이트
            List<PzHint> existingHints = pzHintService.getHintsByWordId(wordId);
            Map<Integer, PzHint> existingHintsMap = new HashMap<>();
            for (PzHint hint : existingHints) {
                existingHintsMap.put(hint.getId(), hint);
            }
            
            // 기존 힌트 업데이트 및 새 힌트 추가
            for (Map<String, Object> hintData : hints) {
                Object hintIdObj = hintData.get("id");
                String hintText = (String) hintData.get("hint_text");
                Integer difficulty = hintData.get("difficulty") != null ? 
                    (Integer) hintData.get("difficulty") : 2;
                Boolean isPrimary = hintData.get("is_primary") != null ? 
                    (Boolean) hintData.get("is_primary") : false;
                
                if (hintText == null || hintText.trim().isEmpty()) {
                    continue; // 빈 힌트는 건너뛰기
                }
                
                PzHint hint;
                if (hintIdObj != null) {
                    // 기존 힌트 업데이트
                    Integer hintId = hintIdObj instanceof String && ((String) hintIdObj).startsWith("new_") ? 
                        null : (Integer) hintIdObj;
                    
                    if (hintId != null && existingHintsMap.containsKey(hintId)) {
                        hint = existingHintsMap.get(hintId);
                        hint.setHintText(hintText);
                        hint.setDifficulty(difficulty);
                        hint.setIsPrimary(isPrimary);
                        hint.setUpdatedAt(LocalDateTime.now());
                        pzHintRepository.save(hint);
                    } else {
                        // 새 힌트 추가
                        hint = new PzHint();
                        hint.setWord(pzWordRepository.findById(wordId).orElseThrow());
                        hint.setHintText(hintText);
                        hint.setDifficulty(difficulty);
                        hint.setIsPrimary(isPrimary);
                        hint.setHintType("TEXT");
                        hint.setLanguageCode("ko");
                        hint.setCreatedAt(LocalDateTime.now());
                        hint.setUpdatedAt(LocalDateTime.now());
                        pzHintRepository.save(hint);
                    }
                } else {
                    // 새 힌트 추가
                    hint = new PzHint();
                    hint.setWord(pzWordRepository.findById(wordId).orElseThrow());
                    hint.setHintText(hintText);
                    hint.setDifficulty(difficulty);
                    hint.setIsPrimary(isPrimary);
                    hint.setHintType("TEXT");
                    hint.setLanguageCode("ko");
                    hint.setCreatedAt(LocalDateTime.now());
                    hint.setUpdatedAt(LocalDateTime.now());
                    pzHintRepository.save(hint);
                }
            }
            
            // puzzle_data의 hints 리스트도 업데이트 (기본 힌트만)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> puzzleHints = (List<Map<String, Object>>) puzzleData.get("hints");
            if (puzzleHints != null) {
                for (Map<String, Object> hint : puzzleHints) {
                    if (word.equals(hint.get("word"))) {
                        // 기본 힌트(is_primary=true) 찾기
                        String primaryHintText = hints.stream()
                            .filter(h -> Boolean.TRUE.equals(h.get("is_primary")))
                            .map(h -> (String) h.get("hint_text"))
                            .findFirst()
                            .orElse(hints.get(0).get("hint_text").toString());
                        hint.put("hint", primaryHintText);
                        hint.put("hintText", primaryHintText);
                        break;
                    }
                }
            }
            
            // words 리스트의 힌트도 업데이트
            if (words != null) {
                for (Map<String, Object> wordData : words) {
                    if (word.equals(wordData.get("word"))) {
                        // 기본 힌트(is_primary=true) 찾기
                        String primaryHintText = hints.stream()
                            .filter(h -> Boolean.TRUE.equals(h.get("is_primary")))
                            .map(h -> (String) h.get("hint_text"))
                            .findFirst()
                            .orElse(hints.get(0).get("hint_text").toString());
                        wordData.put("hint", primaryHintText);
                        break;
                    }
                }
            }
            
            // 퍼즐 데이터 저장
            puzzle.setPuzzleDataFromMap(puzzleData);
            themeDailyPuzzleRepository.save(puzzle);
            
            log.info("여러 힌트 수정 완료: 퍼즐 ID={}, 단어={}, 힌트 개수={}", puzzleId, word, hints.size());
            
            return Map.of(
                "success", true,
                "message", "힌트가 성공적으로 수정되었습니다.",
                "puzzleId", puzzleId,
                "word", word,
                "hintCount", hints.size()
            );
            
        } catch (Exception e) {
            log.error("여러 힌트 수정 중 오류 발생: {} - {}", puzzleId, e.getMessage(), e);
            throw new RuntimeException("힌트 수정에 실패했습니다.", e);
        }
    }
    
    /**
     * word_positions 추출 (퍼즐 데이터에서)
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractWordPositions(Map<String, Object> puzzleData) {
        try {
            if (puzzleData.containsKey("words")) {
                List<Map<String, Object>> words = (List<Map<String, Object>>) puzzleData.get("words");
                List<Map<String, Object>> positions = new ArrayList<>();
                
                for (Map<String, Object> word : words) {
                    if (word.containsKey("position")) {
                        positions.add((Map<String, Object>) word.get("position"));
                    }
                }
                
                return positions;
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("word_positions 추출 중 오류 발생: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}

