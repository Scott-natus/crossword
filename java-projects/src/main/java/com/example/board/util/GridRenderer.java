package com.example.board.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 그리드 렌더링 유틸리티 클래스
 * gridPattern과 wordPositions를 기반으로 HTML 그리드를 생성
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-01-27
 */
@Component
@Slf4j
public class GridRenderer {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 그리드 패턴과 단어 위치 정보를 기반으로 HTML 그리드 생성
     * 퍼즐게임과 동일한 방식으로 렌더링
     * 
     * @param gridPattern 2차원 배열 (1: 흰색, 2: 검은색, 0: 빈칸)
     * @param wordPositions JSON 배열 (단어 위치, 방향, 길이 정보)
     * @param cellSize 셀 크기 (픽셀)
     * @param showNumbers 단어 번호 표시 여부
     * @return HTML 그리드 문자열
     */
    public String renderGrid(String gridPattern, String wordPositions, int cellSize, boolean showNumbers) {
        try {
            // JSON 파싱
            List<List<Integer>> grid = parseGridPattern(gridPattern);
            List<Map<String, Object>> words = parseWordPositions(wordPositions);
            
            if (grid == null || grid.isEmpty()) {
                return "<div class='text-muted'>그리드 데이터가 없습니다.</div>";
            }
            
            int gridSize = grid.size();
            StringBuilder html = new StringBuilder();
            
            // 퍼즐게임과 동일한 그리드 컨테이너 구조
            html.append("<div class='grid-container' style='")
                .append("display: inline-block; ")
                .append("border: 2px solid #333; ")
                .append("max-width: 90vw; ")
                .append("max-height: 90vh; ")
                .append("margin: 0 auto;")
                .append("'>");
            
            // 그리드 행 렌더링 (퍼즐게임과 동일한 구조)
            for (int row = 0; row < gridSize; row++) {
                html.append("<div class='grid-row' style='display: flex;'>");
                
                for (int col = 0; col < gridSize; col++) {
                    int cellValue = grid.get(row).get(col);
                    String cellClass = getPuzzleCellClass(cellValue);
                    String cellContent = "";
                    
                    // 단어 번호 표시 (퍼즐게임과 동일한 방식)
                    if (showNumbers && cellValue == 2) {
                        String wordNumber = getWordNumberAtPosition(row, col, words);
                        if (!wordNumber.isEmpty()) {
                            cellContent = createPuzzleWordNumberBadge(wordNumber, cellSize);
                        }
                    }
                    
                    html.append("<div class='puzzle-cell ")
                        .append(cellClass)
                        .append("' style='")
                        .append("width: ").append(cellSize).append("px; ")
                        .append("height: ").append(cellSize).append("px; ")
                        .append("border: 1px solid #ccc; ")
                        .append("position: relative; ")
                        .append("display: flex; ")
                        .append("align-items: center; ")
                        .append("justify-content: center; ")
                        .append("font-size: ").append(Math.max(12, cellSize * 0.4)).append("px; ")
                        .append("font-weight: bold; ")
                        .append("cursor: pointer; ")
                        .append("'>")
                        .append(cellContent)
                        .append("</div>");
                }
                
                html.append("</div>");
            }
            
            html.append("</div>");
            return html.toString();
            
        } catch (Exception e) {
            log.error("그리드 렌더링 오류", e);
            return "<div class='text-danger'>그리드 렌더링 중 오류가 발생했습니다: " + e.getMessage() + "</div>";
        }
    }
    
