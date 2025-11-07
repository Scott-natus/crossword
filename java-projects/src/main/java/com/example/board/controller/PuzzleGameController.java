package com.example.board.controller;

import com.example.board.entity.PuzzleLevel;
import com.example.board.entity.UserPuzzleGame;
import com.example.board.entity.PzWord;
import com.example.board.entity.PzHint;
import com.example.board.entity.User;
import com.example.board.repository.UserRepository;
import com.example.board.service.PuzzleGridTemplateService;
import com.example.board.service.UserPuzzleGameService;
import com.example.board.service.PuzzleLevelService;
import com.example.board.service.PzWordService;
import com.example.board.service.PzHintService;
import com.example.board.service.PuzzleGameRecordService;
// import com.example.board.service.HintGeneratorManagementService;
import com.example.board.service.WordService;
import com.example.board.service.UserWrongAnswerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/puzzle-game")
public class PuzzleGameController {
    
    private static final Logger logger = LoggerFactory.getLogger(PuzzleGameController.class);
    
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
    private PuzzleGameRecordService puzzleGameRecordService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    // 비활성화된 서비스들 - 임시 제거
    // private HintGeneratorManagementService hintGeneratorManagementService;
    // private WordService wordService;
    
    @Autowired
    private UserWrongAnswerService userWrongAnswerService;

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
                logger.warn("정답 확인 실패 - 단어를 찾을 수 없음: wordId={}, userId={}, guestId={}", wordId, userId, guestId);
                return ResponseEntity.badRequest().body(Map.of("error", "단어를 찾을 수 없습니다."));
            }
            
            boolean isCorrect = word.getWord().equals(answer.trim());
            
            // 정답 확인 로그 기록
            logger.info("정답 확인 요청 - userId={}, guestId={}, wordId={}, 제출답안='{}', 정답='{}', 결과={}", 
                userId, guestId, wordId, answer, word.getWord(), isCorrect ? "정답" : "오답");
            
            // 게임 데이터 업데이트
            UserPuzzleGame game = userPuzzleGameService.getOrCreateGameByUserId(userId);
            
            // 퍼즐 데이터가 없으면 게임 상태만 저장 (테마별 퍼즐의 경우)
            if (game.getCurrentPuzzleData() == null) {
                logger.warn("퍼즐 데이터가 없는 상태에서 정답 확인 - gameId: {}, wordId: {}", game.getId(), wordId);
                // 게임 상태만 업데이트 (퍼즐 데이터는 나중에 저장됨)
            }
            
            Map<String, Object> response = new HashMap<>();
            
            if (isCorrect) {
                // 이미 맞춘 단어인지 확인
                boolean isAlreadyAnswered = isWordAlreadyAnswered(game, wordId);
                
                if (!isAlreadyAnswered) {
                    // 새로운 정답인 경우에만 카운트 증가
                    game.setCurrentLevelCorrectAnswers(game.getCurrentLevelCorrectAnswers() + 1);
                    logger.info("정답 처리 완료 - userId={}, wordId={}, 정답='{}', 현재정답수={}, 레벨={}", 
                        userId, wordId, answer, game.getCurrentLevelCorrectAnswers(), game.getCurrentLevel());
                } else {
                    // 이미 맞춘 단어
                    logger.info("이미 맞춘 단어 재제출 - userId={}, wordId={}, 정답='{}'", userId, wordId, answer);
                }
                
                // 정답 단어를 게임 상태에 추가
                updateGameStateWithCorrectAnswer(game, wordId, answer);
                
                response.put("is_correct", true);
                response.put("message", "정답입니다!");
                response.put("correct_count", game.getCurrentLevelCorrectAnswers());
                response.put("wrong_count", game.getCurrentLevelWrongAnswers());
                response.put("correct_answer", answer);
                
            } else {
                // 오답 처리
                game.setCurrentLevelWrongAnswers(game.getCurrentLevelWrongAnswers() + 1);
                int wrongCount = game.getCurrentLevelWrongAnswers();
                
                // 오답을 user_wrong_answers 테이블에 저장 (Laravel과 동일)
                try {
                    userWrongAnswerService.saveWrongAnswer(
                        userId, 
                        wordId.longValue(), 
                        answer, 
                        word.getWord(), 
                        word.getCategory(), 
                        game.getCurrentLevel()
                    );
                    logger.info("오답 기록 저장 완료 - userId={}, wordId={}, userAnswer='{}', correctAnswer='{}', category='{}', level={}", 
                        userId, wordId, answer, word.getWord(), word.getCategory(), game.getCurrentLevel());
                } catch (Exception e) {
                    logger.error("오답 기록 저장 실패 - userId={}, wordId={}, error={}", userId, wordId, e.getMessage());
                }
                
                logger.info("오답 처리 - userId={}, wordId={}, 제출답안='{}', 정답='{}', 현재오답수={}, 레벨={}", 
                    userId, wordId, answer, word.getWord(), wrongCount, game.getCurrentLevel());
                
                // 오답 4회일 때 특별한 메시지
                if (wrongCount == 4) {
                    response.put("message", "현재 오답이 4회 입니다, 5회 오답시 레벨을 재시작합니다");
                } else {
                    response.put("message", "오답입니다. 누적 오답: " + wrongCount + "회");
                }
                
                response.put("is_correct", false);
                response.put("correct_count", game.getCurrentLevelCorrectAnswers());
                response.put("wrong_count", wrongCount);
                
                // 오답 5회 초과 체크
                if (wrongCount >= 5) {
                    logger.warn("오답 5회 초과로 레벨 재시작 - userId={}, 레벨={}, 오답수={}", 
                        userId, game.getCurrentLevel(), wrongCount);
                    
                    // 정답 정보를 먼저 클라이언트에 전송
                    Map<String, Object> puzzleData = game.getCurrentPuzzleData();
                    if (puzzleData != null) {
                        Object extractedWordsObj = puzzleData.get("extracted_words");
                        List<Map<String, Object>> extractedWords = null;
                        
                        // extracted_words 구조 확인 (Map 또는 List)
                        if (extractedWordsObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> extractedWordsData = (Map<String, Object>) extractedWordsObj;
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> wordOrder = (List<Map<String, Object>>) extractedWordsData.get("word_order");
                            extractedWords = wordOrder;
                        } else if (extractedWordsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> extractedWordsList = (List<Map<String, Object>>) extractedWordsObj;
                            extractedWords = extractedWordsList;
                        }
                        
                        if (extractedWords != null && !extractedWords.isEmpty()) {
                            List<Map<String, Object>> allAnswers = new ArrayList<>();
                            for (int i = 0; i < extractedWords.size(); i++) {
                                Map<String, Object> wordData = extractedWords.get(i);
                                Map<String, Object> answerData = new HashMap<>();
                                answerData.put("word_id", wordData.get("word_id"));
                                
                                // pz_word_id를 사용해서 실제 단어 조회 (8081과 동일)
                                Integer pzWordId = Integer.valueOf(wordData.get("pz_word_id").toString());
                                Optional<PzWord> pzWordOpt = pzWordService.getPzWordById(pzWordId);
                                String actualWord = pzWordOpt.map(PzWord::getWord).orElse("");
                                answerData.put("word", actualWord);
                                
                                answerData.put("position", (i + 1) + "번");
                                allAnswers.add(answerData);
                            }
                            response.put("all_answers", allAnswers);
                        }
                    }
                    
                    // 그 다음에 게임 상태 초기화 (라라벨과 동일)
                    game.setCurrentLevelCorrectAnswers(0);
                    game.setCurrentLevelWrongAnswers(0);
                    
                    // 현재 퍼즐 세션 종료 (라라벨의 endCurrentPuzzle()과 동일)
                    game.completeActivePuzzle();
                    userPuzzleGameService.save(game);
                    
                    response.put("message", "오답회수가 초과되었습니다. 레벨을 다시 시작합니다.");
                    response.put("restart_level", true);
                    response.put("wrong_count_exceeded", true);
                    response.put("correct_count", 0);
                    response.put("wrong_count", 0);
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            userPuzzleGameService.save(game);
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
                // 세션 기반 인증으로 사용자 ID 가져오기
                userId = getUserIdFromSession(request);
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "사용자 인증이 필요합니다."));
            }
            
            // 관리자 권한 확인
            if (!isAdmin(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "관리자만 접근 가능합니다."));
            }
            
            PzWord word = pzWordService.getById(word_id);
            if (word == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "단어를 찾을 수 없습니다."));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("answer", word.getWord());
            response.put("message", "정답: " + word.getWord());
                
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 관리자 권한 확인
     */
    private boolean isAdmin(Long userId) {
        try {
            // Spring Security의 SecurityContext에서 인증 정보 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
                
                String email = authentication.getName();
                
                // 데이터베이스에서 사용자 정보 조회
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    return user.getIsAdmin() != null && user.getIsAdmin();
                }
            }
        } catch (Exception e) {
            System.err.println("관리자 권한 확인 실패: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 레벨 완료 처리 (Laravel과 동일한 클리어 조건 로직)
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
            
            // 사용자 ID 유효성 검증
            Optional<User> userCheck = userRepository.findById(userId);
            if (!userCheck.isPresent()) {
                System.err.println("ERROR: 사용자 ID " + userId + "가 users 테이블에 존재하지 않습니다.");
                return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 사용자입니다."));
            }
            
            logger.info("=== 레벨 완료 처리 시작 - userId={}, guestId={} ===", userId, guestId);
            
            UserPuzzleGame game = userPuzzleGameService.getOrCreateGameByUserId(userId);
            Integer currentLevel = game.getCurrentLevel();
            
            logger.info("레벨 완료 요청 - userId={}, 현재레벨={}, 정답수={}, 오답수={}", 
                userId, currentLevel, game.getCurrentLevelCorrectAnswers(), game.getCurrentLevelWrongAnswers());
            
            // 현재 레벨 정보 조회
            PuzzleLevel level = puzzleLevelService.getById(currentLevel.longValue());
            if (level == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "레벨 정보를 찾을 수 없습니다."));
            }
            
            // 레벨 클리어 기록 저장 (조건 충족 여부와 관계없이 항상 저장)
            Map<String, Object> gameData = new HashMap<>();
            gameData.put("score", requestData.getOrDefault("score", 0));
            gameData.put("play_time", requestData.getOrDefault("play_time", 0));
            gameData.put("hints_used", requestData.getOrDefault("hints_used", 0));
            gameData.put("words_found", requestData.getOrDefault("words_found", 0));
            gameData.put("total_words", requestData.getOrDefault("total_words", 0));
            gameData.put("accuracy", requestData.getOrDefault("accuracy", 0.0));
            
            System.out.println("=== 레벨 클리어 기록 저장 시도 ===");
            System.out.println("사용자 ID: " + userId + ", 레벨: " + currentLevel);
            System.out.println("게임 데이터: " + gameData);
            
            try {
                puzzleGameRecordService.recordLevelClear(userId, currentLevel, gameData);
                System.out.println("레벨 클리어 기록 저장 성공");
            } catch (Exception e) {
                System.err.println("레벨 클리어 기록 저장 실패: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.badRequest().body(Map.of("error", "레벨 클리어 기록 저장에 실패했습니다: " + e.getMessage()));
            }
            
            // 레벨 클리어 조건 확인 (기록 저장 후)
            if (!puzzleGameRecordService.checkLevelClearCondition(userId, currentLevel, level)) {
                Long clearCount = puzzleGameRecordService.getClearCountByUserAndLevel(userId, currentLevel);
                int remaining = Math.max(0, level.getClearCondition() - clearCount.intValue());
                
                // 클리어 조건 미충족 시 새로운 퍼즐 생성 (라라벨과 동일한 로직)
                System.out.println("=== 클리어 조건 미충족 - 새로운 퍼즐 생성 시작 ===");
                System.out.println("사용자 ID: " + userId + ", 레벨: " + currentLevel);
                logger.warn("레벨 클리어 조건 미충족 - userId={}, 레벨={}, 현재클리어횟수={}, 필요횟수={}, 남은횟수={}", 
                    userId, currentLevel, clearCount, level.getClearCondition(), remaining);
                
                regenerateWordsAndHintsForCurrentPuzzle(game);
                
                logger.info("클리어 조건 미충족으로 새 퍼즐 생성 완료 - userId={}, 레벨={}", userId, currentLevel);
                
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "레벨 클리어 조건을 만족하지 않습니다.",
                    "message", "클리어까지 " + remaining + "회 남았습니다.",
                    "remaining", remaining,
                    "condition_not_met", true
                ));
            }
            
            // 다음 레벨 존재 여부 확인
            PuzzleLevel nextLevel = puzzleLevelService.getById(Long.valueOf(currentLevel + 1));
            if (nextLevel == null) {
                // 다음 레벨이 없으면 현재 레벨 유지
                logger.info("모든 레벨 완료 - userId={}, 마지막레벨={}", userId, currentLevel);
                return ResponseEntity.ok(Map.of(
                    "message", "축하합니다! 모든 레벨을 완료했습니다!",
                    "new_level", currentLevel,
                    "success", true,
                    "no_next_level", true
                ));
            }
            
            // 현재 퍼즐 세션 종료
            game.completeActivePuzzle();
            
            // 다음 레벨로 진행
            game.setCurrentLevel(currentLevel + 1);
            game.setCurrentLevelCorrectAnswers(0);
            game.setCurrentLevelWrongAnswers(0);
            
            userPuzzleGameService.save(game);
            
            logger.info("레벨 완료 및 다음 레벨 진행 - userId={}, 이전레벨={}, 새레벨={}", 
                userId, currentLevel, game.getCurrentLevel());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "축하합니다! 다음 레벨로 진행합니다.");
            response.put("new_level", game.getCurrentLevel());
            response.put("success", true);
            response.put("condition_met", true);
            response.put("is_final_level", false);
            
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
     * 게스트 사용자 생성 또는 조회 (라라벨과 동일한 방식)
     */
    private Long getOrCreateGuestUser(String guestId, HttpServletRequest request) {
        try {
            HttpSession session = request.getSession();
            
            // 세션에서 기존 게스트 ID 확인
            String existingGuestId = (String) session.getAttribute("guest_id");
            Long existingUserId = (Long) session.getAttribute("user_id");
            
            // 동일한 게스트 ID면 기존 사용자 ID 반환 (단, users 테이블에 존재하는지 확인)
            if (existingGuestId != null && existingGuestId.equals(guestId) && existingUserId != null) {
                // 기존 사용자 ID가 users 테이블에 존재하는지 확인
                Optional<User> existingUser = userRepository.findById(existingUserId);
                if (existingUser.isPresent()) {
                    System.out.println("기존 게스트 세션 사용: " + guestId + " -> " + existingUserId);
                    return existingUserId;
                } else {
                    System.out.println("기존 세션의 사용자 ID가 users 테이블에 없음, 새로 생성: " + existingUserId);
                    // 기존 세션의 사용자 ID가 users 테이블에 없으면 새로 생성
                }
            }
            
            // 라라벨과 동일한 방식: 게스트 사용자를 users 테이블에 생성/조회
            User guestUser = null;
            
            // String guestId를 UUID로 변환
            UUID guestUuid;
            try {
                guestUuid = UUID.fromString(guestId);
            } catch (IllegalArgumentException e) {
                System.err.println("잘못된 UUID 형식: " + guestId);
                // UUID가 아닌 경우 해시코드로 폴백
                guestUuid = UUID.nameUUIDFromBytes(guestId.getBytes());
            }
            
            // 1. guest_id로 기존 게스트 사용자 조회
            Optional<User> existingUser = userRepository.findByGuestId(guestUuid);
            if (existingUser.isPresent()) {
                guestUser = existingUser.get();
                System.out.println("기존 게스트 사용자 조회: " + guestId + " -> " + guestUser.getId());
            } else {
                // 2. 게스트 이메일로 조회
                String guestEmail = guestId + "@guest.local";
                Optional<User> emailUser = userRepository.findByEmailAndIsGuestTrue(guestEmail);
                if (emailUser.isPresent()) {
                    guestUser = emailUser.get();
                    System.out.println("기존 게스트 이메일 사용자 조회: " + guestEmail + " -> " + guestUser.getId());
                } else {
                    // 3. 새로운 게스트 사용자 생성
                    guestUser = new User();
                    guestUser.setName("게스트_" + guestId.substring(0, Math.min(8, guestId.length())));
                    guestUser.setEmail(guestEmail);
                    guestUser.setPassword("guest_password_" + System.currentTimeMillis()); // 임시 비밀번호
                    guestUser.setGuestId(guestUuid);
                    guestUser.setIsGuest(true);
                    guestUser.setIsAdmin(false);
                    
                    guestUser = userRepository.save(guestUser);
                    System.out.println("새 게스트 사용자 생성: " + guestId + " -> " + guestUser.getId());
                }
            }
            
            // 세션에 저장
            session.setAttribute("guest_id", guestId);
            session.setAttribute("user_id", guestUser.getId());
            
            return guestUser.getId();
            
        } catch (Exception e) {
            System.err.println("게스트 사용자 생성/조회 오류: " + e.getMessage());
            e.printStackTrace();
            
            // 오류 시 기존 방식으로 폴백
            Long userId = Math.abs((long) guestId.hashCode()) + 1000;
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
                
                // 데이터베이스에서 사용자 정보 조회
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    return user.getId(); // 실제 사용자 ID 반환
                } else {
                    // 사용자가 데이터베이스에 없는 경우 (게스트 등)
                    // 이메일 해시를 사용하여 일관된 ID 생성
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
                @SuppressWarnings("unchecked")
                Map<String, Object> template = (Map<String, Object>) puzzleData.get("template");
                
                if (template == null) {
                    System.out.println("템플릿 데이터가 없음 - 새 퍼즐 생성");
                    return createNewPuzzle(game, level);
                }
                
                System.out.println("기존 퍼즐 데이터 복원 성공");
                
                // 레벨 클리어 횟수 조회
                Long clearCount = puzzleGameRecordService.getClearCountByUserAndLevel(game.getUserId(), level.getLevel());
                
                Map<String, Object> result = new HashMap<>();
                result.put("template", template);
                result.put("level", level);
                result.put("game", game);
                result.put("game_state", gameState);
                result.put("answered_words_with_answers", gameState.get("answered_words_with_answers"));
                result.put("is_restored", true);
                result.put("clear_count", clearCount); // 레벨 클리어 횟수 추가
                
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
            // 퍼즐 그리드 템플릿에서 단어 추출 (레벨 기반)
            Map<String, Object> extractionResult = puzzleGridTemplateService.extractWordsFromTemplate(
                level.getWordDifficulty(), 
                level.getHintDifficulty(), 
                level.getLevel(), // levelId로 사용
                level.getIntersectionCount()
            );
            
            if (!(Boolean) extractionResult.get("success")) {
                throw new RuntimeException("퍼즐 생성에 실패했습니다.");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> template = (Map<String, Object>) extractionResult.get("template");
            if (template == null) {
                throw new RuntimeException("템플릿 데이터가 없습니다.");
            }
            
            // 퍼즐 데이터와 게임 상태 분리 저장
            Map<String, Object> puzzleData = new HashMap<>();
            puzzleData.put("template", template);
            puzzleData.put("extracted_words", extractionResult.get("extracted_words"));
            
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("answered_words", new ArrayList<>());
            gameState.put("answered_words_with_answers", new HashMap<>());
            gameState.put("wrong_answers", new ArrayList<>());
            gameState.put("hints_used", new ArrayList<>());
            
            // 활성 퍼즐 시작 (새 퍼즐이므로 정답/오답 카운트 초기화)
            game.startActivePuzzle(puzzleData, gameState);
            game.setCurrentLevelCorrectAnswers(0);
            game.setCurrentLevelWrongAnswers(0);
            userPuzzleGameService.save(game);
            
            System.out.println("새 퍼즐 생성 완료 - 레벨: " + level.getLevel() + ", current_level_correct_answers: " + game.getCurrentLevelCorrectAnswers() + " (새 퍼즐로 초기화)");
                
            // 레벨 클리어 횟수 조회
            Long clearCount = puzzleGameRecordService.getClearCountByUserAndLevel(game.getUserId(), level.getLevel());
            
            Map<String, Object> result = new HashMap<>();
            result.put("template", template);
            result.put("level", level);
            result.put("game", game);
            result.put("game_state", gameState);
            result.put("answered_words_with_answers", new HashMap<>());
            result.put("clear_count", clearCount); // 레벨 클리어 횟수 추가
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("퍼즐 생성 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 이미 맞춘 단어인지 확인
     */
    private boolean isWordAlreadyAnswered(UserPuzzleGame game, Long wordId) {
        try {
            Map<String, Object> gameState = game.getCurrentGameState();
            if (gameState != null) {
                @SuppressWarnings("unchecked")
                List<Long> answeredWords = (List<Long>) gameState.get("answered_words");
                if (answeredWords != null) {
                    return answeredWords.contains(wordId);
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("단어 답변 상태 확인 중 오류: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 정답 단어를 게임 상태에 추가
     */
    private void updateGameStateWithCorrectAnswer(UserPuzzleGame game, Long wordId, String answer) {
        try {
            Map<String, Object> gameState = game.getCurrentGameState();
            
            // 게임 상태가 없으면 초기화
            if (gameState == null) {
                gameState = new HashMap<>();
                gameState.put("answered_words", new ArrayList<Long>());
                gameState.put("answered_words_with_answers", new HashMap<String, String>());
                gameState.put("wrong_answers", new ArrayList<Long>());
                gameState.put("hints_used", new ArrayList<Long>());
                gameState.put("additional_hints", new ArrayList<Long>());
                gameState.put("started_at", java.time.LocalDateTime.now().toString());
            }
            
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
            
            // 퍼즐 데이터가 없으면 현재 게임의 퍼즐 데이터 유지 (테마별 퍼즐 포함)
            if (game.getCurrentPuzzleData() == null) {
                logger.warn("퍼즐 데이터가 없습니다. 게임 상태만 저장합니다. - gameId: {}", game.getId());
            } else {
                // 활성 퍼즐이 있는지 확인하고 없으면 설정
                if (!game.hasActivePuzzle()) {
                    game.setHasActivePuzzle(true);
                    game.setCurrentPuzzleStartedAt(java.time.LocalDateTime.now());
                }
            }
            
            // 게임 상태 업데이트
            game.setCurrentGameState(gameState);
            userPuzzleGameService.save(game);
            
            logger.info("게임 상태 업데이트 완료 - gameId: {}, wordId: {}, answeredWords: {}", 
                game.getId(), wordId, answeredWords.size());
            
        } catch (Exception e) {
            logger.error("게임 상태 업데이트 중 오류: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    /**
     * 현재 퍼즐의 단어와 힌트를 새로 생성 (템플릿은 유지)
     * 1. 사용자별 진행데이터에서 템플릿 정보만 추출
     * 2. 사용자별 데이터를 삭제
     * 3. 퍼즐 제작 로직 2~6번을 다시 실행
     * 4. 새로운 데이터를 사용자별 진행데이터에 저장
     */
    private void regenerateWordsAndHintsForCurrentPuzzle(UserPuzzleGame game) {
        try {
            logger.info("퍼즐 재생성 시작 - userId: {}, currentLevel: {}", game.getUserId(), game.getCurrentLevel());
            
            // 1. 현재 퍼즐 데이터에서 템플릿 정보만 추출
            Map<String, Object> currentPuzzleData = game.getCurrentPuzzleData();
            if (currentPuzzleData == null) {
                logger.error("현재 퍼즐 데이터가 없습니다 - userId: {}, currentLevel: {}", game.getUserId(), game.getCurrentLevel());
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> template = (Map<String, Object>) currentPuzzleData.get("template");
            if (template == null) {
                logger.error("템플릿 데이터가 없습니다 - userId: {}, currentLevel: {}", game.getUserId(), game.getCurrentLevel());
                return;
            }
            
            // 레벨 정보 가져오기
            PuzzleLevel level = puzzleLevelService.getById(game.getCurrentLevel().longValue());
            if (level == null) {
                logger.error("레벨 정보를 찾을 수 없습니다 - userId: {}, currentLevel: {}", game.getUserId(), game.getCurrentLevel());
                return;
            }
            
            logger.info("템플릿 정보 추출 완료 - templateId: {}, levelId: {}, wordDifficulty: {}, hintDifficulty: {}", 
                template.get("id"), level.getLevel(), level.getWordDifficulty(), level.getHintDifficulty());
            
            // 2. 사용자별 데이터를 삭제 (현재 퍼즐 종료)
            game.completeActivePuzzle();
            userPuzzleGameService.save(game);
            logger.info("기존 퍼즐 데이터 삭제 완료 - userId: {}", game.getUserId());
            
            // 3. 퍼즐 제작 로직 2~6번을 다시 실행 (템플릿 기반으로 단어 추출 → 교차점 처리 → 힌트 생성)
            logger.info("새 퍼즐 단어 추출 시작 - levelId: {}, wordDifficulty: {}, hintDifficulty: {}, intersectionCount: {}", 
                level.getLevel(), level.getWordDifficulty(), level.getHintDifficulty(), level.getIntersectionCount());
            
            Map<String, Object> extractionResult = puzzleGridTemplateService.extractWordsFromTemplate(
                level.getWordDifficulty(), 
                level.getHintDifficulty(), 
                level.getLevel(), // levelId로 사용
                level.getIntersectionCount()
            );
            
            if (!(Boolean) extractionResult.get("success")) {
                logger.error("퍼즐 재생성에 실패했습니다 - userId: {}, currentLevel: {}, message: {}", 
                    game.getUserId(), game.getCurrentLevel(), extractionResult.get("message"));
                return;
            }
            
            logger.info("새 퍼즐 단어 추출 성공 - userId: {}, extractedWordsCount: {}", 
                game.getUserId(), ((List<?>) extractionResult.get("extracted_words")).size());
            
            // 4. 새로운 데이터를 사용자별 진행데이터에 저장
            Map<String, Object> newPuzzleData = new HashMap<>();
            newPuzzleData.put("template", extractionResult.get("template"));
            newPuzzleData.put("extracted_words", extractionResult.get("extracted_words"));
            
            Map<String, Object> newGameState = new HashMap<>();
            newGameState.put("answered_words", new ArrayList<>());
            newGameState.put("answered_words_with_answers", new HashMap<>());
            newGameState.put("wrong_answers", new ArrayList<>());
            newGameState.put("hints_used", new ArrayList<>());
            
            // 새로운 활성 퍼즐 시작 (새 퍼즐이므로 정답/오답 카운트 초기화)
            game.startActivePuzzle(newPuzzleData, newGameState);
            game.setCurrentLevelCorrectAnswers(0);
            game.setCurrentLevelWrongAnswers(0);
            userPuzzleGameService.save(game);
            
            logger.info("새 퍼즐 시작 완료 - userId: {}, currentLevel: {}, correctAnswers: {}, wrongAnswers: {}", 
                game.getUserId(), game.getCurrentLevel(), game.getCurrentLevelCorrectAnswers(), game.getCurrentLevelWrongAnswers());
            
            logger.info("퍼즐 재생성 완료 - userId: {}, currentLevel: {}", game.getUserId(), game.getCurrentLevel());
            
        } catch (Exception e) {
            logger.error("퍼즐 재생성 중 오류 발생 - userId: {}, currentLevel: {}, error: {}", 
                game.getUserId(), game.getCurrentLevel(), e.getMessage(), e);
        }
    }
    
    /**
     * 단어 상세 정보 조회 (단어정제용)
     */
    @GetMapping("/word-detail")
    public ResponseEntity<Map<String, Object>> getWordDetail(@RequestParam Long word_id) {
        try {
            PzWord word = pzWordService.getById(word_id);
            if (word == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "단어를 찾을 수 없습니다."));
            }
            
            // 단어의 힌트 목록 조회
            List<PzHint> hints = pzHintService.getHintsByWordId(word_id.intValue());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("word", Map.of(
                "id", word.getId(),
                "word", word.getWord(),
                "category", word.getCategory(),
                "difficulty", word.getDifficulty(),
                "conf_yn", word.getConfYn(),
                "created_at", word.getCreatedAt()
            ));
            
            List<Map<String, Object>> hintsList = new ArrayList<>();
            for (PzHint hint : hints) {
                hintsList.add(Map.of(
                    "id", hint.getId(),
                    "hint_text", hint.getHintText(),
                    "difficulty", hint.getDifficulty(),
                    "is_primary", hint.getIsPrimary()
                ));
            }
            response.put("hints", hintsList);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("단어 상세 정보 조회 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 새 힌트 생성 (Gemini API 사용)
     */
    @PostMapping("/generate-hints")
    public ResponseEntity<Map<String, Object>> generateNewHints(@RequestBody Map<String, Object> requestData) {
        try {
            Long wordId = Long.valueOf(requestData.get("word_id").toString());
            String word = (String) requestData.get("word");
            String category = (String) requestData.get("category");
            
            if (wordId == null || word == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "필수 파라미터가 누락되었습니다."));
            }
            
            // Gemini API로 새 힌트 생성 (덮어쓰기 모드) - 임시 비활성화
            // Map<String, Object> geminiResult = hintGeneratorManagementService.generateForWord(wordId.intValue(), true);
            
            // if (!(Boolean) geminiResult.get("success")) {
            //     return ResponseEntity.badRequest().body(Map.of("success", false, "message", "힌트 생성에 실패했습니다."));
            // }
            
            // @SuppressWarnings("unchecked")
            // List<Map<String, Object>> generatedHints = (List<Map<String, Object>>) geminiResult.get("hints");
            
            // 생성된 힌트를 데이터베이스에 저장 - 임시 비활성화
            List<Map<String, Object>> savedHints = new ArrayList<>();
            // PzWord wordEntity = pzWordService.getById(wordId);
            // for (Map<String, Object> hintData : generatedHints) {
            //     PzHint hint = new PzHint();
            //     hint.setWord(wordEntity);
            //     hint.setHintText((String) hintData.get("hint_text"));
            //     hint.setDifficulty((Integer) hintData.get("difficulty"));
            //     hint.setIsPrimary(false);
            //     hint.setCreatedAt(java.time.LocalDateTime.now());
            //     hint.setUpdatedAt(java.time.LocalDateTime.now());
                
            //     PzHint savedHint = pzHintService.savePzHint(hint);
            //     savedHints.add(Map.of(
            //         "id", savedHint.getId(),
            //         "hint_text", savedHint.getHintText(),
            //         "difficulty", savedHint.getDifficulty(),
            //         "is_primary", savedHint.getIsPrimary()
            //     ));
            // }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "힌트 생성 기능이 임시로 비활성화되었습니다.");
            response.put("hints", savedHints);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("새 힌트 생성 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 단어정제 확정
     */
    @PostMapping("/refine-word")
    public ResponseEntity<Map<String, Object>> refineWord(@RequestBody Map<String, Object> refinementData) {
        try {
            Integer wordId = (Integer) refinementData.get("wordId");
            Integer difficulty = (Integer) refinementData.get("difficulty");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hints = (List<Map<String, Object>>) refinementData.get("hints");
            
            if (wordId == null || difficulty == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "필수 파라미터가 누락되었습니다."));
            }
            
            // 단어정제 실행
            boolean success = pzWordService.refineWord(wordId, difficulty, hints);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "단어정제가 완료되었습니다." : "정제 기능이 임시로 비활성화되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("단어정제 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 단어 비활성화 API
     */
    @PostMapping("/admin/api/words/deactivate/{wordId}")
    public ResponseEntity<Map<String, Object>> deactivateWord(@PathVariable Integer wordId) {
        try {
            pzWordService.deactivateWord(wordId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어가 성공적으로 비활성화되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("단어 비활성화 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "단어 비활성화 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 오답 통계 조회 API
     */
    @GetMapping("/wrong-answers/statistics")
    public ResponseEntity<Map<String, Object>> getWrongAnswerStatistics() {
        try {
            Map<String, Object> statistics = userWrongAnswerService.getWrongAnswerStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("오답 통계 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "오답 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 오답으로 자주 등록된 단어들 조회 API
     */
    @GetMapping("/wrong-answers/most-wrong")
    public ResponseEntity<List<Object[]>> getMostWrongAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            List<Object[]> mostWrongAnswers = userWrongAnswerService.getMostWrongAnswers(pageable);
            return ResponseEntity.ok(mostWrongAnswers);
        } catch (Exception e) {
            logger.error("자주 틀린 단어 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }
    
    /**
     * 특정 사용자의 오답 기록 조회 API
     */
    @GetMapping("/wrong-answers/user/{userId}")
    public ResponseEntity<List<com.example.board.entity.UserWrongAnswer>> getUserWrongAnswers(@PathVariable Long userId) {
        try {
            List<com.example.board.entity.UserWrongAnswer> wrongAnswers = 
                userWrongAnswerService.getUserWrongAnswers(userId);
            return ResponseEntity.ok(wrongAnswers);
        } catch (Exception e) {
            logger.error("사용자 오답 기록 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * 오답 초과 시 모든 정답 반환 (보안: wrong_count_exceeded 상태에서만 접근 가능)
     */
    @PostMapping("/get-all-answers")
    public ResponseEntity<Map<String, Object>> getAllAnswers(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            System.out.println("=== get-all-answers API 호출 ===");
            System.out.println("requestBody: " + requestBody);
            
            // 1. 사용자 ID 추출 (로그인 사용자 또는 게스트)
            String guestId = (String) requestBody.get("guestId");
            String authHeader = request.getHeader("Authorization");
            
            System.out.println("guestId: " + guestId);
            System.out.println("authHeader: " + authHeader);
            
            String userId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // 로그인 사용자
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && 
                    !authentication.getPrincipal().equals("anonymousUser")) {
                    String email = authentication.getName();
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        userId = userOpt.get().getId().toString();
                    }
                }
            } else if (guestId != null) {
                // 게스트 사용자
                userId = guestId;
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "사용자 정보를 찾을 수 없습니다."));
            }
            
            // 2. 현재 활성 게임 조회 (로그인 사용자 또는 게스트)
            Optional<UserPuzzleGame> gameOpt;
            
            if (userId.startsWith("guest_")) {
                // 게스트 사용자: getOrCreateGuestUser 호출하여 users 테이블에서 조회/생성
                Long guestUserId = getOrCreateGuestUser(userId, request);
                userId = guestUserId.toString(); // userId를 실제 users 테이블의 ID로 업데이트
                gameOpt = userPuzzleGameService.findActiveGameByUserIdOrGuestId(guestUserId, null);
                System.out.println("게스트 사용자로 조회: " + userId + " -> " + guestUserId);
            } else {
                // 일반 사용자 ID (숫자)
                try {
                    Long userIdLong = Long.parseLong(userId);
                    gameOpt = userPuzzleGameService.findActiveGameByUserIdOrGuestId(userIdLong, null);
                    System.out.println("일반 사용자로 조회: " + userIdLong);
                } catch (NumberFormatException ex) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "잘못된 사용자 ID 형식입니다."));
                }
            }
            
            if (!gameOpt.isPresent()) {
                System.out.println("활성 게임을 찾을 수 없습니다. userId: " + userId);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "활성 게임을 찾을 수 없습니다."));
            }
            
            UserPuzzleGame game = gameOpt.get();
            
            // 3. 퍼즐 데이터 확인
            Map<String, Object> puzzleData = game.getCurrentPuzzleData();
            if (puzzleData == null) {
                System.out.println("퍼즐 데이터를 찾을 수 없습니다. game: " + game);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "퍼즐 데이터를 찾을 수 없습니다."));
            }
            
            // 4. extracted_words에서 모든 단어 정보 추출
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> extractedWords = (List<Map<String, Object>>) puzzleData.get("extracted_words");
            if (extractedWords == null || extractedWords.isEmpty()) {
                System.out.println("추출된 단어를 찾을 수 없습니다. puzzleData: " + puzzleData);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "추출된 단어를 찾을 수 없습니다."));
            }
            
            // 5. 정답 목록 생성
            List<Map<String, Object>> answers = new ArrayList<>();
            for (int i = 0; i < extractedWords.size(); i++) {
                Map<String, Object> word = extractedWords.get(i);
                Map<String, Object> answer = new HashMap<>();
                answer.put("word_id", word.get("word_id"));
                answer.put("word", word.get("word"));
                answer.put("position", (i + 1) + "번");
                answers.add(answer);
            }
            
            System.out.println("정답 목록 생성 완료: " + answers.size() + "개");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "answers", answers
            ));
            
        } catch (Exception e) {
            System.err.println("모든 정답 조회 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }
}