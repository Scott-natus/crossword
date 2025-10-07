package com.example.crossword.service;

import com.example.crossword.entity.PuzzleGridTemplate;
import com.example.crossword.entity.PuzzleLevel;
import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.repository.PuzzleGridTemplateRepository;
import com.example.crossword.repository.PuzzleLevelRepository;
import com.example.crossword.repository.PzWordRepository;
import com.example.crossword.repository.PzHintRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
            
            // 랜덤하게 템플릿 선택
            PuzzleGridTemplate template = templates.get(new Random().nextInt(templates.size()));
            
            // 레벨 정보에서 난이도 가져오기
            PuzzleLevel level = levelRepository.findByLevel(template.getLevelId()).orElse(null);
            if (level == null) {
                return Map.of("success", false, "message", "레벨 정보를 찾을 수 없습니다.");
            }
            
            // 2. word_positions를 id 순서대로 정렬 (라라벨과 동일)
            List<Map<String, Object>> wordPositions = new ArrayList<>(template.getWordPositions());
            wordPositions.sort((a, b) -> {
                Integer idA = (Integer) a.get("id");
                Integer idB = (Integer) b.get("id");
                return idA.compareTo(idB);
            });
            
            // 3. 최대 5회 재시도
            int maxRetries = 5;
            int retryCount = 0;
            List<Map<String, Object>> extractedWords = new ArrayList<>();
            Map<Integer, String> confirmedWords = new HashMap<>(); // word_id => word
            boolean extractionFailed = false;
            
            while (retryCount < maxRetries) {
                retryCount++;
                extractedWords.clear();
                confirmedWords.clear();
                extractionFailed = false;
                
                System.out.println("단어 추출 시도 #" + retryCount + " 시작");
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
                        Map<String, Object> extractedWord = extractIndependentWord(word, level);
                        if ("추출 실패".equals(extractedWord.get("word"))) {
                            System.out.println("단어 ID " + wordId + " 독립 추출 실패");
                            extractionFailed = true;
                            break;
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
                        
                        System.out.println("교차점 발견 - 단어 ID " + wordId + 
                            ", 교차점 개수: " + intersections.size());
                        
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
                            extractedWords.add(Map.of(
                                "word_id", wordId,
                                "pz_word_id", extractedWord.get("pz_word_id"),
                                "hint_id", extractedWord.get("hint_id"),
                                "position", word,
                                "type", "intersection_connected",
                                "extracted_word", maskWord((String) extractedWord.get("word")),
                                "hint", extractedWord.get("hint")
                            ));
                            confirmedWords.put(wordId, (String) extractedWord.get("word"));
                        } else {
                            extractionFailed = true;
                            break;
                        }
                    }
                }
                
                // 모든 단어가 성공적으로 추출되었으면 루프 종료
                if (!extractionFailed) {
                    System.out.println("단어 추출 성공 - 시도 횟수: " + retryCount);
                    break;
                } else {
                    System.out.println("단어 추출 실패 - 재시도: " + retryCount);
                }
            }
            
            // 5회 시도 후에도 실패한 경우 (라라벨과 동일한 방어 로직)
            if (extractionFailed || extractedWords.size() != wordPositions.size()) {
                System.out.println("단어 추출 최종 실패 - 추출된 단어: " + extractedWords.size() + 
                    ", 필요한 단어: " + wordPositions.size());
                return Map.of("success", false, "message", 
                    "단어 추출에 실패했습니다. 모든 단어를 추출할 수 없습니다. (추출된 단어: " + 
                    extractedWords.size() + "/" + wordPositions.size() + ")");
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
        
        if (direction.equals("horizontal")) {
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
        } else if (direction.equals("vertical")) {
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
     * 라라벨의 extractIndependentWord와 동일한 로직
     */
    private Map<String, Object> extractIndependentWord(Map<String, Object> word, PuzzleLevel level) {
        try {
            String direction = (String) word.get("direction");
            Integer length = (Integer) word.get("length");
            
            // 조건에 맞는 단어 찾기
            List<PzWord> words = wordRepository.findByDifficultyAndLength(level.getWordDifficulty(), length);
            
            if (words.isEmpty()) {
                return Map.of("word", "추출 실패");
            }
            
            // 랜덤하게 단어 선택
            PzWord selectedWord = words.get(new Random().nextInt(words.size()));
            
            // 힌트 찾기
            List<PzHint> hints = hintRepository.findByWordIdAndDifficulty(selectedWord.getId(), level.getHintDifficulty());
            String hint = hints.isEmpty() ? "힌트가 없습니다." : hints.get(0).getHintText();
            
            return Map.of(
                "word", selectedWord.getWord(),
                "pz_word_id", selectedWord.getId(),
                "hint_id", hints.isEmpty() ? null : hints.get(0).getId(),
                "hint", hint
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("word", "추출 실패");
        }
    }

    /**
     * 라라벨의 extractWordWithConfirmedSyllables와 동일한 로직
     */
    private Map<String, Object> extractWordWithConfirmedSyllables(
            Map<String, Object> word, 
            PuzzleLevel level, 
            List<Map<String, Object>> confirmedIntersectionSyllables,
            Map<Integer, String> confirmedWords) {
        
        try {
            String direction = (String) word.get("direction");
            Integer length = (Integer) word.get("length");
            
            // 조건에 맞는 단어들 찾기
            List<PzWord> words = wordRepository.findByDifficultyAndLength(level.getWordDifficulty(), length);
            
            for (PzWord candidateWord : words) {
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
     * JSON 파싱 헬퍼 메서드들
     */
    private List<Map<String, Object>> parseWordPositions(String wordPositionsJson) {
        try {
            return objectMapper.readValue(wordPositionsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<List<Integer>> parseGridPattern(String gridPatternJson) {
        try {
            return objectMapper.readValue(gridPatternJson, new TypeReference<List<List<Integer>>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}