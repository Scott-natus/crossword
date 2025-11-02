package com.example.board.controller;

import com.example.board.service.DailyPuzzleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Map;

/**
 * 퍼즐게임 화면 라우팅 컨트롤러
 * 문서에 따른 라우팅 구조 구현
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PuzzleViewController {
    
    private final DailyPuzzleService dailyPuzzleService;
    
    /**
     * 퍼즐게임 화면들 - 모두 static/index.html 서빙
     */
    @GetMapping("/Korean/game")
    public String koreanGame() {
        return "forward:/game.html";
    }
    
    @GetMapping("/K-pop")
    public String kpop(Model model) {
        return loadThemePuzzle("K-POP", model);
    }
    
    @GetMapping("/K-movie")
    public String kmovie(Model model) {
        return loadThemePuzzle("K-MOVIE", model);
    }
    
    @GetMapping("/K-Drama")
    public String kdrama(Model model) {
        return loadThemePuzzle("K-DRAMA", model);
    }
    
    @GetMapping("/K-Culture")
    public String kculture(Model model) {
        return loadThemePuzzle("K-CULTURE", model);
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
            
            if (puzzleData == null || !Boolean.TRUE.equals(puzzleData.get("success"))) {
                log.warn("오늘 날짜의 {} 테마 퍼즐이 없거나 생성에 실패했습니다.", theme);
                model.addAttribute("theme", theme);
                model.addAttribute("error", "오늘의 퍼즐이 아직 준비되지 않았습니다.");
                return "forward:/index.html";
            }
            
            // 퍼즐 데이터를 모델에 추가
            model.addAttribute("theme", theme);
            model.addAttribute("puzzleDate", today);
            model.addAttribute("puzzleData", puzzleData);
            
            log.info("테마별 퍼즐 로드 완료: {} - 퍼즐 ID: {}", theme, puzzleData.get("puzzleId"));
            
            return "forward:/index.html";
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 로드 중 오류 발생: {} - {}", theme, e.getMessage(), e);
            model.addAttribute("theme", theme);
            model.addAttribute("error", "퍼즐을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "forward:/index.html";
        }
    }
}
