package com.example.crossword.service;

import com.example.crossword.entity.ThemeDailyPuzzle;
import com.example.crossword.entity.UserPuzzleCompletion;
import com.example.crossword.repository.ThemeDailyPuzzleRepository;
import com.example.crossword.repository.UserPuzzleCompletionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 테마별 퍼즐 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThemePuzzleService {
    
    private final ThemeDailyPuzzleRepository themeDailyPuzzleRepository;
    private final UserPuzzleCompletionRepository userPuzzleCompletionRepository;
    
    /**
     * 퍼즐 완료 기록 저장
     */
    @Transactional
    public Map<String, Object> saveCompletion(String theme, Integer completionTime, 
                                            Integer hintsUsed, Integer wrongAttempts) {
        try {
            log.info("퍼즐 완료 기록 저장: {} - 시간: {}초", theme, completionTime);
            
            // 사용자 ID 생성 (게스트 사용자용)
            String userId = generateGuestUserId();
            
            // 완료 기록 생성
            UserPuzzleCompletion completion = new UserPuzzleCompletion();
            completion.setUserId(userId);
            completion.setTheme(theme);
            completion.setPuzzleDate(LocalDate.now());
            completion.setCompletionTime(completionTime);
            completion.setHintsUsed(hintsUsed);
            completion.setWrongAttempts(wrongAttempts);
            
            // 저장
            userPuzzleCompletionRepository.save(completion);
            
            log.info("퍼즐 완료 기록 저장 완료: {} - 사용자: {}", theme, userId);
            
            return Map.of(
                "success", true,
                "message", "완료 기록이 저장되었습니다.",
                "completionId", completion.getId()
            );
            
        } catch (Exception e) {
            log.error("퍼즐 완료 기록 저장 중 오류 발생: {} - {}", theme, e.getMessage());
            throw new RuntimeException("완료 기록 저장에 실패했습니다.", e);
        }
    }
    
    /**
     * 테마별 랭킹 조회
     */
    public Map<String, Object> getThemeRanking(String theme) {
        try {
            log.info("테마별 랭킹 조회: {}", theme);
            
            // 오늘 날짜의 완료 기록 조회
            LocalDate today = LocalDate.now();
            List<UserPuzzleCompletion> completions = userPuzzleCompletionRepository
                .findByThemeAndPuzzleDateOrderByCompletionTimeAsc(theme, today);
            
            // 랭킹 데이터 생성
            List<Map<String, Object>> ranking = completions.stream()
                .map(completion -> Map.of(
                    "rank", completions.indexOf(completion) + 1,
                    "userId", completion.getUserId(),
                    "completionTime", completion.getCompletionTime(),
                    "hintsUsed", completion.getHintsUsed(),
                    "wrongAttempts", completion.getWrongAttempts(),
                    "completedAt", completion.getCompletedAt()
                ))
                .collect(Collectors.toList());
            
            log.info("테마별 랭킹 조회 완료: {} - {}명", theme, ranking.size());
            
            return Map.of(
                "success", true,
                "theme", theme,
                "puzzleDate", today,
                "ranking", ranking,
                "totalPlayers", ranking.size()
            );
            
        } catch (Exception e) {
            log.error("테마별 랭킹 조회 중 오류 발생: {} - {}", theme, e.getMessage());
            throw new RuntimeException("랭킹 조회에 실패했습니다.", e);
        }
    }
    
    /**
     * 사용 가능한 테마 목록 조회
     */
    public Map<String, Object> getAvailableThemes() {
        try {
            log.info("사용 가능한 테마 목록 조회");
            
            List<Map<String, Object>> themes = Arrays.asList(
                Map.of("theme", "K-POP", "name", "케이팝", "description", "한국의 대중음악"),
                Map.of("theme", "K-DRAMA", "name", "케이드라마", "description", "한국의 드라마"),
                Map.of("theme", "K-MOVIE", "name", "케이무비", "description", "한국의 영화"),
                Map.of("theme", "K-CULTURE", "name", "케이컬처", "description", "한국의 문화"),
                Map.of("theme", "Korean", "name", "한국어", "description", "한국어 일반")
            );
            
            log.info("사용 가능한 테마 목록 조회 완료: {}개", themes.size());
            
            return Map.of(
                "success", true,
                "themes", themes
            );
            
        } catch (Exception e) {
            log.error("테마 목록 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("테마 목록 조회에 실패했습니다.", e);
        }
    }
    
    /**
     * 게스트 사용자 ID 생성
     */
    private String generateGuestUserId() {
        // 게스트 사용자 ID 생성 (UUID 기반)
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "guest_" + uuid;
    }
    
    /**
     * 테마별 누적 랭킹 조회
     */
    public Map<String, Object> getCumulativeRanking(String theme) {
        try {
            log.info("테마별 누적 랭킹 조회: {}", theme);
            
            // 최근 30일간의 완료 기록 조회
            LocalDate startDate = LocalDate.now().minusDays(30);
            List<UserPuzzleCompletion> completions = userPuzzleCompletionRepository
                .findByThemeAndPuzzleDateAfterOrderByCompletionTimeAsc(theme, startDate);
            
            // 사용자별 평균 완료 시간 계산
            Map<String, List<UserPuzzleCompletion>> userCompletions = completions.stream()
                .collect(Collectors.groupingBy(UserPuzzleCompletion::getUserId));
            
            List<Map<String, Object>> cumulativeRanking = userCompletions.entrySet().stream()
                .map(entry -> {
                    String userId = entry.getKey();
                    List<UserPuzzleCompletion> userComps = entry.getValue();
                    
                    double avgTime = userComps.stream()
                        .mapToInt(UserPuzzleCompletion::getCompletionTime)
                        .average()
                        .orElse(0.0);
                    
                    int totalHints = userComps.stream()
                        .mapToInt(UserPuzzleCompletion::getHintsUsed)
                        .sum();
                    
                    int totalWrongAttempts = userComps.stream()
                        .mapToInt(UserPuzzleCompletion::getWrongAttempts)
                        .sum();
                    
                    return Map.of(
                        "userId", userId,
                        "avgCompletionTime", Math.round(avgTime),
                        "totalGames", userComps.size(),
                        "totalHints", totalHints,
                        "totalWrongAttempts", totalWrongAttempts
                    );
                })
                .sorted((a, b) -> Integer.compare(
                    (Integer) a.get("avgCompletionTime"), 
                    (Integer) b.get("avgCompletionTime")
                ))
                .collect(Collectors.toList());
            
            log.info("테마별 누적 랭킹 조회 완료: {} - {}명", theme, cumulativeRanking.size());
            
            return Map.of(
                "success", true,
                "theme", theme,
                "period", "30일",
                "ranking", cumulativeRanking,
                "totalPlayers", cumulativeRanking.size()
            );
            
        } catch (Exception e) {
            log.error("테마별 누적 랭킹 조회 중 오류 발생: {} - {}", theme, e.getMessage());
            throw new RuntimeException("누적 랭킹 조회에 실패했습니다.", e);
        }
    }
}
