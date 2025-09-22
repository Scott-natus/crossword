package com.example.crossword.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 웹 페이지 컨트롤러
 * 정적 파일 서빙을 위한 컨트롤러
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-17
 */
@Controller
public class WebController {

    /**
     * 메인 페이지 (퍼즐게임)
     */
    @GetMapping({"/", "/K-CrossWord/"})
    public ResponseEntity<String> index() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 퍼즐게임 페이지
     */
    @GetMapping("/puzzle")
    public ResponseEntity<String> puzzle() {
        return index();
    }

    /**
     * 실제 게임 페이지
     */
    @GetMapping("/game.html")
    public ResponseEntity<String> game() {
        try {
            Resource resource = new ClassPathResource("static/game.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * CSS 파일 서빙
     */
    @GetMapping({"/css/{filename}", "/K-CrossWord/css/{filename}"})
    public ResponseEntity<String> css(@PathVariable String filename) {
        try {
            Resource resource = new ClassPathResource("static/css/" + filename);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("text/css"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * JavaScript 파일 서빙
     */
    @GetMapping({"/js/{filename}", "/K-CrossWord/js/{filename}"})
    public ResponseEntity<String> js(@PathVariable String filename) {
        try {
            Resource resource = new ClassPathResource("static/js/" + filename);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/javascript"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Favicon 서빙 (Mixed Content 문제 해결)
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<byte[]> favicon() {
        try {
            Resource resource = new ClassPathResource("static/favicon.ico");
            byte[] content = resource.getInputStream().readAllBytes();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/x-icon"));
            headers.setContentLength(content.length);
            headers.setCacheControl("public, max-age=31536000"); // 1년 캐시
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            // favicon 파일이 없으면 빈 응답
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/x-icon"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new byte[0]);
        }
    }
    
    /**
     * 추가 favicon 경로들 (브라우저 호환성)
     */
    @GetMapping("/K-CrossWord/favicon.ico")
    public ResponseEntity<byte[]> faviconAlternative() {
        return favicon();
    }

    /**
     * 관리자 메인 페이지
     */
    @GetMapping({"/K-CrossWord/admin/", "/K-CrossWord/admin"})
    public ResponseEntity<String> adminIndex() {
        try {
            Resource resource = new ClassPathResource("static/admin/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 단어 관리 페이지
     */
    @GetMapping({"/K-CrossWord/admin/words"})
    public ResponseEntity<String> adminWords() {
        try {
            Resource resource = new ClassPathResource("static/admin/words/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 관리자 통계 API는 WordManagementController로 이동됨

    // 단어 데이터 API는 WordManagementController로 이동됨

    /**
     * AI 힌트 생성 페이지
     */
    @GetMapping({"/K-CrossWord/admin/hint-generator"})
    public ResponseEntity<String> adminHintGenerator() {
        try {
            Resource resource = new ClassPathResource("static/admin/hint-generator/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 레벨 관리 페이지
     */
    @GetMapping({"/K-CrossWord/admin/levels"})
    public ResponseEntity<String> adminLevels() {
        try {
            Resource resource = new ClassPathResource("static/admin/levels/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 그리드 템플릿 관리 페이지
     */
    @GetMapping({"/K-CrossWord/admin/grid-templates"})
    public ResponseEntity<String> adminGridTemplates() {
        try {
            Resource resource = new ClassPathResource("static/admin/grid-templates/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
