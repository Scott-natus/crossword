package com.example.board.controller;

import com.example.board.entity.BoardType;
import com.example.board.entity.User;
import com.example.board.repository.BoardTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        
        // 인증 상태 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && 
                                 authentication.isAuthenticated() && 
                                 !authentication.getName().equals("anonymousUser");
        
        model.addAttribute("title", "게시판 시스템");
        model.addAttribute("message", "Spring Boot로 구축된 게시판 시스템에 오신 것을 환영합니다!");
        model.addAttribute("boardTypes", boardTypes);
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        if (isAuthenticated) {
            // 사용자 이름 설정 (이메일에서 @ 앞부분만 추출)
            String email = authentication.getName();
            String userName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
            model.addAttribute("userName", userName);
        }
        
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
