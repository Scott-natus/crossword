package com.example.board.controller;

import com.example.board.entity.User;
import com.example.board.entity.UserPuzzleGame;
import com.example.board.entity.UserPuzzleCompletion;
import com.example.board.repository.UserRepository;
import com.example.board.repository.UserPuzzleCompletionRepository;
import com.example.board.service.DailyPuzzleService;
import com.example.board.service.UserPuzzleGameService;
import com.example.board.service.PzWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 테마별 퍼즐 게임 API 컨트롤러
 * REST API 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/theme-puzzle")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ThemePuzzleApiController {
    
    private final DailyPuzzleService dailyPuzzleService;
    private final UserPuzzleGameService userPuzzleGameService;
    private final UserRepository userRepository;
    private final UserPuzzleCompletionRepository userPuzzleCompletionRepository;
    private final PzWordService pzWordService;
    
    /**
     * 테마별 오늘의 퍼즐 조회
     * 
     * @param theme 테마 (K-DRAMA, K-POP, K-MOVIE, K-CULTURE)
     * @param date 날짜 (선택사항)
     * @param guestId 게스트 ID (선택사항, 게임 상태 복원용)
     * @return 퍼즐 데이터
     */
    @GetMapping("/{theme}")
    public ResponseEntity<Map<String, Object>> getTodayPuzzle(
            @PathVariable String theme,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String guestId,
            jakarta.servlet.http.HttpServletRequest request) {
        
        try {
            log.info("테마별 퍼즐 조회 API 호출: {} - 날짜: {}, 게스트 ID: {}", theme, date, guestId);
            
            // 날짜가 지정되지 않으면 오늘 날짜 사용
            LocalDate puzzleDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            
            // 오늘의 퍼즐 조회
            Map<String, Object> puzzleData = dailyPuzzleService.getTodayPuzzle(theme, puzzleDate);
            
            if (puzzleData == null || !Boolean.TRUE.equals(puzzleData.get("success"))) {
                log.warn("테마별 퍼즐 조회 실패: {} - {}", theme, puzzleDate);
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "퍼즐을 찾을 수 없습니다. 관리자에게 문의해주세요.");
                errorResponse.put("theme", theme);
                errorResponse.put("date", puzzleDate.toString());
                
                return ResponseEntity.ok(errorResponse);
            }
            
            // 응답에 테마와 날짜 정보 추가 (HashMap으로 변환하여 수정 가능하게)
            Map<String, Object> response = new HashMap<>(puzzleData);
            response.put("theme", theme);
            response.put("puzzleDate", puzzleDate.toString());
            
            // 1. 게스트 ID 확인
            String finalGuestId = guestId;
            Long userId = null;
            
            if (finalGuestId != null && !finalGuestId.isEmpty()) {
                userId = getOrCreateGuestUser(finalGuestId);
            } else {
                // 2. 로그인 사용자 확인 (메인 게임과 동일한 방식)
                userId = getUserIdFromSession(request);
                if (userId != null) {
                    log.info("인증된 사용자 ID 확인: {}", userId);
                }
            }
            
            // 게스트 ID 또는 로그인 사용자 ID가 있는 경우 게임 상태 복원
            if (userId != null) {
                try {
                    // 테마를 포함하여 게임 조회 또는 생성
                    UserPuzzleGame game = userPuzzleGameService.getOrCreateGame(userId, 
                        (guestId != null && !guestId.isEmpty()) ? calculateGuestUuid(guestId) : null,
                        theme);
                    
                    log.info("게임 조회 완료 - gameId: {}, userId: {}, theme: {}", 
                        game.getId(), userId, theme);
                    
                    Map<String, Object> gameState = game.getCurrentGameState();
                    Map<String, Object> savedPuzzleData = game.getCurrentPuzzleData();
                        
                        log.info("게임 조회 완료 - gameId: {}, hasPuzzleData: {}, hasGameState: {}", 
                            game.getId(), savedPuzzleData != null, gameState != null);
                        
                        // 저장된 퍼즐 데이터가 현재 퍼즐과 같은지 확인 (테마와 퍼즐 ID로)
                        if (savedPuzzleData != null && gameState != null) {
                            Object savedPuzzleId = savedPuzzleData.get("puzzleId");
                            Object currentPuzzleId = puzzleData.get("puzzleId");
                            
                            String currentIdStr = currentPuzzleId != null ? String.valueOf(currentPuzzleId) : null;
                            String savedIdStr = savedPuzzleId != null ? String.valueOf(savedPuzzleId) : null;
                            
                            if (currentIdStr != null && currentIdStr.equals(savedIdStr)) {
                                // 같은 퍼즐이면 게임 상태 반환
                                response.put("game_state", gameState);
                                
                                if (gameState.containsKey("answered_words_with_answers")) {
                                    response.put("answered_words_with_answers", gameState.get("answered_words_with_answers"));
                                }
                                
                                log.info("기존 게임 상태 복원 완료: 테마 {}, 퍼즐 ID {}", theme, currentPuzzleId);
                            } else {
                                // 퍼즐 ID가 다르면 초기화
                                log.info("퍼즐 ID가 다르므로 상태 초기화: savedId={}, currentId={}", savedIdStr, currentIdStr);
                                saveThemePuzzleData(game, puzzleData, theme);
                            }
                        } else {
                            // 퍼즐 데이터가 없으면 새로 저장
                            saveThemePuzzleData(game, puzzleData, theme);
                        }
                } catch (IllegalArgumentException e) {
                    log.error("게스트 ID 형식 오류: {} - {}", guestId, e.getMessage(), e);
                } catch (Exception e) {
                    log.error("게임 상태 조회 중 오류: {} - {}", guestId, e.getMessage(), e);
                    e.printStackTrace();
                }
            }
            
            log.info("테마별 퍼즐 조회 완료: {} - 퍼즐 ID: {}", theme, response.get("puzzleId"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 조회 중 오류 발생: {} - {}", theme, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "퍼즐 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("theme", theme);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 퍼즐 상태 확인
     * 
     * @param theme 테마
     * @param date 날짜 (선택사항, 없으면 오늘)
     * @return 퍼즐 존재 여부
     */
    @GetMapping("/{theme}/status")
    public ResponseEntity<Map<String, Object>> checkPuzzleStatus(
            @PathVariable String theme,
            @RequestParam(required = false) String date) {
        
        try {
            log.info("퍼즐 상태 확인: {} - 날짜: {}", theme, date);
            
            LocalDate puzzleDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            boolean available = dailyPuzzleService.isPuzzleAvailable(theme, puzzleDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("theme", theme);
            response.put("date", puzzleDate.toString());
            response.put("available", available);
            response.put("message", available ? "퍼즐이 준비되어 있습니다." : "퍼즐이 아직 준비되지 않았습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 상태 확인 중 오류 발생: {} - {}", theme, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "퍼즐 상태 확인 중 오류가 발생했습니다.");
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 게스트 ID를 UUID로 변환
     */
    private UUID calculateGuestUuid(String guestId) {
        String uuidString = guestId.replace("guest_", "");
        if (uuidString.length() == 36 && uuidString.contains("-")) {
            return UUID.fromString(uuidString);
        } else if (uuidString.length() == 32) {
            uuidString = uuidString.substring(0, 8) + "-" + 
                        uuidString.substring(8, 12) + "-" + 
                        uuidString.substring(12, 16) + "-" + 
                        uuidString.substring(16, 20) + "-" + 
                        uuidString.substring(20, 32);
            return UUID.fromString(uuidString);
        } else {
            return UUID.nameUUIDFromBytes(guestId.getBytes());
        }
    }

    /**
     * 세션에서 사용자 ID 추출
     */
    private Long getUserIdFromSession(jakarta.servlet.http.HttpServletRequest request) {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
                
                String email = authentication.getName();
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    return userOpt.get().getId();
                }
            }
            
            // 세션에서 직접 확인 (게스트 계정 등)
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                return (Long) session.getAttribute("user_id");
            }
        } catch (Exception e) {
            log.error("세션에서 사용자 ID 추출 중 오류: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 게스트 사용자 생성 또는 조회
     */
    private Long getOrCreateGuestUser(String guestId) {
        try {
            // 게스트 ID 형식: "guest_" + UUID (하이픈 포함 또는 제거된 형식)
            String uuidString = guestId.replace("guest_", "");
            UUID guestUuid;
            
            // UUID 형식 확인 및 변환
            if (uuidString.length() == 36 && uuidString.contains("-")) {
                // 이미 하이픈이 포함된 형식 (예: b632d47e-2eb1-4063-8e63-be0211c5fd48)
                guestUuid = UUID.fromString(uuidString);
            } else if (uuidString.length() == 32) {
                // 하이픈이 제거된 형식 (예: b632d47e2eb140638e63be0211c5fd48)
                uuidString = uuidString.substring(0, 8) + "-" + 
                            uuidString.substring(8, 12) + "-" + 
                            uuidString.substring(12, 16) + "-" + 
                            uuidString.substring(16, 20) + "-" + 
                            uuidString.substring(20, 32);
                guestUuid = UUID.fromString(uuidString);
            } else {
                // UUID 형식이 아니면 해시로 변환
                log.warn("게스트 ID 형식이 올바르지 않음: {}", guestId);
                guestUuid = UUID.nameUUIDFromBytes(guestId.getBytes());
            }
            
            // 게스트 ID로 사용자 조회
            Optional<User> existingUser = userRepository.findByGuestId(guestUuid);
            if (existingUser.isPresent()) {
                return existingUser.get().getId();
            }
            
            // 이메일로 조회 (guest_id@guest.local 형식)
            String email = guestId + "@guest.local";
            Optional<User> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                return existingUserByEmail.get().getId();
            }
            
            // 없으면 새로 생성
            User newUser = new User();
            newUser.setName("게스트_" + guestId.substring(0, Math.min(8, guestId.length())));
            newUser.setEmail(email);
            newUser.setPassword("guest_password_" + System.currentTimeMillis()); // 임시 비밀번호
            newUser.setGuestId(guestUuid);
            newUser.setIsGuest(true);
            newUser.setIsAdmin(false);
            newUser.setStatus("active");
            
            User savedUser = userRepository.save(newUser);
            log.info("게스트 사용자 생성: guestId={}, userId={}", guestId, savedUser.getId());
            return savedUser.getId();
            
        } catch (Exception e) {
            log.error("게스트 사용자 생성/조회 중 오류: guestId={}, error={}", guestId, e.getMessage(), e);
            throw new RuntimeException("게스트 사용자 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 테마별 퍼즐 데이터 저장
     */
    private void saveThemePuzzleData(UserPuzzleGame game, Map<String, Object> puzzleData, String theme) {
        try {
            // 퍼즐 데이터에 테마 정보 추가
            Map<String, Object> puzzleDataWithTheme = new HashMap<>(puzzleData);
            puzzleDataWithTheme.put("theme", theme);
            
            // 퍼즐 ID가 변경되었는지 확인하여 상태 초기화 여부 결정 (타입 불일치 방지를 위해 문자열로 비교)
            Object currentPuzzleId = puzzleData.get("puzzleId");
            Object savedPuzzleId = null;
            Map<String, Object> savedPuzzleData = game.getCurrentPuzzleData();
            if (savedPuzzleData != null) {
                savedPuzzleId = savedPuzzleData.get("puzzleId");
            }
            
            String currentIdStr = currentPuzzleId != null ? String.valueOf(currentPuzzleId) : null;
            String savedIdStr = savedPuzzleId != null ? String.valueOf(savedPuzzleId) : null;
            
            // 기존 게임 상태 가져오기
            Map<String, Object> gameState = game.getCurrentGameState();
            
            // 퍼즐 ID가 다르거나 상태가 없으면 초기화
            if (gameState == null || (currentIdStr != null && !currentIdStr.equals(savedIdStr))) {
                // 게임 상태 초기화
                gameState = new HashMap<>();
                gameState.put("answered_words", new java.util.ArrayList<Long>());
                gameState.put("answered_words_with_answers", new HashMap<String, String>());
                gameState.put("wrong_answers", new java.util.ArrayList<Long>());
                gameState.put("hints_used", new java.util.ArrayList<Long>());
                gameState.put("additional_hints", new java.util.ArrayList<Long>());
                gameState.put("started_at", LocalDateTime.now().toString());
                log.info("게임 상태 초기화 (새 퍼즐 시작) - gameId: {}, puzzleId: {}", game.getId(), currentPuzzleId);
            } else {
                log.info("기존 게임 상태 유지 - gameId: {}, answeredWords: {}", 
                    game.getId(), 
                    gameState.get("answered_words") != null ? ((java.util.List<?>) gameState.get("answered_words")).size() : 0);
            }
            
            // 활성 퍼즐 시작 (퍼즐 데이터만 업데이트, 게임 상태는 유지)
            game.setCurrentPuzzleData(puzzleDataWithTheme);
            game.setCurrentGameState(gameState);
            game.setHasActivePuzzle(true);
            game.setCurrentPuzzleStartedAt(LocalDateTime.now());
            userPuzzleGameService.save(game);
            
            log.info("테마별 퍼즐 데이터 저장 완료: gameId={}, theme={}, puzzleId={}", 
                game.getId(), theme, puzzleData.get("puzzleId"));
            
        } catch (Exception e) {
            log.error("테마별 퍼즐 데이터 저장 중 오류: gameId={}, theme={}, error={}", 
                game.getId(), theme, e.getMessage(), e);
        }
    }
    
    /**
     * 테마별 퍼즐 정답 확인 및 상태 저장
     */
    @PostMapping("/{theme}/check-answer")
    public ResponseEntity<Map<String, Object>> checkAnswer(
            @PathVariable String theme,
            @RequestBody Map<String, Object> requestData,
            jakarta.servlet.http.HttpServletRequest request) {
        
        try {
            log.info("테마별 퍼즐 정답 확인 요청: {} - {}", theme, requestData);
            
            Object wordIdObj = requestData.get("wordId");
            if (wordIdObj == null) wordIdObj = requestData.get("word_id"); // 호환성 유지
            
            if (wordIdObj == null || requestData.get("answer") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "필수 파라미터가 누락되었습니다."));
            }
            
            Integer wordId = Integer.valueOf(wordIdObj.toString());
            String answer = (String) requestData.get("answer");
            String guestId = (String) requestData.get("guestId");
            
            // 1. 사용자 식별
            Long userId = null;
            if (guestId != null && !guestId.isEmpty()) {
                userId = getOrCreateGuestUser(guestId);
            } else {
                userId = getUserIdFromSession(request);
            }
            
            if (userId == null) {
                log.warn("checkAnswer 실패: 사용자 인증 필요 (guestId={}, session User=null)", guestId);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "사용자 인증이 필요합니다."));
            }
            log.info("checkAnswer 진행: userId={}", userId);
            
            // 2. 현재 게임 및 퍼즐 데이터 조회
            Optional<UserPuzzleGame> gameOpt = userPuzzleGameService.findActiveGameByUserIdOrGuestId(userId, null, theme);
            if (!gameOpt.isPresent()) {
                log.warn("checkAnswer 실패: 진행 중인 게임 없음 (userId={}, theme={})", userId, theme);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "진행 중인 게임이 없습니다."));
            }
            
            UserPuzzleGame game = gameOpt.get();
            Map<String, Object> puzzleData = game.getCurrentPuzzleData();
            
            if (puzzleData == null || !theme.equals(puzzleData.get("theme"))) {
                log.warn("checkAnswer 실패: 퍼즐 데이터 없음이거나 테마 불일치 (puzzleTheme={})", puzzleData != null ? puzzleData.get("theme") : "null");
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "현재 테마와 일치하는 기록이 없습니다."));
            }
            log.info("checkAnswer 진행: 퍼즐 데이터 조회 완료 (puzzleId={})", puzzleData.get("puzzleId"));
            
            // 3. 퍼즐 데이터에서 정답 찾기 로직 제거 (프론트엔드가 고유 pz_word_id를 전달함)
            Integer pzWordId = Integer.valueOf(wordIdObj.toString());
            Optional<com.example.board.entity.PzWord> pzWordOpt = pzWordService.getPzWordById(pzWordId);
            
            if (!pzWordOpt.isPresent()) {
                log.warn("checkAnswer 실패: 찾으려는 단어(pzWordId={})가 DB에 존재하지 않음", pzWordId);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "퍼즐에서 단어 정보를 찾을 수 없습니다."));
            }
            
            com.example.board.entity.PzWord pzWord = pzWordOpt.get();
            boolean isCorrect = pzWord.getWord().equals(answer.trim());
            log.info("정답 판별 결과: wordId/pzWordId={}, input={}, correct={}, result={}", 
                pzWordId, answer, pzWord.getWord(), isCorrect);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("is_correct", isCorrect);
            response.put("correct_answer", pzWord.getWord());
            
            // 5. 정답 시 상태 업데이트 (템플릿 단어 ID 기준)
            if (isCorrect) {
                updateThemeGameState(game, wordId, answer);
                
                Map<String, Object> gameState = game.getCurrentGameState();
                if (gameState != null && gameState.get("answered_words") != null) {
                    response.put("correct_count", ((java.util.List<?>) gameState.get("answered_words")).size());
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("정답 확인 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "정답 확인 중 오류가 발생했습니다."));
        }
    }

    /**
     * 테마별 게임 상태 업데이트 (템플릿 번호 기준)
     */
    private void updateThemeGameState(UserPuzzleGame game, Integer wordId, String answer) {
        Map<String, Object> gameState = game.getCurrentGameState();
        if (gameState == null) {
            gameState = new HashMap<>();
            gameState.put("answered_words", new java.util.ArrayList<Long>());
            gameState.put("answered_words_with_answers", new HashMap<String, String>());
            gameState.put("wrong_answers", new java.util.ArrayList<Long>());
            gameState.put("started_at", LocalDateTime.now().toString());
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<Long> answeredWords = (java.util.List<Long>) gameState.get("answered_words");
        if (!answeredWords.contains(wordId.longValue())) {
            answeredWords.add(wordId.longValue());
        }
        
        @SuppressWarnings("unchecked")
        Map<String, String> answeredWordsWithAnswers = (Map<String, String>) gameState.get("answered_words_with_answers");
        if (answeredWordsWithAnswers == null) {
            answeredWordsWithAnswers = new HashMap<>();
        }
        answeredWordsWithAnswers.put(String.valueOf(wordId), answer);
        
        gameState.put("answered_words", answeredWords);
        gameState.put("answered_words_with_answers", answeredWordsWithAnswers);
        
        game.setCurrentGameState(gameState);
        userPuzzleGameService.save(game);
        
        log.info("테마 게임 상태 업데이트 완료: gameId={}, wordId={}, totalAnswered={}", 
            game.getId(), wordId, answeredWords.size());
    }

    /**
     * 테마별 퍼즐 완료 기록 저장
     * 
     * @param theme 테마
     * @param request 완료 정보 (completionTime, hintsUsed, wrongAttempts, puzzleDate, guestId)
     * @return 저장 결과
     */
    @PostMapping("/{theme}/complete")
    public ResponseEntity<Map<String, Object>> completePuzzle(
            @PathVariable String theme,
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("퍼즐 완료 기록 저장 요청: {} - {}", theme, request);
            
            // 요청 데이터 추출
            Integer completionTime = (Integer) request.get("completionTime");
            Integer hintsUsed = request.containsKey("hintsUsed") ? (Integer) request.get("hintsUsed") : 0;
            Integer wrongAttempts = request.containsKey("wrongAttempts") ? (Integer) request.get("wrongAttempts") : 0;
            String puzzleDateStr = (String) request.get("puzzleDate");
            String guestId = (String) request.get("guestId");
            
            if (completionTime == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "완료 시간이 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            LocalDate puzzleDate = puzzleDateStr != null ? LocalDate.parse(puzzleDateStr) : LocalDate.now();
            
            // 사용자 ID 확인 (게스트 또는 인증된 사용자)
            String userId = null;
            if (guestId != null && !guestId.isEmpty()) {
                try {
                    Long guestUserId = getOrCreateGuestUser(guestId);
                    userId = guestUserId.toString();
                } catch (Exception e) {
                    log.error("게스트 사용자 조회 실패: {}", e.getMessage());
                    userId = guestId; // 게스트 ID를 그대로 사용
                }
            }
            
            // 완료 기록 저장
            UserPuzzleCompletion completion = UserPuzzleCompletion.builder()
                .userId(userId)
                .theme(theme)
                .puzzleDate(puzzleDate)
                .completionTime(completionTime)
                .hintsUsed(hintsUsed != null ? hintsUsed : 0)
                .wrongAttempts(wrongAttempts != null ? wrongAttempts : 0)
                .build();
            
            userPuzzleCompletionRepository.save(completion);
            
            log.info("퍼즐 완료 기록 저장 완료: theme={}, puzzleDate={}, completionTime={}초", 
                theme, puzzleDate, completionTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "완료 기록이 저장되었습니다.");
            response.put("completionId", completion.getId());
            response.put("theme", theme);
            response.put("puzzleDate", puzzleDate.toString());
            response.put("completionTime", completionTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 완료 기록 저장 중 오류 발생: {} - {}", theme, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "완료 기록 저장 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * SNS 공유 통계 저장
     * 
     * @param theme 테마
     * @param request 공유 정보 (platform: facebook, twitter, kakao, puzzleDate, guestId)
     * @return 저장 결과
     */
    @PostMapping("/{theme}/share")
    public ResponseEntity<Map<String, Object>> saveShareStats(
            @PathVariable String theme,
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("SNS 공유 통계 저장 요청: {} - {}", theme, request);
            
            String platform = (String) request.get("platform");
            String puzzleDateStr = (String) request.get("puzzleDate");
            String guestId = (String) request.get("guestId");
            
            if (platform == null || platform.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "공유 플랫폼이 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            LocalDate puzzleDate = puzzleDateStr != null ? LocalDate.parse(puzzleDateStr) : LocalDate.now();
            
            // 공유 통계 저장 (현재는 로그만 기록, 추후 별도 테이블 생성 가능)
            log.info("SNS 공유 통계: theme={}, platform={}, puzzleDate={}, guestId={}", 
                theme, platform, puzzleDate, guestId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "공유 통계가 기록되었습니다.");
            response.put("theme", theme);
            response.put("platform", platform);
            response.put("puzzleDate", puzzleDate.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("SNS 공유 통계 저장 중 오류 발생: {} - {}", theme, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "공유 통계 저장 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}

