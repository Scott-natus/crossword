package com.example.board.repository;

import com.example.board.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 리포지토리
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-10-27
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이메일로 사용자 찾기
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 상태별 사용자 수 조회
     */
    long countByStatus(String status);
    
    /**
     * 오늘 가입한 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE CAST(u.createdAt AS date) = CURRENT_DATE")
    long countNewUsersToday();
    
    /**
     * 활성 사용자 수 조회
     */
    long countByStatusAndLastLoginAtAfter(String status, LocalDateTime lastLogin);
    
    /**
     * 디버깅용 - 특정 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.id = :id")
    User findByIdForDebug(@Param("id") Long id);
    
    /**
     * 사용자 게임 통계 업데이트 (네이티브 쿼리)
     */
    @Modifying
    @Query(value = """
        UPDATE users SET 
            games_played = subquery.games_played,
            total_score = subquery.total_score,
            wins = subquery.wins
        FROM (
            SELECT 
                user_id,
                COUNT(*) as games_played,
                COALESCE(SUM(score), 0) as total_score,
                COUNT(CASE WHEN game_status = 'completed' THEN 1 END) as wins
            FROM puzzle_game_records 
            GROUP BY user_id
        ) as subquery
        WHERE users.id = subquery.user_id
        """, nativeQuery = true)
    int updateUserGameStatistics();
    
    /**
     * 상태별 사용자 조회
     */
    Page<User> findByStatus(String status, Pageable pageable);
    
    /**
     * 이름 또는 이메일로 검색
     */
    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
    
    /**
     * 상태와 검색어로 조회
     */
    Page<User> findByStatusAndNameContainingIgnoreCaseOrStatusAndEmailContainingIgnoreCase(
        String status1, String name, String status2, String email, Pageable pageable);
    
    /**
     * ID 목록으로 사용자 조회
     */
    List<User> findByIdIn(List<Long> ids);
    
    /**
     * 게스트 ID로 사용자 찾기
     */
    Optional<User> findByGuestId(java.util.UUID guestId);
    
    /**
     * 이메일과 게스트 여부로 사용자 찾기
     */
    Optional<User> findByEmailAndIsGuestTrue(String email);
}