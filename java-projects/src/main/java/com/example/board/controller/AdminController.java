package com.example.board.controller;

import com.example.board.entity.User;
import com.example.board.service.PzWordService;
import com.example.board.service.PzHintService;
import com.example.board.service.PuzzleLevelService;
import com.example.board.service.GameSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 관리자 대시보드 컨트롤러
 * 8080 서비스에 맞게 새로 구현
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final PzWordService pzWordService;
    private final PzHintService pzHintService;
    private final PuzzleLevelService puzzleLevelService;
    private final GameSessionService gameSessionService;
    
    /**
     * 단어 관리 페이지 (테스트)
     */
    @GetMapping("/words/test")
    public String wordsManagementTest(Model model, Authentication authentication) {
        try {
            log.info("단어 관리 테스트 페이지 접근");
            
            // 현재 사용자 정보
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                model.addAttribute("currentUser", user);
            }
            
            return "admin/words/test";
            
        } catch (Exception e) {
            log.error("단어 관리 테스트 페이지 로드 중 오류 발생", e);
            return "error/500";
        }
    }
    
    /**
     * 단어 관리 페이지
     */
    @GetMapping("/words")
    public ResponseEntity<String> wordsManagement() {
        try {
            log.info("단어 관리 페이지 접근");
            
            Resource resource = new ClassPathResource("static/admin/words/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            log.error("단어 관리 페이지 로드 중 오류 발생", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 관리자 메인 대시보드
     */
    @GetMapping({"", "/", "/index"})
    public String adminMain(Model model, Authentication authentication) {
        try {
            log.info("관리자 메인 대시보드 접근");
            
            // 현재 사용자 정보
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                model.addAttribute("currentUser", user);
            }
            
            // 관리자 메뉴 정보
            model.addAttribute("adminMenus", java.util.Arrays.asList(
                java.util.Map.of("name", "단어 관리", "url", "/admin/words", "icon", "fas fa-book", "description", "단어 추가, 수정, 삭제 및 일괄 변경"),
                java.util.Map.of("name", "AI 힌트 생성", "url", "/admin/hint-generator", "icon", "fas fa-magic", "description", "Gemini AI를 활용한 자동 힌트 생성"),
                java.util.Map.of("name", "레벨 관리", "url", "/admin/levels", "icon", "fas fa-layer-group", "description", "퍼즐 레벨 및 난이도 설정"),
                java.util.Map.of("name", "템플릿 관리", "url", "/admin/templates", "icon", "fas fa-th", "description", "퍼즐 그리드 패턴 및 템플릿 관리"),
                java.util.Map.of("name", "사용자 관리", "url", "/admin/users", "icon", "fas fa-users", "description", "사용자 계정 및 권한 관리"),
                java.util.Map.of("name", "통계", "url", "/admin/statistics", "icon", "fas fa-chart-bar", "description", "시스템 사용 통계 및 분석")
            ));
            
            // 현재 시간 정보
            model.addAttribute("currentDate", java.time.LocalDate.now());
            model.addAttribute("currentTime", LocalDateTime.now());
            
            return "admin/index";
            
        } catch (Exception e) {
            log.error("관리자 메인 대시보드 접근 중 오류 발생: {}", e.getMessage());
            model.addAttribute("error", "관리자 대시보드 로드 중 오류가 발생했습니다.");
            return "error";
        }
    }
    
    
    /**
     * AI 힌트 생성 페이지
     */
    @GetMapping("/hint-generator")
    public ResponseEntity<String> hintGenerator() {
        try {
            log.info("AI 힌트 생성 페이지 접근");
            
            Resource resource = new ClassPathResource("static/admin/hint-generator/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            log.error("AI 힌트 생성 페이지 로드 중 오류 발생", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 템플릿 관리 페이지
     */
    @GetMapping("/templates")
    public ResponseEntity<String> templates() {
        try {
            log.info("템플릿 관리 페이지 접근");
            
            Resource resource = new ClassPathResource("static/admin/templates/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            log.error("템플릿 관리 페이지 로드 중 오류 발생", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 새 템플릿 생성 페이지
     */
    @GetMapping("/templates/create")
    public ResponseEntity<String> createTemplate() {
        try {
            log.info("새 템플릿 생성 페이지 접근");
            
            Resource resource = new ClassPathResource("static/admin/templates/create.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            log.error("새 템플릿 생성 페이지 로드 중 오류 발생", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 레벨 관리 페이지
     */
    @GetMapping("/levels")
    public ResponseEntity<String> levelManagement() {
        try {
            log.info("레벨 관리 페이지 접근");
            
            Resource resource = new ClassPathResource("static/admin/levels/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            log.error("레벨 관리 페이지 로드 중 오류 발생", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    
    /**
     * 사용자 관리 페이지
     */
    @GetMapping("/users")
    public String userManagement(Model model) {
        try {
            log.info("사용자 관리 페이지 접근");
            return "admin/users";
        } catch (Exception e) {
            log.error("사용자 관리 페이지 접근 중 오류 발생: {}", e.getMessage());
            return "error";
        }
    }
    
    /**
     * 통계 페이지
     */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        try {
            log.info("통계 페이지 접근");
            return "admin/statistics";
        } catch (Exception e) {
            log.error("통계 페이지 접근 중 오류 발생: {}", e.getMessage());
            return "error";
        }
    }
}
