package com.example.board.controller;

import com.example.board.entity.BoardType;
import com.example.board.repository.BoardTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private BoardTypeRepository boardTypeRepository;

    @GetMapping("/")
    public String home(Model model) {
        List<BoardType> boardTypes = boardTypeRepository.findActiveBoardTypesOrdered();
        
        model.addAttribute("title", "게시판 시스템");
        model.addAttribute("message", "Spring Boot로 구축된 게시판 시스템에 오신 것을 환영합니다!");
        model.addAttribute("boardTypes", boardTypes);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
