package com.example.board.repository;

import com.example.board.entity.UserPuzzleGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UserPuzzleGame 엔티티를 위한 Repository
 */
@Repository
public interface UserPuzzleGameRepository extends JpaRepository<UserPuzzleGame, Long> {
    
    /**
     * 사용자 ID로 활성 게임 조회
     * @param userId 사용자 ID
     * @return 활성 게임
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.userId = :userId AND u.theme = 'common' AND u.isActive = true")
    Optional<UserPuzzleGame> findByUserIdAndIsActiveTrue(@Param("userId") Long userId);
    
    /**
     * 게스트 ID로 활성 게임 조회
     * @param guestId 게스트 ID
     * @return 활성 게임
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.guestId = :guestId AND u.theme = 'common' AND u.isActive = true")
    Optional<UserPuzzleGame> findByGuestIdAndIsActiveTrue(@Param("guestId") UUID guestId);
    
    /**
     * 사용자 ID로 활성 게임 조회 (게스트 ID는 null로 처리)
     * @param userId 사용자 ID
     * @return 활성 게임
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.userId = :userId AND u.theme = 'common' AND u.isActive = true AND u.hasActivePuzzle = true")
    Optional<UserPuzzleGame> findByUserIdAndIsActiveTrueOnly(@Param("userId") Long userId);
    
    /**
     * 게스트 ID로 활성 게임 조회 (사용자 ID는 null로 처리)
     * @param guestId 게스트 ID
     * @return 활성 게임
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.guestId = :guestId AND u.theme = 'common' AND u.isActive = true AND u.hasActivePuzzle = true")
    Optional<UserPuzzleGame> findByGuestIdAndIsActiveTrueOnly(@Param("guestId") UUID guestId);
    
    /**
     * 사용자 ID로 활성 게임 조회 (is_active만 확인, hasActivePuzzle 조건 제외)
     * @param userId 사용자 ID
     * @return 활성 게임
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.userId = :userId AND u.theme = 'common' AND u.isActive = true")
    Optional<UserPuzzleGame> findByUserIdAndIsActiveOnly(@Param("userId") Long userId);
    
    /**
     * 게스트 ID로 활성 게임 조회 (is_active만 확인, hasActivePuzzle 조건 제외)
     * @param guestId 게스트 ID
     * @return 활성 게임
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.guestId = :guestId AND u.theme = 'common' AND u.isActive = true")
    Optional<UserPuzzleGame> findByGuestIdAndIsActiveOnly(@Param("guestId") UUID guestId);
    
    /**
     * 사용자 ID와 테마로 활성 게임 조회
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.userId = :userId AND u.theme = :theme AND u.isActive = true")
    Optional<UserPuzzleGame> findByUserIdAndThemeAndIsActiveTrue(@Param("userId") Long userId, @Param("theme") String theme);

    /**
     * 사용자 ID와 테마 없이(메인 게임) 활성 게임 조회
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.userId = :userId AND u.theme = 'common' AND u.isActive = true")
    Optional<UserPuzzleGame> findByUserIdAndThemeIsNullAndIsActiveTrue(@Param("userId") Long userId);

    /**
     * 게스트 ID와 테마로 활성 게임 조회
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.guestId = :guestId AND u.theme = :theme AND u.isActive = true")
    Optional<UserPuzzleGame> findByGuestIdAndThemeAndIsActiveTrue(@Param("guestId") UUID guestId, @Param("theme") String theme);

    /**
     * 게스트 ID와 테마 없이(메인 게임) 활성 게임 조회
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.guestId = :guestId AND u.theme = 'common' AND u.isActive = true")
    Optional<UserPuzzleGame> findByGuestIdAndThemeIsNullAndIsActiveTrue(@Param("guestId") UUID guestId);
    
    /**
     * 랭킹 순으로 상위 게임들 조회
     * @param limit 조회할 개수
     * @return 랭킹 순 게임 목록
     */
    @Query("SELECT u FROM UserPuzzleGame u WHERE u.isActive = true ORDER BY u.ranking ASC, u.totalCorrectAnswers DESC, u.accuracyRate DESC")
    List<UserPuzzleGame> findTopRankingGames(@Param("limit") int limit);
    
    /**
     * 특정 사용자의 랭킹 조회
     * @param userId 사용자 ID
     * @return 사용자 랭킹
     */
    @Query("SELECT COUNT(u) + 1 FROM UserPuzzleGame u WHERE u.isActive = true AND (u.ranking < (SELECT u2.ranking FROM UserPuzzleGame u2 WHERE u2.userId = :userId AND u2.isActive = true) OR (u.ranking = (SELECT u2.ranking FROM UserPuzzleGame u2 WHERE u2.userId = :userId AND u2.isActive = true) AND u.totalCorrectAnswers > (SELECT u3.totalCorrectAnswers FROM UserPuzzleGame u3 WHERE u3.userId = :userId AND u3.isActive = true)))")
    Long findUserRanking(@Param("userId") Long userId);
}
