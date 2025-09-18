package com.example.crossword.controller;

import com.example.crossword.entity.Hint;
import com.example.crossword.entity.Word;
import com.example.crossword.service.HintService;
import com.example.crossword.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 힌트 관리 API 컨트롤러
 * 힌트 조회, 검색, 통계 등의 REST API를 제공
 */
@RestController
@RequestMapping("/api/hints")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class HintController {
    
    private final HintService hintService;
    private final WordService wordService;
    
    /**
     * ID로 힌트 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getHintById(@PathVariable Integer id) {
        log.debug("ID로 힌트 조회: {}", id);
        
        try {
            Optional<Hint> hint = hintService.getHintById(id);
            
            if (hint.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", hint.get());
                response.put("message", "힌트를 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 단어의 모든 힌트 조회
     */
    @GetMapping("/word/{wordId}")
    public ResponseEntity<Map<String, Object>> getHintsByWordId(
            @PathVariable Integer wordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("단어 ID별 힌트 조회: {}, page={}, size={}", wordId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Hint> hints = hintService.getHintsByWordId(wordId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hints.getContent());
            response.put("pagination", Map.of(
                "currentPage", hints.getNumber(),
                "totalPages", hints.getTotalPages(),
                "totalElements", hints.getTotalElements(),
                "size", hints.getSize(),
                "hasNext", hints.hasNext(),
                "hasPrevious", hints.hasPrevious()
            ));
            response.put("message", "단어별 힌트 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어별 힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어별 힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 단어의 주 힌트 조회
     */
    @GetMapping("/word/{wordId}/primary")
    public ResponseEntity<Map<String, Object>> getPrimaryHintByWordId(@PathVariable Integer wordId) {
        log.debug("단어 ID의 주 힌트 조회: {}", wordId);
        
        try {
            Optional<Hint> hint = hintService.getPrimaryHintByWordId(wordId);
            
            if (hint.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", hint.get());
                response.put("message", "주 힌트를 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("주 힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "주 힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 힌트 타입의 힌트 조회
     */
    @GetMapping("/type/{hintType}")
    public ResponseEntity<Map<String, Object>> getHintsByType(
            @PathVariable String hintType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("힌트 타입별 조회: {}, page={}, size={}", hintType, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Hint> hints = hintService.getHintsByType(hintType, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hints.getContent());
            response.put("pagination", Map.of(
                "currentPage", hints.getNumber(),
                "totalPages", hints.getTotalPages(),
                "totalElements", hints.getTotalElements(),
                "size", hints.getSize(),
                "hasNext", hints.hasNext(),
                "hasPrevious", hints.hasPrevious()
            ));
            response.put("message", "힌트 타입별 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 타입별 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "힌트 타입별 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 단어의 특정 힌트 타입 조회
     */
    @GetMapping("/word/{wordId}/type/{hintType}")
    public ResponseEntity<Map<String, Object>> getHintsByWordIdAndType(
            @PathVariable Integer wordId,
            @PathVariable String hintType) {
        
        log.debug("단어 ID와 힌트 타입별 조회: {}, {}", wordId, hintType);
        
        try {
            List<Hint> hints = hintService.getHintsByWordIdAndType(wordId, hintType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hints);
            response.put("count", hints.size());
            response.put("message", "단어와 힌트 타입별 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어와 힌트 타입별 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어와 힌트 타입별 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 난이도의 힌트 조회
     */
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<Map<String, Object>> getHintsByDifficulty(
            @PathVariable Integer difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("난이도별 힌트 조회: {}, page={}, size={}", difficulty, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Hint> hints = hintService.getHintsByDifficulty(difficulty, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hints.getContent());
            response.put("pagination", Map.of(
                "currentPage", hints.getNumber(),
                "totalPages", hints.getTotalPages(),
                "totalElements", hints.getTotalElements(),
                "size", hints.getSize(),
                "hasNext", hints.hasNext(),
                "hasPrevious", hints.hasPrevious()
            ));
            response.put("message", "난이도별 힌트 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("난이도별 힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "난이도별 힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 힌트 텍스트 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchHintsByText(
            @RequestParam String hintText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("힌트 텍스트 검색: {}, page={}, size={}", hintText, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Hint> hints = hintService.searchHintsByText(hintText, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hints.getContent());
            response.put("pagination", Map.of(
                "currentPage", hints.getNumber(),
                "totalPages", hints.getTotalPages(),
                "totalElements", hints.getTotalElements(),
                "size", hints.getSize(),
                "hasNext", hints.hasNext(),
                "hasPrevious", hints.hasPrevious()
            ));
            response.put("message", "힌트 텍스트 검색을 성공적으로 완료했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 텍스트 검색 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "힌트 텍스트 검색 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 이미지 힌트 조회
     */
    @GetMapping("/with-image")
    public ResponseEntity<Map<String, Object>> getHintsWithImage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("이미지 힌트 조회: page={}, size={}", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Hint> hints = hintService.getHintsWithImage(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hints.getContent());
            response.put("pagination", Map.of(
                "currentPage", hints.getNumber(),
                "totalPages", hints.getTotalPages(),
                "totalElements", hints.getTotalElements(),
                "size", hints.getSize(),
                "hasNext", hints.hasNext(),
                "hasPrevious", hints.hasPrevious()
            ));
            response.put("message", "이미지 힌트 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("이미지 힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "이미지 힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 오디오 힌트 조회
     */
    @GetMapping("/with-audio")
    public ResponseEntity<Map<String, Object>> getHintsWithAudio(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("오디오 힌트 조회: page={}, size={}", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Hint> hints = hintService.getHintsWithAudio(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hints.getContent());
            response.put("pagination", Map.of(
                "currentPage", hints.getNumber(),
                "totalPages", hints.getTotalPages(),
                "totalElements", hints.getTotalElements(),
                "size", hints.getSize(),
                "hasNext", hints.hasNext(),
                "hasPrevious", hints.hasPrevious()
            ));
            response.put("message", "오디오 힌트 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("오디오 힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "오디오 힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 단어의 랜덤 힌트 조회
     */
    @GetMapping("/word/{wordId}/random")
    public ResponseEntity<Map<String, Object>> getRandomHintByWordId(@PathVariable Integer wordId) {
        log.debug("단어 ID의 랜덤 힌트 조회: {}", wordId);
        
        try {
            Optional<Hint> hint = hintService.getRandomHintByWordId(wordId);
            
            if (hint.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", hint.get());
                response.put("message", "랜덤 힌트를 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("랜덤 힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "랜덤 힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 단어의 특정 힌트 타입 랜덤 힌트 조회
     */
    @GetMapping("/word/{wordId}/random/type/{hintType}")
    public ResponseEntity<Map<String, Object>> getRandomHintByWordIdAndType(
            @PathVariable Integer wordId,
            @PathVariable String hintType) {
        
        log.debug("단어 ID와 힌트 타입의 랜덤 힌트 조회: {}, {}", wordId, hintType);
        
        try {
            Optional<Hint> hint = hintService.getRandomHintByWordIdAndType(wordId, hintType);
            
            if (hint.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", hint.get());
                response.put("message", "랜덤 힌트를 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("랜덤 힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "랜덤 힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 단어의 특정 난이도 랜덤 힌트 조회
     */
    @GetMapping("/word/{wordId}/random/difficulty/{difficulty}")
    public ResponseEntity<Map<String, Object>> getRandomHintByWordIdAndDifficulty(
            @PathVariable Integer wordId,
            @PathVariable Integer difficulty) {
        
        log.debug("단어 ID와 난이도의 랜덤 힌트 조회: {}, {}", wordId, difficulty);
        
        try {
            Optional<Hint> hint = hintService.getRandomHintByWordIdAndDifficulty(wordId, difficulty);
            
            if (hint.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", hint.get());
                response.put("message", "랜덤 힌트를 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("랜덤 힌트 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "랜덤 힌트 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 힌트 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getHintStats() {
        log.debug("힌트 통계 조회");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 힌트 타입별 개수
            stats.put("hintsByType", hintService.getHintCountByType());
            
            // 난이도별 힌트 개수
            stats.put("hintsByDifficulty", hintService.getHintCountByDifficulty());
            
            // 수정 상태별 힌트 개수
            stats.put("hintsByCorrectionStatus", hintService.getHintCountByCorrectionStatus());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "힌트 통계를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "힌트 통계 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 단어의 힌트 통계 조회
     */
    @GetMapping("/word/{wordId}/stats")
    public ResponseEntity<Map<String, Object>> getHintStatsByWordId(@PathVariable Integer wordId) {
        log.debug("단어별 힌트 통계 조회: {}", wordId);
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 전체 힌트 개수
            stats.put("totalHints", hintService.getHintCountByWordId(wordId));
            
            // 주 힌트 개수
            stats.put("primaryHints", hintService.getPrimaryHintCountByWordId(wordId));
            
            // 수정된 힌트 개수
            stats.put("correctedHints", hintService.getCorrectedHintCountByWordId(wordId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("wordId", wordId);
            response.put("message", "단어별 힌트 통계를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어별 힌트 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어별 힌트 통계 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 힌트 존재 여부 확인
     */
    @GetMapping("/exists/{id}")
    public ResponseEntity<Map<String, Object>> checkHintExists(@PathVariable Integer id) {
        log.debug("힌트 존재 여부 확인: {}", id);
        
        try {
            boolean exists = hintService.existsHint(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exists", exists);
            response.put("hintId", id);
            response.put("message", exists ? "힌트가 존재합니다." : "힌트가 존재하지 않습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 존재 여부 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "힌트 존재 여부 확인 중 오류가 발생했습니다."));
        }
    }
}
