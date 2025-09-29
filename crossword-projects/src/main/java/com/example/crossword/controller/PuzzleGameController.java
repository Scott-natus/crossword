package com.example.crossword.controller;

import com.example.crossword.entity.PuzzleLevel;
import com.example.crossword.entity.UserPuzzleGame;
import com.example.crossword.entity.PzWord;
import com.example.crossword.service.PuzzleGridTemplateService;
import com.example.crossword.service.UserPuzzleGameService;
import com.example.crossword.service.PuzzleLevelService;
import com.example.crossword.service.PzWordService;
import com.example.crossword.service.PzHintService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/puzzle-game")
public class PuzzleGameController {
    
    @Autowired
    private PuzzleGridTemplateService puzzleGridTemplateService;
    
    @Autowired
    private UserPuzzleGameService userPuzzleGameService;
    
    @Autowired
    private PuzzleLevelService puzzleLevelService;
    
    @Autowired
    private PzWordService pzWordService;
    
    @Autowired
    private PzHintService pzHintService;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 메인 퍼즐게임 페이지 - 라라벨과 동일한 로직
     */
    @GetMapping("/")
    public String index(HttpServletRequest request) {
        // Authorization 헤더에서 로그인 사용자 확인
        String authHeader = request.getHeader("Authorization");
        String guestId = request.getParameter("guest_id");
        
        // 세션에서 게스트 ID 확인
        if (guestId == null) {
            HttpSession session = request.getSession();
            guestId = (String) session.getAttribute("guest_id");
        }
        
        if (authHeader == null && guestId == null) {
            // 로그인/게스트 ID 모두 없으면 게스트 안내 페이지로 리다이렉트
            return "redirect:/K-CrossWord/";
        }
        
        if (guestId != null && authHeader == null) {
            // 게스트 계정 처리
            Long userId = getOrCreateGuestUser(guestId, request);
            if (userId == null) {
                return "redirect:/K-CrossWord/";
            }
            
            // 세션에 게스트 ID 저장
            HttpSession session = request.getSession();
            session.setAttribute("guest_id", guestId);
            
            // 게임 페이지로 리다이렉트
            return "redirect:/K-CrossWord/game.html?guest_id=" + guestId;
        }
        
        // 로그인 사용자는 게임 페이지로 리다이렉트
        return "redirect:/K-CrossWord/game.html";
    }

