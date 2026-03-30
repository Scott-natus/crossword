package com.example.board.service;

import com.example.board.entity.PuzzleLevel;
import com.example.board.entity.Word;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 크로스워드 퍼즐 생성 서비스
 * 퍼즐 레벨에 따른 단어 선택 및 크로스워드 그리드 생성 알고리즘을 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CrosswordGeneratorService {
    
    private final WordService wordService;
    private final PuzzleLevelService puzzleLevelService;
    private final HintService hintService;
    
    /**
     * 퍼즐 레벨에 따른 크로스워드 퍼즐 생성
     */
    public CrosswordPuzzle generatePuzzle(Integer level) {
        // 퍼즐 생성 시작
        
        try {
            // 실제 퍼즐 레벨 정보 조회
            Optional<PuzzleLevel> puzzleLevelOpt = puzzleLevelService.getPuzzleLevelByLevel(level);
            if (puzzleLevelOpt.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 퍼즐 레벨입니다: " + level);
            }
            
            PuzzleLevel puzzleLevel = puzzleLevelOpt.get();
            // 레벨 정보 확인
            
            // 실제 데이터베이스에서 단어 선택
            List<Word> selectedWords = selectWordsForPuzzle(puzzleLevel);
            if (selectedWords.isEmpty()) {
                log.warn("퍼즐 레벨 {}에 적합한 단어를 찾을 수 없습니다", level);
                throw new RuntimeException("적합한 단어를 찾을 수 없습니다: 레벨 " + level);
            }
            
            // 단어 선택 완료
            
            // 크로스워드 그리드 생성
            CrosswordGrid grid = createCrosswordGrid(selectedWords, puzzleLevel);
            if (grid == null) {
                log.warn("퍼즐 레벨 {}의 그리드 생성에 실패했습니다", level);
                throw new RuntimeException("그리드 생성에 실패했습니다: 레벨 " + level);
            }
            
            log.info("퍼즐 생성 성공 - 레벨: {}, 단어: {}개", level, selectedWords.size());
            return new CrosswordPuzzle(puzzleLevel, selectedWords, grid);
            
        } catch (Exception e) {
            log.error("퍼즐 생성 중 오류 발생: 레벨 {}, 오류: {}", level, e.getMessage());
            throw new RuntimeException("퍼즐 생성에 실패했습니다: 레벨 " + level);
        }
    }
    
    /**
     * 더미 단어 생성 (테스트용)
     */
    private List<Word> createDummyWords(Integer level) {
        List<Word> words = new ArrayList<>();
        
        // 레벨에 따른 단어 개수 조정
        int wordCount = Math.min(3 + level, 8);
        
        for (int i = 0; i < wordCount; i++) {
            Word word = Word.builder()
                .id(i + 1)
                .word("단어" + (i + 1))
                .length(3 + (i % 3))
                .category("테스트")
                .difficulty(1)
                .isActive(true)
                .build();
            words.add(word);
        }
        
        return words;
    }
    
    /**
     * 크로스워드 그리드 생성
     */
    private CrosswordGrid createCrosswordGrid(List<Word> words, PuzzleLevel puzzleLevel) {
        log.debug("크로스워드 그리드 생성 시작: 단어 수 {}", words.size());
        
        int gridSize = 15; // 기본 그리드 크기
        CrosswordGrid grid = new CrosswordGrid(gridSize, gridSize);
        
        // 단어들을 길이 순으로 정렬 (긴 단어부터 배치)
        List<Word> sortedWords = new ArrayList<>(words);
        sortedWords.sort((a, b) -> Integer.compare(b.getLength(), a.getLength()));
        
        List<PlacedWord> placedWords = new ArrayList<>();
        
        // 첫 번째 단어를 중앙에 배치
        if (!sortedWords.isEmpty()) {
            Word firstWord = sortedWords.get(0);
            int centerRow = gridSize / 2;
            int centerCol = (gridSize - firstWord.getLength()) / 2;
            
            if (grid.placeWord(firstWord, centerRow, centerCol, true)) {
                placedWords.add(new PlacedWord(firstWord, centerRow, centerCol, true));
                log.debug("첫 번째 단어 배치 성공: {} at ({}, {})", firstWord.getWord(), centerRow, centerCol);
            }
        }
        
        // 나머지 단어들을 교차점을 찾아 배치
        for (int i = 1; i < sortedWords.size(); i++) {
            Word word = sortedWords.get(i);
            boolean placed = false;
            
            // 기존 배치된 단어들과 교차점을 찾아 배치 시도
            for (PlacedWord placedWord : placedWords) {
                if (tryPlaceWordAtIntersection(grid, word, placedWord, placedWords)) {
                    placed = true;
                    break;
                }
            }
            
            // 교차점을 찾지 못한 경우, 빈 공간에 배치
            if (!placed) {
                placed = tryPlaceWordInEmptySpace(grid, word, placedWords);
            }
            
            if (placed) {
                log.debug("단어 배치 성공: {}", word.getWord());
            } else {
                log.warn("단어 배치 실패: {}", word.getWord());
            }
        }
        
        log.debug("그리드 생성 완료: 배치된 단어 수 {}", placedWords.size());
        return grid;
    }
    
    /**
     * 교차점에서 단어 배치 시도
     */
    private boolean tryPlaceWordAtIntersection(CrosswordGrid grid, Word word, PlacedWord placedWord, List<PlacedWord> allPlacedWords) {
        String wordText = word.getWord().toLowerCase();
        String placedWordText = placedWord.word.getWord().toLowerCase();
        
        // 공통 문자 찾기
        for (int i = 0; i < wordText.length(); i++) {
            for (int j = 0; j < placedWordText.length(); j++) {
                if (wordText.charAt(i) == placedWordText.charAt(j)) {
                    // 교차점 계산
                    int crossRow, crossCol;
                    boolean isHorizontal;
                    
                    if (placedWord.isHorizontal) {
                        crossRow = placedWord.row;
                        crossCol = placedWord.col + j;
                        isHorizontal = false; // 세로로 배치
                    } else {
                        crossRow = placedWord.row + j;
                        crossCol = placedWord.col;
                        isHorizontal = true; // 가로로 배치
                    }
                    
                    // 단어 배치 위치 계산
                    int wordRow = crossRow - (isHorizontal ? 0 : i);
                    int wordCol = crossCol - (isHorizontal ? i : 0);
                    
                    // 배치 가능 여부 확인
                    if (canPlaceWordAt(grid, word, wordRow, wordCol, isHorizontal, allPlacedWords)) {
                        if (grid.placeWord(word, wordRow, wordCol, isHorizontal)) {
                            allPlacedWords.add(new PlacedWord(word, wordRow, wordCol, isHorizontal));
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 빈 공간에 단어 배치 시도
     */
    private boolean tryPlaceWordInEmptySpace(CrosswordGrid grid, Word word, List<PlacedWord> allPlacedWords) {
        int gridSize = 15;
        
        // 랜덤한 위치에서 배치 시도
        for (int attempt = 0; attempt < 50; attempt++) {
            int row = (int) (Math.random() * gridSize);
            int col = (int) (Math.random() * gridSize);
            boolean isHorizontal = Math.random() < 0.5;
            
            if (canPlaceWordAt(grid, word, row, col, isHorizontal, allPlacedWords)) {
                if (grid.placeWord(word, row, col, isHorizontal)) {
                    allPlacedWords.add(new PlacedWord(word, row, col, isHorizontal));
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 단어 배치 가능 여부 확인
     */
    private boolean canPlaceWordAt(CrosswordGrid grid, Word word, int row, int col, boolean isHorizontal, List<PlacedWord> allPlacedWords) {
        String wordText = word.getWord().toLowerCase();
        int gridSize = 15;
        
        // 그리드 범위 확인
        if (isHorizontal) {
            if (col + wordText.length() > gridSize) return false;
        } else {
            if (row + wordText.length() > gridSize) return false;
        }
        
        // 기존 단어와의 충돌 확인
        for (PlacedWord placedWord : allPlacedWords) {
            if (isWordColliding(word, row, col, isHorizontal, placedWord)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 단어 충돌 확인
     */
    private boolean isWordColliding(Word word, int row, int col, boolean isHorizontal, PlacedWord placedWord) {
        String wordText = word.getWord().toLowerCase();
        String placedWordText = placedWord.word.getWord().toLowerCase();
        
        for (int i = 0; i < wordText.length(); i++) {
            int wordRow = isHorizontal ? row : row + i;
            int wordCol = isHorizontal ? col + i : col;
            
            for (int j = 0; j < placedWordText.length(); j++) {
                int placedRow = placedWord.isHorizontal ? placedWord.row : placedWord.row + j;
                int placedCol = placedWord.isHorizontal ? placedWord.col + j : placedWord.col;
                
                if (wordRow == placedRow && wordCol == placedCol) {
                    // 같은 위치에 다른 문자가 있으면 충돌
                    if (wordText.charAt(i) != placedWordText.charAt(j)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 더미 그리드 생성 (테스트용)
     */
    private CrosswordGrid createDummyGrid(List<Word> words) {
        int gridSize = 10;
        CrosswordGrid grid = new CrosswordGrid(gridSize, gridSize);
        
        // 단어 배치 (간단한 가로 배치)
        for (int i = 0; i < words.size() && i < 5; i++) {
            int row = i * 2 + 1;
            int col = 1;
            
            // 단어가 그리드 범위를 벗어나지 않도록 조정
            if (col + words.get(i).getLength() <= gridSize) {
                grid.placeWord(words.get(i), row, col, true);
            }
        }
        
        return grid;
    }
    
    /**
     * 퍼즐 레벨에 맞는 단어들을 선택 (최적화된 쿼리 사용)
     */
    private List<Word> selectWordsForPuzzle(PuzzleLevel puzzleLevel) {
        log.debug("퍼즐 레벨에 맞는 단어 선택: {}", puzzleLevel.getLevelName());
        
        List<Word> selectedWords = new ArrayList<>();
        int targetWordCount = puzzleLevel.getWordCount();
        int minIntersectionCount = puzzleLevel.getIntersectionCount();
        
        // 최적화된 쿼리를 사용하여 단어 선택
        // 1단계: 난이도에 맞는 단어들을 최적화된 쿼리로 가져오기
        List<Word> candidateWords = wordService.getWordsByDifficultyRange(
            puzzleLevel.getWordDifficulty(), 
            puzzleLevel.getWordDifficulty()
        );
        
        if (candidateWords.isEmpty()) {
            log.warn("난이도 {}에 해당하는 단어가 없습니다", puzzleLevel.getWordDifficulty());
            return selectedWords;
        }
        
        // 2단계: 길이별로 단어 분류
        Map<Integer, List<Word>> wordsByLength = candidateWords.stream()
            .collect(Collectors.groupingBy(Word::getLength));
        
        // 3단계: 다양한 길이의 단어 선택 (최적화된 랜덤 쿼리 사용)
        List<Integer> lengths = new ArrayList<>(wordsByLength.keySet());
        Collections.shuffle(lengths);
        
        for (Integer length : lengths) {
            if (selectedWords.size() >= targetWordCount) break;
            
            // 최적화된 랜덤 쿼리 사용: 특정 길이의 랜덤 단어 조회
            List<Word> wordsOfLength = wordService.getRandomWordsByLength(length, 2);
            
            if (wordsOfLength != null && !wordsOfLength.isEmpty()) {
                // 랜덤하게 1-2개 선택
                int selectCount = Math.min(2, Math.min(wordsOfLength.size(), targetWordCount - selectedWords.size()));
                
                for (int i = 0; i < selectCount; i++) {
                    selectedWords.add(wordsOfLength.get(i));
                }
            }
        }
        
        // 4단계: 교차점 조건 확인
        if (selectedWords.size() >= 2) {
            selectedWords = filterWordsByIntersection(selectedWords, minIntersectionCount);
        }
        
        log.debug("선택된 단어 수: {} (목표: {})", selectedWords.size(), targetWordCount);
        return selectedWords;
    }
    
    /**
     * 교차점 조건을 만족하는 단어들로 필터링
     */
    private List<Word> filterWordsByIntersection(List<Word> words, int minIntersectionCount) {
        log.debug("교차점 조건으로 단어 필터링: 최소 {}개", minIntersectionCount);
        
        List<Word> filteredWords = new ArrayList<>();
        Set<String> usedWords = new HashSet<>();
        
        // 첫 번째 단어는 무조건 포함
        if (!words.isEmpty()) {
            Word firstWord = words.get(0);
            filteredWords.add(firstWord);
            usedWords.add(firstWord.getWord());
        }
        
        // 나머지 단어들은 교차점 조건 확인
        for (int i = 1; i < words.size(); i++) {
            Word candidateWord = words.get(i);
            if (usedWords.contains(candidateWord.getWord())) {
                continue;
            }
            
            // 교차점 가능성 확인
            boolean canIntersect = false;
            for (Word existingWord : filteredWords) {
                if (canWordsIntersect(existingWord, candidateWord)) {
                    canIntersect = true;
                    break;
                }
            }
            
            if (canIntersect) {
                filteredWords.add(candidateWord);
                usedWords.add(candidateWord.getWord());
            }
        }
        
        log.debug("필터링된 단어 수: {}", filteredWords.size());
        return filteredWords;
    }
    
    /**
     * 두 단어가 교차할 수 있는지 확인
     */
    private boolean canWordsIntersect(Word word1, Word word2) {
        String w1 = word1.getWord().toLowerCase();
        String w2 = word2.getWord().toLowerCase();
        
        // 공통 문자 찾기
        for (int i = 0; i < w1.length(); i++) {
            for (int j = 0; j < w2.length(); j++) {
                if (w1.charAt(i) == w2.charAt(j)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 크로스워드 그리드 생성
     */
    private CrosswordGrid generateCrosswordGrid(List<Word> words, PuzzleLevel puzzleLevel) {
        log.debug("크로스워드 그리드 생성: {}개 단어", words.size());
        
        if (words.isEmpty()) {
            return null;
        }
        
        // 그리드 크기 계산 (단어 수에 따라 동적 조정)
        int gridSize = calculateGridSize(words.size());
        CrosswordGrid grid = new CrosswordGrid(gridSize, gridSize);
        
        // 첫 번째 단어를 중앙에 배치
        Word firstWord = words.get(0);
        int startRow = gridSize / 2;
        int startCol = (gridSize - firstWord.getLength()) / 2;
        
        if (!grid.placeWord(firstWord, startRow, startCol, true)) {
            log.warn("첫 번째 단어 배치 실패: {}", firstWord.getWord());
            return null;
        }
        
        // 나머지 단어들을 교차점을 찾아 배치
        List<Word> placedWords = new ArrayList<>();
        placedWords.add(firstWord);
        
        for (int i = 1; i < words.size(); i++) {
            Word word = words.get(i);
            boolean placed = false;
            
            // 최대 10번 시도
            for (int attempt = 0; attempt < 10; attempt++) {
                if (tryPlaceWord(grid, word, placedWords)) {
                    placedWords.add(word);
                    placed = true;
                    break;
                }
            }
            
            if (!placed) {
                log.warn("단어 배치 실패: {}", word.getWord());
            }
        }
        
        // 교차점 개수 확인
        int intersectionCount = grid.getIntersectionCount();
        if (intersectionCount < puzzleLevel.getIntersectionCount()) {
            log.warn("교차점 개수 부족: {} (필요: {})", intersectionCount, puzzleLevel.getIntersectionCount());
            return null;
        }
        
        log.debug("그리드 생성 완료: {}개 단어 배치, {}개 교차점", placedWords.size(), intersectionCount);
        return grid;
    }
    
    /**
     * 그리드 크기 계산
     */
    private int calculateGridSize(int wordCount) {
        // 단어 수에 따라 그리드 크기 조정
        if (wordCount <= 3) return 10;
        if (wordCount <= 5) return 12;
        if (wordCount <= 7) return 15;
        return 20;
    }
    
    /**
     * 단어 배치 시도
     */
    private boolean tryPlaceWord(CrosswordGrid grid, Word word, List<Word> placedWords) {
        String wordText = word.getWord().toLowerCase();
        
        // 랜덤하게 가로/세로 방향 선택
        boolean isHorizontal = Math.random() < 0.5;
        
        // 기존 단어들과의 교차점 찾기
        for (Word placedWord : placedWords) {
            String placedText = placedWord.getWord().toLowerCase();
            
            // 공통 문자 찾기
            for (int i = 0; i < wordText.length(); i++) {
                for (int j = 0; j < placedText.length(); j++) {
                    if (wordText.charAt(i) == placedText.charAt(j)) {
                        // 교차점 위치 계산
                        if (tryPlaceAtIntersection(grid, word, placedWord, i, j, isHorizontal)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 교차점에 단어 배치 시도
     */
    private boolean tryPlaceAtIntersection(CrosswordGrid grid, Word word, Word placedWord, 
                                         int wordIndex, int placedIndex, boolean isHorizontal) {
        // 교차점 위치 계산 (간단한 구현)
        int[] intersectionPos = grid.findIntersectionPosition(placedWord, placedIndex);
        if (intersectionPos == null) {
            return false;
        }
        
        int row = intersectionPos[0];
        int col = intersectionPos[1];
        
        // 단어 배치 위치 계산
        if (isHorizontal) {
            col -= wordIndex;
        } else {
            row -= wordIndex;
        }
        
        // 그리드 범위 확인
        if (row < 0 || col < 0 || 
            (isHorizontal && col + word.getLength() > grid.getWidth()) ||
            (!isHorizontal && row + word.getLength() > grid.getHeight())) {
            return false;
        }
        
        return grid.placeWord(word, row, col, isHorizontal);
    }
    
    /**
     * 퍼즐 완성도 검증
     */
    private boolean validatePuzzle(CrosswordGrid grid, PuzzleLevel puzzleLevel) {
        log.debug("퍼즐 완성도 검증");
        
        // 1. 교차점 개수 확인
        int intersectionCount = grid.getIntersectionCount();
        if (intersectionCount < puzzleLevel.getIntersectionCount()) {
            log.warn("교차점 개수 부족: {} (필요: {})", intersectionCount, puzzleLevel.getIntersectionCount());
            return false;
        }
        
        // 2. 단어 개수 확인
        int wordCount = grid.getWordCount();
        if (wordCount < puzzleLevel.getWordCount()) {
            log.warn("단어 개수 부족: {} (필요: {})", wordCount, puzzleLevel.getWordCount());
            return false;
        }
        
        // 3. 그리드 밀도 확인 (너무 비어있지 않은지)
        double density = grid.getDensity();
        if (density < 0.1) { // 최소 10% 밀도
            log.warn("그리드 밀도 부족: {}", density);
            return false;
        }
        
        log.debug("퍼즐 검증 통과: 교차점 {}, 단어 {}, 밀도 {}", intersectionCount, wordCount, density);
        return true;
    }
    
    /**
     * 크로스워드 퍼즐 데이터 클래스
     */
    public static class CrosswordPuzzle {
        private final PuzzleLevel puzzleLevel;
        private final List<Word> words;
        private final CrosswordGrid grid;
        
        public CrosswordPuzzle(PuzzleLevel puzzleLevel, List<Word> words, CrosswordGrid grid) {
            this.puzzleLevel = puzzleLevel;
            this.words = words;
            this.grid = grid;
        }
        
        public PuzzleLevel getPuzzleLevel() { return puzzleLevel; }
        public List<Word> getWords() { return words; }
        public CrosswordGrid getGrid() { return grid; }
    }
    
    /**
     * 크로스워드 그리드 데이터 클래스
     */
    public static class CrosswordGrid {
        private final char[][] grid;
        private final int width;
        private final int height;
        private final List<PlacedWord> placedWords;
        
        public CrosswordGrid(int width, int height) {
            this.width = width;
            this.height = height;
            this.grid = new char[height][width];
            this.placedWords = new ArrayList<>();
            
            // 그리드 초기화
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    grid[i][j] = ' ';
                }
            }
        }
        
        public boolean placeWord(Word word, int row, int col, boolean isHorizontal) {
            String wordText = word.getWord().toLowerCase();
            
            // 배치 가능 여부 확인
            if (!canPlaceWord(wordText, row, col, isHorizontal)) {
                return false;
            }
            
            // 단어 배치
            for (int i = 0; i < wordText.length(); i++) {
                if (isHorizontal) {
                    grid[row][col + i] = wordText.charAt(i);
                } else {
                    grid[row + i][col] = wordText.charAt(i);
                }
            }
            
            // 배치된 단어 정보 저장
            placedWords.add(new PlacedWord(word, row, col, isHorizontal));
            return true;
        }
        
        private boolean canPlaceWord(String word, int row, int col, boolean isHorizontal) {
            for (int i = 0; i < word.length(); i++) {
                int r = isHorizontal ? row : row + i;
                int c = isHorizontal ? col + i : col;
                
                if (r < 0 || r >= height || c < 0 || c >= width) {
                    return false;
                }
                
                char existing = grid[r][c];
                if (existing != ' ' && existing != word.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        
        public int[] findIntersectionPosition(Word word, int index) {
            // 간단한 구현: 첫 번째 배치된 단어의 중앙 위치 반환
            if (!placedWords.isEmpty()) {
                PlacedWord firstWord = placedWords.get(0);
                return new int[]{firstWord.row + index, firstWord.col + index};
            }
            return null;
        }
        
        public int getIntersectionCount() {
            int count = 0;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (grid[i][j] != ' ') {
                        // 주변에 다른 문자가 있는지 확인
                        if (hasAdjacentCharacters(i, j)) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }
        
        private boolean hasAdjacentCharacters(int row, int col) {
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < height && newCol >= 0 && newCol < width) {
                    if (grid[newRow][newCol] != ' ') {
                        return true;
                    }
                }
            }
            return false;
        }
        
        public int getWordCount() {
            return placedWords.size();
        }
        
        public double getDensity() {
            int filledCells = 0;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (grid[i][j] != ' ') {
                        filledCells++;
                    }
                }
            }
            return (double) filledCells / (width * height);
        }
        
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public char[][] getGrid() { return grid; }
        public List<PlacedWord> getPlacedWords() { return placedWords; }
    }
    
    /**
     * 배치된 단어 정보 클래스
     */
    public static class PlacedWord {
        public final Word word;
        public final int row;
        public final int col;
        public final boolean isHorizontal;
        
        public PlacedWord(Word word, int row, int col, boolean isHorizontal) {
            this.word = word;
            this.row = row;
            this.col = col;
            this.isHorizontal = isHorizontal;
        }
    }
}
