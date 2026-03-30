package com.example.board.controller;

import com.example.board.entity.*;
import com.example.board.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/board")
public class BoardController {
    
    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);
    
    @Autowired
    private BoardRepository boardRepository;
    
    @Autowired
    private BoardTypeRepository boardTypeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BoardCommentRepository boardCommentRepository;
    
    @Autowired
    private BoardAttachmentRepository boardAttachmentRepository;
    
    @Autowired
    private BoardVoteRepository boardVoteRepository;
    
    /**
     * 게시판 메인 페이지 - 모든 게시판 목록
     */
    @GetMapping({"", "/"})
    public String boardMain(Model model) {
        logger.info("=== 게시판 메인 페이지 접근 ===");
        
        try {
            // 모든 게시판 타입 조회
            List<BoardType> boardTypes = boardTypeRepository.findAll();
            logger.info("게시판 타입 조회 완료: {}개", boardTypes.size());
            
            model.addAttribute("boardTypes", boardTypes);
            model.addAttribute("title", "게시판 시스템");
            model.addAttribute("message", "다양한 주제의 게시판에서 소통해보세요!");
            
            return "board/main";
        } catch (Exception e) {
            logger.error("게시판 메인 페이지 오류: {}", e.getMessage(), e);
            return "error/500";
        }
    }
    
    @GetMapping("/{boardType}")
    public String index(@PathVariable String boardType, 
                       @RequestParam(required = false) String search,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "15") int size,
                       Model model,
                       HttpServletRequest request) {
        
        Optional<BoardType> boardTypeOpt = boardTypeRepository.findBySlug(boardType);
        if (boardTypeOpt.isEmpty()) {
            return "error/404";
        }
        
        BoardType boardTypeEntity = boardTypeOpt.get();
        
        // 인증이 필요한 게시판인지 확인
        if (boardTypeEntity.getRequiresAuth() && !isAuthenticated()) {
            String redirect = buildCurrentUrl(request);
            return "redirect:/login?redirect=" + urlEncode(redirect);
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
        model.addAttribute("currentUrl", buildCurrentUrl(request));
        
        return "board/index";
    }
    
    @GetMapping("/{boardType}/create")
    public String create(@PathVariable String boardType, Model model, HttpServletRequest request) {
        // 인증 확인
        if (!isAuthenticated()) {
            String redirect = buildCurrentUrl(request);
            return "redirect:/login?redirect=" + urlEncode(redirect);
        }
        
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
                       @RequestParam(required = false) Long parentId,
                       @RequestParam(value = "files", required = false) MultipartFile[] files,
                       HttpServletRequest request) {
        
        Optional<BoardType> boardTypeOpt = boardTypeRepository.findBySlug(boardType);
        if (boardTypeOpt.isEmpty()) {
            return "error/404";
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            String redirect = buildCurrentUrl(request);
            return "redirect:/login?redirect=" + urlEncode(redirect);
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
        
        // 파일 업로드 처리
        if (files != null && files.length > 0) {
            String uploadDir = "/var/www/html/storage/app/public/attachments/";
            try {
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String originalFilename = file.getOriginalFilename();
                        String extension = "";
                        if (originalFilename != null && originalFilename.contains(".")) {
                            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                        }
                        
                        // 파일명 중복 방지를 위해 UUID + 타임스탬프 조합 사용
                        String savedFilename = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension;
                        Path path = Paths.get(uploadDir + savedFilename);
                        Files.copy(file.getInputStream(), path);
                        
                        // DB에 저장 (Laravel 호환을 위해 'attachments/' 접두사 포함)
                        BoardAttachment attachment = new BoardAttachment();
                        attachment.setBoard(board);
                        attachment.setOriginalName(originalFilename);
                        attachment.setFilePath("attachments/" + savedFilename);
                        attachment.setFileType(file.getContentType());
                        attachment.setFileSize(file.getSize());
                        boardAttachmentRepository.save(attachment);
                    }
                }
            } catch (Exception e) {
                logger.error("파일 업로드 중 오류 발생: {}", e.getMessage(), e);
            }
        }
        
        return "redirect:/board/" + boardType;
    }
    
    @GetMapping("/{boardType}/{id}")
    public String show(@PathVariable String boardType, 
                     @PathVariable Long id, 
                     Model model,
                     HttpServletRequest request) {
        
        logger.info("=== BoardController.show() 시작 ===");
        logger.info("boardType: {}, id: {}", boardType, id);
        
        try {
            Optional<Board> boardOpt = boardRepository.findById(id);
            if (boardOpt.isEmpty()) {
                logger.warn("게시물을 찾을 수 없음: {}", id);
                return "error/404";
            }
            
            Board board = boardOpt.get();
            logger.info("게시물 찾음: {}", board.getTitle());
            logger.info("게시물 내용 길이: {}", (board.getContent() != null ? board.getContent().length() : "null"));
            logger.info("게시물 내용 미리보기: {}", (board.getContent() != null ? board.getContent().substring(0, Math.min(100, board.getContent().length())) : "null"));
            
            // 게시판 타입 확인
            if (!board.getBoardType().getSlug().equals(boardType)) {
                logger.warn("게시판 타입 불일치: {} != {}", board.getBoardType().getSlug(), boardType);
                return "error/404";
            }
            
            // 인증이 필요한 게시판인지 확인
            if (board.getBoardType().getRequiresAuth() && !isAuthenticated()) {
                logger.info("인증 필요");
                String redirect = buildCurrentUrl(request);
                return "redirect:/login?redirect=" + urlEncode(redirect);
            }
            
            // 조회수 증가
            board.setViews(board.getViews() + 1);
            boardRepository.save(board);
            logger.info("조회수 증가 완료");
            
            // 댓글 조회
            logger.info("댓글 조회 시작 - boardId: {}", board.getId());
            List<BoardComment> comments = new ArrayList<>();
            try {
                // 간단한 쿼리로 댓글 조회 (JOIN FETCH 없이)
                comments = boardCommentRepository.findByBoardAndParentIdIsNullOrderByCreatedAtAsc(board);
                logger.info("댓글 조회 완료: {}개", comments.size());
                
                // 각 댓글의 기본 정보만 로깅 (User, BoardType 접근하지 않음)
                for (int i = 0; i < comments.size(); i++) {
                    BoardComment comment = comments.get(i);
                    logger.info("댓글 {}: id={}, content={}", 
                        i+1, comment.getId(), 
                        comment.getContent() != null ? comment.getContent().substring(0, Math.min(50, comment.getContent().length())) : "null");
                }
            } catch (Exception e) {
                logger.error("댓글 조회 중 오류 발생: {}", e.getMessage(), e);
                logger.error("에러 스택 트레이스:", e);
                // 에러가 발생해도 빈 리스트로 계속 진행
                comments = new ArrayList<>();
            }
            
            // 트리 구조 조회 (원글~답글)
            logger.info("트리 구조 조회 시작");
            List<Board> thread = getThread(board);
            logger.info("트리 구조 조회 완료: {}개", thread.size());
            
            // 이전 글 / 다음 글 조회
            Board prevBoard = boardRepository.findFirstByBoardTypeAndIdLessThanOrderByIdDesc(board.getBoardType(), board.getId());
            Board nextBoard = boardRepository.findFirstByBoardTypeAndIdGreaterThanOrderByIdAsc(board.getBoardType(), board.getId());
            
            model.addAttribute("board", board);
            model.addAttribute("comments", comments);
            model.addAttribute("thread", thread);
            model.addAttribute("prevBoard", prevBoard);
            model.addAttribute("nextBoard", nextBoard);
            model.addAttribute("currentUrl", buildCurrentUrl(request));
            
            logger.info("모델 속성 설정 완료");
            logger.info("board 객체: {}", (board != null ? "OK" : "NULL"));
            logger.info("comments 객체: {}", (comments != null ? "OK" : "NULL"));
            logger.info("thread 객체: {}", (thread != null ? "OK" : "NULL"));
            logger.info("=== BoardController.show() 성공 ===");
            
            return "board/show";
            
        } catch (Exception e) {
            logger.error("=== BoardController.show() 오류 ===");
            logger.error("오류 메시지: {}", e.getMessage(), e);
            return "error/500";
        }
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
                        @RequestParam String title,
                        @RequestParam String content,
                        @RequestParam String password) {
        
        Optional<Board> boardOpt = boardRepository.findById(id);
        if (boardOpt.isEmpty()) {
            return "error/404";
        }
        
        Board board = boardOpt.get();
        
        // 권한 및 비밀번호 확인
        if (!canDeleteBoard(board, password)) {
            return "redirect:/board/" + boardType + "/" + id + "?error=unauthorized";
        }
        
        board.setTitle(title);
        board.setContent(content);
        
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
    
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
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
        try {
            List<Board> children = boardRepository.findByParentIdOrderByCreatedAtAsc(board.getId());
            return children != null ? children : new ArrayList<>();
        } catch (Exception e) {
            // 예외 발생 시 빈 리스트 반환
            return new ArrayList<>();
        }
    }

    private String buildCurrentUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        return qs != null && !qs.isBlank() ? uri + "?" + qs : uri;
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
    
    /* Storage mapping removed and moved to StorageController for root-level access */
    
    /**
     * 댓글 작성
     */
    @PostMapping("/{boardType}/{id}/comment")
    public String createComment(@PathVariable String boardType, 
                               @PathVariable Long id, 
                               @RequestParam String content,
                               HttpServletRequest request) {
        try {
            logger.info("댓글 작성 요청 - boardType: {}, id: {}, content: {}", boardType, id, content);
            
            // 인증 확인
            if (!isAuthenticated()) {
                logger.warn("인증되지 않은 사용자의 댓글 작성 시도");
                String redirect = buildCurrentUrl(request);
                return "redirect:/login?redirect=" + urlEncode(redirect);
            }
            
            // 게시물 조회
            Optional<Board> boardOpt = boardRepository.findById(id);
            if (boardOpt.isEmpty()) {
                logger.warn("게시물을 찾을 수 없음: {}", id);
                return "redirect:/board/" + boardType;
            }
            
            Board board = boardOpt.get();
            
            // 게시판 타입 확인
            if (!board.getBoardType().getSlug().equals(boardType)) {
                logger.warn("게시판 타입 불일치: {} != {}", board.getBoardType().getSlug(), boardType);
                return "redirect:/board/" + boardType;
            }
            
            // 현재 사용자 조회
            String email = getCurrentUserEmail();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                logger.warn("사용자를 찾을 수 없음: {}", email);
                String redirect = "/board/" + boardType + "/" + id;
                return "redirect:/login?redirect=" + urlEncode(redirect);
            }
            
            User user = userOpt.get();
            
            // 댓글 생성
            BoardComment comment = new BoardComment(content, board, user, board.getBoardType());
            boardCommentRepository.save(comment);
            
            logger.info("댓글 작성 완료 - commentId: {}, userId: {}, boardId: {}", 
                comment.getId(), user.getId(), board.getId());
            
            return "redirect:/board/" + boardType + "/" + id;
            
        } catch (Exception e) {
            logger.error("댓글 작성 중 오류 발생: {}", e.getMessage(), e);
            return "redirect:/board/" + boardType + "/" + id + "?error=comment_failed";
        }
    }
    
    /**
     * 댓글 삭제
     */
    @PostMapping("/{boardType}/{id}/comment/{commentId}/delete")
    public String deleteComment(@PathVariable String boardType, 
                               @PathVariable Long id, 
                               @PathVariable Long commentId,
                               HttpServletRequest request) {
        try {
            logger.info("댓글 삭제 요청 - boardType: {}, id: {}, commentId: {}", boardType, id, commentId);
            
            // 인증 확인
            if (!isAuthenticated()) {
                logger.warn("인증되지 않은 사용자의 댓글 삭제 시도");
                return "redirect:/login?error=login_required";
            }
            
            // 댓글 조회
            Optional<BoardComment> commentOpt = boardCommentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                logger.warn("댓글을 찾을 수 없음: {}", commentId);
                return "redirect:/board/" + boardType + "/" + id;
            }
            
            BoardComment comment = commentOpt.get();
            
            // 게시물 확인
            if (!comment.getBoard().getId().equals(id)) {
                logger.warn("댓글과 게시물 ID 불일치: comment.boardId={}, boardId={}", 
                    comment.getBoard().getId(), id);
                return "redirect:/board/" + boardType + "/" + id;
            }
            
            // 작성자 확인
            String email = getCurrentUserEmail();
            if (!comment.getUser().getEmail().equals(email)) {
                logger.warn("댓글 작성자가 아닌 사용자의 삭제 시도: commentUser={}, currentUser={}", 
                    comment.getUser().getEmail(), email);
                return "redirect:/board/" + boardType + "/" + id + "?error=unauthorized";
            }
            
            // 댓글 삭제
            boardCommentRepository.delete(comment);
            
            logger.info("댓글 삭제 완료 - commentId: {}", commentId);
            
            return "redirect:/board/" + boardType + "/" + id;
            
        } catch (Exception e) {
            logger.error("댓글 삭제 중 오류 발생: {}", e.getMessage(), e);
            return "redirect:/board/" + boardType + "/" + id + "?error=delete_failed";
        }
    }
}
