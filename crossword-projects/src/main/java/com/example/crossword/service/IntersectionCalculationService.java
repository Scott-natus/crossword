package com.example.crossword.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 교차점 계산 서비스
 * 그리드 템플릿에서 단어 간 교차점을 계산하는 로직
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-23
 */
@Service
@Slf4j
public class IntersectionCalculationService {

    /**
     * 단어 위치 정보를 분석하여 교차점 정보를 실시간으로 계산
     * Laravel의 analyzeWordPositions 메서드와 동일한 기능
     */
    public Map<Integer, Map<String, Object>> analyzeWordPositions(List<Map<String, Object>> wordPositions) {
        log.info("단어 위치 분석 시작 - 총 {}개 단어", wordPositions.size());
        
        Map<Integer, Map<String, Object>> analysis = new HashMap<>();
        
        for (Map<String, Object> word : wordPositions) {
            Integer wordId = (Integer) word.get("id");
            List<Map<String, Object>> intersections = new ArrayList<>();
            
            // 다른 모든 단어와의 교차점 찾기
            for (Map<String, Object> otherWord : wordPositions) {
                if (wordId.equals(otherWord.get("id"))) continue;
                
                Map<String, Object> intersection = findIntersection(word, otherWord);
                if (intersection != null) {
                    intersections.add(Map.of(
                            "position", intersection,
                            "connected_word_id", otherWord.get("id"),
                            "connected_word", otherWord
                    ));
                }
            }
            
            // 교차점을 좌에서 우로, 위에서 아래로 순서 정렬
            intersections.sort((a, b) -> {
                Map<String, Object> posA = (Map<String, Object>) a.get("position");
                Map<String, Object> posB = (Map<String, Object>) b.get("position");
                
                Integer yA = (Integer) posA.get("y");
                Integer yB = (Integer) posB.get("y");
                
                if (!yA.equals(yB)) {
                    return yA.compareTo(yB); // y 좌표 우선
                }
                Integer xA = (Integer) posA.get("x");
                Integer xB = (Integer) posB.get("x");
                return xA.compareTo(xB); // x 좌표 차선
            });
            
            analysis.put(wordId, Map.of(
                    "word", word,
                    "intersections", intersections
            ));
            
            log.debug("단어 ID {}: {}개 교차점 발견", wordId, intersections.size());
        }
        
        log.info("단어 위치 분석 완료 - 총 {}개 단어 분석", analysis.size());
        return analysis;
    }

