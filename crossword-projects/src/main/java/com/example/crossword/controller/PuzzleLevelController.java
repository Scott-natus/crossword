package com.example.crossword.controller;

import com.example.crossword.entity.PuzzleLevel;
import com.example.crossword.service.PuzzleLevelService;
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
 * 퍼즐 레벨 관리 API 컨트롤러
 * 퍼즐 레벨 CRUD, 조회, 통계 등의 REST API를 제공
 */
@RestController
@RequestMapping("/api/puzzle-levels")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class PuzzleLevelController {
    
    private final PuzzleLevelService puzzleLevelService;
    
    /**
     * 모든 퍼즐 레벨 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPuzzleLevels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "level") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.debug("모든 퍼즐 레벨 조회: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<PuzzleLevel> levels = puzzleLevelService.getAllPuzzleLevelsOrdered(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 레벨 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "퍼즐 레벨 목록 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * ID로 퍼즐 레벨 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelById(@PathVariable Long id) {
        log.debug("ID로 퍼즐 레벨 조회: {}", id);
        
        try {
            Optional<PuzzleLevel> puzzleLevel = puzzleLevelService.getPuzzleLevelById(id);
            
            if (puzzleLevel.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", puzzleLevel.get());
                response.put("message", "퍼즐 레벨을 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 레벨 번호로 퍼즐 레벨 조회
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelByLevel(@PathVariable Integer level) {
        log.debug("레벨 번호로 퍼즐 레벨 조회: {}", level);
        
        try {
            Optional<PuzzleLevel> puzzleLevel = puzzleLevelService.getPuzzleLevelByLevel(level);
            
            if (puzzleLevel.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", puzzleLevel.get());
                response.put("message", "퍼즐 레벨을 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 단어 개수별 퍼즐 레벨 조회
     */
    @GetMapping("/word-count/{wordCount}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelsByWordCount(
            @PathVariable Integer wordCount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("단어 개수별 퍼즐 레벨 조회: {}, page={}, size={}", wordCount, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.getPuzzleLevelsByMinWordCount(wordCount, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "단어 개수별 퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 개수별 퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어 개수별 퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 난이도별 퍼즐 레벨 조회
     */
    @GetMapping("/word-difficulty/{wordDifficulty}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelsByWordDifficulty(
            @PathVariable Integer wordDifficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("단어 난이도별 퍼즐 레벨 조회: {}, page={}, size={}", wordDifficulty, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.getPuzzleLevelsByMinWordDifficulty(wordDifficulty, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "단어 난이도별 퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 난이도별 퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어 난이도별 퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 힌트 난이도별 퍼즐 레벨 조회
     */
    @GetMapping("/hint-difficulty/{hintDifficulty}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelsByHintDifficulty(
            @PathVariable Integer hintDifficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("힌트 난이도별 퍼즐 레벨 조회: {}, page={}, size={}", hintDifficulty, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.getPuzzleLevelsByMinHintDifficulty(hintDifficulty, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "힌트 난이도별 퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("힌트 난이도별 퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "힌트 난이도별 퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 교차점 개수별 퍼즐 레벨 조회
     */
    @GetMapping("/intersection-count/{intersectionCount}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelsByIntersectionCount(
            @PathVariable Integer intersectionCount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("교차점 개수별 퍼즐 레벨 조회: {}, page={}, size={}", intersectionCount, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.getPuzzleLevelsByMinIntersectionCount(intersectionCount, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "교차점 개수별 퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("교차점 개수별 퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "교차점 개수별 퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 시간 제한별 퍼즐 레벨 조회
     */
    @GetMapping("/time-limit/{timeLimit}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelsByTimeLimit(
            @PathVariable Integer timeLimit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("시간 제한별 퍼즐 레벨 조회: {}, page={}, size={}", timeLimit, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.getPuzzleLevelsByMinTimeLimit(timeLimit, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "시간 제한별 퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("시간 제한별 퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "시간 제한별 퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 클리어 조건별 퍼즐 레벨 조회
     */
    @GetMapping("/clear-condition/{clearCondition}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelsByClearCondition(
            @PathVariable Integer clearCondition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("클리어 조건별 퍼즐 레벨 조회: {}, page={}, size={}", clearCondition, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.getPuzzleLevelsByClearCondition(clearCondition, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "클리어 조건별 퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("클리어 조건별 퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "클리어 조건별 퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 업데이트자별 퍼즐 레벨 조회
     */
    @GetMapping("/updated-by/{updatedBy}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelsByUpdatedBy(
            @PathVariable String updatedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("업데이트자별 퍼즐 레벨 조회: {}, page={}, size={}", updatedBy, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.getPuzzleLevelsByUpdatedBy(updatedBy, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "업데이트자별 퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("업데이트자별 퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "업데이트자별 퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 레벨명으로 퍼즐 레벨 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPuzzleLevelsByName(
            @RequestParam String levelName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("레벨명으로 퍼즐 레벨 검색: {}, page={}, size={}", levelName, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.searchPuzzleLevelsByName(levelName, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels.getContent());
            response.put("pagination", Map.of(
                "currentPage", levels.getNumber(),
                "totalPages", levels.getTotalPages(),
                "totalElements", levels.getTotalElements(),
                "size", levels.getSize(),
                "hasNext", levels.hasNext(),
                "hasPrevious", levels.hasPrevious()
            ));
            response.put("message", "레벨명으로 퍼즐 레벨 검색을 성공적으로 완료했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("레벨명으로 퍼즐 레벨 검색 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "레벨명으로 퍼즐 레벨 검색 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 퍼즐 레벨 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelStats() {
        log.debug("퍼즐 레벨 통계 조회");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 전체 퍼즐 레벨 개수
            stats.put("totalLevels", puzzleLevelService.getTotalPuzzleLevelCount());
            
            // 최대/최소 레벨 번호
            stats.put("maxLevel", puzzleLevelService.getMaxLevel().orElse(0));
            stats.put("minLevel", puzzleLevelService.getMinLevel().orElse(0));
            
            // 단어 개수별 통계
            stats.put("levelsByWordCount", puzzleLevelService.getPuzzleLevelCountByWordCount());
            
            // 난이도별 통계
            stats.put("levelsByWordDifficulty", puzzleLevelService.getPuzzleLevelCountByWordDifficulty());
            stats.put("levelsByHintDifficulty", puzzleLevelService.getPuzzleLevelCountByHintDifficulty());
            
            // 교차점 개수별 통계
            stats.put("levelsByIntersectionCount", puzzleLevelService.getPuzzleLevelCountByIntersectionCount());
            
            // 시간 제한별 통계
            stats.put("levelsByTimeLimit", puzzleLevelService.getPuzzleLevelCountByTimeLimit());
            
            // 클리어 조건별 통계
            stats.put("levelsByClearCondition", puzzleLevelService.getPuzzleLevelCountByClearCondition());
            
            // 업데이트자별 통계
            stats.put("levelsByUpdatedBy", puzzleLevelService.getPuzzleLevelCountByUpdatedBy());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "퍼즐 레벨 통계를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 레벨 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "퍼즐 레벨 통계 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 퍼즐 레벨 존재 여부 확인
     */
    @GetMapping("/exists/level/{level}")
    public ResponseEntity<Map<String, Object>> checkPuzzleLevelExistsByLevel(@PathVariable Integer level) {
        log.debug("퍼즐 레벨 존재 여부 확인 (레벨 번호): {}", level);
        
        try {
            boolean exists = puzzleLevelService.existsByLevel(level);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exists", exists);
            response.put("level", level);
            response.put("message", exists ? "퍼즐 레벨이 존재합니다." : "퍼즐 레벨이 존재하지 않습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 레벨 존재 여부 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "퍼즐 레벨 존재 여부 확인 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 퍼즐 레벨 존재 여부 확인 (ID)
     */
    @GetMapping("/exists/{id}")
    public ResponseEntity<Map<String, Object>> checkPuzzleLevelExistsById(@PathVariable Long id) {
        log.debug("퍼즐 레벨 존재 여부 확인 (ID): {}", id);
        
        try {
            boolean exists = puzzleLevelService.existsPuzzleLevel(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exists", exists);
            response.put("id", id);
            response.put("message", exists ? "퍼즐 레벨이 존재합니다." : "퍼즐 레벨이 존재하지 않습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 레벨 존재 여부 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "퍼즐 레벨 존재 여부 확인 중 오류가 발생했습니다."));
        }
    }
}
