package com.example.board.repository;

import com.example.board.entity.ThemeDailyPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 테마별 일일 퍼즐 레포지토리
 */
@Repository
public interface ThemeDailyPuzzleRepository extends JpaRepository<ThemeDailyPuzzle, Long> {
    
    /**
     * 테마와 날짜로 활성 퍼즐 조회
     */
    Optional<ThemeDailyPuzzle> findByThemeAndPuzzleDateAndIsActive(String theme, LocalDate puzzleDate, Boolean isActive);
    
    /**
     * 테마별 퍼즐 목록 조회
     */
    List<ThemeDailyPuzzle> findByThemeOrderByPuzzleDateDesc(String theme);
    
    /**
     * 특정 날짜 이후의 퍼즐 목록 조회
     */
    List<ThemeDailyPuzzle> findByThemeAndPuzzleDateAfterOrderByPuzzleDateDesc(String theme, LocalDate startDate);
    
    /**
     * 테마별 퍼즐 개수 조회
     */
    long countByThemeAndPuzzleDateAfter(String theme, LocalDate startDate);
    
    /**
     * 테마별 활성 퍼즐 개수 조회
     */
    long countByThemeAndPuzzleDateAfterAndIsActive(String theme, LocalDate startDate, Boolean isActive);
    
    /**
     * 테마와 날짜로 퍼즐 비활성화
     */
    @Modifying
    @Transactional
    @Query("UPDATE ThemeDailyPuzzle t SET t.isActive = false WHERE t.theme = :theme AND t.puzzleDate = :puzzleDate")
    void deactivateByThemeAndDate(@Param("theme") String theme, @Param("puzzleDate") LocalDate puzzleDate);
    
    /**
     * 모든 테마의 특정 날짜 퍼즐 조회
     */
    List<ThemeDailyPuzzle> findByPuzzleDateAndIsActive(LocalDate puzzleDate, Boolean isActive);
    
    /**
     * 특정 날짜의 모든 퍼즐 비활성화
     */
    @Modifying
    @Transactional
    @Query("UPDATE ThemeDailyPuzzle t SET t.isActive = false WHERE t.puzzleDate = :puzzleDate")
    void deactivateByDate(@Param("puzzleDate") LocalDate puzzleDate);
}
