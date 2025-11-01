package com.example.board.controller;

import com.example.board.service.DailyPuzzleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

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
    
    /**
     * 테마별 오늘의 퍼즐 조회
     * 
     * @param theme 테마 (K-DRAMA, K-POP, K-MOVIE, K-CULTURE)
     * @return 퍼즐 데이터
     */
    @GetMapping("/{theme}")
    public ResponseEntity<Map<String, Object>> getTodayPuzzle(
            @PathVariable String theme,
            @RequestParam(required = false) String date) {
        
        try {
            log.info("테마별 퍼즐 조회 API 호출: {} - 날짜: {}", theme, date);
            
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
}

