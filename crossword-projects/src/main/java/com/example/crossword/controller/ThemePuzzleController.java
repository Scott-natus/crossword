package com.example.crossword.controller;

import com.example.crossword.service.ThemePuzzleService;
import com.example.crossword.service.DailyPuzzleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 테마별 퍼즐 게임 컨트롤러
 * K-POP, K-DRAMA, K-MOVIE, K-CULTURE, Korean 테마별 퍼즐 제공
 */
@Controller
@RequestMapping("/K-CrossWord")
@RequiredArgsConstructor
@Slf4j
public class ThemePuzzleController {
    
    private final ThemePuzzleService themePuzzleService;
    private final DailyPuzzleService dailyPuzzleService;
    
    /**
     * K-POP 테마 퍼즐 게임
     */
    @GetMapping("/K-POP")
    public String kpopPuzzle(Model model) {
        return loadThemePuzzle("K-POP", model);
    }
    
    /**
     * K-DRAMA 테마 퍼즐 게임
     */
    @GetMapping("/K-DRAMA")
    public String kdramaPuzzle(Model model) {
        return loadThemePuzzle("K-DRAMA", model);
    }
    
    /**
     * K-MOVIE 테마 퍼즐 게임
     */
    @GetMapping("/K-MOVIE")
    public String kmoviePuzzle(Model model) {
        return loadThemePuzzle("K-MOVIE", model);
    }
    
    /**
     * K-CULTURE 테마 퍼즐 게임
     */
    @GetMapping("/K-CULTURE")
    public String kculturePuzzle(Model model) {
        return loadThemePuzzle("K-CULTURE", model);
    }
    
    /**
     * Korean 테마 퍼즐 게임
     */
    @GetMapping("/Korean")
    public String koreanPuzzle(Model model) {
        return loadThemePuzzle("Korean", model);
    }
    
    /**
     * 테마별 퍼즐 로드 공통 메서드
     */
    private String loadThemePuzzle(String theme, Model model) {
        try {
            log.info("테마별 퍼즐 로드 시작: {}", theme);
            
            // 오늘 날짜의 퍼즐 조회
            LocalDate today = LocalDate.now();
            Map<String, Object> puzzleData = dailyPuzzleService.getTodayPuzzle(theme, today);
            
            if (puzzleData == null) {
                log.warn("오늘 날짜의 {} 테마 퍼즐이 없습니다.", theme);
                model.addAttribute("error", "오늘의 퍼즐이 아직 준비되지 않았습니다.");
                return "theme-puzzle/error";
            }
            
            // 퍼즐 데이터를 모델에 추가
            model.addAttribute("theme", theme);
            model.addAttribute("puzzleDate", today);
            model.addAttribute("puzzleData", puzzleData);
            model.addAttribute("puzzleId", puzzleData.get("puzzleId"));
            model.addAttribute("words", puzzleData.get("words"));
            model.addAttribute("hints", puzzleData.get("hints"));
            model.addAttribute("grid", puzzleData.get("grid"));
            
            log.info("테마별 퍼즐 로드 완료: {} - 퍼즐 ID: {}", theme, puzzleData.get("puzzleId"));
            
            return "theme-puzzle/game";
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 로드 중 오류 발생: {} - {}", theme, e.getMessage());
            model.addAttribute("error", "퍼즐을 불러오는 중 오류가 발생했습니다.");
            return "theme-puzzle/error";
        }
    }
    
    /**
     * 퍼즐 완료 처리
     */
    @PostMapping("/{theme}/complete")
    @ResponseBody
    public Map<String, Object> completePuzzle(
            @PathVariable String theme,
            @RequestParam Integer completionTime,
            @RequestParam Integer hintsUsed,
            @RequestParam Integer wrongAttempts) {
        
        try {
            log.info("퍼즐 완료 처리: {} - 시간: {}초, 힌트: {}개, 오답: {}회", 
                    theme, completionTime, hintsUsed, wrongAttempts);
            
            // 완료 기록 저장
            Map<String, Object> result = themePuzzleService.saveCompletion(
                theme, completionTime, hintsUsed, wrongAttempts);
            
            return result;
            
        } catch (Exception e) {
            log.error("퍼즐 완료 처리 중 오류 발생: {} - {}", theme, e.getMessage());
            return Map.of(
                "success", false,
                "message", "완료 처리 중 오류가 발생했습니다."
            );
        }
    }
    
    /**
     * 테마별 랭킹 조회
     */
    @GetMapping("/{theme}/ranking")
    @ResponseBody
    public Map<String, Object> getThemeRanking(@PathVariable String theme) {
        try {
            log.info("테마별 랭킹 조회: {}", theme);
            
            Map<String, Object> ranking = themePuzzleService.getThemeRanking(theme);
            return ranking;
            
        } catch (Exception e) {
            log.error("테마별 랭킹 조회 중 오류 발생: {} - {}", theme, e.getMessage());
            return Map.of(
                "success", false,
                "message", "랭킹 조회 중 오류가 발생했습니다."
            );
        }
    }
    
    /**
     * 다른 테마 목록 조회
     */
    @GetMapping("/themes")
    @ResponseBody
    public Map<String, Object> getAvailableThemes() {
        try {
            log.info("사용 가능한 테마 목록 조회");
            
            Map<String, Object> themes = themePuzzleService.getAvailableThemes();
            return themes;
            
        } catch (Exception e) {
            log.error("테마 목록 조회 중 오류 발생: {}", e.getMessage());
            return Map.of(
                "success", false,
                "message", "테마 목록 조회 중 오류가 발생했습니다."
            );
        }
    }
}
