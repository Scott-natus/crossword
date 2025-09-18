package com.example.crossword.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
}
