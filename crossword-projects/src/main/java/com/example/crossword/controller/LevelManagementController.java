package com.example.crossword.controller;

import com.example.crossword.entity.PzLevel;
import com.example.crossword.service.LevelManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/api/level-management")
@CrossOrigin(origins = "*")
public class LevelManagementController {
    
    @Autowired
    private LevelManagementService levelManagementService;
    
    /**
     * 레벨 목록 조회 (DataTables용)
     */
    @GetMapping("/levels-ajax")
    public ResponseEntity<Map<String, Object>> getLevelsAjax(
            @RequestParam(defaultValue = "0") int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(defaultValue = "level") String orderColumn,
            @RequestParam(defaultValue = "asc") String orderDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer minWords,
            @RequestParam(required = false) Integer maxWords,
            @RequestParam(required = false) Integer wordDifficulty,
            @RequestParam(required = false) Integer hintDifficulty) {
        
        try {
            int page = start / length;
            Page<PzLevel> levelPage;
            
            // 검색 조건이 있으면 검색, 없으면 전체 조회
            if (search != null && !search.trim().isEmpty() || 
                minWords != null || maxWords != null || 
                wordDifficulty != null || hintDifficulty != null) {
                levelPage = levelManagementService.searchLevels(
                    search, minWords, maxWords, wordDifficulty, hintDifficulty,
                    page, length, orderColumn, orderDir);
            } else {
                levelPage = levelManagementService.getAllLevels(
                    page, length, orderColumn, orderDir);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", levelPage.getTotalElements());
            response.put("recordsFiltered", levelPage.getTotalElements());
            response.put("data", levelPage.getContent());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("draw", draw);
            errorResponse.put("recordsTotal", 0);
            errorResponse.put("recordsFiltered", 0);
            errorResponse.put("data", List.of());
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 레벨 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getLevelStats() {
        try {
            Map<String, Object> stats = levelManagementService.getLevelStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 특정 레벨 조회
     */
    @GetMapping("/level/{id}")
    public ResponseEntity<Map<String, Object>> getLevel(@PathVariable Long id) {
        try {
            Optional<PzLevel> level = levelManagementService.getLevelById(id);
            Map<String, Object> response = new HashMap<>();
            
            if (level.isPresent()) {
                response.put("success", true);
                response.put("data", level.get());
            } else {
                response.put("success", false);
                response.put("message", "레벨을 찾을 수 없습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 레벨 생성
     */
    @PostMapping("/level")
    public ResponseEntity<Map<String, Object>> createLevel(@RequestBody PzLevel level) {
        try {
            PzLevel createdLevel = levelManagementService.createLevel(level);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "레벨이 성공적으로 생성되었습니다.");
            response.put("data", createdLevel);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 레벨 수정
     */
    @PutMapping("/level/{id}")
    public ResponseEntity<Map<String, Object>> updateLevel(@PathVariable Long id, @RequestBody Map<String, Object> updateData) {
        try {
            // 기존 레벨 조회
            Optional<PzLevel> existingLevelOpt = levelManagementService.getLevelById(id);
            if (!existingLevelOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "레벨을 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            PzLevel existingLevel = existingLevelOpt.get();
            
            // 업데이트할 필드만 적용
            if (updateData.containsKey("levelName")) {
                existingLevel.setLevelName((String) updateData.get("levelName"));
            }
            if (updateData.containsKey("wordDifficulty")) {
                existingLevel.setWordDifficulty(Integer.valueOf(updateData.get("wordDifficulty").toString()));
            }
            if (updateData.containsKey("hintDifficulty")) {
                existingLevel.setHintDifficulty(Integer.valueOf(updateData.get("hintDifficulty").toString()));
            }
            if (updateData.containsKey("intersectionCount")) {
                existingLevel.setIntersectionCount(Integer.valueOf(updateData.get("intersectionCount").toString()));
            }
            if (updateData.containsKey("timeLimit")) {
                existingLevel.setTimeLimit(Integer.valueOf(updateData.get("timeLimit").toString()));
            }
            if (updateData.containsKey("clearCondition")) {
                Object clearConditionObj = updateData.get("clearCondition");
                if (clearConditionObj instanceof Integer) {
                    existingLevel.setClearCondition((Integer) clearConditionObj);
                } else if (clearConditionObj instanceof String) {
                    try {
                        existingLevel.setClearCondition(Integer.valueOf((String) clearConditionObj));
                    } catch (NumberFormatException e) {
                        existingLevel.setClearCondition(null);
                    }
                } else {
                    existingLevel.setClearCondition(null);
                }
            }
            
            PzLevel updatedLevel = levelManagementService.updateLevel(id, existingLevel);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "레벨이 성공적으로 수정되었습니다.");
            response.put("data", updatedLevel);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 레벨 삭제
     */
    @DeleteMapping("/level/{id}")
    public ResponseEntity<Map<String, Object>> deleteLevel(@PathVariable Long id) {
        try {
            levelManagementService.deleteLevel(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "레벨이 성공적으로 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 레벨 복사
     */
    @PostMapping("/level/{id}/copy")
    public ResponseEntity<Map<String, Object>> copyLevel(
            @PathVariable Long id,
            @RequestParam Integer newLevel,
            @RequestParam String newLevelName) {
        try {
            PzLevel copiedLevel = levelManagementService.copyLevel(id, newLevel, newLevelName);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "레벨이 성공적으로 복사되었습니다.");
            response.put("data", copiedLevel);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 레벨 일괄 업데이트
     */
    @PutMapping("/levels/bulk-update")
    public ResponseEntity<Map<String, Object>> bulkUpdateLevels(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> levelIds = (List<Long>) request.get("levelIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> updateData = (Map<String, Object>) request.get("updateData");
            
            levelManagementService.bulkUpdateLevels(levelIds, updateData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "선택된 레벨들이 성공적으로 업데이트되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 레벨 일괄 삭제
     */
    @DeleteMapping("/levels/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDeleteLevels(@RequestBody List<Long> levelIds) {
        try {
            for (Long id : levelIds) {
                levelManagementService.deleteLevel(id);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "선택된 레벨들이 성공적으로 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * API 연결 테스트
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "레벨 관리 API 연결 성공");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 간단한 레벨 목록 테스트
     */
    @GetMapping("/test-levels")
    public ResponseEntity<Map<String, Object>> testLevels() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "레벨 조회 테스트 성공");
            response.put("timestamp", System.currentTimeMillis());
            
            // 간단한 레벨 조회 테스트
            Page<PzLevel> levels = levelManagementService.getAllLevels(0, 5, "level", "asc");
            response.put("totalElements", levels.getTotalElements());
            response.put("content", levels.getContent());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 매우 간단한 테스트
     */
    @GetMapping("/simple-test")
    public ResponseEntity<String> simpleTest() {
        return ResponseEntity.ok("Simple test successful");
    }
}
