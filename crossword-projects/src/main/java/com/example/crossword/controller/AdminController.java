package com.example.crossword.controller;

import com.example.crossword.service.PzWordService;
import com.example.crossword.service.PzHintService;
import com.example.crossword.service.PuzzleLevelService;
import com.example.crossword.service.PuzzleGridTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 퍼즐 관리 시스템 컨트롤러
 * 라라벨의 퍼즐 관리 시스템을 Spring Boot로 포팅
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final PzWordService pzWordService;
    private final PzHintService pzHintService;
    private final PuzzleLevelService puzzleLevelService;
    private final PuzzleGridTemplateService templateService;

    /**
     * 관리자 메인 페이지
     */
    @GetMapping("/")
    public String adminMain(Model model) {
        // 통계 데이터 조회
        Map<String, Object> stats = getAdminStats();
        model.addAttribute("stats", stats);
        return "admin/index";
    }

    /**
     * 단어 관리 페이지
     */
    @GetMapping("/words")
    public String wordManagement(Model model) {
        // 통계 데이터 조회
        Map<String, Object> stats = getWordStats();
        model.addAttribute("stats", stats);
        return "admin/words/index";
    }

    /**
     * AI 힌트 생성 페이지
     */
    @GetMapping("/hint-generator")
    public String hintGenerator(Model model) {
        return "admin/hint-generator/index";
    }

    /**
     * 레벨 관리 페이지
     */
    @GetMapping("/levels")
    public String levelManagement(Model model) {
        return "admin/levels/index";
    }

    /**
     * 그리드 템플릿 관리 페이지
     */
    @GetMapping("/grid-templates")
    public String gridTemplateManagement(Model model) {
        return "admin/grid-templates/index";
    }

    /**
     * 관리자 통계 데이터 조회
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 전체 단어 수
            long totalWords = pzWordService.getTotalWordCount();
            stats.put("totalWords", totalWords);
            
            // 활성 단어 수
            long activeWords = pzWordService.getActiveWordCount();
            stats.put("activeWords", activeWords);
            
            // 힌트 보유 단어 수
            long wordsWithHints = pzWordService.getWordsWithHintsCount();
            stats.put("wordsWithHints", wordsWithHints);
            
            // 전체 힌트 수
            long totalHints = pzHintService.getTotalHintCount();
            stats.put("totalHints", totalHints);
            
            // 레벨 수
            long totalLevels = puzzleLevelService.getTotalLevelCount();
            stats.put("totalLevels", totalLevels);
            
            // 그리드 템플릿 수
            long totalTemplates = templateService.getTotalTemplateCount();
            stats.put("totalTemplates", totalTemplates);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("관리자 통계 조회 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 단어 통계 데이터 조회
     */
    private Map<String, Object> getWordStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("totalWords", pzWordService.getTotalWordCount());
            stats.put("activeWords", pzWordService.getActiveWordCount());
            stats.put("wordsWithHints", pzWordService.getWordsWithHintsCount());
            stats.put("totalHints", pzHintService.getTotalHintCount());
        } catch (Exception e) {
            log.error("단어 통계 조회 오류: {}", e.getMessage());
            stats.put("totalWords", 0);
            stats.put("activeWords", 0);
            stats.put("wordsWithHints", 0);
            stats.put("totalHints", 0);
        }
        
        return stats;
    }

    /**
     * 관리자 통계 데이터 조회 (내부용)
     */
    private Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("totalWords", pzWordService.getTotalWordCount());
            stats.put("activeWords", pzWordService.getActiveWordCount());
            stats.put("wordsWithHints", pzWordService.getWordsWithHintsCount());
            stats.put("totalHints", pzHintService.getTotalHintCount());
            stats.put("totalLevels", puzzleLevelService.getTotalLevelCount());
            stats.put("totalTemplates", templateService.getTotalTemplateCount());
        } catch (Exception e) {
            log.error("관리자 통계 조회 오류: {}", e.getMessage());
            // 기본값 설정
            stats.put("totalWords", 0);
            stats.put("activeWords", 0);
            stats.put("wordsWithHints", 0);
            stats.put("totalHints", 0);
            stats.put("totalLevels", 0);
            stats.put("totalTemplates", 0);
        }
        
        return stats;
    }
}