    /**
     * 그리드 패턴 JSON 파싱
     */
    private List<List<Integer>> parseGridPattern(String gridPattern) {
        try {
            if (gridPattern == null || gridPattern.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(gridPattern, new TypeReference<List<List<Integer>>>() {});
        } catch (Exception e) {
            log.error("그리드 패턴 파싱 오류", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 단어 위치 정보 JSON 파싱
     */
    private List<Map<String, Object>> parseWordPositions(String wordPositions) {
        try {
            if (wordPositions == null || wordPositions.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(wordPositions, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("단어 위치 정보 파싱 오류", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 셀 값에 따른 CSS 클래스 반환 (퍼즐게임과 동일)
     */
    private String getPuzzleCellClass(int cellValue) {
        switch (cellValue) {
            case 2: return "bg-dark text-white"; // 검은색칸 (단어가 있는 칸)
            case 0: return "bg-light"; // 빈칸
            default: return "bg-white"; // 흰색칸
        }
    }
    
    /**
     * 셀 값에 따른 CSS 클래스 반환 (기존 방식)
     */
    private String getCellClass(int cellValue) {
        switch (cellValue) {
            case 2: return "black";
            case 0: return "empty";
            default: return "white";
        }
    }
    
    /**
     * 셀 값에 따른 배경색 반환
     */
    private String getCellBackgroundColor(int cellValue) {
        switch (cellValue) {
            case 2: return "#2c3e50"; // 검은색
            case 0: return "#ecf0f1"; // 빈칸
            default: return "#ffffff"; // 흰색
        }
    }
    
    /**
     * 특정 위치의 단어 번호 반환
     */
    private String getWordNumberAtPosition(int row, int col, List<Map<String, Object>> words) {
        for (Map<String, Object> word : words) {
            try {
                // 단어 추출 결과 구조: word_order 배열의 각 항목
                Object wordId = word.get("word_id");
                Map<String, Object> position = (Map<String, Object>) word.get("position");
                
                if (position != null && wordId != null) {
                    Integer startY = (Integer) position.get("start_y");
                    Integer startX = (Integer) position.get("start_x");
                    
                    if (startY != null && startX != null && 
                        startY.equals(row) && startX.equals(col)) {
                        return wordId.toString();
                    }
                }
                
                // 기존 구조도 지원 (템플릿 상세보기 등)
                Integer startY = (Integer) word.get("start_y");
                Integer startX = (Integer) word.get("start_x");
                Object id = word.get("id");
                
                if (startY != null && startX != null && id != null && 
                    startY.equals(row) && startX.equals(col)) {
                    return id.toString();
                }
            } catch (Exception e) {
                log.warn("단어 정보 파싱 오류", e);
            }
        }
        return "";
    }
    
    /**
     * 단어 번호 배지 HTML 생성 (퍼즐게임과 동일)
     */
    private String createPuzzleWordNumberBadge(String wordNumber, int cellSize) {
        int badgeSize = Math.max(12, Math.min(16, (int)(cellSize * 0.4)));
        int fontSize = Math.max(8, Math.min(10, (int)(cellSize * 0.25)));
        
        return "<span class='word-number' style='"
            + "position: absolute; "
            + "top: 2px; "
            + "left: 2px; "
            + "z-index: 5; "
            + "opacity: 0.8; "
            + "width: " + badgeSize + "px; "
            + "height: " + badgeSize + "px; "
            + "aspect-ratio: 1; "
            + "font-size: " + fontSize + "px; "
            + "display: flex; "
            + "align-items: center; "
            + "justify-content: center; "
            + "background: #e74c3c; "
            + "border-radius: 50%; "
            + "color: white; "
            + "font-weight: bold; "
            + "box-shadow: 0 1px 2px rgba(0,0,0,0.2);"
            + "'>" + wordNumber + "</span>";
    }
    
    /**
     * 단어 번호 배지 HTML 생성 (기존 방식)
     */
    private String createWordNumberBadge(String wordNumber, int cellSize) {
        int badgeSize = Math.max(10, Math.min(14, (int)(cellSize * 0.35)));
        int fontSize = Math.max(7, Math.min(9, (int)(cellSize * 0.2)));
        
        return "<span class='word-number' style='"
            + "position: absolute; "
            + "top: 2px; "
            + "left: 2px; "
            + "width: " + badgeSize + "px; "
            + "height: " + badgeSize + "px; "
            + "font-size: " + fontSize + "px; "
            + "background: #e74c3c; "
            + "border-radius: 50%; "
            + "display: flex; "
            + "align-items: center; "
            + "justify-content: center; "
            + "color: white; "
            + "font-weight: bold; "
            + "box-shadow: 0 1px 2px rgba(0,0,0,0.2); "
            + "z-index: 10;"
            + "'>" + wordNumber + "</span>";
    }
    
    /**
     * 그리드 크기 계산 (정사각형)
     */
    public int calculateGridSize(String gridPattern) {
        try {
            List<List<Integer>> grid = parseGridPattern(gridPattern);
            return grid != null ? grid.size() : 0;
        } catch (Exception e) {
            log.error("그리드 크기 계산 오류", e);
            return 0;
        }
    }
    
    /**
     * 기본 설정으로 그리드 렌더링 (20px 셀, 번호 표시)
     */
    public String renderGridDefault(String gridPattern, String wordPositions) {
        return renderGrid(gridPattern, wordPositions, 20, true);
    }
    
    /**
     * 큰 셀로 그리드 렌더링 (25px 셀, 번호 표시)
     */
    public String renderGridLarge(String gridPattern, String wordPositions) {
        return renderGrid(gridPattern, wordPositions, 25, true);
    }
    
    /**
     * 번호 없이 그리드 렌더링 (20px 셀, 번호 미표시)
     */
    public String renderGridNoNumbers(String gridPattern, String wordPositions) {
        return renderGrid(gridPattern, wordPositions, 20, false);
    }
}
