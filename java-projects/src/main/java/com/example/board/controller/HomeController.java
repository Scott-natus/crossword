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
    public String home() {
        // static/index.html을 서빙 (퍼즐게임 메인 화면)
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
