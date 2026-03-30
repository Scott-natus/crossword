package com.example.board.controller;

import com.example.board.service.CrosswordGeneratorService;
import com.example.board.service.PuzzleLevelService;
import com.example.board.entity.PuzzleLevel;
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
 * 퍼즐 관리 API 컨트롤러
 * 퍼즐 생성, 조회, 레벨 관리 등의 REST API를 제공
 */
@RestController
@RequestMapping("/api/puzzles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class PuzzleController {
    
    private final CrosswordGeneratorService crosswordGeneratorService;
    private final PuzzleLevelService puzzleLevelService;
    
    /**
     * 특정 레벨의 크로스워드 퍼즐 생성
     */
    @PostMapping("/generate/{level}")
    public ResponseEntity<Map<String, Object>> generatePuzzle(@PathVariable Integer level) {
        log.debug("크로스워드 퍼즐 생성: 레벨 {}", level);
        
        try {
            // 퍼즐 레벨 존재 여부 확인
            Optional<PuzzleLevel> puzzleLevelOpt = puzzleLevelService.getPuzzleLevelByLevel(level);
            if (puzzleLevelOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "존재하지 않는 퍼즐 레벨입니다: " + level);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 퍼즐 생성
            CrosswordGeneratorService.CrosswordPuzzle puzzle = crosswordGeneratorService.generatePuzzle(level);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "puzzleLevel", puzzle.getPuzzleLevel(),
                "words", puzzle.getWords(),
                "grid", Map.of(
                    "width", puzzle.getGrid().getWidth(),
                    "height", puzzle.getGrid().getHeight(),
                    "grid", puzzle.getGrid().getGrid(),
                    "placedWords", puzzle.getGrid().getPlacedWords(),
                    "intersectionCount", puzzle.getGrid().getIntersectionCount(),
                    "wordCount", puzzle.getGrid().getWordCount(),
                    "density", puzzle.getGrid().getDensity()
                )
            ));
            response.put("message", "크로스워드 퍼즐을 성공적으로 생성했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 생성 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "퍼즐 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 퍼즐 레벨 목록 조회
     */
    @GetMapping("/levels")
    public ResponseEntity<Map<String, Object>> getPuzzleLevels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "level") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.debug("퍼즐 레벨 목록 조회: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
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
     * 특정 레벨의 퍼즐 레벨 정보 조회
     */
    @GetMapping("/levels/{level}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelByLevel(@PathVariable Integer level) {
        log.debug("특정 레벨의 퍼즐 레벨 정보 조회: {}", level);
        
        try {
            Optional<PuzzleLevel> puzzleLevel = puzzleLevelService.getPuzzleLevelByLevel(level);
            
            if (puzzleLevel.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", puzzleLevel.get());
                response.put("message", "퍼즐 레벨 정보를 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("퍼즐 레벨 정보 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "퍼즐 레벨 정보 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 단어 개수별 퍼즐 레벨 조회
     */
    @GetMapping("/levels/word-count/{wordCount}")
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
    @GetMapping("/levels/difficulty/{difficulty}")
    public ResponseEntity<Map<String, Object>> getPuzzleLevelsByDifficulty(
            @PathVariable Integer difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("난이도별 퍼즐 레벨 조회: {}, page={}, size={}", difficulty, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PuzzleLevel> levels = puzzleLevelService.getPuzzleLevelsByMinWordDifficulty(difficulty, pageable);
            
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
            response.put("message", "난이도별 퍼즐 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("난이도별 퍼즐 레벨 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "난이도별 퍼즐 레벨 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 교차점 개수별 퍼즐 레벨 조회
     */
    @GetMapping("/levels/intersection/{intersectionCount}")
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
    @GetMapping("/levels/time-limit/{timeLimit}")
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
     * 레벨명으로 퍼즐 레벨 검색
     */
    @GetMapping("/levels/search")
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
    @GetMapping("/levels/stats")
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
    @GetMapping("/levels/exists/{level}")
    public ResponseEntity<Map<String, Object>> checkPuzzleLevelExists(@PathVariable Integer level) {
        log.debug("퍼즐 레벨 존재 여부 확인: {}", level);
        
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
     * 퍼즐 생성 가능한 레벨 목록 조회
     */
    @GetMapping("/levels/available")
    public ResponseEntity<Map<String, Object>> getAvailablePuzzleLevels() {
        log.debug("퍼즐 생성 가능한 레벨 목록 조회");
        
        try {
            List<PuzzleLevel> levels = puzzleLevelService.getAllPuzzleLevelsOrdered();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", levels);
            response.put("count", levels.size());
            response.put("message", "퍼즐 생성 가능한 레벨 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("퍼즐 생성 가능한 레벨 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "퍼즐 생성 가능한 레벨 목록 조회 중 오류가 발생했습니다."));
        }
    }
}