    /**
     * 두 단어 사이의 교차점 찾기
     * Laravel의 findIntersection 메서드와 동일한 로직
     */
    public Map<String, Object> findIntersection(Map<String, Object> word1, Map<String, Object> word2) {
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
                    "vertical_word_id", vertical.get("id"),
                    "type", "cross"
            );
        }
        
        return null;
    }

    /**
     * 같은 방향의 두 단어 사이의 연결점 찾기
     * Laravel의 findConnectionPoint 메서드와 동일한 로직
     */
    public Map<String, Object> findConnectionPoint(Map<String, Object> word1, Map<String, Object> word2) {
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
                            "word2_start", true,
                            "type", "connection"
                    );
                }
                // word2의 끝점과 word1의 시작점이 연결되는지 확인
                if (endX2 + 1 == startX1) {
                    return Map.of(
                            "x", endX2,
                            "y", y1,
                            "word1_start", true,
                            "word2_end", true,
                            "type", "connection"
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
                            "word2_start", true,
                            "type", "connection"
                    );
                }
                // word2의 끝점과 word1의 시작점이 연결되는지 확인
                if (endY2 + 1 == startY1) {
                    return Map.of(
                            "x", x1,
                            "y", endY2,
                            "word1_start", true,
                            "word2_end", true,
                            "type", "connection"
                    );
                }
            }
        }
        
        return null;
    }

    /**
     * 음절 위치 계산
     * Laravel의 getSyllablePosition 메서드와 동일한 로직
     */
    public Integer getSyllablePosition(Map<String, Object> word, Map<String, Object> intersection) {
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
     * 그리드 패턴에서 교차점 수 계산
     * Laravel의 countIntersections 메서드와 동일한 로직
     */
    public Integer countIntersections(List<List<Integer>> gridPattern) {
        int count = 0;
        int size = gridPattern.size();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (gridPattern.get(i).get(j) == 1) { // 흰색 칸 (단어가 들어갈 수 있는 칸)
                    // 가로와 세로 방향 모두에 단어가 있는지 확인
                    boolean hasHorizontal = false;
                    boolean hasVertical = false;
                    
                    // 가로 방향 확인
                    if (j > 0 && gridPattern.get(i).get(j-1) == 1) hasHorizontal = true;
                    if (j < size-1 && gridPattern.get(i).get(j+1) == 1) hasHorizontal = true;
                    
                    // 세로 방향 확인
                    if (i > 0 && gridPattern.get(i-1).get(j) == 1) hasVertical = true;
                    if (i < size-1 && gridPattern.get(i+1).get(j) == 1) hasVertical = true;
                    
                    if (hasHorizontal && hasVertical) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }

    /**
     * 그리드 패턴에서 단어 수 계산
     * Laravel의 countWords 메서드와 동일한 로직
     */
    public Integer countWords(List<List<Integer>> gridPattern) {
        int size = gridPattern.size();
        int wordCount = 0;
        boolean[][] visited = new boolean[size][size];
        
        // 가로 단어 찾기
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (gridPattern.get(i).get(j) == 1 && !visited[i][j]) { // 흰색 칸이고 방문하지 않음
                    // 가로 방향으로 연속된 흰색 칸 확인
                    int startJ = j;
                    int endJ = j;
                    int k = j;
                    while (k < size && gridPattern.get(i).get(k) == 1) {
                        visited[i][k] = true;
                        endJ = k;
                        k++;
                    }
                    
                    // 2칸 이상이면 단어로 인정
                    if (endJ - startJ + 1 >= 2) {
                        wordCount++;
                    }
                }
            }
        }
        
        // visited 배열 초기화
        for (int i = 0; i < size; i++) {
            Arrays.fill(visited[i], false);
        }
        
        // 세로 단어 찾기
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                if (gridPattern.get(i).get(j) == 1 && !visited[i][j]) { // 흰색 칸이고 방문하지 않음
                    // 세로 방향으로 연속된 흰색 칸 확인
                    int startI = i;
                    int endI = i;
                    int k = i;
                    while (k < size && gridPattern.get(k).get(j) == 1) {
                        visited[k][j] = true;
                        endI = k;
                        k++;
                    }
                    
                    // 2칸 이상이면 단어로 인정
                    if (endI - startI + 1 >= 2) {
                        wordCount++;
                    }
                }
            }
        }
        
        return wordCount;
    }

    /**
     * 그리드 패턴에서 단어 위치 정보 추출
     * Laravel의 extractWordPositions 메서드와 동일한 로직
     */
    public List<Map<String, Object>> extractWordPositions(List<List<Integer>> gridPattern) {
        int size = gridPattern.size();
        List<Map<String, Object>> wordPositions = new ArrayList<>();
        int wordId = 1;
        boolean[][] visited = new boolean[size][size];
        
        // 가로 단어 찾기
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (gridPattern.get(i).get(j) == 1 && !visited[i][j]) { // 흰색 칸이고 방문하지 않음
                    // 가로 방향으로 연속된 흰색 칸 확인
                    int startJ = j;
                    int endJ = j;
                    int k = j;
                    while (k < size && gridPattern.get(i).get(k) == 1) {
                        visited[i][k] = true;
                        endJ = k;
                        k++;
                    }
                    
                    // 2칸 이상이면 단어로 인정
                    if (endJ - startJ + 1 >= 2) {
                        wordPositions.add(Map.of(
                                "id", wordId++,
                                "start_x", startJ,
                                "start_y", i,
                                "end_x", endJ,
                                "end_y", i,
                                "direction", "horizontal",
                                "length", endJ - startJ + 1
                        ));
                    }
                }
            }
        }
        
        // visited 배열 초기화
        for (int i = 0; i < size; i++) {
            Arrays.fill(visited[i], false);
        }
        
        // 세로 단어 찾기
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                if (gridPattern.get(i).get(j) == 1 && !visited[i][j]) { // 흰색 칸이고 방문하지 않음
                    // 세로 방향으로 연속된 흰색 칸 확인
                    int startI = i;
                    int endI = i;
                    int k = i;
                    while (k < size && gridPattern.get(k).get(j) == 1) {
                        visited[k][j] = true;
                        endI = k;
                        k++;
                    }
                    
                    // 2칸 이상이면 단어로 인정
                    if (endI - startI + 1 >= 2) {
                        wordPositions.add(Map.of(
                                "id", wordId++,
                                "start_x", j,
                                "start_y", startI,
                                "end_x", j,
                                "end_y", endI,
                                "direction", "vertical",
                                "length", endI - startI + 1
                        ));
                    }
                }
            }
        }
        
        return wordPositions;
    }

    /**
     * 검은색 칸 개수 계산
     * Laravel의 countBlackCells 메서드와 동일한 로직
     */
    public Integer countBlackCells(List<List<Integer>> gridPattern) {
        int count = 0;
        int size = gridPattern.size();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (gridPattern.get(i).get(j) == 2) { // 검은색 칸
                    count++;
                }
            }
        }
        
        return count;
    }

    /**
     * 흰색 칸 개수 계산
     * Laravel의 countWhiteCells 메서드와 동일한 로직
     */
    public Integer countWhiteCells(List<List<Integer>> gridPattern) {
        int count = 0;
        int size = gridPattern.size();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (gridPattern.get(i).get(j) == 1) { // 흰색 칸
                    count++;
                }
            }
        }
        
        return count;
    }

    /**
     * 그리드 패턴 유효성 검증
     * 기본적인 그리드 패턴 검증 로직
     */
    public Map<String, Object> validateGridPattern(List<List<Integer>> gridPattern) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (gridPattern == null || gridPattern.isEmpty()) {
            errors.add("그리드 패턴이 비어있습니다.");
            result.put("valid", false);
            result.put("errors", errors);
            result.put("warnings", warnings);
            return result;
        }
        
        int size = gridPattern.size();
        
        // 그리드가 정사각형인지 확인
        for (int i = 0; i < size; i++) {
            if (gridPattern.get(i).size() != size) {
                errors.add("그리드가 정사각형이 아닙니다.");
                break;
            }
        }
        
        // 최소 크기 확인
        if (size < 3) {
            errors.add("그리드 크기는 최소 3x3이어야 합니다.");
        }
        
        // 최대 크기 확인
        if (size > 20) {
            errors.add("그리드 크기는 최대 20x20까지 가능합니다.");
        }
        
        // 단어 수 계산
        Integer wordCount = countWords(gridPattern);
        if (wordCount < 2) {
            errors.add("최소 2개 이상의 단어가 필요합니다.");
        }
        
        // 교차점 수 계산
        Integer intersectionCount = countIntersections(gridPattern);
        if (intersectionCount == 0 && wordCount > 1) {
            warnings.add("단어들이 교차하지 않습니다. 교차점이 있는 패턴을 권장합니다.");
        }
        
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("wordCount", wordCount);
        result.put("intersectionCount", intersectionCount);
        result.put("blackCells", countBlackCells(gridPattern));
        result.put("whiteCells", countWhiteCells(gridPattern));
        
        return result;
    }
}

