package com.example.crossword.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 관리자 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    /**
     * AI 힌트 생성 페이지
     */
    @GetMapping("/hint-generator")
    public String hintGenerator() {
        return "forward:/admin/hint-generator/index.html";
    }
}
