package com.example.board.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 퍼즐게임 화면 라우팅 컨트롤러
 * 문서에 따른 라우팅 구조 구현
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PuzzleViewController {
    
    // 향후 퍼즐 데이터 사전 로드가 필요할 경우를 위해 유지
    // private final DailyPuzzleService dailyPuzzleService;
    
    /**
     * 퍼즐게임 화면들 - 모두 static/index.html 서빙
     */
    @GetMapping("/Korean/game")
    public String koreanGame() {
        return "forward:/game.html";
    }
    
    @GetMapping({"/K-pop", "/K-POP"})
    public String kpop(Model model, 
            @org.springframework.web.bind.annotation.RequestParam(required = false) String guestId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String game) {
        return loadThemePuzzle("K-POP", model, guestId, game);
    }
    
    @GetMapping({"/K-movie", "/K-MOVIE"})
    public String kmovie(Model model,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String guestId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String game) {
        return loadThemePuzzle("K-MOVIE", model, guestId, game);
    }
    
    @GetMapping({"/K-Drama", "/K-DRAMA"})
    public String kdrama(Model model,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String guestId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String game) {
        return loadThemePuzzle("K-DRAMA", model, guestId, game);
    }
    
    @GetMapping({"/K-Culture", "/K-CULTURE"})
    public String kculture(Model model,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String guestId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String game) {
        return loadThemePuzzle("K-CULTURE", model, guestId, game);
    }
    
    /**
     * 테마별 퍼즐 로드 공통 메서드
     * 바로 게임 화면으로 이동하여 오늘의 퍼즐 게임 시작
     */
    private String loadThemePuzzle(String theme, Model model, 
            @org.springframework.web.bind.annotation.RequestParam(required = false) String guestId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String game) {
        try {
            log.info("테마별 퍼즐 게임 시작: {} - guestId: {}", theme, guestId);
            
            // 테마 정보 모델에 추가 (게임 화면에서 사용)
            model.addAttribute("theme", theme);
            if (guestId != null) {
                model.addAttribute("guestId", guestId);
            }
            
            // 테마별 게임 전용 UI로 이동
            return "forward:/theme-game.html";
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 화면 로드 중 오류 발생: {} - {}", theme, e.getMessage(), e);
            model.addAttribute("theme", theme);
            model.addAttribute("error", "화면을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "forward:/theme-game.html"; // 오류 발생 시에도 테마별 게임 화면으로 이동
        }
    }
}
