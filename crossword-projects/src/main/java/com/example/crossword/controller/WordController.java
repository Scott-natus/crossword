package com.example.crossword.controller;

import com.example.crossword.entity.Word;
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
 * 단어 관리 API 컨트롤러
 * 단어 조회, 검색, 통계 등의 REST API를 제공
 */
@RestController
@RequestMapping("/api/words")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class WordController {
    
    private final WordService wordService;
    
    /**
     * 모든 활성 단어 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllActiveWords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.debug("모든 활성 단어 조회: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Word> words = wordService.getActiveWords(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words.getContent());
            response.put("pagination", Map.of(
                "currentPage", words.getNumber(),
                "totalPages", words.getTotalPages(),
                "totalElements", words.getTotalElements(),
                "size", words.getSize(),
                "hasNext", words.hasNext(),
                "hasPrevious", words.hasPrevious()
            ));
            response.put("message", "활성 단어 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("활성 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * ID로 단어 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWordById(@PathVariable Integer id) {
        log.debug("ID로 단어 조회: {}", id);
        
        try {
            Optional<Word> word = wordService.getWordById(id);
            
            if (word.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", word.get());
                response.put("message", "단어를 성공적으로 조회했습니다.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 단어명으로 단어 조회
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchWordsByName(
            @RequestParam String word,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("단어명으로 검색: {}, page={}, size={}", word, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Word> words = wordService.searchWordsByName(word, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words.getContent());
            response.put("pagination", Map.of(
                "currentPage", words.getNumber(),
                "totalPages", words.getTotalPages(),
                "totalElements", words.getTotalElements(),
                "size", words.getSize(),
                "hasNext", words.hasNext(),
                "hasPrevious", words.hasPrevious()
            ));
            response.put("message", "단어 검색을 성공적으로 완료했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 검색 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어 검색 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 난이도별 단어 조회
     */
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<Map<String, Object>> getWordsByDifficulty(
            @PathVariable Integer difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("난이도별 단어 조회: {}, page={}, size={}", difficulty, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Word> words = wordService.getWordsByDifficulty(difficulty, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words.getContent());
            response.put("pagination", Map.of(
                "currentPage", words.getNumber(),
                "totalPages", words.getTotalPages(),
                "totalElements", words.getTotalElements(),
                "size", words.getSize(),
                "hasNext", words.hasNext(),
                "hasPrevious", words.hasPrevious()
            ));
            response.put("message", "난이도별 단어 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("난이도별 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "난이도별 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 카테고리별 단어 조회
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getWordsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("카테고리별 단어 조회: {}, page={}, size={}", category, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Word> words = wordService.getWordsByCategory(category, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words.getContent());
            response.put("pagination", Map.of(
                "currentPage", words.getNumber(),
                "totalPages", words.getTotalPages(),
                "totalElements", words.getTotalElements(),
                "size", words.getSize(),
                "hasNext", words.hasNext(),
                "hasPrevious", words.hasPrevious()
            ));
            response.put("message", "카테고리별 단어 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카테고리별 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "카테고리별 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 길이별 단어 조회
     */
    @GetMapping("/length/{length}")
    public ResponseEntity<Map<String, Object>> getWordsByLength(
            @PathVariable Integer length,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("길이별 단어 조회: {}, page={}, size={}", length, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Word> words = wordService.getWordsByLength(length, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words.getContent());
            response.put("pagination", Map.of(
                "currentPage", words.getNumber(),
                "totalPages", words.getTotalPages(),
                "totalElements", words.getTotalElements(),
                "size", words.getSize(),
                "hasNext", words.hasNext(),
                "hasPrevious", words.hasPrevious()
            ));
            response.put("message", "길이별 단어 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("길이별 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "길이별 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 난이도 범위로 단어 조회
     */
    @GetMapping("/difficulty-range")
    public ResponseEntity<Map<String, Object>> getWordsByDifficultyRange(
            @RequestParam Integer minDifficulty,
            @RequestParam Integer maxDifficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("난이도 범위별 단어 조회: {} - {}, page={}, size={}", minDifficulty, maxDifficulty, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Word> words = wordService.getWordsByDifficultyRange(minDifficulty, maxDifficulty, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words.getContent());
            response.put("pagination", Map.of(
                "currentPage", words.getNumber(),
                "totalPages", words.getTotalPages(),
                "totalElements", words.getTotalElements(),
                "size", words.getSize(),
                "hasNext", words.hasNext(),
                "hasPrevious", words.hasPrevious()
            ));
            response.put("message", "난이도 범위별 단어 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("난이도 범위별 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "난이도 범위별 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 길이 범위로 단어 조회
     */
    @GetMapping("/length-range")
    public ResponseEntity<Map<String, Object>> getWordsByLengthRange(
            @RequestParam Integer minLength,
            @RequestParam Integer maxLength,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("길이 범위별 단어 조회: {} - {}, page={}, size={}", minLength, maxLength, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Word> words = wordService.getWordsByLengthRange(minLength, maxLength, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words.getContent());
            response.put("pagination", Map.of(
                "currentPage", words.getNumber(),
                "totalPages", words.getTotalPages(),
                "totalElements", words.getTotalElements(),
                "size", words.getSize(),
                "hasNext", words.hasNext(),
                "hasPrevious", words.hasPrevious()
            ));
            response.put("message", "길이 범위별 단어 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("길이 범위별 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "길이 범위별 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 랜덤 단어 조회 (퍼즐 생성용)
     */
    @GetMapping("/random")
    public ResponseEntity<Map<String, Object>> getRandomWords(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.debug("랜덤 단어 조회: {}개", limit);
        
        try {
            List<Word> words = wordService.getRandomWords(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words);
            response.put("count", words.size());
            response.put("message", "랜덤 단어를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("랜덤 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "랜덤 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 난이도의 랜덤 단어 조회
     */
    @GetMapping("/random/difficulty/{difficulty}")
    public ResponseEntity<Map<String, Object>> getRandomWordsByDifficulty(
            @PathVariable Integer difficulty,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.debug("난이도별 랜덤 단어 조회: {}, {}개", difficulty, limit);
        
        try {
            List<Word> words = wordService.getRandomWordsByDifficulty(difficulty, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words);
            response.put("count", words.size());
            response.put("message", "난이도별 랜덤 단어를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("난이도별 랜덤 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "난이도별 랜덤 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 카테고리의 랜덤 단어 조회
     */
    @GetMapping("/random/category/{category}")
    public ResponseEntity<Map<String, Object>> getRandomWordsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.debug("카테고리별 랜덤 단어 조회: {}, {}개", category, limit);
        
        try {
            List<Word> words = wordService.getRandomWordsByCategory(category, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words);
            response.put("count", words.size());
            response.put("message", "카테고리별 랜덤 단어를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카테고리별 랜덤 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "카테고리별 랜덤 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 특정 길이의 랜덤 단어 조회
     */
    @GetMapping("/random/length/{length}")
    public ResponseEntity<Map<String, Object>> getRandomWordsByLength(
            @PathVariable Integer length,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.debug("길이별 랜덤 단어 조회: {}, {}개", length, limit);
        
        try {
            List<Word> words = wordService.getRandomWordsByLength(length, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", words);
            response.put("count", words.size());
            response.put("message", "길이별 랜덤 단어를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("길이별 랜덤 단어 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "길이별 랜덤 단어 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 단어 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWordStats() {
        log.debug("단어 통계 조회");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 전체 활성 단어 개수
            stats.put("totalActiveWords", wordService.getTotalActiveWordCount());
            
            // 난이도별 단어 개수
            stats.put("wordsByDifficulty", wordService.getWordCountByDifficulty());
            
            // 카테고리별 단어 개수
            stats.put("wordsByCategory", wordService.getWordCountByCategory());
            
            // 길이별 단어 개수
            stats.put("wordsByLength", wordService.getWordCountByLength());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "단어 통계를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어 통계 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 단어 존재 여부 확인
     */
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Object>> checkWordExists(@RequestParam String word) {
        log.debug("단어 존재 여부 확인: {}", word);
        
        try {
            boolean exists = wordService.existsWord(word);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exists", exists);
            response.put("word", word);
            response.put("message", exists ? "단어가 존재합니다." : "단어가 존재하지 않습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 존재 여부 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "단어 존재 여부 확인 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 모든 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        log.debug("모든 카테고리 목록 조회");
        
        try {
            List<String> categories = wordService.getAllCategories();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);
            response.put("count", categories.size());
            response.put("message", "카테고리 목록을 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카테고리 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "카테고리 목록 조회 중 오류가 발생했습니다."));
        }
    }
}
