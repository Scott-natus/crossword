package com.example.board.controller;

import com.example.board.entity.*;
import com.example.board.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/board")
public class BoardController {
    
    @Autowired
    private BoardRepository boardRepository;
    
    @Autowired
    private BoardTypeRepository boardTypeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BoardCommentRepository boardCommentRepository;
    
    @Autowired
    private BoardVoteRepository boardVoteRepository;
    
    @GetMapping("/{boardType}")
    public String index(@PathVariable String boardType, 
                       @RequestParam(required = false) String search,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "15") int size,
                       Model model) {
        
        Optional<BoardType> boardTypeOpt = boardTypeRepository.findBySlug(boardType);
        if (boardTypeOpt.isEmpty()) {
            return "error/404";
        }
        
        BoardType boardTypeEntity = boardTypeOpt.get();
        
        // 인증이 필요한 게시판인지 확인
        if (boardTypeEntity.getRequiresAuth() && !isAuthenticated()) {
            return "redirect:/login?error=login_required";
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Board> boards;
        
        if (search != null && !search.trim().isEmpty()) {
            boards = boardRepository.findByBoardTypeAndSearch(boardTypeEntity, search.trim(), pageable);
        } else {
            boards = boardRepository.findByBoardTypeOrderByCreatedAtDesc(boardTypeEntity, pageable);
        }
        
        model.addAttribute("boards", boards);
        model.addAttribute("boardType", boardTypeEntity);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", boards.getTotalPages());
        
        return "board/index";
    }
    
    @GetMapping("/{boardType}/create")
    public String create(@PathVariable String boardType, Model model) {
        Optional<BoardType> boardTypeOpt = boardTypeRepository.findBySlug(boardType);
        if (boardTypeOpt.isEmpty()) {
            return "error/404";
        }
        
        model.addAttribute("boardType", boardTypeOpt.get());
        model.addAttribute("board", new Board());
        
        return "board/create";
    }
    
    @PostMapping("/{boardType}")
    public String store(@PathVariable String boardType, 
                       @RequestParam String title,
                       @RequestParam String content,
                       @RequestParam String password,
                       @RequestParam(required = false) Long parentId) {
        
        Optional<BoardType> boardTypeOpt = boardTypeRepository.findBySlug(boardType);
        if (boardTypeOpt.isEmpty()) {
            return "error/404";
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        BoardType boardTypeEntity = boardTypeOpt.get();
        
        Board board = new Board();
        board.setTitle(title);
        board.setContent(content);
        board.setPassword(password);
        board.setUser(user);
        board.setBoardType(boardTypeEntity);
        board.setViews(0L);
        
        if (parentId != null) {
            board.setParentId(parentId);
        }
        
        boardRepository.save(board);
        
        return "redirect:/board/" + boardType;
    }
    
    @GetMapping("/{boardType}/{id}")
    public String show(@PathVariable String boardType, 
                      @PathVariable Long id, 
                      Model model) {
        
        Optional<Board> boardOpt = boardRepository.findById(id);
        if (boardOpt.isEmpty()) {
            return "error/404";
        }
        
        Board board = boardOpt.get();
        
        // 게시판 타입 확인
        if (!board.getBoardType().getSlug().equals(boardType)) {
            return "error/404";
        }
        
        // 인증이 필요한 게시판인지 확인
        if (board.getBoardType().getRequiresAuth() && !isAuthenticated()) {
            return "redirect:/login?error=login_required";
        }
        
        // 조회수 증가
        board.setViews(board.getViews() + 1);
        boardRepository.save(board);
        
        // 댓글 조회
        List<BoardComment> comments = boardCommentRepository.findTopLevelCommentsByBoard(board);
        
        // 트리 구조 조회 (원글~답글)
        List<Board> thread = getThread(board);
        
        model.addAttribute("board", board);
        model.addAttribute("comments", comments);
        model.addAttribute("thread", thread);
        
        return "board/show";
    }
    
    @GetMapping("/{boardType}/{id}/edit")
    public String edit(@PathVariable String boardType, 
                      @PathVariable Long id, 
                      Model model) {
        
        Optional<Board> boardOpt = boardRepository.findById(id);
        if (boardOpt.isEmpty()) {
            return "error/404";
        }
        
        Board board = boardOpt.get();
        
        // 권한 확인
        if (!canEditBoard(board)) {
            return "error/403";
        }
        
        model.addAttribute("board", board);
        model.addAttribute("boardType", board.getBoardType());
        
        return "board/edit";
    }
    
    @PostMapping("/{boardType}/{id}")
    public String update(@PathVariable String boardType, 
                        @PathVariable Long id, 
                        @ModelAttribute Board boardData) {
        
        Optional<Board> boardOpt = boardRepository.findById(id);
        if (boardOpt.isEmpty()) {
            return "error/404";
        }
        
        Board board = boardOpt.get();
        
        // 권한 확인
        if (!canEditBoard(board)) {
            return "error/403";
        }
        
        board.setTitle(boardData.getTitle());
        board.setContent(boardData.getContent());
        
        boardRepository.save(board);
        
        return "redirect:/board/" + boardType + "/" + id;
    }
    
    @PostMapping("/{boardType}/{id}/delete")
    public String delete(@PathVariable String boardType, 
                        @PathVariable Long id,
                        @RequestParam String password) {
        
        Optional<Board> boardOpt = boardRepository.findById(id);
        if (boardOpt.isEmpty()) {
            return "error/404";
        }
        
        Board board = boardOpt.get();
        
        // 권한 확인
        if (!canDeleteBoard(board, password)) {
            return "error/403";
        }
        
        boardRepository.delete(board);
        
        return "redirect:/board/" + boardType;
    }
    
    // Helper methods
    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");
    }
    
    private boolean canEditBoard(Board board) {
        if (!isAuthenticated()) {
            return false;
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // 관리자 권한 확인
        if ("rainynux@gmail.com".equals(email)) {
            return true;
        }
        
        // 작성자 확인
        return board.getUser().getEmail().equals(email);
    }
    
    private boolean canDeleteBoard(Board board, String password) {
        if (!isAuthenticated()) {
            return false;
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // 관리자 권한 확인
        if ("rainynux@gmail.com".equals(email)) {
            return "tngkrrhk".equals(password);
        }
        
        // 작성자 확인
        return board.getUser().getEmail().equals(email) && board.getPassword().equals(password);
    }
    
    private List<Board> getThread(Board board) {
        // 트리 구조를 평탄화하는 로직
        // 실제 구현에서는 더 복잡한 트리 구조 처리가 필요할 수 있음
        return boardRepository.findByParentIdOrderByCreatedAtAsc(board.getId());
    }
}
