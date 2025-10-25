package com.example.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 퍼즐게임 화면 라우팅 컨트롤러
 * 문서에 따른 라우팅 구조 구현
 */
@Controller
public class PuzzleViewController {
    
    /**
     * 퍼즐게임 화면들 - 모두 static/index.html 서빙
     */
    @GetMapping("/Korean/game")
    public String koreanGame() {
        return "forward:/game.html";
    }
    
    @GetMapping("/K-pop")
    public String kpop() {
        return "forward:/index.html";
    }
    
    @GetMapping("/K-movie")
    public String kmovie() {
        return "forward:/index.html";
    }
    
    @GetMapping("/K-Drama")
    public String kdrama() {
        return "forward:/index.html";
    }
    
    @GetMapping("/K-Culture")
    public String kculture() {
        return "forward:/index.html";
    }
}
