package com.example.crossword.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 관리자 대시보드 컨트롤러
 * 매일 갱신 퍼즐 관리 페이지 제공
 */
@Controller
@RequestMapping("/K-CrossWord/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {
    
    /**
     * 매일 갱신 퍼즐 관리 대시보드
     */
    @GetMapping("/daily-puzzles")
    public String dailyPuzzleDashboard(Model model) {
        try {
            log.info("매일 갱신 퍼즐 관리 대시보드 접근");
            
            // 현재 날짜 정보 추가
            model.addAttribute("currentDate", java.time.LocalDate.now());
            model.addAttribute("currentTime", java.time.LocalDateTime.now());
            
            return "admin/daily-puzzle-dashboard";
            
        } catch (Exception e) {
            log.error("매일 갱신 퍼즐 관리 대시보드 접근 중 오류 발생: {}", e.getMessage());
            return "error";
        }
    }
    
    /**
     * 관리자 메인 대시보드
     */
    @GetMapping("/")
    public String adminMain(Model model) {
        try {
            log.info("관리자 메인 대시보드 접근");
            
            // 관리자 메뉴 정보 추가
            model.addAttribute("adminMenus", java.util.Arrays.asList(
                java.util.Map.of("name", "매일 갱신 퍼즐 관리", "url", "/K-CrossWord/admin/daily-puzzles", "icon", "fas fa-calendar-day"),
                java.util.Map.of("name", "테마별 퍼즐 관리", "url", "/K-CrossWord/admin/theme-puzzles", "icon", "fas fa-puzzle-piece"),
                java.util.Map.of("name", "단어 관리", "url", "/K-CrossWord/admin/words", "icon", "fas fa-book"),
                java.util.Map.of("name", "힌트 관리", "url", "/K-CrossWord/admin/hints", "icon", "fas fa-lightbulb"),
                java.util.Map.of("name", "사용자 관리", "url", "/K-CrossWord/admin/users", "icon", "fas fa-users"),
                java.util.Map.of("name", "통계", "url", "/K-CrossWord/admin/statistics", "icon", "fas fa-chart-bar")
            ));
            
            return "admin/dashboard";
            
        } catch (Exception e) {
            log.error("관리자 메인 대시보드 접근 중 오류 발생: {}", e.getMessage());
            return "error";
        }
    }
}
