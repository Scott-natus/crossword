package com.example.board.controller;

import com.example.board.entity.User;
import com.example.board.entity.UserPuzzleGame;
import com.example.board.repository.UserRepository;
import com.example.board.service.DailyPuzzleService;
import com.example.board.service.UserPuzzleGameService;
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
            @RequestParam(required = false) String guestId) {
        
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
            
            // 게스트 ID가 있으면 게임 상태 조회 및 퍼즐 데이터 저장
            if (guestId != null && !guestId.isEmpty()) {
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
                    
                    log.info("게스트 UUID 변환 완료: {} -> {}", guestId, guestUuid);
                    Optional<UserPuzzleGame> gameOpt = userPuzzleGameService.findActiveGameByGuestId(guestUuid);
                    log.info("게임 조회 결과: gameOpt.isPresent() = {}", gameOpt.isPresent());
                    
                    UserPuzzleGame game;
                    if (gameOpt.isPresent()) {
                        game = gameOpt.get();
                        Map<String, Object> gameState = game.getCurrentGameState();
                        Map<String, Object> savedPuzzleData = game.getCurrentPuzzleData();
                        
                        log.info("게임 조회 완료 - gameId: {}, hasPuzzleData: {}, hasGameState: {}", 
                            game.getId(), savedPuzzleData != null, gameState != null);
                        
                        // 저장된 퍼즐 데이터가 현재 퍼즐과 같은지 확인 (테마와 퍼즐 ID로)
                        if (savedPuzzleData != null && gameState != null) {
                            Object savedTheme = savedPuzzleData.get("theme");
                            Object savedPuzzleId = savedPuzzleData.get("puzzleId");
                            Object currentPuzzleId = puzzleData.get("puzzleId");
                            
                            log.info("퍼즐 비교 - savedTheme: {}, currentTheme: {}, savedPuzzleId: {}, currentPuzzleId: {}", 
                                savedTheme, theme, savedPuzzleId, currentPuzzleId);
                            
                            if (theme.equals(savedTheme) && currentPuzzleId != null && currentPuzzleId.equals(savedPuzzleId)) {
                                // 같은 퍼즐이면 게임 상태 반환
                                response.put("game_state", gameState);
                                log.info("게스트 ID {}의 게임 상태 복원: 테마 {}, 퍼즐 ID {}, answeredWords: {}", 
                                    guestId, theme, currentPuzzleId, 
                                    gameState.get("answered_words") != null ? ((java.util.List<?>) gameState.get("answered_words")).size() : 0);
                            } else {
                                // 다른 퍼즐이면 새로운 퍼즐 데이터로 저장
                                log.info("다른 퍼즐이므로 새로 저장 - savedTheme: {}, currentTheme: {}, savedPuzzleId: {}, currentPuzzleId: {}", 
                                    savedTheme, theme, savedPuzzleId, currentPuzzleId);
                                saveThemePuzzleData(game, puzzleData, theme);
                            }
                        } else {
                            // 퍼즐 데이터나 게임 상태가 없으면 새로 저장
                            log.info("퍼즐 데이터나 게임 상태가 없어 새로 저장 - hasPuzzleData: {}, hasGameState: {}", 
                                savedPuzzleData != null, gameState != null);
                            saveThemePuzzleData(game, puzzleData, theme);
                        }
                    } else {
                        // 게임이 없으면 새로 생성하고 퍼즐 데이터 저장
                        log.info("게임이 없어 새로 생성 - guestId: {}, guestUuid: {}", guestId, guestUuid);
                        Long guestUserId = getOrCreateGuestUser(guestId);
                        log.info("게스트 사용자 ID: {}", guestUserId);
                        game = userPuzzleGameService.getOrCreateGame(guestUserId, guestUuid);
                        log.info("게임 생성/조회 완료 - gameId: {}, userId: {}, guestId: {}", 
                            game.getId(), game.getUserId(), game.getGuestId());
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
            
            // 기존 게임 상태 유지 (있는 경우) 또는 초기화
            Map<String, Object> gameState = game.getCurrentGameState();
            if (gameState == null) {
                // 게임 상태가 없으면 초기화
                gameState = new HashMap<>();
                gameState.put("answered_words", new java.util.ArrayList<Long>());
                gameState.put("answered_words_with_answers", new HashMap<String, String>());
                gameState.put("wrong_answers", new java.util.ArrayList<Long>());
                gameState.put("hints_used", new java.util.ArrayList<Long>());
                gameState.put("additional_hints", new java.util.ArrayList<Long>());
                gameState.put("started_at", LocalDateTime.now().toString());
                log.info("게임 상태 초기화 - gameId: {}", game.getId());
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
}

