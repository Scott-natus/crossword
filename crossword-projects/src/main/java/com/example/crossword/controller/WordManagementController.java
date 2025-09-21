package com.example.crossword.controller;

import com.example.crossword.entity.PzWord;
import com.example.crossword.service.PzWordService;
import com.example.crossword.service.PzHintService;
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

/**
 * 단어 관리 컨트롤러
 * 라라벨의 PzWordController를 Spring Boot로 포팅
 */
@RestController
@RequestMapping("/admin/api/words")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class WordManagementController {

    private final PzWordService pzWordService;
    private final PzHintService pzHintService;

    /**
     * 단어 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Boolean isActive) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<PzWord> wordPage = pzWordService.getWordsWithFilters(
                pageable, search, category, difficulty, isActive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("words", wordPage.getContent());
            response.put("totalElements", wordPage.getTotalElements());
            response.put("totalPages", wordPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("단어 목록 조회 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 단어 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<PzWord> getWord(@PathVariable Long id) {
        try {
            PzWord word = pzWordService.findById(id);
            if (word != null) {
                return ResponseEntity.ok(word);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("단어 조회 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 단어 추가
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWord(@RequestBody Map<String, Object> wordData) {
        try {
            String word = (String) wordData.get("word");
            String category = (String) wordData.get("category");
            Integer difficulty = (Integer) wordData.get("difficulty");
            Boolean isActive = (Boolean) wordData.getOrDefault("isActive", true);
            
            // 유효성 검사
            if (word == null || word.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "단어는 필수입니다."));
            }
            
            if (word.length() > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "단어는 5글자 이하로 입력하세요."));
            }
            
            if (category == null || category.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "카테고리는 필수입니다."));
            }
            
            if (category.length() > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "카테고리는 5글자 이하로 입력하세요."));
            }
            
            // 중복 단어 체크
            if (pzWordService.existsByWord(word)) {
                return ResponseEntity.badRequest().body(Map.of("error", "이미 존재하는 단어입니다."));
            }
            
            PzWord newWord = new PzWord();
            newWord.setWord(word);
            newWord.setCategory(category);
            newWord.setDifficulty(difficulty != null ? difficulty : 1);
            newWord.setIsActive(isActive);
            newWord.setLength(word.length());
            
            PzWord savedWord = pzWordService.save(newWord);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어가 성공적으로 추가되었습니다.");
            response.put("word", savedWord);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("단어 추가 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 단어 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateWord(@PathVariable Long id, @RequestBody Map<String, Object> wordData) {
        try {
            PzWord existingWord = pzWordService.findById(id);
            if (existingWord == null) {
                return ResponseEntity.notFound().build();
            }
            
            String word = (String) wordData.get("word");
            String category = (String) wordData.get("category");
            Integer difficulty = (Integer) wordData.get("difficulty");
            Boolean isActive = (Boolean) wordData.get("isActive");
            
            // 유효성 검사
            if (word != null && !word.trim().isEmpty()) {
                if (word.length() > 5) {
                    return ResponseEntity.badRequest().body(Map.of("error", "단어는 5글자 이하로 입력하세요."));
                }
                existingWord.setWord(word);
                existingWord.setLength(word.length());
            }
            
            if (category != null && !category.trim().isEmpty()) {
                if (category.length() > 5) {
                    return ResponseEntity.badRequest().body(Map.of("error", "카테고리는 5글자 이하로 입력하세요."));
                }
                existingWord.setCategory(category);
            }
            
            if (difficulty != null) {
                existingWord.setDifficulty(difficulty);
            }
            
            if (isActive != null) {
                existingWord.setIsActive(isActive);
            }
            
            PzWord updatedWord = pzWordService.save(existingWord);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어가 성공적으로 수정되었습니다.");
            response.put("word", updatedWord);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("단어 수정 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 단어 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWord(@PathVariable Long id) {
        try {
            PzWord word = pzWordService.findById(id);
            if (word == null) {
                return ResponseEntity.notFound().build();
            }
            
            pzWordService.deleteById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어가 성공적으로 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("단어 삭제 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 일괄 변경
     */
    @PostMapping("/batch-update")
    public ResponseEntity<Map<String, Object>> batchUpdate(@RequestBody Map<String, Object> batchData) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> wordIds = (List<Long>) batchData.get("wordIds");
            String updateType = (String) batchData.get("updateType");
            Object updateValue = batchData.get("updateValue");
            
            if (wordIds == null || wordIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "선택된 단어가 없습니다."));
            }
            
            int updatedCount = 0;
            
            for (Long wordId : wordIds) {
                PzWord word = pzWordService.findById(wordId);
                if (word != null) {
                    switch (updateType) {
                        case "difficulty":
                            if (updateValue instanceof Integer) {
                                word.setDifficulty((Integer) updateValue);
                                updatedCount++;
                            }
                            break;
                        case "isActive":
                            if (updateValue instanceof Boolean) {
                                word.setIsActive((Boolean) updateValue);
                                updatedCount++;
                            }
                            break;
                    }
                    pzWordService.save(word);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", updatedCount + "개의 단어가 성공적으로 업데이트되었습니다.");
            response.put("updatedCount", updatedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("일괄 변경 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        try {
            List<String> categories = pzWordService.getDistinctCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("카테고리 목록 조회 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 난이도별 단어 수 통계
     */
    @GetMapping("/stats/difficulty")
    public ResponseEntity<Map<String, Long>> getDifficultyStats() {
        try {
            Map<String, Long> stats = pzWordService.getDifficultyStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("난이도별 통계 조회 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
