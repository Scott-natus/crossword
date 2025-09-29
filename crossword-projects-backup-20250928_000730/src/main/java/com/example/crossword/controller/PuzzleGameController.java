package com.example.crossword.controller;

import com.example.crossword.entity.PuzzleLevel;
import com.example.crossword.service.PuzzleLevelService;
import com.example.crossword.service.WordService;
import com.example.crossword.service.HintService;
import com.example.crossword.service.PuzzleGridTemplateService;
import com.example.crossword.service.PzWordService;
import com.example.crossword.service.PzHintService;
import com.example.crossword.service.UserPuzzleGameService;
import com.example.crossword.entity.UserPuzzleGame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * 퍼즐게임 API 컨트롤러
 * Laravel의 PuzzleGameController를 Java로 포팅
 * 퍼즐 템플릿 로드, 정답 확인, 힌트 제공 등의 기능을 제공
 */
@RestController
@RequestMapping("/api/puzzle-game")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class PuzzleGameController {
    
    /**
     * 퍼즐 게임 메인 페이지 (라라벨과 동일)
     */
    @GetMapping("/")
    public ResponseEntity<String> index(jakarta.servlet.http.HttpServletRequest request) {
        try {
            // 라라벨과 동일: $user = Auth::user(); $guestId = $request->query('guest_id') ?? session('guest_id');
            String authHeader = request.getHeader("Authorization");
            String guestId = request.getParameter("guest_id");
            
            // 세션에서 게스트 ID 확인
            if (guestId == null || guestId.isEmpty()) {
                jakarta.servlet.http.HttpSession session = request.getSession(false);
                if (session != null) {
                    guestId = (String) session.getAttribute("guest_id");
                }
            }
            
            // 라라벨과 동일: if (!$user && !$guestId) { return view('puzzle.game.guest'); }
            if ((authHeader == null || !authHeader.startsWith("Bearer ")) && (guestId == null || guestId.isEmpty())) {
                // 로그인/게스트 ID 모두 없으면 게스트 안내 페이지 표시
                return ResponseEntity.status(302)
                        .header("Location", "/K-CrossWord/")
                        .build();
            }
            
            // 라라벨과 동일: 게임 데이터 조회/생성 후 게임 페이지 표시
            String userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(302)
                        .header("Location", "/K-CrossWord/")
                        .build();
            }
            
            // 라라벨과 동일: return view('puzzle.game.index', compact('game', 'level', 'clearCount'));
            return ResponseEntity.status(302)
                    .header("Location", "/K-CrossWord/game.html?guest_id=" + guestId)
                    .build();
                    
        } catch (Exception e) {
            log.error("퍼즐 게임 메인 페이지 오류: {}", e.getMessage());
            return ResponseEntity.status(302)
                    .header("Location", "/K-CrossWord/")
                    .build();
        }
    }
    
    private final PuzzleLevelService puzzleLevelService;
    private final WordService wordService;
    private final HintService hintService;
    private final PuzzleGridTemplateService templateService;
    private final PzWordService pzWordService;
    private final PzHintService pzHintService;
    private final UserPuzzleGameService userPuzzleGameService;
    // 메모리 기반 게임 상태 저장소 (Redis 대신 사용)
    private final Map<String, Map<String, Object>> gameStateStore = new ConcurrentHashMap<>();
    
    /**
     * 퍼즐 템플릿 로드 (Laravel의 getTemplate 기능을 정확히 똑같이 포팅)
     */
    @GetMapping("/template")
    public ResponseEntity<Map<String, Object>> getTemplate(
            @RequestParam(required = false) String guestId, HttpServletRequest request) {
        
        log.info("getTemplate 진입: guestId={}", guestId);
        
        try {
            // 사용자 인증 처리 (라라벨과 동일한 세션 기반)
            String userId = getUserIdFromRequest(request);
            if (userId == null) {
                log.warn("getTemplate: 로그인/게스트 ID 모두 없음");
                Map<String, Object> response = new HashMap<>();
                response.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            log.info("getTemplate: 사용자 ID {}", userId);
            
            // 라라벨과 동일: 게임 데이터 조회/생성 (user_id 기준)
            // $game = \App\Models\UserPuzzleGame::where('user_id', $userId)->where('is_active', true)->first();
            Long userIdLong = Long.parseLong(userId);
            Optional<UserPuzzleGame> gameOpt = userPuzzleGameService.findActiveGameByUserId(userIdLong);
            
            UserPuzzleGame game;
            if (gameOpt.isPresent()) {
                game = gameOpt.get();
            } else {
                // 라라벨과 동일: 새로운 게임 생성
                // $game = \App\Models\UserPuzzleGame::create([...]);
                game = new UserPuzzleGame();
                game.setUserId(userIdLong);
                game.setCurrentLevel(1);
                game.setIsActive(true);
                game.setFirstAttemptAt(java.time.LocalDateTime.now());
                game = userPuzzleGameService.save(game);
            }
            
            // 라라벨과 동일: 현재 레벨 조회
            Integer currentLevel = game.getCurrentLevel();
            Optional<PuzzleLevel> levelOpt = puzzleLevelService.getPuzzleLevelByLevel(currentLevel);
            if (levelOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "레벨을 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(response);
            }
            
            PuzzleLevel level = levelOpt.get();
            
            // 라라벨과 동일: 현재 활성 퍼즐이 있는지 확인
            if (game.hasActivePuzzle()) {
                log.info("기존 퍼즐 세션 복원: userId={}, level={}", userId, currentLevel);
                
                // 라라벨과 동일: 저장된 퍼즐 데이터와 게임 상태 반환
                Map<String, Object> puzzleData = game.getCurrentPuzzleData();
                Map<String, Object> gameState = game.getCurrentGameState();
                
                if (puzzleData == null || gameState == null) {
                    log.warn("퍼즐 데이터 또는 게임 상태가 없음, 새 퍼즐 생성: userId={}", userId);
                    // 기존 데이터가 손상된 경우 새 퍼즐 생성
                } else {
                    // 정답 단어 정보 추가 (보안을 위해 맞춘 단어만)
                    Map<String, String> answeredWordsWithAnswers = new HashMap<>();
                    if (gameState.containsKey("answered_words")) {
                        @SuppressWarnings("unchecked")
                        List<Integer> answeredWords = (List<Integer>) gameState.get("answered_words");
                        for (Integer wordId : answeredWords) {
                            Optional<com.example.crossword.entity.PzWord> wordOpt = pzWordService.getPzWordById(wordId);
                            if (wordOpt.isPresent()) {
                                answeredWordsWithAnswers.put(String.valueOf(wordId), wordOpt.get().getWord());
                            }
                        }
                    }
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("template", puzzleData.get("template"));
                    response.put("level", level);
                    response.put("game", game);
                    response.put("game_state", gameState);
                    response.put("answered_words_with_answers", answeredWordsWithAnswers);
                    response.put("is_restored", true);
                    
                    log.info("게임 상태 복원 완료: userId={}, answeredWords={}, correctCount={}, wrongCount={}", 
                            userId, answeredWordsWithAnswers.size(), 
                            gameState.getOrDefault("correct_count", 0),
                            gameState.getOrDefault("wrong_count", 0));
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            // 새로운 퍼즐 생성
            log.info("새로운 퍼즐 세션 시작: userId={}, level={}", userId, currentLevel);
            
            // 레벨에 해당하는 템플릿 중 랜덤으로 하나 선택
            List<Map<String, Object>> templates = templateService.getTemplatesByLevelId(level.getId().intValue());
            if (templates.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "해당 레벨의 템플릿을 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(response);
            }
            
            // 랜덤으로 템플릿 선택
            Map<String, Object> template = templates.get((int) (Math.random() * templates.size()));
            Integer templateId = ((Number) template.get("id")).intValue();
            
            // 5회 반복 로직으로 단어 추출 시도
            int maxRetries = 5;
            int retryCount = 0;
            Map<String, Object> extractData = null;
            
            while (retryCount < maxRetries) {
                retryCount++;
                
                // 단어 추출
                extractData = templateService.extractWordsFromTemplate(template);
                
                // 성공하면 루프 종료
                if ((Boolean) extractData.get("success")) {
                    log.info("게임 단어 추출 성공 - 시도 #{}에서 완료: templateId={}, levelId={}, userId={}", 
                            retryCount, templateId, level.getId(), userId);
                    break;
                }
                
                log.info("게임 단어 추출 실패 - 시도 #{}: templateId={}, levelId={}, userId={}, error={}", 
                        retryCount, templateId, level.getId(), userId, extractData.get("message"));
                
                // 마지막 시도가 아니면 잠시 대기 후 재시도
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(100); // 0.1초 대기
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            // 5회 시도 후에도 실패한 경우
            if (!(Boolean) extractData.get("success")) {
                log.error("게임 단어 추출 최종 실패 - 5회 시도 후 실패: templateId={}, levelId={}, userId={}, finalError={}", 
                        templateId, level.getId(), userId, extractData.get("message"));
                
                Map<String, Object> response = new HashMap<>();
                response.put("error", "단어 추출에 실패했습니다. 잠시 후 다시 시도해주세요.");
                response.put("retry_count", retryCount);
                response.put("message", extractData.get("message"));
                return ResponseEntity.status(500).body(response);
            }
            
            // 단어 정보에 실제 pz_words ID 추가 (보안을 위해 단어 텍스트는 제거)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> wordOrder = (List<Map<String, Object>>) 
                    ((Map<String, Object>) extractData.get("extracted_words")).get("word_order");
            
            List<Map<String, Object>> wordsWithIds = new ArrayList<>();
            for (Map<String, Object> wordInfo : wordOrder) {
                // extractWordsFromTemplate에서 이미 pz_word_id와 hint를 제공하므로 직접 사용
                Map<String, Object> secureWordInfo = new HashMap<>();
                
                // pz_word_id가 있으면 해당 정보 사용
                Object pzWordIdObj = wordInfo.get("pz_word_id");
                if (pzWordIdObj != null) {
                    Integer pzWordId = (Integer) pzWordIdObj;
                    secureWordInfo.put("pz_word_id", pzWordId);
                    
                    // 카테고리 정보 조회
                    Optional<com.example.crossword.entity.PzWord> pzWordOpt = pzWordService.getPzWordById(pzWordId);
                    if (pzWordOpt.isPresent()) {
                        secureWordInfo.put("category", pzWordOpt.get().getCategory() != null ? pzWordOpt.get().getCategory() : "일반");
                    } else {
                        secureWordInfo.put("category", "일반");
                    }
                    
                    // 힌트 정보는 extractWordsFromTemplate에서 이미 제공됨
                    secureWordInfo.put("hint", wordInfo.get("hint"));
                    
                    // hint_id는 별도로 조회 (필요한 경우)
                    List<com.example.crossword.entity.PzHint> hints = pzHintService.getPrimaryHintsByWordId(pzWordId);
                    if (!hints.isEmpty()) {
                        secureWordInfo.put("hint_id", hints.get(0).getId());
                    } else {
                        secureWordInfo.put("hint_id", null);
                    }
                } else {
                    secureWordInfo.put("pz_word_id", null);
                    secureWordInfo.put("category", "일반");
                    secureWordInfo.put("hint_id", null);
                    secureWordInfo.put("hint", wordInfo.get("hint")); // extractWordsFromTemplate에서 제공된 힌트 사용
                }
                
                secureWordInfo.put("position", wordInfo.get("position"));
                secureWordInfo.put("word_id", wordInfo.get("word_id")); // 그리드 내 단어 ID
                
                wordsWithIds.add(secureWordInfo);
            }
            
            // 퍼즐 데이터 구성
            @SuppressWarnings("unchecked")
            Map<String, Object> gridInfo = (Map<String, Object>) 
                    ((Map<String, Object>) extractData.get("extracted_words")).get("grid_info");
            
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("id", template.get("id"));
            templateData.put("template_name", template.get("template_name"));
            templateData.put("grid_pattern", gridInfo.get("pattern"));
            templateData.put("grid_width", gridInfo.get("width"));
            templateData.put("grid_height", gridInfo.get("height"));
            templateData.put("words", wordsWithIds);
            
            Map<String, Object> puzzleData = new HashMap<>();
            puzzleData.put("template", templateData);
            // level 객체도 LocalDateTime 때문에 JSON 변환 실패하므로 제거
            // puzzleData.put("level", level);
            // game 객체는 LocalDateTime 때문에 JSON 변환 실패하므로 제거
            // puzzleData.put("game", game);
            
            // 새로운 퍼즐 세션 시작 (데이터베이스에 저장)
            Map<String, Object> initialGameState = new HashMap<>();
            initialGameState.put("correct_count", 0);
            initialGameState.put("wrong_count", 0);
            initialGameState.put("clear_count", 0);
            initialGameState.put("answered_words", new ArrayList<Integer>());
            initialGameState.put("wrong_answers", new HashMap<String, Integer>());
            initialGameState.put("hints_used", new HashMap<String, Boolean>());
            
            userPuzzleGameService.startActivePuzzle(game, puzzleData, initialGameState);
            
            Map<String, Object> response = new HashMap<>();
            response.put("template", templateData);
            response.put("level", level);
            response.put("game", game);
            response.put("is_restored", false);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("getTemplate 중 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 정답 확인 (Laravel의 submitAnswer 기능 포팅)
     */
    @PostMapping("/check-answer")
    public ResponseEntity<Map<String, Object>> checkAnswer(
            @RequestParam Integer wordId,
            @RequestParam String answer,
            @RequestParam(required = false) String guestId,
            HttpServletRequest request) {
        
        log.info("checkAnswer 진입: wordId={}, answer={}, guestId={}", wordId, answer, guestId);
        
        try {
            // 사용자 인증 처리 (라라벨과 동일한 세션 기반)
            String userId = getUserIdFromRequest(request);
            if (userId == null) {
                log.warn("checkAnswer: 로그인/게스트 ID 모두 없음");
                Map<String, Object> response = new HashMap<>();
                response.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            log.info("checkAnswer: 사용자 ID {}", userId);
            
            // 게임 상태 조회
            Map<String, Object> gameState = getGameState(userId);
            if (gameState == null) {
                log.warn("checkAnswer: 게임을 찾을 수 없음, userId={}", userId);
                Map<String, Object> response = new HashMap<>();
                response.put("error", "게임을 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(response);
            }
            
            // 단어 조회
            Optional<com.example.crossword.entity.PzWord> wordOpt = pzWordService.getPzWordById(wordId);
            if (wordOpt.isEmpty()) {
                log.warn("checkAnswer: 단어를 찾을 수 없음, wordId={}", wordId);
                Map<String, Object> response = new HashMap<>();
                response.put("error", "단어를 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(response);
            }
            
            com.example.crossword.entity.PzWord word = wordOpt.get();
            
            // 정답 확인 (대소문자 무시)
            String userAnswer = answer.trim().toLowerCase();
            String correctAnswer = word.getWord().trim().toLowerCase();
            boolean isCorrect = userAnswer.equals(correctAnswer);
            
            log.info("checkAnswer 정답 비교: userAnswer={}, correctAnswer={}, isCorrect={}", 
                    userAnswer, correctAnswer, isCorrect);
            
            // Laravel과 동일한 게임 상태 관리 로직
            gameState = getGameState(userId);
            if (gameState == null) {
                gameState = new HashMap<>();
                gameState.put("correct_count", 0);
                gameState.put("wrong_count", 0);
            }
            
            int correctCount = (Integer) gameState.getOrDefault("correct_count", 0);
            int wrongCount = (Integer) gameState.getOrDefault("wrong_count", 0);
            
            String message;
            
            if (isCorrect) {
                // 정답 처리 (Laravel과 동일)
                correctCount++;
                gameState.put("correct_count", correctCount);
                message = "정답입니다!";
                
                // 정답된 단어 ID 저장 (게임 상태 복원용)
                @SuppressWarnings("unchecked")
                List<Integer> answeredWords = (List<Integer>) gameState.getOrDefault("answered_words", new ArrayList<>());
                if (!answeredWords.contains(wordId)) {
                    answeredWords.add(wordId);
                    gameState.put("answered_words", answeredWords);
                }
                
                log.info("정답 처리: wordId={}, correctCount={}, answeredWords={}", wordId, correctCount, answeredWords.size());
            } else {
                // 오답 처리 (Laravel과 동일)
                wrongCount++;
                gameState.put("wrong_count", wrongCount);
                
                // 틀린 답변 기록
                recordWrongAnswer(userId, wordId, answer, word.getWord(), word.getCategory(), getCurrentLevel(userId));
                
                // 오답 4회일 때 특별한 메시지
                if (wrongCount == 4) {
                    message = "현재 오답이 4회 입니다, 5회 오답시 레벨을 재시작합니다";
                } else {
                    message = "오답입니다. 누적 오답: " + wrongCount + "회";
                }
                
                log.info("오답 처리: wordId={}, wrongCount={}", wordId, wrongCount);
                
                // 오답 5회 초과 체크 (Laravel과 동일)
                if (wrongCount >= 5) {
                    // 게임 상태 초기화
                    gameState.put("correct_count", 0);
                    gameState.put("wrong_count", 0);
                    gameState.put("answered_words", new ArrayList<Integer>());
                    gameState.put("wrong_answers", new HashMap<String, Integer>());
                    gameState.put("hints_used", new HashMap<String, Boolean>());
                    saveGameState(userId, gameState);
                    
                    // 현재 퍼즐 세션 종료 (Laravel의 endCurrentPuzzle()과 동일)
                    endCurrentPuzzle(userId);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("is_correct", false);
                    response.put("message", "오답회수가 초과했습니다, 레벨을 다시 시작합니다.");
                    response.put("restart_level", true);
                    response.put("wrong_count_exceeded", true);
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            // 게임 상태 저장
            saveGameState(userId, gameState);
            
            Map<String, Object> response = new HashMap<>();
            response.put("is_correct", isCorrect);
            response.put("message", message);
            response.put("correct_answer", isCorrect ? word.getWord() : null); // 정답일 때만 정답 전송
            response.put("correct_count", correctCount);
            response.put("wrong_count", wrongCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("checkAnswer 예외 발생: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "서버 오류");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 힌트 제공 (Laravel의 getPuzzleHints 기능 포팅)
     */
    @GetMapping("/get-hints")
    public ResponseEntity<Map<String, Object>> getHints(
            @RequestParam Integer wordId,
            @RequestParam(required = false) Integer currentHintId,
            @RequestParam(required = false) Integer baseHintId,
            @RequestParam(required = false) String guestId,
            @RequestParam(required = false) Boolean isPrimary,
            @RequestParam(required = false) String isAdditionalHint,
            HttpServletRequest request) {
        
        log.debug("힌트 제공: 단어 ID {}, 현재 힌트 ID {}, 기본 힌트 ID {}, 추가힌트 여부 {}", wordId, currentHintId, baseHintId, isAdditionalHint);
        
        try {
            // Laravel과 동일한 로직: 기본 힌트(base_hint_id) 제외하고 난이도 순서로 힌트 선택 (쉬운 것부터)
            List<com.example.crossword.entity.PzHint> hints;
            
            if (isPrimary != null && isPrimary) {
                // 기본 힌트 요청인 경우
                hints = pzHintService.getHintsByWordIdAndIsPrimary(wordId, true);
                log.debug("기본 힌트 조회: {}개", hints.size());
            } else if ("true".equals(isAdditionalHint)) {
                // 추가힌트 요청인 경우: primary=false이고 id가 작은 힌트를 가져옴
                hints = pzHintService.getHintsByWordIdAndIsPrimary(wordId, false);
                log.debug("추가힌트 조회 (primary=false): {}개", hints.size());
                
                // ID가 작은 순서로 정렬 (가장 오래된 힌트부터)
                hints.sort(Comparator.comparing(com.example.crossword.entity.PzHint::getId));
            } else if (baseHintId != null) {
                // 기본 힌트 ID가 있으면 해당 힌트를 제외하고 조회 (추가 힌트 요청)
                hints = pzHintService.getHintsByWordIdExcludingHintId(wordId, baseHintId);
                log.debug("기본 힌트 ID {} 제외하고 추가 힌트 조회: {}개", baseHintId, hints.size());
            } else {
                // 기본 힌트 ID가 없으면 is_primary=false인 힌트들만 조회
                hints = pzHintService.getHintsByWordIdAndIsPrimary(wordId, false);
                log.debug("기본 힌트 ID 없이 추가 힌트 조회: {}개", hints.size());
            }
            
            if (hints.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("hint", null);
                response.put("message", "더 이상 사용할 수 있는 힌트가 없습니다.");
                return ResponseEntity.ok(response);
            }
            
            // 추가힌트가 아닌 경우에만 난이도 순서로 정렬 (쉬운 것부터)
            if (!"true".equals(isAdditionalHint)) {
                hints.sort(Comparator.comparing(com.example.crossword.entity.PzHint::getDifficulty));
            }
            
            // 첫 번째 힌트 반환 (Laravel과 동일한 구조)
            com.example.crossword.entity.PzHint hint = hints.get(0);
            
            // 힌트 사용 기록 저장
            String userId = getUserIdFromRequest(request);
            if (userId != null) {
                recordHintUsage(userId, wordId, hint.getId(), hint.getHintText());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hints", Arrays.asList(hint.getHintText())); // Laravel과 동일한 배열 형태
            response.put("hint_id", hint.getId()); // 힌트 ID 추가
            response.put("message", isPrimary != null && isPrimary ? "기본 힌트를 제공합니다." : "추가 힌트를 제공합니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 제공 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "힌트를 불러올 수 없습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 레벨 완료 처리 (Laravel의 completeLevel 기능 포팅)
     */
    @PostMapping("/complete-level")
    public ResponseEntity<Map<String, Object>> completeLevel(
            @RequestParam(defaultValue = "0") Integer score,
            @RequestParam(defaultValue = "0") Integer playTime,
            @RequestParam(defaultValue = "0") Integer hintsUsed,
            @RequestParam(defaultValue = "0") Integer wordsFound,
            @RequestParam(defaultValue = "0") Integer totalWords,
            @RequestParam(defaultValue = "0") Integer accuracy,
            @RequestParam(required = false) String guestId,
            HttpServletRequest request) {
        
        log.debug("레벨 완료 처리: 점수 {}, 플레이 시간 {}, 힌트 사용 {}, 단어 찾음 {}/{}, 정확도 {}%", 
                 score, playTime, hintsUsed, wordsFound, totalWords, accuracy);
        
        try {
            // 사용자 인증 처리
            String userId = getUserIdFromRequest(request);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            // 현재 레벨 조회
            Integer currentLevel = getCurrentLevel(userId);
            Optional<PuzzleLevel> levelOpt = puzzleLevelService.getPuzzleLevelByLevel(currentLevel);
            if (levelOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "레벨을 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(response);
            }
            
            PuzzleLevel level = levelOpt.get();
            Integer clearCondition = level.getClearCondition();
            
            // 클리어 조건 확인 (Laravel과 동일한 로직)
            if (wordsFound >= clearCondition) {
                // 클리어 조건 충족: 다음 레벨로 진행
                Integer nextLevel = currentLevel + 1;
                
                // 다음 레벨 존재 여부 확인
                Optional<PuzzleLevel> nextLevelOpt = puzzleLevelService.getPuzzleLevelByLevel(nextLevel);
                if (nextLevelOpt.isEmpty()) {
                    // 다음 레벨이 없으면 현재 레벨 유지
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "축하합니다! 모든 레벨을 완료했습니다!");
                    response.put("new_level", currentLevel);
                    response.put("success", true);
                    response.put("condition_met", true);
                    response.put("is_final_level", true);
                    
                    return ResponseEntity.ok(response);
                }
                
                // 게임 상태 업데이트: 다음 레벨로 진행
                updateUserLevel(userId, nextLevel);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "축하합니다! 다음 레벨로 진행합니다.");
                response.put("new_level", nextLevel);
                response.put("success", true);
                response.put("condition_met", true);
                response.put("is_final_level", false);
                
                log.info("레벨 완료: userId={}, currentLevel={}, nextLevel={}, wordsFound={}, clearCondition={}", 
                        userId, currentLevel, nextLevel, wordsFound, clearCondition);
                
                return ResponseEntity.ok(response);
                
            } else {
                // 클리어 조건 미달: 새 퍼즐로 재도전
                Map<String, Object> response = new HashMap<>();
                response.put("message", String.format("아직 클리어 조건을 충족하지 못했습니다. (필요: %d개, 현재: %d개)", 
                        clearCondition, wordsFound));
                response.put("new_level", currentLevel); // 현재 레벨 유지
                response.put("success", true);
                response.put("condition_met", false);
                response.put("condition_not_met", true);
                response.put("required_words", clearCondition);
                response.put("current_words", wordsFound);
                
                log.info("클리어 조건 미달: userId={}, level={}, wordsFound={}, clearCondition={}", 
                        userId, currentLevel, wordsFound, clearCondition);
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("레벨 완료 처리 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 클리어 조건 조회 (Laravel의 getClearCondition 기능 포팅)
     */
    @GetMapping("/get-clear-condition")
    public ResponseEntity<Map<String, Object>> getClearCondition(
            @RequestParam(defaultValue = "1") Integer level,
            @RequestParam(required = false) String guestId) {
        
        log.debug("클리어 조건 조회: 레벨 {}, 게스트 ID {}", level, guestId);
        
        try {
            Optional<PuzzleLevel> puzzleLevelOpt = puzzleLevelService.getPuzzleLevelByLevel(level);
            if (puzzleLevelOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "레벨을 찾을 수 없습니다: " + level);
                return ResponseEntity.badRequest().body(response);
            }
            
            PuzzleLevel puzzleLevel = puzzleLevelOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("clear_count", 0); // 임시로 0으로 설정
            response.put("clear_condition", puzzleLevel.getClearCondition());
            response.put("current_level", level);
            response.put("level_name", puzzleLevel.getLevelName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("클리어 조건 조회 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 게임오버 처리 (Laravel의 gameOver 기능 포팅)
     */
    @PostMapping("/game-over")
    public ResponseEntity<Map<String, Object>> gameOver(
            @RequestParam(required = false) String guestId,
            HttpServletRequest request) {
        
        log.info("게임오버 처리: guestId={}", guestId);
        
        try {
            // 사용자 인증 처리 (게스트 ID 또는 로그인 사용자)
            String userId = getUserIdFromRequest(request);
            if (userId == null) {
                log.warn("게임오버: 로그인/게스트 ID 모두 없음");
                Map<String, Object> response = new HashMap<>();
                response.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            log.info("게임오버: 사용자 ID {}", userId);
            
            // 게스트 ID를 UUID로 변환
            UUID uuidGuestId = null;
            Long userIdLong = null;
            
            try {
                // UUID 형식인지 확인하고 변환
                uuidGuestId = UUID.fromString(userId);
                userIdLong = null; // 게스트는 null로 설정
            } catch (IllegalArgumentException e) {
                // UUID가 아닌 경우 로그인 사용자로 처리
                try {
                    userIdLong = Long.parseLong(userId);
                } catch (NumberFormatException ex) {
                    log.error("잘못된 사용자 ID 형식: {}", userId);
                    Map<String, Object> response = new HashMap<>();
                    response.put("error", "잘못된 사용자 ID입니다.");
                    return ResponseEntity.status(400).body(response);
                }
            }
            
            // 활성 게임 조회
            Optional<UserPuzzleGame> gameOpt = userPuzzleGameService.findActiveGameByUserIdOrGuestId(userIdLong, uuidGuestId);
            if (gameOpt.isPresent()) {
                UserPuzzleGame game = gameOpt.get();
                
                // 마지막 플레이 시간 업데이트 (Laravel과 동일)
                game.updateLastPlayedAt();
                userPuzzleGameService.save(game);
                
                log.info("게임오버 처리 완료: userId={}", userId);
            } else {
                log.warn("활성 게임을 찾을 수 없음: userId={}", userId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "게임오버! 5분 후 재시도 가능합니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("게임오버 처리 중 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 게임 상태 초기화 (새 퍼즐 시작 시)
     */
    @PostMapping("/reset-game-state")
    public ResponseEntity<Map<String, Object>> resetGameState(
            @RequestParam(required = false) String guestId,
            HttpServletRequest request) {
        
        log.debug("게임 상태 초기화: 게스트 ID {}", guestId);
        
        try {
            // 사용자 인증 처리
            String userId = getUserIdFromRequest(request);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            // 게임 상태 초기화 (Redis에서 게임 상태 삭제)
            String gameKey = "game:" + userId;
            gameStateStore.remove(gameKey);
            log.info("게임 상태 초기화 완료: userId={}", userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "게임 상태가 초기화되었습니다.");
            
            log.info("게임 상태 초기화 완료: userId={}", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("게임 상태 초기화 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 정답보기 (관리자 전용) - Laravel과 동일한 로직
     */
    @GetMapping("/show-answer")
    public ResponseEntity<Map<String, Object>> showAnswer(
            @RequestParam Integer wordId,
            @RequestParam(required = false) String guestId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.debug("정답보기: 단어 ID {}, 게스트 ID {}", wordId, guestId);
        
        try {
            // 관리자 권한 확인
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "인증이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            String token = authHeader.substring(7);
            // 토큰에서 사용자 정보 추출 (실제로는 JWT 파싱 필요)
            // 임시로 토큰에 admin이 포함되어 있으면 관리자로 간주
            boolean isAdmin = token.contains("admin");
            
            if (!isAdmin) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "관리자만 접근 가능합니다.");
                return ResponseEntity.status(403).body(response);
            }
            
            Optional<com.example.crossword.entity.PzWord> wordOpt = pzWordService.getPzWordById(wordId);
            if (wordOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "단어를 찾을 수 없습니다: " + wordId);
                return ResponseEntity.badRequest().body(response);
            }
            
            com.example.crossword.entity.PzWord word = wordOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("answer", word.getWord()); // Laravel과 동일한 응답 구조
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("정답보기 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 퍼즐 생성 (JavaScript에서 호출하는 엔드포인트)
     */
    @PostMapping("/puzzles/generate/{level}")
    @GetMapping("/puzzles/generate/{level}")
    public ResponseEntity<Map<String, Object>> generatePuzzle(@PathVariable Integer level) {
        log.info("=== 퍼즐 생성 시작 ===");
        log.info("요청된 레벨: {}", level);
        
        try {
            // 레벨 존재 여부 확인
            log.info("레벨 존재 여부 확인 중...");
            Optional<PuzzleLevel> puzzleLevelOpt = puzzleLevelService.getPuzzleLevelByLevel(level);
            if (puzzleLevelOpt.isEmpty()) {
                log.error("레벨을 찾을 수 없음: {}", level);
                Map<String, Object> response = new HashMap<>();
                response.put("error", "레벨을 찾을 수 없습니다: " + level);
                return ResponseEntity.badRequest().body(response);
            }
            
            PuzzleLevel puzzleLevel = puzzleLevelOpt.get();
            log.info("레벨 정보: ID={}, Level={}, Name={}", puzzleLevel.getId(), puzzleLevel.getLevel(), puzzleLevel.getLevelName());
            
            // Laravel과 동일한 방식으로 템플릿 조회
            log.info("템플릿 조회 시작 - 레벨 ID: {}", puzzleLevel.getId().intValue());
            List<Map<String, Object>> templates = templateService.getTemplatesByLevelId(puzzleLevel.getId().intValue());
            log.info("조회된 템플릿 개수: {}", templates.size());
            
            if (templates.isEmpty()) {
                log.error("해당 레벨의 템플릿을 찾을 수 없음: {}", level);
                Map<String, Object> response = new HashMap<>();
                response.put("error", "해당 레벨의 템플릿을 찾을 수 없습니다: " + level);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 랜덤으로 템플릿 선택
            Map<String, Object> template = templates.get(new Random().nextInt(templates.size()));
            
            // Laravel의 extractWords 로직을 Spring Boot로 포팅
            Map<String, Object> extractResult = templateService.extractWordsFromTemplate(template);
            
            if (!(Boolean) extractResult.get("success")) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "단어 추출 실패: " + extractResult.get("message"));
                return ResponseEntity.badRequest().body(response);
            }
            
            // Laravel과 동일한 구조로 퍼즐 데이터 생성
            Map<String, Object> puzzleData = createPuzzleDataFromExtractResult(puzzleLevel, template, extractResult);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "크로스워드 퍼즐을 성공적으로 생성했습니다.");
            response.put("data", puzzleData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 생성 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "퍼즐을 생성할 수 없습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Laravel extractWords 결과로 퍼즐 데이터 생성
     */
    private Map<String, Object> createPuzzleDataFromExtractResult(
            PuzzleLevel puzzleLevel, 
            Map<String, Object> template, 
            Map<String, Object> extractResult) {
        
        Map<String, Object> puzzleData = new HashMap<>();
        
        // 퍼즐 레벨 정보
        puzzleData.put("puzzleLevel", Map.of(
            "id", puzzleLevel.getId(),
            "level", puzzleLevel.getLevel(),
            "name", puzzleLevel.getLevelName(),
            "description", puzzleLevel.getDifficultyDescription() + " 난이도의 퍼즐입니다."
        ));
        
        // Laravel과 동일한 구조로 템플릿 데이터 구성
        Map<String, Object> extractedWords = (Map<String, Object>) extractResult.get("extracted_words");
        Map<String, Object> gridInfo = (Map<String, Object>) extractedWords.get("grid_info");
        List<Map<String, Object>> wordOrder = (List<Map<String, Object>>) extractedWords.get("word_order");
        
        // Laravel과 동일한 words 구조로 변환
        List<Map<String, Object>> words = new ArrayList<>();
        for (Map<String, Object> wordInfo : wordOrder) {
            Map<String, Object> word = new HashMap<>();
            word.put("word_id", wordInfo.get("word_id"));
            word.put("pz_word_id", wordInfo.get("pz_word_id")); // 실제 pz_words 테이블의 ID
            // 보안: 정답은 숨기고 힌트만 전송
            word.put("word", "***"); // 정답 숨김
            word.put("hint", wordInfo.get("hint"));
            word.put("category", "일반"); // 임시로 설정
            word.put("difficulty", 1); // 임시로 설정
            
            // position 객체 생성
            Map<String, Object> position = (Map<String, Object>) wordInfo.get("position");
            word.put("position", position);
            
            words.add(word);
        }
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("grid_pattern", gridInfo.get("pattern"));
        templateData.put("words", words);
        puzzleData.put("template", templateData);
        
        return puzzleData;
    }
    
    /**
     * 실제 템플릿 데이터로 퍼즐 데이터 생성
     */
    private Map<String, Object> createPuzzleDataFromTemplate(
            PuzzleLevel puzzleLevel, 
            com.example.crossword.entity.PuzzleGridTemplate template, 
            Map<String, Object> extractResult) {
        
        Map<String, Object> puzzleData = new HashMap<>();
        
        // 퍼즐 레벨 정보
        puzzleData.put("puzzleLevel", Map.of(
            "id", puzzleLevel.getId(),
            "level", puzzleLevel.getLevel(),
            "name", puzzleLevel.getLevelName(),
            "description", puzzleLevel.getDifficultyDescription() + " 난이도의 퍼즐입니다."
        ));
        
        // 실제 템플릿 데이터 사용
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("grid_pattern", template.getGridPattern());
        templateData.put("words", extractResult.get("words"));
        puzzleData.put("template", templateData);
        
        return puzzleData;
    }
    
    /**
     * 퍼즐 데이터 생성 (Laravel과 동일한 구조로 변환) - 임시 메서드
     */
    private Map<String, Object> createPuzzleData(PuzzleLevel puzzleLevel) {
        Map<String, Object> puzzleData = new HashMap<>();
        
        // 퍼즐 레벨 정보
        puzzleData.put("puzzleLevel", Map.of(
            "id", puzzleLevel.getId(),
            "level", puzzleLevel.getLevel(),
            "name", puzzleLevel.getLevelName(),
            "description", puzzleLevel.getDifficultyDescription() + " 난이도의 퍼즐입니다."
        ));
        
        // Laravel과 동일한 템플릿 구조 생성
        Map<String, Object> template = createTemplateData();
        puzzleData.put("template", template);
        
        return puzzleData;
    }
    
    /**
     * Laravel과 동일한 템플릿 데이터 생성
     */
    private Map<String, Object> createTemplateData() {
        Map<String, Object> template = new HashMap<>();
        
        // 그리드 패턴 (Laravel과 동일한 구조)
        int[][] gridPattern = {
            {2, 2, 2, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
        };
        
        template.put("grid_pattern", gridPattern);
        
        // 단어 데이터 생성 (Laravel과 동일한 구조)
        List<Map<String, Object>> words = createWordsData();
        template.put("words", words);
        
        return template;
    }
    
    /**
     * 단어 데이터 생성 (Laravel과 동일한 구조)
     */
    private List<Map<String, Object>> createWordsData() {
        List<Map<String, Object>> words = new ArrayList<>();
        
        // 첫 번째 단어 (가로) - "가나다"
        Map<String, Object> word1 = new HashMap<>();
        word1.put("word_id", 1);
        word1.put("pz_word_id", 1);
        word1.put("word", "가나다");
        word1.put("hint", "첫 번째 힌트입니다");
        word1.put("category", "일반");
        word1.put("difficulty", 1);
        
        // Laravel과 동일한 position 구조
        Map<String, Object> position1 = new HashMap<>();
        position1.put("direction", "horizontal");
        position1.put("start_x", 0);
        position1.put("start_y", 0);
        position1.put("end_x", 2);
        position1.put("end_y", 0);
        word1.put("position", position1);
        
        words.add(word1);
        
        // 두 번째 단어 (세로) - "가라마"
        Map<String, Object> word2 = new HashMap<>();
        word2.put("word_id", 2);
        word2.put("pz_word_id", 2);
        word2.put("word", "가라마");
        word2.put("hint", "두 번째 힌트입니다");
        word2.put("category", "일반");
        word2.put("difficulty", 1);
        
        // Laravel과 동일한 position 구조
        Map<String, Object> position2 = new HashMap<>();
        position2.put("direction", "vertical");
        position2.put("start_x", 0);
        position2.put("start_y", 0);
        position2.put("end_x", 0);
        position2.put("end_y", 2);
        word2.put("position", position2);
        
        words.add(word2);
        
        return words;
    }
    
    /**
     * 샘플 퍼즐 템플릿 생성 (임시 구현)
     * 실제로는 데이터베이스에서 템플릿을 가져와야 함
     */
    private Map<String, Object> createSampleTemplate(PuzzleLevel puzzleLevel) {
        Map<String, Object> template = new HashMap<>();
        template.put("id", 1);
        template.put("template_name", "샘플 템플릿");
        template.put("grid_width", 10);
        template.put("grid_height", 10);
        
        // 샘플 그리드 패턴 (2 = 검은색 칸, 0 = 흰색 칸)
        int[][] gridPattern = {
            {2, 2, 2, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
        };
        template.put("grid_pattern", gridPattern);
        
        // 샘플 단어 목록
        List<Map<String, Object>> words = new ArrayList<>();
        
        // 첫 번째 단어 (가로)
        Map<String, Object> word1 = new HashMap<>();
        word1.put("pz_word_id", 1);
        word1.put("word_id", 1);
        word1.put("category", "일반");
        word1.put("hint_id", 1);
        word1.put("hint", "첫 번째 힌트입니다");
        
        Map<String, Object> position1 = new HashMap<>();
        position1.put("direction", "horizontal");
        position1.put("start_x", 0);
        position1.put("start_y", 0);
        position1.put("end_x", 2);
        position1.put("end_y", 0);
        word1.put("position", position1);
        
        words.add(word1);
        
        // 두 번째 단어 (세로)
        Map<String, Object> word2 = new HashMap<>();
        word2.put("pz_word_id", 2);
        word2.put("word_id", 2);
        word2.put("category", "일반");
        word2.put("hint_id", 2);
        word2.put("hint", "두 번째 힌트입니다");
        
        Map<String, Object> position2 = new HashMap<>();
        position2.put("direction", "vertical");
        position2.put("start_x", 0);
        position2.put("start_y", 0);
        position2.put("end_x", 0);
        position2.put("end_y", 2);
        word2.put("position", position2);
        
        words.add(word2);
        
        template.put("words", words);
        
        return template;
    }
    
    // ==================== 헬퍼 메서드들 (Laravel과 동일한 로직) ====================
    
    /**
     * 요청에서 사용자 ID 추출 (라라벨과 완전히 동일)
     */
    private String getUserIdFromRequest(HttpServletRequest request) {
        // Authorization 헤더에서 토큰 추출 (로그인 사용자)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // TODO: JWT 토큰에서 사용자 ID 추출
            return token;
        }
        
        // 라라벨과 동일: $guestId = $request->query('guest_id') ?? session('guest_id');
        String guestId = request.getParameter("guestId");
        if (guestId == null || guestId.isEmpty()) {
            guestId = getGuestIdFromSession(request);
        }
        
        if (guestId != null && !guestId.isEmpty()) {
            log.info("게스트 계정 사용: guestId={}", guestId);
            
            // 라라벨과 동일: session(['guest_id' => $guestId]);
            setGuestIdToSession(request, guestId);
            
            // 라라벨과 동일: 게스트 계정 자동 생성/조회
            return getOrCreateGuestUser(guestId);
        }
        
        return null;
    }
    
    /**
     * 세션에서 게스트 ID 조회 (라라벨과 동일)
     */
    private String getGuestIdFromSession(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                return (String) session.getAttribute("guest_id");
            }
        } catch (Exception e) {
            log.warn("세션에서 게스트 ID 조회 실패: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 세션에 게스트 ID 저장 (라라벨과 동일)
     */
    private void setGuestIdToSession(HttpServletRequest request, String guestId) {
        try {
            HttpSession session = request.getSession(true);
            session.setAttribute("guest_id", guestId);
            log.debug("세션에 게스트 ID 저장: {}", guestId);
        } catch (Exception e) {
            log.warn("세션에 게스트 ID 저장 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 게스트 계정 자동 생성/조회 (라라벨과 완전히 동일)
     */
    private String getOrCreateGuestUser(String guestId) {
        try {
            // 라라벨과 동일: User 테이블에 게스트 계정 생성/조회
            // $userModel = \App\Models\User::where('guest_id', $guestId)->first();
            // if (!$userModel) { $userModel = \App\Models\User::create([...]); }
            // $userId = $userModel->id;
            
            // 임시로 게스트 ID를 숫자로 변환하여 User ID로 사용
            // 실제로는 User 테이블 조회/생성 로직 필요
            String userId = guestId.replaceAll("\\D+", ""); // 숫자만 추출
            if (userId.isEmpty()) {
                userId = String.valueOf(System.currentTimeMillis() % 1000000); // 임시 ID
            }
            
            log.info("게스트 사용자 처리: guestId={}, userId={}", guestId, userId);
            return userId;
        } catch (Exception e) {
            log.error("게스트 사용자 생성/조회 오류: {}", e.getMessage());
            return String.valueOf(System.currentTimeMillis() % 1000000); // 기본값 반환
        }
    }
    
    /**
     * 게임 데이터 조회/생성 (쿠키 기반 세션 관리)
     */
    private Map<String, Object> getOrCreateGameData(String userId) {
        try {
            String sessionKey = "session:" + userId;
            Object sessionObj = gameStateStore.get(sessionKey);
            
            if (sessionObj instanceof Map) {
                Map<String, Object> session = (Map<String, Object>) sessionObj;
                
                // 기존 게임 데이터가 있는지 확인
                String gameDataKey = "game_data:" + userId;
                Object gameDataObj = gameStateStore.get(gameDataKey);
                
                if (gameDataObj instanceof Map) {
                    // 기존 게임 데이터 복원
                    Map<String, Object> gameData = (Map<String, Object>) gameDataObj;
                    log.info("기존 게임 데이터 복원: userId={}", userId);
                    return gameData;
                } else {
                    // 새로운 게임 데이터 생성
                    Map<String, Object> gameData = createNewGameData();
                    gameStateStore.put(gameDataKey, gameData);
                    log.info("새로운 게임 데이터 생성: userId={}", userId);
                    return gameData;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("게임 데이터 조회/생성 오류: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 새로운 게임 데이터 생성
     */
    private Map<String, Object> createNewGameData() {
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("level", 1);
        gameData.put("correct_answers", 0);
        gameData.put("wrong_answers", 0);
        gameData.put("time_remaining", 300);
        gameData.put("clear_condition", 5);
        gameData.put("current_attempts", 0);
        gameData.put("game_state", new HashMap<>());
        gameData.put("puzzle_data", new HashMap<>());
        gameData.put("created_at", System.currentTimeMillis());
        return gameData;
    }
    
    /**
     * 게임 상태 조회 (메모리에서)
     */
    private Map<String, Object> getGameState(String userId) {
        try {
            Map<String, Object> gameState = gameStateStore.get(userId);
            
            if (gameState == null) {
                // 게임 상태가 없으면 기본값 생성
                gameState = new HashMap<>();
                gameState.put("answered_words", new ArrayList<>());
                gameState.put("wrong_answers", new HashMap<>());
                gameState.put("correct_count", 0);
                gameState.put("wrong_count", 0);
                gameState.put("current_level", 1);
                
                // 메모리에 저장
                gameStateStore.put(userId, gameState);
            }
            
            return gameState;
        } catch (Exception e) {
            log.error("게임 상태 조회 오류: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 게임 상태 저장 (메모리에)
     */
    private void saveGameState(String userId, Map<String, Object> gameState) {
        try {
            gameStateStore.put(userId, gameState);
            log.debug("게임 상태 저장 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("게임 상태 저장 오류: {}", e.getMessage());
        }
    }
    
    
    /**
     * 현재 레벨 조회
     */
    private Integer getCurrentLevel(String userId) {
        Map<String, Object> gameState = getGameState(userId);
        if (gameState != null) {
            return (Integer) gameState.getOrDefault("current_level", 1);
        }
        return 1;
    }
    
    /**
     * 사용자 레벨 업데이트
     */
    private void updateUserLevel(String userId, Integer newLevel) {
        try {
            Map<String, Object> gameState = getGameState(userId);
            if (gameState != null) {
                gameState.put("current_level", newLevel);
                saveGameState(userId, gameState);
                log.info("사용자 레벨 업데이트: userId={}, newLevel={}", userId, newLevel);
            }
        } catch (Exception e) {
            log.error("사용자 레벨 업데이트 오류: {}", e.getMessage());
        }
    }
    
    /**
     * 현재 퍼즐 세션 종료 (Laravel의 endCurrentPuzzle()과 동일)
     */
    private void endCurrentPuzzle(String userId) {
        try {
            log.info("현재 퍼즐 세션 종료: userId={}", userId);
            
            // 게스트 ID를 UUID로 변환
            UUID uuidGuestId = null;
            Long userIdLong = null;
            
            try {
                // UUID 형식인지 확인하고 변환
                uuidGuestId = UUID.fromString(userId);
                userIdLong = null; // 게스트는 null로 설정
            } catch (IllegalArgumentException e) {
                // UUID가 아닌 경우 로그인 사용자로 처리
                try {
                    userIdLong = Long.parseLong(userId);
                } catch (NumberFormatException ex) {
                    log.error("잘못된 사용자 ID 형식: {}", userId);
                    return;
                }
            }
            
            // 활성 게임 조회
            Optional<UserPuzzleGame> gameOpt = userPuzzleGameService.findActiveGameByUserIdOrGuestId(userIdLong, uuidGuestId);
            if (gameOpt.isPresent()) {
                UserPuzzleGame game = gameOpt.get();
                
                // 현재 퍼즐 세션 종료 (Laravel과 동일)
                game.setCurrentPuzzleData(null);
                game.setCurrentGameState(null);
                game.setCurrentPuzzleStartedAt(null);
                game.setHasActivePuzzle(false);
                
                // 데이터베이스에 저장
                userPuzzleGameService.save(game);
                
                log.info("퍼즐 세션 종료 완료: userId={}", userId);
            } else {
                log.warn("활성 게임을 찾을 수 없음: userId={}", userId);
            }
            
        } catch (Exception e) {
            log.error("퍼즐 세션 종료 오류: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 오답 기록 저장 (Laravel과 동일한 로직)
     */
    private void recordWrongAnswer(String userId, Integer wordId, String userAnswer, 
                                 String correctAnswer, String category, Integer level) {
        try {
            // 게임 상태에 오답 기록 추가
            Map<String, Object> gameState = getGameState(userId);
            if (gameState != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> wrongAnswers = (Map<String, Object>) gameState.getOrDefault("wrong_answers", new HashMap<>());
                
                Map<String, Object> wrongAnswerRecord = new HashMap<>();
                wrongAnswerRecord.put("wordId", wordId);
                wrongAnswerRecord.put("userAnswer", userAnswer);
                wrongAnswerRecord.put("correctAnswer", correctAnswer);
                wrongAnswerRecord.put("category", category);
                wrongAnswerRecord.put("level", level);
                wrongAnswerRecord.put("timestamp", System.currentTimeMillis());
                
                wrongAnswers.put(String.valueOf(wordId), wrongAnswerRecord);
                gameState.put("wrong_answers", wrongAnswers);
                
                saveGameState(userId, gameState);
                
                log.info("오답 기록 저장: userId={}, wordId={}, userAnswer={}, correctAnswer={}, category={}, level={}", 
                        userId, wordId, userAnswer, correctAnswer, category, level);
            }
            
        } catch (Exception e) {
            log.error("오답 기록 저장 오류: {}", e.getMessage());
        }
    }
    
    /**
     * 힌트 사용 기록 저장
     */
    private void recordHintUsage(String userId, Integer wordId, Integer hintId, String hintText) {
        try {
            // 게임 상태에 힌트 사용 기록 추가
            Map<String, Object> gameState = getGameState(userId);
            if (gameState != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> hintsUsed = (Map<String, Object>) gameState.getOrDefault("hints_used", new HashMap<>());
                
                Map<String, Object> hintUsageRecord = new HashMap<>();
                hintUsageRecord.put("wordId", wordId);
                hintUsageRecord.put("hintId", hintId);
                hintUsageRecord.put("hintText", hintText);
                hintUsageRecord.put("timestamp", System.currentTimeMillis());
                
                hintsUsed.put(String.valueOf(hintId), hintUsageRecord);
                gameState.put("hints_used", hintsUsed);
                
                saveGameState(userId, gameState);
                
                log.info("힌트 사용 기록 저장: userId={}, wordId={}, hintId={}", userId, wordId, hintId);
            }
            
        } catch (Exception e) {
            log.error("힌트 사용 기록 저장 오류: {}", e.getMessage());
        }
    }
    
    // ==================== Laravel과 동일한 게임 관리 메서드들 ====================
    
    /**
     * 게임 데이터 조회/생성 (Laravel과 동일)
     */
    private Map<String, Object> getOrCreateGame(String userId, String guestId) {
        try {
            String gameKey = "game:" + userId;
            Map<String, Object> game = (Map<String, Object>) gameStateStore.get(gameKey);
            
            if (game == null) {
                // 새로운 게임 생성
                game = new HashMap<>();
                game.put("user_id", userId);
                game.put("guest_id", guestId);
                game.put("current_level", 1);
                game.put("first_attempt_at", System.currentTimeMillis());
                game.put("is_active", true);
                
                gameStateStore.put(gameKey, game);
                log.info("새로운 게임 생성: userId={}", userId);
            }
            
            return game;
        } catch (Exception e) {
            log.error("게임 조회/생성 오류: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 활성 퍼즐이 있는지 확인 (Laravel과 동일)
     */
    private boolean hasActivePuzzle(String userId) {
        try {
            String puzzleKey = "puzzle:" + userId;
            return gameStateStore.containsKey(puzzleKey);
        } catch (Exception e) {
            log.error("활성 퍼즐 확인 오류: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 현재 퍼즐 데이터 조회 (Laravel과 동일)
     */
    private Map<String, Object> getCurrentPuzzleData(String userId) {
        try {
            String puzzleKey = "puzzle:" + userId;
            return (Map<String, Object>) gameStateStore.get(puzzleKey);
        } catch (Exception e) {
            log.error("현재 퍼즐 데이터 조회 오류: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 새로운 퍼즐 세션 시작 (Laravel과 동일)
     */
    private void startNewPuzzle(String userId, Map<String, Object> puzzleData) {
        try {
            String puzzleKey = "puzzle:" + userId;
            gameStateStore.put(puzzleKey, puzzleData);
            log.info("새로운 퍼즐 세션 시작: userId={}", userId);
        } catch (Exception e) {
            log.error("새로운 퍼즐 세션 시작 오류: {}", e.getMessage());
        }
    }
    
    /**
     * 현재 게임 상태 조회 (Laravel과 동일)
     */
    private Map<String, Object> getCurrentGameState(String userId) {
        try {
            String gameStateKey = "gameState:" + userId;
            return (Map<String, Object>) gameStateStore.get(gameStateKey);
        } catch (Exception e) {
            log.error("현재 게임 상태 조회 오류: {}", e.getMessage());
            return null;
        }
    }
}