    /**
     * 퍼즐 템플릿 가져오기 - 라라벨과 동일한 로직
     */
    @GetMapping("/template")
    public ResponseEntity<Map<String, Object>> getTemplate(HttpServletRequest request) {
        try {
            // 게스트 ID 또는 로그인 사용자 ID 가져오기
            String guestId = request.getParameter("guestId");
            String authHeader = request.getHeader("Authorization");
            
            Long userId = null;
            
            if (guestId != null) {
                // 게스트 사용자 처리
                userId = getOrCreateGuestUser(guestId, request);
                if (userId == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "잘못된 게스트 ID입니다."));
                }
            } else {
                // 로그인 사용자 처리 (통합 인증)
                // 1. Authorization 헤더 확인
                if (authHeader != null) {
                    userId = getUserIdFromAuthHeader(authHeader);
                }
                
                // 2. 세션에서 인증 정보 확인 (8080 게시판과 통합)
                if (userId == null) {
                    userId = getUserIdFromSession(request);
                }
                
                if (userId == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "사용자 인증이 필요합니다."));
                }
            }
            
            // 게임 데이터 조회/생성
            UserPuzzleGame game = userPuzzleGameService.getOrCreateGameByUserId(userId);
            
            if (game == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "게임 데이터를 생성할 수 없습니다."));
            }
            
            // 현재 레벨 정보 가져오기
            PuzzleLevel level = puzzleLevelService.getByLevel(game.getCurrentLevel());
            if (level == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "레벨 정보를 찾을 수 없습니다."));
            }
            
            // 기존 퍼즐 데이터가 있는지 확인
            if (game.hasActivePuzzle()) {
                // 기존 퍼즐 데이터 복원
                Map<String, Object> result = restoreExistingPuzzle(game, level);
                return ResponseEntity.ok(result);
                    } else {
                // 새로운 퍼즐 생성
                Map<String, Object> result = createNewPuzzle(game, level);
                return ResponseEntity.ok(result);
            }
            
        } catch (Exception e) {
            System.err.println("=== PuzzleGameController.getTemplate 오류 ===");
            System.err.println("오류 메시지: " + e.getMessage());
            System.err.println("오류 클래스: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 정답 확인 - 라라벨과 동일한 로직
     */
    @PostMapping("/check-answer")
    public ResponseEntity<Map<String, Object>> checkAnswer(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        try {
            String guestId = (String) requestData.get("guestId");
            String authHeader = request.getHeader("Authorization");
            
            Long userId = null;
            if (guestId != null) {
                userId = getOrCreateGuestUser(guestId, request);
            } else {
                // 로그인 사용자 처리 (통합 인증)
                if (authHeader != null) {
                    userId = getUserIdFromAuthHeader(authHeader);
                }
                if (userId == null) {
                    userId = getUserIdFromSession(request);
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "사용자 인증이 필요합니다."));
            }
            
            Long wordId = Long.valueOf(requestData.get("word_id").toString());
            String answer = (String) requestData.get("answer");
            
            // 정답 확인 로직
            PzWord word = pzWordService.getById(wordId);
            if (word == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "단어를 찾을 수 없습니다."));
            }
            
            boolean isCorrect = word.getWord().equals(answer.trim());
            
            // 게임 데이터 업데이트
            UserPuzzleGame game = userPuzzleGameService.getOrCreateGameByUserId(userId);
            if (isCorrect) {
                game.setCurrentLevelCorrectAnswers(game.getCurrentLevelCorrectAnswers() + 1);
                // 정답 단어를 게임 상태에 추가
                updateGameStateWithCorrectAnswer(game, wordId, answer);
            } else {
                game.setCurrentLevelWrongAnswers(game.getCurrentLevelWrongAnswers() + 1);
            }
            
            userPuzzleGameService.save(game);
                    
                    Map<String, Object> response = new HashMap<>();
            response.put("is_correct", isCorrect);
            response.put("message", isCorrect ? "정답입니다!" : "오답입니다.");
            response.put("correct_count", game.getCurrentLevelCorrectAnswers());
            response.put("wrong_count", game.getCurrentLevelWrongAnswers());
            
            if (isCorrect) {
                response.put("correct_answer", answer);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 힌트 가져오기 - 라라벨과 동일한 로직
     */
    @GetMapping("/hints")
    public ResponseEntity<Map<String, Object>> getHints(
            @RequestParam Long word_id,
            @RequestParam(required = false) Long current_hint_id,
            @RequestParam(required = false) Long base_hint_id,
            @RequestParam(required = false) Boolean is_additional_hint,
            @RequestParam(required = false) String guestId,
            HttpServletRequest request) {
        
        try {
            Long userId = null;
            if (guestId != null) {
                userId = getOrCreateGuestUser(guestId, request);
            } else {
                // 로그인 사용자 처리 (통합 인증)
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    userId = getUserIdFromAuthHeader(authHeader);
                }
                if (userId == null) {
                    userId = getUserIdFromSession(request);
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "사용자 인증이 필요합니다."));
            }
            
            List<Map<String, Object>> hints = new ArrayList<>();
            
            if (is_additional_hint != null && is_additional_hint) {
                // 추가 힌트 요청 - primary=false이고 id가 가장 작은 힌트
                hints = pzHintService.getAdditionalHints(word_id);
            } else {
                // 기본 힌트 요청 - primary=true
                hints = pzHintService.getPrimaryHints(word_id);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("hints", hints);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 정답 보기 - 관리자용
     */
    @GetMapping("/show-answer")
    public ResponseEntity<Map<String, Object>> showAnswer(
            @RequestParam Long word_id,
            @RequestParam(required = false) String guestId,
            HttpServletRequest request) {
        
        try {
            Long userId = null;
            if (guestId != null) {
                userId = getOrCreateGuestUser(guestId, request);
            } else {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    userId = getUserIdFromAuthHeader(authHeader);
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "사용자 인증이 필요합니다."));
            }
            
            // 관리자 권한 확인 (추후 구현)
            // if (!isAdmin(userId)) {
            //     return ResponseEntity.forbidden().body(Map.of("error", "관리자 권한이 필요합니다."));
            // }
            
            PzWord word = pzWordService.getById(word_id);
            if (word == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "단어를 찾을 수 없습니다."));
            }
                
                Map<String, Object> response = new HashMap<>();
            response.put("answer", word.getWord());
                
                return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 레벨 완료 처리
     */
    @PostMapping("/complete-level")
    public ResponseEntity<Map<String, Object>> completeLevel(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        try {
            String guestId = (String) requestData.get("guestId");
            String authHeader = request.getHeader("Authorization");
            
            Long userId = null;
            if (guestId != null) {
                userId = getOrCreateGuestUser(guestId, request);
            } else {
                // 로그인 사용자 처리 (통합 인증)
                if (authHeader != null) {
                    userId = getUserIdFromAuthHeader(authHeader);
                }
                if (userId == null) {
                    userId = getUserIdFromSession(request);
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "사용자 인증이 필요합니다."));
            }
            
            UserPuzzleGame game = userPuzzleGameService.getOrCreateGameByUserId(userId);
            game.setCurrentLevel(game.getCurrentLevel() + 1);
            game.setCurrentLevelCorrectAnswers(0);
            game.setCurrentLevelWrongAnswers(0);
            
            // 활성 퍼즐 완료 (새로운 레벨을 위해 게임 상태 초기화)
            game.completeActivePuzzle();
            
            userPuzzleGameService.save(game);
            
            System.out.println("레벨 완료 처리 - 새 레벨: " + game.getCurrentLevel());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("new_level", game.getCurrentLevel());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게임오버 처리
     */
    @PostMapping("/game-over")
    public ResponseEntity<Map<String, Object>> gameOver(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        try {
            String guestId = (String) requestData.get("guestId");
            String authHeader = request.getHeader("Authorization");
            
            Long userId = null;
            if (guestId != null) {
                userId = getOrCreateGuestUser(guestId, request);
            } else if (authHeader != null) {
                userId = getUserIdFromAuthHeader(authHeader);
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "사용자 인증이 필요합니다."));
            }
            
            UserPuzzleGame game = userPuzzleGameService.getOrCreateGameByUserId(userId);
            game.setIsActive(false);
            game.setGameState(null);
            
            userPuzzleGameService.save(game);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 클리어 조건 조회
     */
    @GetMapping("/clear-condition")
    public ResponseEntity<Map<String, Object>> getClearCondition(
            @RequestParam(required = false) String guestId,
            HttpServletRequest request) {
        
        try {
            Long userId = null;
            if (guestId != null) {
                userId = getOrCreateGuestUser(guestId, request);
            } else {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    userId = getUserIdFromAuthHeader(authHeader);
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "사용자 인증이 필요합니다."));
            }
            
            UserPuzzleGame game = userPuzzleGameService.getOrCreateGameByUserId(userId);
            PuzzleLevel level = puzzleLevelService.getByLevel(game.getCurrentLevel());
            
            Map<String, Object> response = new HashMap<>();
            response.put("clear_condition", level.getClearCondition());
            response.put("current_count", game.getCurrentLevelCorrectAnswers());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    // ===== 헬퍼 메서드들 =====

    /**
     * 게스트 사용자 생성 또는 조회 (세션 기반)
     */
    private Long getOrCreateGuestUser(String guestId, HttpServletRequest request) {
        try {
            HttpSession session = request.getSession();
            
            // 세션에서 기존 게스트 ID 확인
            String existingGuestId = (String) session.getAttribute("guest_id");
            Long existingUserId = (Long) session.getAttribute("user_id");
            
            // 동일한 게스트 ID면 기존 사용자 ID 반환
            if (existingGuestId != null && existingGuestId.equals(guestId) && existingUserId != null) {
                System.out.println("기존 게스트 세션 사용: " + guestId + " -> " + existingUserId);
                return existingUserId;
            }
            
            // 새로운 게스트 ID 처리
            Long userId;
            try {
                // 게스트 ID에서 숫자 부분만 추출하여 사용자 ID로 사용
                String numericPart = guestId.replace("guest_", "");
                userId = Long.parseLong(numericPart);
            } catch (NumberFormatException e) {
                // 숫자 변환 실패 시 해시코드 사용
                userId = Math.abs((long) guestId.hashCode());
            }
            
            // 세션에 저장
            session.setAttribute("guest_id", guestId);
            session.setAttribute("user_id", userId);
            
            System.out.println("새 게스트 세션 생성: " + guestId + " -> " + userId);
            return userId;
            
        } catch (Exception e) {
            System.err.println("게스트 사용자 생성/조회 오류: " + e.getMessage());
            // 기본값 반환
            Long userId = Math.abs((long) guestId.hashCode());
            HttpSession session = request.getSession();
            session.setAttribute("guest_id", guestId);
            session.setAttribute("user_id", userId);
            return userId;
        }
    }

    /**
     * Authorization 헤더에서 사용자 ID 추출 (통합 인증)
     */
    private Long getUserIdFromAuthHeader(String authHeader) {
        // TODO: 8080 스프링부트 게시판과 통합 인증 구현
        // 현재는 임시로 1 반환
        return 1L;
    }
    
    /**
     * 세션에서 인증된 사용자 ID 가져오기 (통합 인증)
     */
    private Long getUserIdFromSession(HttpServletRequest request) {
        try {
            // Spring Security의 SecurityContext에서 인증 정보 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
                
                String email = authentication.getName();
                System.out.println("인증된 사용자 이메일: " + email);
                
                // 이메일로 사용자 ID 조회 (8080 서비스의 users 테이블과 동일)
                // TODO: 실제로는 UserRepository를 주입받아서 사용해야 함
                // 임시로 이메일 기반으로 사용자 ID 생성
                if ("rainynux@gmail.com".equals(email)) {
                    return 1L; // 관리자 계정
                } else {
                    // 다른 사용자들의 경우 이메일 해시를 사용하여 일관된 ID 생성
                    return (long) Math.abs(email.hashCode() % 10000) + 1000;
                }
            }
        } catch (Exception e) {
            System.err.println("세션에서 사용자 ID 추출 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 기존 퍼즐 데이터 복원
     */
    private Map<String, Object> restoreExistingPuzzle(UserPuzzleGame game, PuzzleLevel level) {
        try {
            System.out.println("=== 기존 퍼즐 복원 시도 ===");
            System.out.println("hasActivePuzzle: " + game.hasActivePuzzle());
            System.out.println("currentPuzzleData: " + (game.getCurrentPuzzleData() != null ? "존재" : "없음"));
            System.out.println("currentGameState: " + (game.getCurrentGameState() != null ? "존재" : "없음"));
            
            // 현재 퍼즐 데이터에서 템플릿 복원
            Map<String, Object> puzzleData = game.getCurrentPuzzleData();
            Map<String, Object> gameState = game.getCurrentGameState();
            
            if (puzzleData != null && gameState != null) {
                System.out.println("기존 퍼즐 데이터 복원 성공");
                
                Map<String, Object> result = new HashMap<>();
                result.put("template", puzzleData.get("template"));
                result.put("level", level);
                result.put("game", game);
                result.put("game_state", gameState);
                result.put("answered_words_with_answers", gameState.get("answered_words_with_answers"));
                result.put("is_restored", true);
                
                return result;
            } else {
                System.out.println("퍼즐 데이터 또는 게임 상태가 없음 - 새 퍼즐 생성");
            }
        } catch (Exception e) {
            System.err.println("퍼즐 복원 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 복원 실패 시 새 퍼즐 생성
        return createNewPuzzle(game, level);
    }

    /**
     * 새로운 퍼즐 생성
     */
    private Map<String, Object> createNewPuzzle(UserPuzzleGame game, PuzzleLevel level) {
        try {
            // 퍼즐 그리드 템플릿에서 단어 추출
            Map<String, Object> extractionResult = puzzleGridTemplateService.extractWordsFromTemplate(
                level.getWordDifficulty(), 
                level.getHintDifficulty(), 
                level.getWordCount(), 
                level.getIntersectionCount()
            );
            
            if (!(Boolean) extractionResult.get("success")) {
                throw new RuntimeException("퍼즐 생성에 실패했습니다.");
            }
            
            // 퍼즐 데이터와 게임 상태 분리 저장
            Map<String, Object> puzzleData = new HashMap<>();
            puzzleData.put("template", extractionResult.get("template"));
            puzzleData.put("extracted_words", extractionResult.get("extracted_words"));
            
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("answered_words", new ArrayList<>());
            gameState.put("answered_words_with_answers", new HashMap<>());
            gameState.put("wrong_answers", new ArrayList<>());
            gameState.put("hints_used", new ArrayList<>());
            
            // 활성 퍼즐 시작
            game.startActivePuzzle(puzzleData, gameState);
            userPuzzleGameService.save(game);
            
            System.out.println("=== 새 퍼즐 생성 완료 ===");
            System.out.println("hasActivePuzzle: " + game.hasActivePuzzle());
            System.out.println("currentPuzzleData: " + (game.getCurrentPuzzleData() != null ? "존재" : "없음"));
            System.out.println("currentGameState: " + (game.getCurrentGameState() != null ? "존재" : "없음"));
                
            Map<String, Object> result = new HashMap<>();
            result.put("template", extractionResult.get("template"));
            result.put("level", level);
            result.put("game", game);
            result.put("game_state", gameState);
            result.put("answered_words_with_answers", new HashMap<>());
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("퍼즐 생성 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 정답 단어를 게임 상태에 추가
     */
    private void updateGameStateWithCorrectAnswer(UserPuzzleGame game, Long wordId, String answer) {
        try {
            Map<String, Object> gameState = game.getCurrentGameState();
            if (gameState != null) {
                @SuppressWarnings("unchecked")
                List<Long> answeredWords = (List<Long>) gameState.get("answered_words");
                if (answeredWords == null) {
                    answeredWords = new ArrayList<>();
                }
                
                @SuppressWarnings("unchecked")
                Map<String, String> answeredWordsWithAnswers = (Map<String, String>) gameState.get("answered_words_with_answers");
                if (answeredWordsWithAnswers == null) {
                    answeredWordsWithAnswers = new HashMap<>();
                }
                
                // 정답 단어 추가
                if (!answeredWords.contains(wordId)) {
                    answeredWords.add(wordId);
                }
                answeredWordsWithAnswers.put(wordId.toString(), answer);
                
                gameState.put("answered_words", answeredWords);
                gameState.put("answered_words_with_answers", answeredWordsWithAnswers);
                
                // 게임 상태 업데이트
                game.setCurrentGameState(gameState);
                userPuzzleGameService.save(game);
                
                System.out.println("게임 상태 업데이트 완료 - 정답 단어 ID: " + wordId + ", 정답: " + answer);
            }
        } catch (Exception e) {
            System.err.println("게임 상태 업데이트 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}