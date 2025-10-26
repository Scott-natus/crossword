package com.example.board.controller;

import com.example.board.entity.PzWord;
import com.example.board.entity.PzHint;
import com.example.board.service.WordManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(WordManagementController.class);

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
            @RequestParam(defaultValue = "0") int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String difficulty_filter,
            @RequestParam(required = false) String refinement,
            @RequestParam(required = false) String active_filter) {
        
        try {
            Map<String, Object> result = wordManagementService.getWordsData(
                draw, start, length, search, difficulty_filter, refinement, active_filter);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 데이터 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "데이터 조회 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 단어 상세 정보 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWordDetail(@PathVariable Integer id) {
        try {
            Map<String, Object> result = wordManagementService.getWordDetail(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 상세 정보 조회 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "단어 정보를 찾을 수 없습니다.");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 단어 추가 (8081과 동일한 방식)
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addWord(
            @RequestParam String word,
            @RequestParam String category,
            @RequestParam Integer difficulty) {
        
        try {
            Map<String, Object> wordData = new HashMap<>();
            wordData.put("word", word);
            wordData.put("category", category);
            wordData.put("difficulty", difficulty);
            
            Map<String, Object> result = wordManagementService.createWord(wordData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 추가 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 단어 생성
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWord(@RequestBody Map<String, Object> wordData) {
        try {
            Map<String, Object> result = wordManagementService.createWord(wordData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 생성 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "단어 생성 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 단어 수정 (8081과 동일한 방식)
     */
    @PostMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateWordPost(
            @PathVariable Integer id,
            @RequestParam String word,
            @RequestParam String category,
            @RequestParam Integer difficulty) {
        try {
            Map<String, Object> wordData = new HashMap<>();
            wordData.put("word", word);
            wordData.put("category", category);
            wordData.put("difficulty", difficulty);
            
            Map<String, Object> result = wordManagementService.updateWord(id, wordData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 수정 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 단어 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateWord(@PathVariable Integer id, @RequestBody Map<String, Object> wordData) {
        try {
            Map<String, Object> result = wordManagementService.updateWord(id, wordData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 수정 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "단어 수정 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 단어 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWord(@PathVariable Integer id) {
        try {
            Map<String, Object> result = wordManagementService.deleteWord(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 삭제 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "단어 삭제 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 단어 비활성화 (8081과 동일한 방식)
     */
    @PostMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, Object>> deactivateWord(@PathVariable Integer id) {
        try {
            Map<String, Object> result = wordManagementService.deactivateWord(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 비활성화 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 단어 활성화 (8081과 동일한 방식)
     */
    @PostMapping("/activate/{id}")
    public ResponseEntity<Map<String, Object>> activateWord(@PathVariable Integer id) {
        try {
            Map<String, Object> result = wordManagementService.activateWord(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 활성화 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 단어 활성화/비활성화
     */
    @PostMapping("/{id}/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Integer id) {
        try {
            Map<String, Object> result = wordManagementService.toggleActive(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 활성화 상태 변경 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "단어 상태 변경 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 단어 상세 정보 조회 (정제용)
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<Map<String, Object>> getWordDetailForRefinement(@PathVariable Integer id) {
        try {
            Map<String, Object> result = wordManagementService.getWordDetail(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 상세 정보 조회 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 단어 정제 (8081과 동일한 방식)
     */
    @PostMapping("/refine")
    public ResponseEntity<Map<String, Object>> refineWord(@RequestBody Map<String, Object> refinementData) {
        logger.debug("단어 정제 요청: {}", refinementData);
        
        try {
            Integer wordId = (Integer) refinementData.get("wordId");
            Integer difficulty = (Integer) refinementData.get("difficulty");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hints = (List<Map<String, Object>>) refinementData.get("hints");
            
            if (wordId == null || difficulty == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "필수 파라미터가 누락되었습니다."));
            }
            
            boolean success = wordManagementService.refineWordWithData(wordId, difficulty, hints);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "정제가 완료되었습니다." : "정제 중 오류가 발생했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("단어 정제 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "정제 중 오류가 발생했습니다."));
        }
    }

    /**
     * 단어 승인/미승인
     */
    @PostMapping("/{id}/toggle-approval")
    public ResponseEntity<Map<String, Object>> toggleApproval(@PathVariable Integer id) {
        try {
            Map<String, Object> result = wordManagementService.toggleApproval(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 승인 상태 변경 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "단어 승인 상태 변경 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
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
            logger.error("일괄 난이도 변경 중 오류 발생", e);
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
            logger.error("일괄 활성화 상태 변경 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 단어 일괄 처리
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchProcess(@RequestBody Map<String, Object> batchData) {
        try {
            Map<String, Object> result = wordManagementService.batchProcess(batchData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 일괄 처리 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "일괄 처리 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 단어 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> result = wordManagementService.getStatistics();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 통계 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "통계 조회 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 단어 파일 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadWords(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = wordManagementService.uploadWords(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("단어 파일 업로드 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "파일 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
