package com.example.crossword.controller;

import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.service.WordManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 단어 관리 컨트롤러
 * 라라벨 퍼즐 관리 시스템과 동일한 API 제공
 */
@RestController
@RequestMapping("/admin/api/words")
public class WordManagementController {

    @Autowired
    private WordManagementService wordManagementService;

    /**
     * 테스트 엔드포인트
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "WordManagementController is working!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 단어 데이터 조회 (DataTables용)
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getWordsData(
            @RequestParam(defaultValue = "1") int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String difficulty_filter,
            @RequestParam(defaultValue = "") String refinement) {
        
        try {
            Map<String, Object> data = wordManagementService.getWordsData(
                draw, start, length, search, difficulty_filter, refinement);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "데이터 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 단어 추가
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addWord(
            @RequestParam String word,
            @RequestParam String category,
            @RequestParam Integer difficulty) {
        
        try {
            PzWord newWord = wordManagementService.addWord(word, category, difficulty);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어가 성공적으로 추가되었습니다.");
            response.put("word", newWord);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 단어 수정
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateWord(
            @PathVariable Integer id,
            @RequestParam String word,
            @RequestParam String category,
            @RequestParam Integer difficulty,
            @RequestParam Boolean isActive) {
        
        try {
            PzWord updatedWord = wordManagementService.updateWord(id, word, category, difficulty, isActive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어가 성공적으로 수정되었습니다.");
            response.put("word", updatedWord);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 단어 삭제
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteWord(@PathVariable Integer id) {
        try {
            wordManagementService.deleteWord(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어가 성공적으로 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 일괄 난이도 변경
     */
    @PostMapping("/batch-update-difficulty")
    public ResponseEntity<Map<String, Object>> updateDifficultyBatch(
            @RequestParam List<Integer> wordIds,
            @RequestParam Integer newDifficulty) {
        
        try {
            int updatedCount = wordManagementService.updateDifficultyBatch(wordIds, newDifficulty);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", updatedCount + "개의 단어 난이도가 변경되었습니다.");
            response.put("updated_count", updatedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 일괄 활성화 상태 변경
     */
    @PostMapping("/batch-update-active")
    public ResponseEntity<Map<String, Object>> updateActiveStatusBatch(
            @RequestParam List<Integer> wordIds,
            @RequestParam Boolean isActive) {
        
        try {
            int updatedCount = wordManagementService.updateActiveStatusBatch(wordIds, isActive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", updatedCount + "개의 단어 상태가 변경되었습니다.");
            response.put("updated_count", updatedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 단어 상세 조회
     */
    @GetMapping("/{id}/view")
    public ResponseEntity<Map<String, Object>> getWord(@PathVariable Integer id) {
        try {
            Optional<PzWord> wordOpt = wordManagementService.getWordById(id);
            
            if (wordOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("word", wordOpt.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "단어를 찾을 수 없습니다.");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories-list")
    public ResponseEntity<Map<String, Object>> getCategories() {
        try {
            List<String> categories = wordManagementService.getCategories();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("categories", categories);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }



    /**
     * 관리자 페이지 통계 조회
     */
    @GetMapping("/admin-stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        try {
            Map<String, Object> stats = wordManagementService.getAdminStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "통계 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 난이도별 통계 조회
     */
    @GetMapping("/difficulty-stats")
    public ResponseEntity<Map<String, Object>> getDifficultyStats() {
        try {
            List<Object[]> stats = wordManagementService.getDifficultyStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 힌트 추가
     */
    @PostMapping("/{wordId}/hints")
    public ResponseEntity<Map<String, Object>> addHint(
            @PathVariable Integer wordId,
            @RequestParam String hintType,
            @RequestParam Integer difficulty,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Boolean isPrimary,
            @RequestParam(required = false) MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            PzHint hint = wordManagementService.addHint(wordId, hintType, difficulty, content, isPrimary, file);
            response.put("success", true);
            response.put("message", "힌트가 성공적으로 추가되었습니다.");
            response.put("hint", hint);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "힌트 추가 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 단어의 힌트 목록 조회
     */
    @GetMapping("/{wordId}/hints")
    public ResponseEntity<Map<String, Object>> getWordHints(@PathVariable Integer wordId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 단어 조회
            Optional<PzWord> wordOpt = wordManagementService.getWordById(wordId);
            if (!wordOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "단어를 찾을 수 없습니다.");
                return ResponseEntity.notFound().build();
            }
            
            // 힌트 조회 (간단한 방법으로 시도)
            List<PzHint> hints = wordManagementService.getWordHints(wordId);
            
            response.put("success", true);
            response.put("hints", hints);
            response.put("word", wordOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // 스택 트레이스 출력
            response.put("success", false);
            response.put("message", "힌트 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 힌트 수정
     */
    @PutMapping("/hints/{hintId}")
    public ResponseEntity<Map<String, Object>> updateHint(
            @PathVariable Integer hintId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Boolean isPrimary,
            @RequestParam(required = false) MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            PzHint hint = wordManagementService.updateHint(hintId, content, difficulty, isPrimary, file);
            response.put("success", true);
            response.put("message", "힌트가 성공적으로 수정되었습니다.");
            response.put("hint", hint);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "힌트 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 힌트 삭제
     */
    @DeleteMapping("/hints/{hintId}")
    public ResponseEntity<Map<String, Object>> deleteHint(@PathVariable Integer hintId) {
        Map<String, Object> response = new HashMap<>();
        try {
            wordManagementService.deleteHint(hintId);
            response.put("success", true);
            response.put("message", "힌트가 성공적으로 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "힌트 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
