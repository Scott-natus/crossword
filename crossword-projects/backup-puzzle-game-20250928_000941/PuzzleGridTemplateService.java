package com.example.crossword.service;

import com.example.crossword.entity.PuzzleGridTemplate;
import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.repository.PuzzleGridTemplateRepository;
import com.example.crossword.repository.PzWordRepository;
import com.example.crossword.repository.PzHintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;

/**
 * 퍼즐 그리드 템플릿 서비스 - Laravel 방식으로 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PuzzleGridTemplateService {
    
    private final JdbcTemplate jdbcTemplate;
    private final PuzzleGridTemplateRepository templateRepository;
    private final PzWordRepository wordRepository;
    private final PzHintRepository hintRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 레벨 ID로 템플릿 목록 조회 (Laravel 방식)
     */
    public List<Map<String, Object>> getTemplatesByLevelId(Integer levelId) {
        log.info("=== 템플릿 조회 시작 ===");
        log.info("레벨 ID: {}", levelId);
        
        // Laravel과 동일한 JOIN 쿼리: puzzle_grid_templates와 puzzle_levels를 JOIN
        // 교차점이 있는 템플릿을 우선적으로 선택
        String sql = "SELECT pgt.*, pl.word_difficulty, pl.hint_difficulty, pl.word_count, pl.intersection_count " +
                    "FROM puzzle_grid_templates pgt " +
                    "JOIN puzzle_levels pl ON pgt.level_id = pl.id " +
                    "WHERE pgt.level_id = ? AND pgt.is_active = true " +
                    "ORDER BY pgt.intersection_count DESC, RANDOM()";
        log.info("실행할 SQL: {}", sql);
        
        List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql, levelId);
        log.info("조회된 템플릿 개수: {}", templates.size());
        
        if (!templates.isEmpty()) {
            log.info("첫 번째 템플릿: {}", templates.get(0));
        }
        
        return templates;
    }
    
    /**
     * 템플릿에서 단어 추출 (Laravel의 extractWords 로직 포팅)
     */
    public Map<String, Object> extractWordsFromTemplate(Map<String, Object> template) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 템플릿 정보 조회 (이미 전달받음)
            Integer templateId = ((Number) template.get("id")).intValue();
            String gridPatternJson = template.get("grid_pattern").toString();
            String wordPositionsJson = template.get("word_positions").toString();
            
            // JSON 파싱
            List<List<Integer>> gridPattern = parseGridPattern(gridPatternJson);
            List<Map<String, Object>> wordPositions = parseWordPositions(wordPositionsJson);
            
            // 2. 단어 위치를 ID 순으로 정렬
            wordPositions.sort((a, b) -> {
                Integer idA = (Integer) a.get("id");
                Integer idB = (Integer) b.get("id");
                return idA.compareTo(idB);
            });
            
            // 3. 단어 추출 시도 (최대 5회)
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
                    Integer length = (Integer) word.get("length");
                    String direction = (String) word.get("direction");
                    
                    log.info("=== 단어 처리 시작 ===");
                    log.info("단어 ID: {}, 길이: {}, 방향: {}", wordId, length, direction);
                    log.info("현재 확정된 단어 개수: {}", confirmedWords.size());
                    
                    // 이미 확정된 단어는 건너뛰기
                    if (confirmedWords.containsKey(wordId)) {
                        log.info("단어 ID {}는 이미 확정됨, 건너뛰기", wordId);
                        continue;
                    }
                    
                    // 교차점 찾기
                    log.info("교차점 찾기 시작 - 단어 ID: {}", wordId);
                    List<Map<String, Object>> intersections = findIntersectionsWithConfirmedWords(word, confirmedWords, wordPositions);
                    log.info("교차점 개수: {}", intersections.size());
                    
                    if (intersections.isEmpty()) {
                        // 교차점이 없는 경우 - 독립적으로 단어 추출
                        Map<String, Object> extractedWord = extractIndependentWord(word, template);
                        if ("추출 실패".equals(extractedWord.get("word"))) {
                            extractionFailed = true;
                            break;
                        }
                        
                        extractedWords.add(Map.of(
                            "word_id", wordId,
                            "pz_word_id", extractedWord.get("pz_word_id"), // 실제 pz_words 테이블의 ID
                            "position", word,
                            "type", "no_intersection",
                            "extracted_word", "***", // 보안: 정답 숨김
                            "hint", extractedWord.get("hint")
                        ));
                        confirmedWords.put(wordId, (String) extractedWord.get("word"));
                    } else {
                        // 교차점이 있는 경우 - 확정된 음절과 매칭되는 단어 추출
                        List<Map<String, Object>> confirmedSyllables = new ArrayList<>();
                        
                        for (Map<String, Object> intersection : intersections) {
                            Integer connectedWordId = (Integer) intersection.get("connected_word_id");
                            String connectedWord = confirmedWords.get(connectedWordId);
                            Map<String, Object> connectedWordPosition = findWordById(connectedWordId, wordPositions);
                            
                            Integer connectedSyllablePos = getSyllablePosition(connectedWordPosition, (Map<String, Object>) intersection.get("position"));
                            String connectedSyllable = connectedWord.substring(connectedSyllablePos - 1, connectedSyllablePos);
                            
                            Integer currentSyllablePos = getSyllablePosition(word, (Map<String, Object>) intersection.get("position"));
                            
                            confirmedSyllables.add(Map.of(
                                "syllable", connectedSyllable,
                                "position", currentSyllablePos
                            ));
                        }
                        
                        Map<String, Object> extractedWord = extractWordWithConfirmedSyllables(word, template, confirmedSyllables, confirmedWords);
                        
                        if ((Boolean) extractedWord.get("success")) {
                            extractedWords.add(Map.of(
                                "word_id", wordId,
                                "pz_word_id", extractedWord.get("pz_word_id"), // 실제 pz_words 테이블의 ID
                                "position", word,
                                "type", "intersection_connected",
                                "extracted_word", "***", // 보안: 정답 숨김
                                "hint", extractedWord.get("hint")
                            ));
                            confirmedWords.put(wordId, (String) extractedWord.get("word"));
                        } else {
                            log.warn("교차점 단어 추출 실패 - 단어 ID: {}", wordId);
                            extractionFailed = true;
                            break;
                        }
                    }
                    
                    log.info("단어 ID {} 처리 완료", wordId);
                }
                
                log.info("단어 추출 루프 완료 - 추출된 단어 개수: {}, 실패 여부: {}", extractedWords.size(), extractionFailed);
                
                if (!extractionFailed) {
                    log.info("단어 추출 성공 - 시도 #{}에서 완료", retryCount);
                    break;
                }
                
                log.info("단어 추출 실패 - 시도 #{}, 재시도 예정", retryCount);
            }
            
            // extractionFailed 변수는 while 루프 내에서만 사용되므로 여기서는 제거
            // 대신 extractedWords가 비어있는지 확인
            if (extractedWords.isEmpty()) {
                result.put("success", false);
                result.put("message", "단어 추출에 실패했습니다.");
                return result;
            }
            
            // 성공 결과 구성
            result.put("success", true);
            result.put("template", Map.of(
                "id", templateId,
                "template_name", template.get("template_name"),
                "level_id", template.get("level_id")
            ));
            result.put("extracted_words", Map.of(
                "grid_info", Map.of(
                    "width", template.get("grid_width"),
                    "height", template.get("grid_height"),
                    "pattern", gridPattern
                ),
                "word_order", extractedWords
            ));
            
        } catch (Exception e) {
            log.error("단어 추출 중 오류 발생: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "단어 추출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    // 헬퍼 메서드들
    private List<List<Integer>> parseGridPattern(String gridPatternJson) {
        try {
            log.info("그리드 패턴 JSON 파싱 시작: {}", gridPatternJson);
            
            // Jackson ObjectMapper를 사용해서 JSON 파싱
            List<List<Integer>> gridPattern = objectMapper.readValue(
                gridPatternJson, 
                new TypeReference<List<List<Integer>>>() {}
            );
            
            log.info("파싱된 그리드 패턴: {}", gridPattern);
            log.info("그리드 크기: {}x{}", gridPattern.size(), gridPattern.get(0).size());
            
            return gridPattern;
        } catch (Exception e) {
            log.error("그리드 패턴 파싱 오류: {}", e.getMessage(), e);
            // 기본 5x5 그리드 반환
            List<List<Integer>> gridPattern = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                List<Integer> row = new ArrayList<>();
                for (int j = 0; j < 5; j++) {
                    row.add(1); // 기본적으로 글자칸
                }
                gridPattern.add(row);
            }
            return gridPattern;
        }
    }
    
    private List<Map<String, Object>> parseWordPositions(String wordPositionsJson) {
        // 간단한 JSON 파싱 (실제 데이터베이스에서 가져온 위치 정보 사용)
        List<Map<String, Object>> wordPositions = new ArrayList<>();
        
        try {
            log.info("단어 위치 JSON 파싱 시작: {}", wordPositionsJson);
            
            // Jackson ObjectMapper를 사용하여 JSON 파싱
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>> typeRef = 
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {};
            
            wordPositions = mapper.readValue(wordPositionsJson, typeRef);
            
            log.info("파싱된 단어 위치 개수: {}", wordPositions.size());
            for (Map<String, Object> word : wordPositions) {
                log.info("단어 위치: {}", word);
            }
            
        } catch (Exception e) {
            log.error("단어 위치 파싱 오류: {}", e.getMessage());
            // 기본 단어 위치 반환
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> wordPosition = new HashMap<>();
                wordPosition.put("id", i);
                wordPosition.put("start_x", 0);
                wordPosition.put("start_y", i - 1);
                wordPosition.put("end_x", 4);
                wordPosition.put("end_y", i - 1);
                wordPosition.put("direction", "horizontal");
                wordPosition.put("length", 5);
                wordPositions.add(wordPosition);
            }
        }
        
        return wordPositions;
    }
    
    private List<Map<String, Object>> findIntersectionsWithConfirmedWords(
            Map<String, Object> word, 
            Map<Integer, String> confirmedWords, 
            List<Map<String, Object>> wordPositions) {
        
        List<Map<String, Object>> intersections = new ArrayList<>();
        
        try {
            Integer currentWordId = (Integer) word.get("id");
            String currentDirection = (String) word.get("direction");
            Integer currentStartX = (Integer) word.get("start_x");
            Integer currentStartY = (Integer) word.get("start_y");
            Integer currentEndX = (Integer) word.get("end_x");
            Integer currentEndY = (Integer) word.get("end_y");
            
            log.debug("교차점 찾기 - 현재 단어 ID: {}, 방향: {}, 시작: ({},{}), 끝: ({},{})", 
                     currentWordId, currentDirection, currentStartX, currentStartY, currentEndX, currentEndY);
            
            // 이미 확정된 단어들과 교차점 찾기
            for (Map.Entry<Integer, String> entry : confirmedWords.entrySet()) {
                Integer confirmedWordId = entry.getKey();
                String confirmedWord = entry.getValue();
                
                // 확정된 단어의 위치 정보 찾기
                Map<String, Object> confirmedWordPosition = findWordById(confirmedWordId, wordPositions);
                if (confirmedWordPosition == null) {
                    continue;
                }
                
                String confirmedDirection = (String) confirmedWordPosition.get("direction");
                Integer confirmedStartX = (Integer) confirmedWordPosition.get("start_x");
                Integer confirmedStartY = (Integer) confirmedWordPosition.get("start_y");
                Integer confirmedEndX = (Integer) confirmedWordPosition.get("end_x");
                Integer confirmedEndY = (Integer) confirmedWordPosition.get("end_y");
                
                log.debug("확정된 단어 ID: {}, 방향: {}, 시작: ({},{}), 끝: ({},{})", 
                         confirmedWordId, confirmedDirection, confirmedStartX, confirmedStartY, confirmedEndX, confirmedEndY);
                
                // 교차점 찾기
                Map<String, Object> intersection = findIntersection(
                    currentStartX, currentStartY, currentEndX, currentEndY, currentDirection,
                    confirmedStartX, confirmedStartY, confirmedEndX, confirmedEndY, confirmedDirection
                );
                
                if (intersection != null) {
                    intersection.put("connected_word_id", confirmedWordId);
                    intersection.put("connected_word", confirmedWord);
                    intersections.add(intersection);
                    
                    log.debug("교차점 발견: {}", intersection);
                }
            }
            
            log.debug("총 교차점 개수: {}", intersections.size());
            
        } catch (Exception e) {
            log.error("교차점 찾기 중 오류: {}", e.getMessage(), e);
        }
        
        return intersections;
    }
    
    // 두 단어의 교차점 찾기 (Laravel과 동일한 로직)
    private Map<String, Object> findIntersection(
            Integer x1, Integer y1, Integer x2, Integer y2, String dir1,
            Integer x3, Integer y3, Integer x4, Integer y4, String dir2) {
        
        // 같은 방향인 경우 연결점 확인
        if (dir1.equals(dir2)) {
            return findConnectionPoint(x1, y1, x2, y2, dir1, x3, y3, x4, y4, dir2);
        }
        
        // 가로-세로 교차 확인
        Map<String, Object> horizontal, vertical;
        if (dir1.equals("horizontal")) {
            horizontal = Map.of("start_x", x1, "start_y", y1, "end_x", x2, "end_y", y2);
            vertical = Map.of("start_x", x3, "start_y", y3, "end_x", x4, "end_y", y4);
        } else {
            horizontal = Map.of("start_x", x3, "start_y", y3, "end_x", x4, "end_y", y4);
            vertical = Map.of("start_x", x1, "start_y", y1, "end_x", x2, "end_y", y2);
        }
        
        // 교차점 좌표 계산
        Integer intersectX = (Integer) vertical.get("start_x");
        Integer intersectY = (Integer) horizontal.get("start_y");
        
        // 교차점이 두 단어 범위 내에 있는지 확인
        if (intersectX >= (Integer) horizontal.get("start_x") && intersectX <= (Integer) horizontal.get("end_x") &&
            intersectY >= (Integer) vertical.get("start_y") && intersectY <= (Integer) vertical.get("end_y")) {
            
            return Map.of(
                "x", intersectX,
                "y", intersectY,
                "horizontal_word_id", horizontal.get("id"),
                "vertical_word_id", vertical.get("id")
            );
        }
        
        return null;
    }
    
    // 같은 방향의 두 단어 사이의 연결점 찾기
    private Map<String, Object> findConnectionPoint(
            Integer x1, Integer y1, Integer x2, Integer y2, String dir1,
            Integer x3, Integer y3, Integer x4, Integer y4, String dir2) {
        
        // 가로 방향 연결점 확인
        if (dir1.equals("horizontal") && dir2.equals("horizontal")) {
            // 같은 y좌표에서 연결되는지 확인
            if (y1.equals(y3)) {
                // word1의 끝점과 word2의 시작점이 연결되는지 확인
                if (x2 + 1 == x3) {
                    return Map.of(
                        "x", x2,
                        "y", y1,
                        "word1_end", true,
                        "word2_start", true
                    );
                }
                // word2의 끝점과 word1의 시작점이 연결되는지 확인
                if (x4 + 1 == x1) {
                    return Map.of(
                        "x", x4,
                        "y", y1,
                        "word1_start", true,
                        "word2_end", true
                    );
                }
            }
        }
        // 세로 방향 연결점 확인
        else if (dir1.equals("vertical") && dir2.equals("vertical")) {
            // 같은 x좌표에서 연결되는지 확인
            if (x1.equals(x3)) {
                // word1의 끝점과 word2의 시작점이 연결되는지 확인
                if (y2 + 1 == y3) {
                    return Map.of(
                        "x", x1,
                        "y", y2,
                        "word1_end", true,
                        "word2_start", true
                    );
                }
                // word2의 끝점과 word1의 시작점이 연결되는지 확인
                if (y4 + 1 == y1) {
                    return Map.of(
                        "x", x1,
                        "y", y4,
                        "word1_start", true,
                        "word2_end", true
                    );
                }
            }
        }
        
        return null;
    }
    
    private Map<String, Object> extractIndependentWord(Map<String, Object> word, Map<String, Object> template) {
        // Laravel과 동일한 독립적인 단어 추출 로직
        Map<String, Object> result = new HashMap<>();
        
        try {
            Integer length = (Integer) word.get("length");
            Integer wordDifficulty = (Integer) template.get("word_difficulty");
            
            log.info("=== 독립 단어 추출 시작 ===");
            log.info("길이: {}, 난이도: {}", length, wordDifficulty);
            
            // Laravel의 getAllowedDifficulties 로직 적용
            List<Integer> allowedDifficulties = getAllowedDifficulties(wordDifficulty);
            log.info("허용 난이도: {}", allowedDifficulties);
            
            // Laravel과 동일한 JOIN 쿼리: pz_words와 pz_hints를 JOIN
            String sql = "SELECT a.id, a.word, b.hint_text as hint " +
                        "FROM pz_words a " +
                        "JOIN pz_hints b ON a.id = b.word_id " +
                        "WHERE a.length = ? " +
                        "AND a.difficulty IN (" + allowedDifficulties.stream()
                            .map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(",")) + ") " +
                        "AND a.is_active = true " +
                        "ORDER BY RANDOM() LIMIT 1";
            
            log.info("실행할 SQL: {}", sql);
            log.info("SQL 파라미터: length={}", length);
            
            List<Map<String, Object>> words = jdbcTemplate.queryForList(sql, length);
            
            log.info("쿼리 결과 개수: {}", words.size());
            if (!words.isEmpty()) {
                log.info("첫 번째 결과: {}", words.get(0));
            }
            
            if (!words.isEmpty()) {
                Map<String, Object> selectedWord = words.get(0);
                String wordText = (String) selectedWord.get("word");
                String hintText = (String) selectedWord.get("hint");
                Integer pzWordId = (Integer) selectedWord.get("id"); // pz_words 테이블의 ID
                
                log.debug("선택된 단어: {}, 힌트: {}, ID: {}", wordText, hintText, pzWordId);
                
                result.put("word", wordText);
                result.put("hint", hintText != null ? hintText : "힌트가 없습니다.");
                result.put("pz_word_id", pzWordId);
            } else {
                log.warn("길이 {}에 맞는 단어를 찾을 수 없음 (허용 난이도: {})", length, allowedDifficulties);
                result.put("word", "추출 실패");
                result.put("hint", "조건에 맞는 단어를 찾을 수 없습니다.");
                result.put("pz_word_id", null);
            }
        } catch (Exception e) {
            log.error("독립 단어 추출 오류: {}", e.getMessage(), e);
            result.put("word", "추출 실패");
            result.put("hint", "힌트 없음");
        }
        
        return result;
    }
    
    // Laravel의 getAllowedDifficulties 메서드와 동일한 로직
    private List<Integer> getAllowedDifficulties(Integer levelDifficulty) {
        switch (levelDifficulty) {
            case 1:
                return List.of(1, 2); // 레벨 1: 난이도 1,2
            case 2:
                return List.of(1, 2, 3); // 레벨 2: 난이도 1,2,3
            case 3:
                return List.of(2, 3, 4); // 레벨 3: 난이도 2,3,4
            case 4:
                return List.of(3, 4, 5); // 레벨 4: 난이도 3,4,5
            case 5:
                return List.of(4, 5); // 레벨 5: 난이도 4,5
            default:
                return List.of(1, 2, 3, 4, 5); // 기본값: 모든 난이도
        }
    }
    
    private Map<String, Object> findWordById(Integer wordId, List<Map<String, Object>> wordPositions) {
        // ID로 단어 위치 찾기
        return wordPositions.stream()
                .filter(wp -> wordId.equals(wp.get("id")))
                .findFirst()
                .orElse(new HashMap<>());
    }
    
    private Integer getSyllablePosition(Map<String, Object> wordPosition, Map<String, Object> intersectionPosition) {
        try {
            String direction = (String) wordPosition.get("direction");
            Integer startX = (Integer) wordPosition.get("start_x");
            Integer startY = (Integer) wordPosition.get("start_y");
            Integer intersectionX = (Integer) intersectionPosition.get("x");
            Integer intersectionY = (Integer) intersectionPosition.get("y");
            
            if (direction.equals("horizontal")) {
                // 가로 방향: X 좌표 차이 + 1
                return intersectionX - startX + 1;
            } else if (direction.equals("vertical")) {
                // 세로 방향: Y 좌표 차이 + 1
                return intersectionY - startY + 1;
            }
        } catch (Exception e) {
            log.error("음절 위치 계산 오류: {}", e.getMessage(), e);
        }
        
        return 1; // 기본값
    }
    
    private Map<String, Object> extractWordWithConfirmedSyllables(
            Map<String, Object> word, 
            Map<String, Object> template, 
            List<Map<String, Object>> confirmedSyllables, 
            Map<Integer, String> confirmedWords) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Integer length = (Integer) word.get("length");
            Integer wordDifficulty = (Integer) template.get("word_difficulty");
            
            log.info("=== 교차점 단어 추출 시작 ===");
            log.info("길이: {}, 난이도: {}, 확정된 음절 개수: {}", length, wordDifficulty, confirmedSyllables.size());
            
            // Laravel의 getAllowedDifficulties 로직 적용
            List<Integer> allowedDifficulties = getAllowedDifficulties(wordDifficulty);
            log.info("허용 난이도: {}", allowedDifficulties);
            
            // 기본 쿼리 구성
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT a.id, a.word, b.hint_text as hint ");
            sqlBuilder.append("FROM pz_words a ");
            sqlBuilder.append("JOIN pz_hints b ON a.id = b.word_id ");
            sqlBuilder.append("WHERE a.length = ? ");
            sqlBuilder.append("AND a.difficulty IN (").append(allowedDifficulties.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","))).append(") ");
            sqlBuilder.append("AND a.is_active = true ");
            
            // 교차점을 공유하는 다른 단어들과 같은 단어 제외
            if (!confirmedWords.isEmpty()) {
                String excludedWords = confirmedWords.values().stream()
                    .map(w -> "'" + w + "'")
                    .collect(java.util.stream.Collectors.joining(","));
                sqlBuilder.append("AND a.word NOT IN (").append(excludedWords).append(") ");
            }
            
            // 각 확정된 음절에 대한 조건 추가
            List<Object> params = new ArrayList<>();
            params.add(length);
            
            for (Map<String, Object> syllableInfo : confirmedSyllables) {
                String syllable = (String) syllableInfo.get("syllable");
                Integer position = (Integer) syllableInfo.get("position");
                
                log.info("음절 일치 조건 추가 - 위치: {}, 음절: {}", position, syllable);
                
                sqlBuilder.append("AND SUBSTRING(a.word, ").append(position).append(", 1) = ? ");
                params.add(syllable);
            }
            
            sqlBuilder.append("ORDER BY RANDOM() LIMIT 1");
            
            String sql = sqlBuilder.toString();
            log.info("실행할 SQL: {}", sql);
            log.info("SQL 파라미터: {}", params);
            
            List<Map<String, Object>> words = jdbcTemplate.queryForList(sql, params.toArray());
            
            log.info("쿼리 결과 개수: {}", words.size());
            
            if (!words.isEmpty()) {
                Map<String, Object> selectedWord = words.get(0);
                String wordText = (String) selectedWord.get("word");
                String hintText = (String) selectedWord.get("hint");
                
                Integer pzWordId = (Integer) selectedWord.get("id"); // pz_words 테이블의 ID
                
                log.info("선택된 단어: {}, 힌트: {}, ID: {}", wordText, hintText, pzWordId);
                
                result.put("success", true);
                result.put("word", wordText);
                result.put("hint", hintText != null ? hintText : "힌트가 없습니다.");
                result.put("pz_word_id", pzWordId);
            } else {
                log.warn("교차점 조건에 맞는 단어를 찾을 수 없음");
                result.put("success", false);
                result.put("word", "추출 실패");
                result.put("hint", "교차점 조건에 맞는 단어를 찾을 수 없습니다.");
            }
            
        } catch (Exception e) {
            log.error("교차점 단어 추출 오류: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("word", "추출 실패");
            result.put("hint", "힌트 없음");
        }
        
        return result;
    }
    
    /**
     * 템플릿의 단어들에 실제 단어 데이터 매핑
     */
    public Map<String, Object> extractWordsWithData(PuzzleGridTemplate template) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 템플릿의 word_positions에서 단어 정보 추출
            List<Map<String, Object>> wordPositions = template.getWordPositions();
            List<Map<String, Object>> wordsWithData = new ArrayList<>();
            
            for (Map<String, Object> wordPosition : wordPositions) {
                Map<String, Object> wordData = new HashMap<>();
                
                // 위치 정보 복사
                wordData.put("id", wordPosition.get("id"));
                wordData.put("start_x", wordPosition.get("start_x"));
                wordData.put("start_y", wordPosition.get("start_y"));
                wordData.put("end_x", wordPosition.get("end_x"));
                wordData.put("end_y", wordPosition.get("end_y"));
                wordData.put("direction", wordPosition.get("direction"));
                wordData.put("length", wordPosition.get("length"));
                
                // 실제 단어 데이터 조회 (길이와 난이도에 맞는 단어)
                Integer length = (Integer) wordPosition.get("length");
                List<PzWord> availableWords = wordRepository.findByLengthAndIsActiveTrue(length);
                
                if (!availableWords.isEmpty()) {
                    // 랜덤으로 단어 선택
                    PzWord selectedWord = availableWords.get(new Random().nextInt(availableWords.size()));
                    
                    wordData.put("word_id", selectedWord.getId());
                    wordData.put("pz_word_id", selectedWord.getId());
                    wordData.put("word", selectedWord.getWord());
                    wordData.put("category", selectedWord.getCategory());
                    wordData.put("difficulty", selectedWord.getDifficulty());
                    
                    // 힌트 조회
                    List<PzHint> hints = hintRepository.findTextHintsByWordId(selectedWord.getId());
                    if (!hints.isEmpty()) {
                        wordData.put("hint", hints.get(0).getHintText());
                    } else {
                        wordData.put("hint", "힌트가 없습니다.");
                    }
                    
                    // position 객체 생성 (Laravel 구조와 동일)
                    Map<String, Object> position = new HashMap<>();
                    position.put("direction", wordPosition.get("direction"));
                    position.put("start_x", wordPosition.get("start_x"));
                    position.put("start_y", wordPosition.get("start_y"));
                    position.put("end_x", wordPosition.get("end_x"));
                    position.put("end_y", wordPosition.get("end_y"));
                    wordData.put("position", position);
                    
                    wordsWithData.add(wordData);
                }
            }
            
            result.put("success", true);
            result.put("template", template);
            result.put("words", wordsWithData);
            result.put("message", "단어 추출 성공");
            
        } catch (Exception e) {
            log.error("단어 추출 중 오류 발생: {}", e.getMessage());
            result.put("success", false);
            result.put("error", "단어 추출 실패: " + e.getMessage());
        }
        
        return result;
    }
}
