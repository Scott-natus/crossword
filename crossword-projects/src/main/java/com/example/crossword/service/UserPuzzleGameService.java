package com.example.crossword.service;

import com.example.crossword.entity.UserPuzzleGame;
import com.example.crossword.repository.UserPuzzleGameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * UserPuzzleGame 관련 비즈니스 로직을 처리하는 Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserPuzzleGameService {
    
    private final UserPuzzleGameRepository userPuzzleGameRepository;
    
    /**
     * 사용자 ID로 활성 게임 조회
     * @param userId 사용자 ID
     * @return 활성 게임
     */
    @Transactional(readOnly = true)
    public Optional<UserPuzzleGame> findActiveGameByUserId(Long userId) {
        return userPuzzleGameRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    /**
     * 게스트 ID로 활성 게임 조회
     * @param guestId 게스트 ID
     * @return 활성 게임
     */
    @Transactional(readOnly = true)
    public Optional<UserPuzzleGame> findActiveGameByGuestId(UUID guestId) {
        return userPuzzleGameRepository.findByGuestIdAndIsActiveTrue(guestId);
    }
    
    /**
     * 사용자 ID 또는 게스트 ID로 활성 게임 조회
     * @param userId 사용자 ID
     * @param guestId 게스트 ID
     * @return 활성 게임
     */
    @Transactional(readOnly = true)
    public Optional<UserPuzzleGame> findActiveGameByUserIdOrGuestId(Long userId, UUID guestId) {
        // 먼저 사용자 ID로 조회
        if (userId != null) {
            Optional<UserPuzzleGame> userGame = userPuzzleGameRepository.findByUserIdAndIsActiveTrueOnly(userId);
            if (userGame.isPresent()) {
                return userGame;
            }
        }
        
        // 사용자 ID로 찾지 못했으면 게스트 ID로 조회
        if (guestId != null) {
            return userPuzzleGameRepository.findByGuestIdAndIsActiveTrueOnly(guestId);
        }
        
        return Optional.empty();
    }
    
    /**
     * 새로운 게임 생성
     * @param userId 사용자 ID
     * @param guestId 게스트 ID
     * @return 생성된 게임
     */
    public UserPuzzleGame createNewGame(Long userId, UUID guestId) {
        UserPuzzleGame game = UserPuzzleGame.builder()
                .userId(userId)
                .guestId(guestId)
                .currentLevel(1)
                .isActive(true)
                .build();
        
        UserPuzzleGame savedGame = userPuzzleGameRepository.save(game);
        log.info("새로운 게임 생성: userId={}, guestId={}, gameId={}", userId, guestId, savedGame.getId());
        
        return savedGame;
    }
    
    /**
     * 게임 조회 또는 생성
     * @param userId 사용자 ID
     * @param guestId 게스트 ID
     * @return 게임
     */
    public UserPuzzleGame getOrCreateGame(Long userId, UUID guestId) {
        Optional<UserPuzzleGame> existingGame = findActiveGameByUserIdOrGuestId(userId, guestId);
        
        if (existingGame.isPresent()) {
            return existingGame.get();
        }
        
        return createNewGame(userId, guestId);
    }
    
    /**
     * 활성 퍼즐 시작
     * @param game 게임
     * @param puzzleData 퍼즐 데이터
     * @param gameState 게임 상태
     */
    public void startActivePuzzle(UserPuzzleGame game, Map<String, Object> puzzleData, Map<String, Object> gameState) {
        game.startActivePuzzle(puzzleData, gameState);
        game.updateLastPlayedAt();
        userPuzzleGameRepository.save(game);
        
        log.info("활성 퍼즐 시작: gameId={}, level={}", game.getId(), game.getCurrentLevel());
    }
    
    /**
     * 활성 퍼즐 완료
     * @param game 게임
     */
    public void completeActivePuzzle(UserPuzzleGame game) {
        game.completeActivePuzzle();
        game.updateLastPlayedAt();
        userPuzzleGameRepository.save(game);
        
        log.info("활성 퍼즐 완료: gameId={}, level={}", game.getId(), game.getCurrentLevel());
    }
    
    /**
     * 정답 추가
     * @param game 게임
     */
    public void addCorrectAnswer(UserPuzzleGame game) {
        game.addCorrectAnswer();
        game.updateLastPlayedAt();
        userPuzzleGameRepository.save(game);
    }
    
    /**
     * 오답 추가
     * @param game 게임
     */
    public void addWrongAnswer(UserPuzzleGame game) {
        game.addWrongAnswer();
        game.updateLastPlayedAt();
        userPuzzleGameRepository.save(game);
    }
    
    /**
     * 레벨 업
     * @param game 게임
     */
    public void levelUp(UserPuzzleGame game) {
        game.levelUp();
        game.updateLastPlayedAt();
        userPuzzleGameRepository.save(game);
        
        log.info("레벨 업: gameId={}, newLevel={}", game.getId(), game.getCurrentLevel());
    }
    
    /**
     * 게임 상태 업데이트
     * @param game 게임
     * @param gameState 게임 상태
     */
    public void updateGameState(UserPuzzleGame game, Map<String, Object> gameState) {
        game.setCurrentGameState(gameState);
        game.updateLastPlayedAt();
        userPuzzleGameRepository.save(game);
    }
    
    /**
     * 게임 저장
     * @param game 저장할 게임
     * @return 저장된 게임
     */
    @Transactional
    public UserPuzzleGame save(UserPuzzleGame game) {
        return userPuzzleGameRepository.save(game);
    }
    
    /**
     * 상위 랭킹 게임들 조회
     * @param limit 조회할 개수
     * @return 랭킹 순 게임 목록
     */
    @Transactional(readOnly = true)
    public List<UserPuzzleGame> getTopRankingGames(int limit) {
        return userPuzzleGameRepository.findTopRankingGames(limit);
    }
    
    /**
     * 사용자 랭킹 조회
     * @param userId 사용자 ID
     * @return 사용자 랭킹
     */
    @Transactional(readOnly = true)
    public Long getUserRanking(Long userId) {
        return userPuzzleGameRepository.findUserRanking(userId);
    }
    
    /**
     * 사용자 ID로 게임 조회 또는 생성 (라라벨과 동일한 로직)
     */
    public UserPuzzleGame getOrCreateGameByUserId(Long userId) {
        Optional<UserPuzzleGame> existingGame = userPuzzleGameRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (existingGame.isPresent()) {
            return existingGame.get();
        }
        
        // 새 게임 생성
        UserPuzzleGame newGame = UserPuzzleGame.builder()
                .userId(userId)
                .currentLevel(1)
                .currentLevelCorrectAnswers(0)
                .currentLevelWrongAnswers(0)
                .isActive(true)
                .build();
        
        return userPuzzleGameRepository.save(newGame);
    }
}
