package com.example.crossword.controller;

import com.example.crossword.service.HintGenerationFromTempService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 힌트 생성 스케줄러 API 컨트롤러
 * tmp_for_make_words 테이블의 단어들을 이용한 힌트 생성 관리
 */
@RestController
@RequestMapping("/api/hint-generation-scheduler")
@RequiredArgsConstructor
@Slf4j
public class HintGenerationSchedulerController {
    
    private final HintGenerationFromTempService hintGenerationFromTempService;
    
    /**
     * 힌트 생성 스케줄러 실행
     * @param limit 처리할 단어 개수 (기본값: 50)
     * @return 처리 결과
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runHintGenerationScheduler(
            @RequestParam(defaultValue = "50") int limit) {
        
        log.info("힌트 생성 스케줄러 실행 요청 - 처리 개수: {}개", limit);
        
        try {
            Map<String, Object> result = hintGenerationFromTempService.generateHintsFromTempWords(limit);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.internalServerError().body(result);
            }
            
        } catch (Exception e) {
            log.error("힌트 생성 스케줄러 실행 중 오류 발생", e);
            
            Map<String, Object> errorResult = Map.of(
                    "success", false,
                    "message", "힌트 생성 스케줄러 실행 중 오류가 발생했습니다: " + e.getMessage(),
                    "error", e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 임시 테이블 통계 조회
     * @return 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("임시 테이블 통계 조회 요청");
        
        try {
            Map<String, Object> stats = hintGenerationFromTempService.getTempWordsStatistics();
            stats.put("success", true);
            stats.put("message", "통계 조회가 완료되었습니다.");
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("통계 조회 중 오류 발생", e);
            
            Map<String, Object> errorResult = Map.of(
                    "success", false,
                    "message", "통계 조회 중 오류가 발생했습니다: " + e.getMessage(),
                    "error", e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * API 사용법 안내
     * @return API 사용법
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp() {
        Map<String, Object> help = Map.of(
                "title", "힌트 생성 스케줄러 API 사용법",
                "description", "tmp_for_make_words 테이블의 단어들을 이용한 힌트 생성 관리",
                "endpoints", Map.of(
                        "POST /api/hint-generation-scheduler/run", "힌트 생성 스케줄러 실행 (limit 파라미터 선택)",
                        "GET /api/hint-generation-scheduler/statistics", "임시 테이블 통계 조회",
                        "GET /api/hint-generation-scheduler/help", "API 사용법 안내"
                ),
                "examples", Map.of(
                        "스케줄러 실행 (기본 50개)", "POST /api/hint-generation-scheduler/run",
                        "스케줄러 실행 (100개)", "POST /api/hint-generation-scheduler/run?limit=100",
                        "통계 조회", "GET /api/hint-generation-scheduler/statistics"
                ),
                "features", new String[]{
                        "tmp_for_make_words 테이블의 hint_yn=false인 단어들 처리",
                        "재미나이 API를 이용한 3단계 난이도 힌트 생성",
                        "pz_words, pz_hints 테이블에 최종 데이터 저장",
                        "중복 단어 자동 스킵 처리",
                        "처리 결과 상세 통계 제공"
                },
                "workflow", new String[]{
                        "1. tmp_for_make_words에서 hint_yn=false인 단어들 조회",
                        "2. 재미나이 API로 각 단어의 힌트 생성",
                        "3. pz_words 테이블에 단어 저장 (중복 체크)",
                        "4. pz_hints 테이블에 3단계 난이도 힌트 저장",
                        "5. tmp_for_make_words의 hint_yn을 true로 업데이트"
                }
        );
        
        return ResponseEntity.ok(help);
    }
}
