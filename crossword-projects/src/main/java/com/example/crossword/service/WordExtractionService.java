package com.example.crossword.service;

import com.example.crossword.entity.PzGridTemplate;
import com.example.crossword.entity.PzLevel;
import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.repository.PzWordRepository;
import com.example.crossword.repository.PzHintRepository;
import com.example.crossword.repository.PzLevelRepository;
import com.example.crossword.repository.PzGridTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 단어 추출 서비스
 * Laravel의 extractWords 메서드를 Spring Boot로 포팅
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordExtractionService {

    private final PzWordRepository pzWordRepository;
    private final PzHintRepository pzHintRepository;
    private final PzLevelRepository pzLevelRepository;
    private final PzGridTemplateRepository pzGridTemplateRepository;

    /**
     * 템플릿에서 단어 추출
     * Laravel의 extractWords 메서드와 동일한 로직
     */
    @Transactional(readOnly = true)
    public Map<String, Object> extractWords(Long templateId) {
        log.info("단어 추출 시작 - 템플릿 ID: {}", templateId);
        
        // 쿼리 로그 수집을 위한 리스트
        List<Map<String, Object>> queryLog = new ArrayList<>();
        
        try {
            // 1. 템플릿 정보 조회
            PzGridTemplate template = getTemplateWithLevel(templateId, queryLog);
            if (template == null) {
                return createErrorResponse("템플릿을 찾을 수 없습니다.", queryLog);
            }

            // 2. 단어 위치 정보 파싱
            List<Map<String, Object>> wordPositions = parseWordPositions(template.getWordPositions());
            if (wordPositions.isEmpty()) {
                return createErrorResponse("단어 위치 정보가 없습니다.", queryLog);
            }

            // 3. 단어 순서대로 정렬
            wordPositions.sort((a, b) -> {
                Integer idA = (Integer) a.get("id");
                Integer idB = (Integer) b.get("id");
                return idA.compareTo(idB);
            });

            log.info("정렬된 단어 위치: {}", wordPositions.stream()
                    .map(wp -> wp.get("id"))
                    .collect(Collectors.toList()));

            // 4. 최대 5회 재시도
            int maxRetries = 5;
            int retryCount = 0;
            List<Map<String, Object>> extractedWords = new ArrayList<>();
            Map<Integer, String> confirmedWords = new HashMap<>();

            while (retryCount < maxRetries) {
                retryCount++;
                extractedWords.clear();
                confirmedWords.clear();
                boolean extractionFailed = false;

                log.info("단어 추출 시도 #{} 시작", retryCount);

                for (Map<String, Object> word : wordPositions) {
                    Integer wordId = (Integer) word.get("id");
                    
                    // 이미 확정된 단어는 건너뛰기
                    if (confirmedWords.containsKey(wordId)) {
                        continue;
                    }

                    // 5. 현재 단어와 이미 확정된 단어들 사이의 교차점 찾기
                    List<Map<String, Object>> intersections = findIntersectionsWithConfirmedWords(
                            word, confirmedWords, wordPositions);
                    
                    if (intersections.isEmpty()) {
                        // 6. 교차점이 없는 독립 단어 추출
                        Map<String, Object> extractedWord = extractIndependentWord(word, template, queryLog);
                        if ("추출 실패".equals(extractedWord.get("word"))) {
                            extractionFailed = true;
                            break;
                        }
                        extractedWords.add(createExtractedWordResult(wordId, word, "no_intersection", extractedWord));
                        confirmedWords.put(wordId, (String) extractedWord.get("word"));
                    } else {
                        // 7. 교차점이 있는 단어 추출
                        Map<String, Object> extractedWord = extractWordWithConfirmedSyllables(
                                word, template, intersections, queryLog, confirmedWords, wordPositions);
                        
                        if ((Boolean) extractedWord.get("success")) {
                            extractedWords.add(createExtractedWordResult(wordId, word, "intersection_connected", extractedWord));
                            confirmedWords.put(wordId, (String) extractedWord.get("word"));
                        } else {
                            extractionFailed = true;
                            break;
                        }
                    }
                }

                // 모든 단어가 성공적으로 추출되었으면 루프 종료
                if (!extractionFailed) {
                    log.info("단어 추출 성공 - 시도 #{}에서 완료", retryCount);
                    break;
                }

                log.info("단어 추출 실패 - 시도 #{}, 재시도 예정", retryCount);
            }

            // 5회 시도 후에도 실패한 경우
            if (extractedWords.size() != wordPositions.size()) {
                Integer failedWordId = wordPositions.stream()
                        .map(wp -> (Integer) wp.get("id"))
                        .filter(id -> !confirmedWords.containsKey(id))
                        .findFirst()
                        .orElse(null);

                return createErrorResponse(
                        "단어 ID " + failedWordId + "에서 확정된 음절들과 매칭되는 단어를 찾을 수 없습니다.",
                        queryLog, retryCount);
            }

            // 8. 성공 응답 생성
            return createSuccessResponse(template, extractedWords, wordPositions, retryCount, queryLog);

        } catch (Exception e) {
            log.error("단어 추출 중 오류 발생", e);
            return createErrorResponse("단어 추출 중 오류가 발생했습니다: " + e.getMessage(), queryLog);
        }
    }

    /**
     * 템플릿과 레벨 정보 조회
     */
    private PzGridTemplate getTemplateWithLevel(Long templateId, List<Map<String, Object>> queryLog) {
        queryLog.add(createQueryLog("template_info", "SELECT * FROM puzzle_grid_templates WHERE id = ?", 
                Arrays.asList(templateId), "템플릿 정보 조회"));
        
        return pzGridTemplateRepository.findById(templateId).orElse(null);
    }


    /**
     * 현재 단어와 이미 확정된 단어들 사이의 교차점 찾기
     */
    private List<Map<String, Object>> findIntersectionsWithConfirmedWords(
            Map<String, Object> currentWord, Map<Integer, String> confirmedWords, 
            List<Map<String, Object>> allWordPositions) {
        
        List<Map<String, Object>> intersections = new ArrayList<>();
        
        // 이미 확정된 단어들과만 교차점 확인
        for (Map.Entry<Integer, String> entry : confirmedWords.entrySet()) {
            Integer confirmedWordId = entry.getKey();
            String confirmedWordText = entry.getValue();
            
            Map<String, Object> confirmedWordPosition = findWordById(confirmedWordId, allWordPositions);
            if (confirmedWordPosition == null) continue;
            
            Map<String, Object> intersection = findIntersection(currentWord, confirmedWordPosition);
            if (intersection != null) {
                intersections.add(Map.of(
                        "position", intersection,
                        "connected_word_id", confirmedWordId,
                        "connected_word", confirmedWordPosition
                ));
            }
        }
        
        return intersections;
    }

    /**
     * 두 단어 사이의 교차점 찾기
     */
    private Map<String, Object> findIntersection(Map<String, Object> word1, Map<String, Object> word2) {
        String direction1 = (String) word1.get("direction");
        String direction2 = (String) word2.get("direction");
        
        // 같은 방향인 경우 연결점 확인
        if (direction1.equals(direction2)) {
            return findConnectionPoint(word1, word2);
        }
        
        // 가로-세로 교차 확인
        Map<String, Object> horizontal = "horizontal".equals(direction1) ? word1 : word2;
        Map<String, Object> vertical = "vertical".equals(direction1) ? word1 : word2;
        
        // 교차점 좌표 계산
        Integer intersectX = (Integer) vertical.get("start_x");
        Integer intersectY = (Integer) horizontal.get("start_y");
        
        // 교차점이 두 단어 범위 내에 있는지 확인
        Integer hStartX = (Integer) horizontal.get("start_x");
        Integer hEndX = (Integer) horizontal.get("end_x");
        Integer vStartY = (Integer) vertical.get("start_y");
        Integer vEndY = (Integer) vertical.get("end_y");
        
        if (intersectX >= hStartX && intersectX <= hEndX &&
            intersectY >= vStartY && intersectY <= vEndY) {
            
            return Map.of(
                    "x", intersectX,
                    "y", intersectY,
                    "horizontal_word_id", horizontal.get("id"),
                    "vertical_word_id", vertical.get("id")
            );
        }
        
        return null;
    }

    /**
     * 같은 방향의 두 단어 사이의 연결점 찾기
     */
    private Map<String, Object> findConnectionPoint(Map<String, Object> word1, Map<String, Object> word2) {
        String direction = (String) word1.get("direction");
        
        if ("horizontal".equals(direction)) {
            // 가로 방향 연결점 확인
            Integer y1 = (Integer) word1.get("start_y");
            Integer y2 = (Integer) word2.get("start_y");
            
            if (y1.equals(y2)) {
                Integer endX1 = (Integer) word1.get("end_x");
                Integer startX2 = (Integer) word2.get("start_x");
                Integer startX1 = (Integer) word1.get("start_x");
                Integer endX2 = (Integer) word2.get("end_x");
                
                // word1의 끝점과 word2의 시작점이 연결되는지 확인
                if (endX1 + 1 == startX2) {
                    return Map.of(
                            "x", endX1,
                            "y", y1,
                            "word1_end", true,
                            "word2_start", true
                    );
                }
                // word2의 끝점과 word1의 시작점이 연결되는지 확인
                if (endX2 + 1 == startX1) {
                    return Map.of(
                            "x", endX2,
                            "y", y1,
                            "word1_start", true,
                            "word2_end", true
                    );
                }
            }
        } else if ("vertical".equals(direction)) {
            // 세로 방향 연결점 확인
            Integer x1 = (Integer) word1.get("start_x");
            Integer x2 = (Integer) word2.get("start_x");
            
            if (x1.equals(x2)) {
                Integer endY1 = (Integer) word1.get("end_y");
                Integer startY2 = (Integer) word2.get("start_y");
                Integer startY1 = (Integer) word1.get("start_y");
                Integer endY2 = (Integer) word2.get("end_y");
                
                // word1의 끝점과 word2의 시작점이 연결되는지 확인
                if (endY1 + 1 == startY2) {
                    return Map.of(
                            "x", x1,
                            "y", endY1,
                            "word1_end", true,
                            "word2_start", true
                    );
                }
                // word2의 끝점과 word1의 시작점이 연결되는지 확인
                if (endY2 + 1 == startY1) {
                    return Map.of(
                            "x", x1,
                            "y", endY2,
                            "word1_start", true,
                            "word2_end", true
                    );
                }
            }
        }
        
        return null;
    }

    /**
     * 독립 단어 추출 (교차점이 없는 단어)
     */
    private Map<String, Object> extractIndependentWord(Map<String, Object> word, PzGridTemplate template, 
                                                      List<Map<String, Object>> queryLog) {
        Integer length = (Integer) word.get("length");
        
        // 레벨에 설정된 단어 난이도 사용
        Integer wordDifficulty = template.getDifficultyRating();
        
        // 새로운 난이도 규칙 적용
        List<Integer> allowedDifficulties = getAllowedDifficulties(wordDifficulty);
        
        // 랜덤 단어 추출
        List<PzWord> words = pzWordRepository.findByLengthAndDifficultyInAndIsActiveTrue(length, allowedDifficulties);
        
        if (words.isEmpty()) {
            return Map.of(
                    "word", "추출 실패",
                    "hint", "조건에 맞는 단어를 찾을 수 없습니다."
            );
        }
        
        // 랜덤 선택
        PzWord selectedWord = words.get(new Random().nextInt(words.size()));
        
        // 힌트 조회
        List<PzHint> hints = pzHintRepository.findByWordIdOrderById(selectedWord.getId());
        String hint = hints.isEmpty() ? "힌트 없음" : hints.get(0).getHintText();
        
        // 쿼리 로그 추가
        String allowedDifficultiesText = allowedDifficulties.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        queryLog.add(createQueryLog("independent_word", 
                "SELECT * FROM pz_words WHERE length = ? AND difficulty IN (?) AND is_active = true ORDER BY RANDOM()",
                Arrays.asList(length, allowedDifficultiesText),
                "독립 단어 추출 (길이: " + length + ", 허용난이도: " + allowedDifficultiesText + ")"));
        
        return Map.of(
                "word", selectedWord.getWord(),
                "hint", hint
        );
    }

    /**
     * 확정된 음절 정보를 기반으로 단어 추출
     */
    private Map<String, Object> extractWordWithConfirmedSyllables(Map<String, Object> word, PzGridTemplate template,
                                                                 List<Map<String, Object>> intersections,
                                                                 List<Map<String, Object>> queryLog,
                                                                 Map<Integer, String> confirmedWords,
                                                                 List<Map<String, Object>> allWordPositions) {
        Integer length = (Integer) word.get("length");
        
        if (intersections.isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "확정된 음절 정보가 없습니다."
            );
        }
        
        // 레벨에 설정된 단어 난이도 사용
        Integer wordDifficulty = template.getDifficultyRating();
        
        // 새로운 난이도 규칙 적용
        List<Integer> allowedDifficulties = getAllowedDifficulties(wordDifficulty);
        
        // 확정된 음절들을 기반으로 조건 생성
        List<PzWord> candidates = pzWordRepository.findByLengthAndDifficultyInAndIsActiveTrue(length, allowedDifficulties);
        
        // 교차점을 공유하는 다른 단어들과 같은 단어 제외
        if (!confirmedWords.isEmpty()) {
            candidates = candidates.stream()
                    .filter(w -> !confirmedWords.containsValue(w.getWord()))
                    .collect(Collectors.toList());
        }
        
        // 각 확정된 음절에 대한 조건 추가
        List<PzWord> filteredCandidates = new ArrayList<>();
        for (PzWord candidate : candidates) {
            boolean matchesAllSyllables = true;
            
            for (Map<String, Object> intersection : intersections) {
                Map<String, Object> position = (Map<String, Object>) intersection.get("position");
                Integer syllablePos = getSyllablePosition(word, position);
                String requiredSyllable = getConfirmedSyllable(intersection, confirmedWords, allWordPositions);
                
                if (syllablePos > candidate.getWord().length() || 
                    !candidate.getWord().substring(syllablePos - 1, syllablePos).equals(requiredSyllable)) {
                    matchesAllSyllables = false;
                    break;
                }
            }
            
            if (matchesAllSyllables) {
                filteredCandidates.add(candidate);
            }
        }
        
        if (filteredCandidates.isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "확정된 음절들과 매칭되는 단어를 찾을 수 없습니다."
            );
        }
        
        // 랜덤 선택
        PzWord selectedWord = filteredCandidates.get(new Random().nextInt(filteredCandidates.size()));
        
        // 힌트 조회
        List<PzHint> hints = pzHintRepository.findByWordIdOrderById(selectedWord.getId());
        String hint = hints.isEmpty() ? "힌트 없음" : hints.get(0).getHintText();
        
        return Map.of(
                "success", true,
                "word", selectedWord.getWord(),
                "hint", hint
        );
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

    /**
     * 음절 위치 계산
     */
    private Integer getSyllablePosition(Map<String, Object> word, Map<String, Object> intersection) {
        String direction = (String) word.get("direction");
        if ("horizontal".equals(direction)) {
            Integer intersectX = (Integer) intersection.get("x");
            Integer startX = (Integer) word.get("start_x");
            return intersectX - startX + 1;
        } else {
            Integer intersectY = (Integer) intersection.get("y");
            Integer startY = (Integer) word.get("start_y");
            return intersectY - startY + 1;
        }
    }

    /**
     * ID로 단어 찾기
     */
    private Map<String, Object> findWordById(Integer id, List<Map<String, Object>> wordPositions) {
        return wordPositions.stream()
                .filter(wp -> id.equals(wp.get("id")))
                .findFirst()
                .orElse(null);
    }

    /**
     * 확정된 음절 가져오기
     */
    private String getConfirmedSyllable(Map<String, Object> intersection, Map<Integer, String> confirmedWords, 
                                       List<Map<String, Object>> wordPositions) {
        Integer connectedWordId = (Integer) intersection.get("connected_word_id");
        String connectedWord = confirmedWords.get(connectedWordId);
        Map<String, Object> connectedWordPosition = findWordById(connectedWordId, wordPositions);
        Map<String, Object> position = (Map<String, Object>) intersection.get("position");
        Integer syllablePos = getSyllablePosition(connectedWordPosition, position);
        return connectedWord.substring(syllablePos - 1, syllablePos);
    }

    /**
     * 단어 위치 정보 파싱 (JSON 문자열을 List<Map>으로 변환)
     */
    private List<Map<String, Object>> parseWordPositions(String wordPositionsJson) {
        try {
            if (wordPositionsJson == null || wordPositionsJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            // Jackson ObjectMapper를 사용하여 JSON 파싱
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(wordPositionsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            log.error("단어 위치 정보 파싱 오류: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 추출된 단어 결과 생성
     */
    private Map<String, Object> createExtractedWordResult(Integer wordId, Map<String, Object> position, 
                                                         String type, Map<String, Object> extractedWord) {
        return Map.of(
                "word_id", wordId,
                "position", position,
                "type", type,
                "extracted_word", extractedWord.get("word"),
                "hint", extractedWord.get("hint")
        );
    }

    /**
     * 성공 응답 생성
     */
    private Map<String, Object> createSuccessResponse(PzGridTemplate template, List<Map<String, Object>> extractedWords,
                                                     List<Map<String, Object>> wordPositions, int retryCount,
                                                     List<Map<String, Object>> queryLog) {
        return Map.of(
                "success", true,
                "template", Map.of(
                        "id", template.getId(),
                        "template_name", template.getTemplateName(),
                        "level_id", template.getLevelId()
                ),
                "extracted_words", Map.of(
                        "grid_info", Map.of(
                                "width", template.getGridWidth(),
                                "height", template.getGridHeight(),
                                "pattern", template.getGridPattern()
                        ),
                        "word_order", extractedWords
                ),
                "word_analysis", Map.of(
                        "total_words", wordPositions.size(),
                        "extracted_words", extractedWords.size(),
                        "required_word_count", template.getWordCount(),
                        "required_intersection_count", template.getIntersectionCount(),
                        "retry_count", retryCount
                ),
                "query_log", queryLog
        );
    }

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message, List<Map<String, Object>> queryLog) {
        return createErrorResponse(message, queryLog, 0);
    }

    private Map<String, Object> createErrorResponse(String message, List<Map<String, Object>> queryLog, int retryCount) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("query_log", queryLog);
        if (retryCount > 0) {
            response.put("retry_count", retryCount);
        }
        return response;
    }

    /**
     * 쿼리 로그 생성
     */
    private Map<String, Object> createQueryLog(String type, String sql, List<Object> bindings, String description) {
        return Map.of(
                "type", type,
                "sql", sql,
                "bindings", bindings,
                "description", description
        );
    }
}
