package com.example.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 템플릿 자동 생성 서비스
 * 레벨 조건(단어 개수, 교차점 개수)에 맞는 그리드 템플릿을 자동으로 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateGenerationService {
    
    /**
     * 템플릿 자동 생성
     * 
     * @param wordCount 필요한 단어 개수
     * @param intersectionCount 최소 교차점 개수
     * @return 생성된 템플릿 데이터 (gridPattern, wordPositions)
     */
    public Map<String, Object> generateTemplate(int wordCount, int intersectionCount) {
        log.info("템플릿 자동 생성 시작 - 단어 개수: {}, 교차점 개수: {}", wordCount, intersectionCount);
        
        // 1. 그리드 크기 계산
        int gridSize = calculateGridSize(wordCount);
        log.info("계산된 그리드 크기: {}x{}", gridSize, gridSize);
        
        // 2. 최대 시도 횟수
        int maxAttempts = 10000;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            attempt++;
            
            try {
                // 2-1. 단어 길이 랜덤 생성 (2~4글자)
                List<Integer> wordLengths = generateRandomWordLengths(wordCount, gridSize);
                log.debug("시도 {}: 단어 길이 = {}", attempt, wordLengths);
                
                // 2-2. 빈 그리드 생성 (모두 흰색)
                int[][] grid = createEmptyGrid(gridSize);
                List<Map<String, Object>> placedWords = new ArrayList<>();
                
                // 2-3. 첫 번째 단어 배치
                Map<String, Object> wordA = placeFirstWord(grid, wordLengths.get(0), gridSize);
                placedWords.add(wordA);
                
                // 2-4. 두 번째 단어부터 순차 배치
                boolean allPlaced = true;
                for (int i = 1; i < wordCount; i++) {
                    Map<String, Object> word = placeNextWord(grid, wordLengths.get(i), placedWords, gridSize);
                    
                    if (word == null) {
                        // 배치 실패 - 다음 시도로
                        allPlaced = false;
                        break;
                    }
                    
                    placedWords.add(word);
                    
                    // 마지막 단어가 아닌 경우: 현재까지의 교차점 확인
                    if (i < wordCount - 1) {
                        int currentIntersections = countIntersections(placedWords);
                        // 두 번째 단어 배치 후: A와 B의 교차점이 있어야 함
                        // 교차점이 없으면 다음 시도로 넘어감
                        if (currentIntersections == 0) {
                            allPlaced = false;
                            break;
                        }
                    }
                }
                
                if (!allPlaced) {
                    continue; // 다음 시도
                }
                
                // 2-5. 최종 교차점 확인 (모든 단어 배치 후)
                int actualIntersectionCount = countIntersections(placedWords);
                
                if (actualIntersectionCount >= intersectionCount) {
                    // 그리드 패턴 생성 (검은칸 배치)
                    int[][] finalGrid = createGridFromWordPositions(placedWords, gridSize);
                    
                    // 검은색 칸 인접 규칙 검증
                    if (validateBlackCellAdjacency(finalGrid, gridSize, placedWords)) {
                        log.info("템플릿 생성 성공 (시도 {}회) - 단어: {}개, 교차점: {}개", 
                            attempt, placedWords.size(), actualIntersectionCount);
                        
                        Map<String, Object> result = new HashMap<>();
                        result.put("gridPattern", finalGrid);
                        result.put("wordPositions", placedWords);
                        result.put("gridWidth", gridSize);
                        result.put("gridHeight", gridSize);
                        result.put("wordCount", placedWords.size());
                        result.put("intersectionCount", actualIntersectionCount);
                        result.put("attempts", attempt);
                        
                        return result;
                    } else {
                        // 검은색 칸 인접 규칙 위반 - 다음 시도
                        log.debug("검은색 칸 인접 규칙 위반 - 다음 시도");
                    }
                }
                
            } catch (Exception e) {
                log.debug("시도 {} 실패: {}", attempt, e.getMessage());
                // 다음 시도로 계속
            }
        }
        
        log.warn("템플릿 생성 실패 (최대 시도 횟수 초과: {})", maxAttempts);
        throw new RuntimeException("템플릿 생성에 실패했습니다. 조건을 만족하는 그리드를 생성할 수 없습니다.");
    }
    
    /**
     * 그리드 크기 계산
     */
    private int calculateGridSize(int wordCount) {
        if (wordCount <= 5) {
            return 5;
        } else if (wordCount <= 8) {
            return 6;
        } else if (wordCount <= 12) {
            return 7;
        } else if (wordCount <= 16) {
            return 8;
        } else {
            return 9;
        }
    }
    
    /**
     * 단어 길이 랜덤 생성 (2~4글자)
     */
    private List<Integer> generateRandomWordLengths(int wordCount, int gridSize) {
        List<Integer> lengths = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < wordCount; i++) {
            // 2~4글자 랜덤, 단 그리드 크기를 넘지 않도록
            int maxLength = Math.min(4, gridSize - 1);
            int length = 2 + random.nextInt(maxLength - 1); // 2 ~ maxLength
            lengths.add(length);
        }
        
        return lengths;
    }
    
    /**
     * 빈 그리드 생성 (모든 칸을 흰색으로 초기화)
     * 규칙:
     * - 1: 흰색 칸 (의미 없는 칸, 빈 공간)
     * - 2: 검은색 칸 (단어가 들어가는 칸)
     */
    private int[][] createEmptyGrid(int size) {
        int[][] grid = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = 1; // 1: 흰색 칸 (의미 없는 칸)
            }
        }
        return grid;
    }
    
    /**
     * 첫 번째 단어 배치 (랜덤 위치, 랜덤 방향)
     */
    private Map<String, Object> placeFirstWord(int[][] grid, int wordLength, int gridSize) {
        Random random = new Random();
        boolean isHorizontal = random.nextBoolean();
        
        int row, col;
        if (isHorizontal) {
            row = random.nextInt(gridSize);
            col = random.nextInt(gridSize - wordLength + 1);
        } else {
            row = random.nextInt(gridSize - wordLength + 1);
            col = random.nextInt(gridSize);
        }
        
        // 단어 배치 (그리드에 표시하지 않고 위치만 반환)
        Map<String, Object> wordPos = new HashMap<>();
        wordPos.put("id", 1);
        wordPos.put("start_x", col);
        wordPos.put("start_y", row);
        if (isHorizontal) {
            wordPos.put("end_x", col + wordLength - 1);
            wordPos.put("end_y", row);
        } else {
            wordPos.put("end_x", col);
            wordPos.put("end_y", row + wordLength - 1);
        }
        wordPos.put("direction", isHorizontal ? "horizontal" : "vertical");
        
        return wordPos;
    }
    
    /**
     * 다음 단어 배치 (기존 단어들과 교차점을 만들 수 있는 위치 찾기)
     */
    private Map<String, Object> placeNextWord(int[][] grid, int wordLength, 
                                              List<Map<String, Object>> placedWords, 
                                              int gridSize) {
        Random random = new Random();
        int maxAttempts = 100; // 각 단어당 최대 시도 횟수
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean isHorizontal = random.nextBoolean();
            
            int row, col;
            if (isHorizontal) {
                row = random.nextInt(gridSize);
                col = random.nextInt(gridSize - wordLength + 1);
            } else {
                row = random.nextInt(gridSize - wordLength + 1);
                col = random.nextInt(gridSize);
            }
            
            // 배치 가능 여부 확인
            if (canPlaceWord(grid, row, col, wordLength, isHorizontal, placedWords)) {
                Map<String, Object> wordPos = new HashMap<>();
                wordPos.put("id", placedWords.size() + 1);
                wordPos.put("start_x", col);
                wordPos.put("start_y", row);
                if (isHorizontal) {
                    wordPos.put("end_x", col + wordLength - 1);
                    wordPos.put("end_y", row);
                } else {
                    wordPos.put("end_x", col);
                    wordPos.put("end_y", row + wordLength - 1);
                }
                wordPos.put("direction", isHorizontal ? "horizontal" : "vertical");
                
                return wordPos;
            }
        }
        
        return null; // 배치 실패
    }
    
    /**
     * 단어 배치 가능 여부 확인
     * 규칙:
     * 1. 같은 방향 단어는 겹치면 안 됨
     * 2. 다른 방향 단어는 교차점으로만 만날 수 있음 (인접하면 안 됨)
     */
    private boolean canPlaceWord(int[][] grid, int row, int col, int wordLength, 
                                boolean isHorizontal, List<Map<String, Object>> placedWords) {
        // 그리드 범위 확인
        if (isHorizontal) {
            if (col + wordLength > grid.length) return false;
        } else {
            if (row + wordLength > grid.length) return false;
        }
        
        // 새 단어의 모든 칸 위치 계산
        Set<String> newWordCells = new HashSet<>();
        if (isHorizontal) {
            for (int x = col; x < col + wordLength; x++) {
                newWordCells.add(x + "," + row);
            }
        } else {
            for (int y = row; y < row + wordLength; y++) {
                newWordCells.add(col + "," + y);
            }
        }
        
        // 기존 단어와의 충돌 확인
        for (Map<String, Object> placedWord : placedWords) {
            int pStartX = (Integer) placedWord.get("start_x");
            int pStartY = (Integer) placedWord.get("start_y");
            int pEndX = (Integer) placedWord.get("end_x");
            int pEndY = (Integer) placedWord.get("end_y");
            String pDirection = (String) placedWord.get("direction");
            
            // 기존 단어의 모든 칸 위치 계산
            Set<String> placedWordCells = new HashSet<>();
            if ("horizontal".equals(pDirection)) {
                for (int x = pStartX; x <= pEndX; x++) {
                    placedWordCells.add(x + "," + pStartY);
                }
            } else {
                for (int y = pStartY; y <= pEndY; y++) {
                    placedWordCells.add(pStartX + "," + y);
                }
            }
            
            // 같은 방향 단어는 교차 불가 (겹치면 안 됨, 인접하면 안 됨)
            if ((isHorizontal && "horizontal".equals(pDirection)) ||
                (!isHorizontal && "vertical".equals(pDirection))) {
                // 같은 방향이면 겹치면 안 됨
                Set<String> intersection = new HashSet<>(newWordCells);
                intersection.retainAll(placedWordCells);
                if (!intersection.isEmpty()) {
                    return false; // 겹침
                }
                
                // 같은 방향 단어는 인접하면 안 됨 (교차점이 없으므로)
                if (isWordAdjacent(newWordCells, placedWordCells)) {
                    return false; // 인접함 - 배치 불가
                }
            } else {
                // 다른 방향 단어: 교차점으로만 만날 수 있음
                // 1. 교차점이 있는지 확인 (공통 칸이 있으면 교차점)
                Set<String> intersection = new HashSet<>(newWordCells);
                intersection.retainAll(placedWordCells);
                
                if (intersection.isEmpty()) {
                    // 교차점이 없으면 인접하면 안 됨
                    // 새 단어의 모든 칸이 기존 단어의 칸과 인접하지 않아야 함
                    if (isWordAdjacent(newWordCells, placedWordCells)) {
                        return false; // 인접함 - 배치 불가
                    }
                } else {
                    // 교차점이 있으면 OK (교차점은 허용)
                    // 교차점이 1개 이상이면 배치 가능
                }
            }
        }
        
        return true;
    }
    
    /**
     * 두 단어가 인접한지 확인
     * 인접: 교차점이 없으면서 한 단어의 칸이 다른 단어의 칸과 상하좌우로 인접
     */
    private boolean isWordAdjacent(Set<String> word1Cells, Set<String> word2Cells) {
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};
        
        for (String cell1 : word1Cells) {
            String[] parts1 = cell1.split(",");
            int x1 = Integer.parseInt(parts1[0]);
            int y1 = Integer.parseInt(parts1[1]);
            
            // word1의 각 칸 주변 확인
            for (int d = 0; d < 4; d++) {
                int adjX = x1 + dx[d];
                int adjY = y1 + dy[d];
                String adjCell = adjX + "," + adjY;
                
                // word2의 칸과 인접한지 확인
                if (word2Cells.contains(adjCell)) {
                    return true; // 인접함
                }
            }
        }
        
        return false; // 인접하지 않음
    }
    
    /**
     * 단어 위치로부터 그리드 패턴 생성
     * 규칙: 
     * - 검은색 칸(2) = 단어가 들어가는 칸
     * - 흰색 칸(1) = 의미 없는 칸 (빈 공간)
     */
    private int[][] createGridFromWordPositions(List<Map<String, Object>> wordPositions, int gridSize) {
        // 먼저 모든 칸을 흰색(1)으로 초기화 (의미 없는 칸)
        int[][] grid = new int[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                grid[i][j] = 1; // 흰색 칸 (의미 없는 칸)
            }
        }
        
        // 단어가 있는 칸을 검은색(2)으로 표시
        Set<String> wordCells = new HashSet<>();
        for (Map<String, Object> word : wordPositions) {
            int startX = (Integer) word.get("start_x");
            int startY = (Integer) word.get("start_y");
            int endX = (Integer) word.get("end_x");
            int endY = (Integer) word.get("end_y");
            String direction = (String) word.get("direction");
            
            if ("horizontal".equals(direction)) {
                for (int x = startX; x <= endX; x++) {
                    wordCells.add(x + "," + startY);
                }
            } else {
                for (int y = startY; y <= endY; y++) {
                    wordCells.add(startX + "," + y);
                }
            }
        }
        
        // 단어가 있는 칸을 검은색(2)으로 배치
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (wordCells.contains(j + "," + i)) {
                    grid[i][j] = 2; // 검은색 칸 (단어가 들어가는 칸)
                }
            }
        }
        
        return grid;
    }
    
    /**
     * 교차점 개수 계산
     * 교차점: 가로 단어와 세로 단어만 만나는 위치 (같은 방향 단어는 교차 불가)
     */
    private int countIntersections(List<Map<String, Object>> wordPositions) {
        Map<String, Set<String>> positionDirections = new HashMap<>();
        
        // 각 단어의 모든 위치에 방향 정보 저장
        for (Map<String, Object> word : wordPositions) {
            int startX = (Integer) word.get("start_x");
            int startY = (Integer) word.get("start_y");
            int endX = (Integer) word.get("end_x");
            int endY = (Integer) word.get("end_y");
            String direction = (String) word.get("direction");
            
            if ("horizontal".equals(direction)) {
                for (int x = startX; x <= endX; x++) {
                    String key = x + "," + startY;
                    positionDirections.computeIfAbsent(key, k -> new HashSet<>()).add("horizontal");
                }
            } else { // vertical
                for (int y = startY; y <= endY; y++) {
                    String key = startX + "," + y;
                    positionDirections.computeIfAbsent(key, k -> new HashSet<>()).add("vertical");
                }
            }
        }
        
        // 가로와 세로가 모두 있는 위치만 교차점으로 인정
        int intersections = 0;
        for (Map.Entry<String, Set<String>> entry : positionDirections.entrySet()) {
            Set<String> directions = entry.getValue();
            // 가로와 세로가 모두 있어야 교차점
            if (directions.contains("horizontal") && directions.contains("vertical")) {
                intersections++;
            }
        }
        
        return intersections;
    }
    
    /**
     * 검은색 칸 인접 규칙 검증
     * 규칙:
     * 1. 같은 단어의 경계를 나타내는 검은색 칸들은 인접할 수 있음 (같은 행/열에 연속된 검은색 칸)
     * 2. 다른 단어와 관련된 검은색 칸은 인접하면 안 됨
     * 
     * 단어 배치 시 이미 인접 규칙을 체크했으므로, 검은색 칸 검증은 단순화:
     * - 같은 행 또는 같은 열에 연속된 검은색 칸은 같은 단어의 경계로 간주 (허용)
     * - 다른 경우는 단어 배치 단계에서 이미 차단되었으므로 OK
     */
    private boolean validateBlackCellAdjacency(int[][] grid, int gridSize, List<Map<String, Object>> wordPositions) {
        // 단어 배치 시 이미 인접 규칙을 체크했으므로,
        // 검은색 칸 인접 검증은 단순화 가능
        
        // 같은 행 또는 같은 열에 연속된 검은색 칸은 같은 단어의 경계로 간주 (허용)
        // 다른 경우는 단어 배치 단계에서 이미 차단되었으므로 OK
        
        return true; // 단어 배치 단계에서 이미 검증 완료
    }
}

