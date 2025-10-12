package com.example.crossword.controller;

import com.example.crossword.entity.FailedWordExtraction;
import com.example.crossword.service.FailedWordExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/failed-extractions")
public class FailedWordExtractionController {
    
    @Autowired
    private FailedWordExtractionService failedWordExtractionService;
    
    /**
     * 특정 레벨의 실패 기록 조회
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<FailedWordExtraction>> getFailuresByLevel(@PathVariable Integer level) {
        List<FailedWordExtraction> failures = failedWordExtractionService.getFailuresByLevel(level);
        return ResponseEntity.ok(failures);
    }
    
    /**
     * 특정 템플릿의 실패 기록 조회
     */
    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<FailedWordExtraction>> getFailuresByTemplate(@PathVariable Integer templateId) {
        List<FailedWordExtraction> failures = failedWordExtractionService.getFailuresByTemplate(templateId);
        return ResponseEntity.ok(failures);
    }
    
    /**
     * 특정 기간의 실패 기록 조회
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<FailedWordExtraction>> getFailuresByDateRange(
            @RequestParam String startDate, 
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        List<FailedWordExtraction> failures = failedWordExtractionService.getFailuresByDateRange(start, end);
        return ResponseEntity.ok(failures);
    }
    
    /**
     * 특정 실패 이유로 실패한 기록 조회
     */
    @GetMapping("/reason")
    public ResponseEntity<List<FailedWordExtraction>> getFailuresByReason(@RequestParam String reason) {
        List<FailedWordExtraction> failures = failedWordExtractionService.getFailuresByReason(reason);
        return ResponseEntity.ok(failures);
    }
    
    /**
     * 특정 단어 ID로 실패한 기록 조회
     */
    @GetMapping("/word/{wordId}")
    public ResponseEntity<List<FailedWordExtraction>> getFailuresByWordId(@PathVariable Integer wordId) {
        List<FailedWordExtraction> failures = failedWordExtractionService.getFailuresByWordId(wordId);
        return ResponseEntity.ok(failures);
    }
    
    /**
     * 최근 N개의 실패 기록 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<List<FailedWordExtraction>> getRecentFailures(@RequestParam(defaultValue = "10") int limit) {
        List<FailedWordExtraction> failures = failedWordExtractionService.getRecentFailures(limit);
        return ResponseEntity.ok(failures);
    }
    
    /**
     * 레벨별 실패 횟수 통계
     */
    @GetMapping("/stats/level")
    public ResponseEntity<List<Object[]>> getFailureCountByLevel() {
        List<Object[]> stats = failedWordExtractionService.getFailureCountByLevel();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 실패 이유별 통계
     */
    @GetMapping("/stats/reason")
    public ResponseEntity<List<Object[]>> getFailureCountByReason() {
        List<Object[]> stats = failedWordExtractionService.getFailureCountByReason();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 특정 레벨에서 가장 많이 실패한 단어 ID 조회
     */
    @GetMapping("/stats/most-failed-words/{level}")
    public ResponseEntity<List<Object[]>> getMostFailedWordsByLevel(@PathVariable Integer level) {
        List<Object[]> stats = failedWordExtractionService.getMostFailedWordsByLevel(level);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 실패 통계 요약 정보
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getFailureSummary() {
        Map<String, Object> summary = failedWordExtractionService.getFailureSummary();
        return ResponseEntity.ok(summary);
    }
}
