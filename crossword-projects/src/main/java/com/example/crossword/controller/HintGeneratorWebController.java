package com.example.crossword.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 힌트생성 관리 웹 컨트롤러
 * 라라벨의 PzHintGeneratorController index 메서드와 동일한 기능
 * 경로: /K-CrossWord/admin/hint-generator
 */
@Controller
@RequestMapping("/admin/hint-generator")
@Slf4j
public class HintGeneratorWebController {
    
    /**
     * 힌트 생성 관리 페이지 표시 (정적 HTML 파일 사용)
     * 기존 단어 관리 페이지와 동일한 방식
     */
    @GetMapping
    public String index() {
        log.debug("힌트생성 관리 페이지 요청");
        // 정적 HTML 파일을 직접 반환 (templates가 아닌 static 폴더의 파일)
        return "forward:/admin/hint-generator/index.html";
    }
}