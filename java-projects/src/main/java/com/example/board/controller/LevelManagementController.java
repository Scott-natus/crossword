package com.example.board.controller;

import com.example.board.entity.PzLevel;
import com.example.board.service.LevelManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 레벨 관리 컨트롤러
 * 
 * @author Board Team
 * @version 1.0.0
 * @since 2025-10-26
 */
@RestController
@RequestMapping("/admin/api/level-management")
@CrossOrigin(origins = "*")
@Slf4j
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
            
            // 레벨 데이터를 Map으로 변환
            List<Map<String, Object>> levelData = levelPage.getContent().stream()
                .map(levelManagementService::mapLevelToMap)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", levelPage.getTotalElements());
            response.put("recordsFiltered", levelPage.getTotalElements());
            response.put("data", levelData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("레벨 목록 조회 중 오류 발생", e);
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
            log.error("레벨 통계 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 레벨 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLevel(@PathVariable Long id) {
        try {
            Optional<PzLevel> levelOpt = levelManagementService.getLevelById(id);
            if (levelOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", levelManagementService.mapLevelToMap(levelOpt.get()));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("레벨 상세 조회 중 오류 발생: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 레벨 생성
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createLevel(@RequestBody PzLevel level) {
        try {
            PzLevel createdLevel = levelManagementService.createLevel(level);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "레벨이 성공적으로 생성되었습니다.");
            response.put("data", levelManagementService.mapLevelToMap(createdLevel));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("레벨 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 레벨 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateLevel(@PathVariable Long id, @RequestBody Map<String, Object> updateData) {
        try {
            PzLevel updatedLevel = levelManagementService.updateLevelPartial(id, updateData);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "레벨이 성공적으로 수정되었습니다.");
            response.put("data", levelManagementService.mapLevelToMap(updatedLevel));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("레벨 수정 중 오류 발생: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 레벨 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteLevel(@PathVariable Long id) {
        try {
            levelManagementService.deleteLevel(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "레벨이 성공적으로 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("레벨 삭제 중 오류 발생: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 레벨 일괄 수정
     */
    @PutMapping("/bulk-update")
    public ResponseEntity<Map<String, Object>> bulkUpdateLevels(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> levelIds = (List<Integer>) request.get("levelIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> updateData = (Map<String, Object>) request.get("updateData");
            
            if (levelIds == null || levelIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "수정할 레벨을 선택해주세요."));
            }
            
            int updatedCount = levelManagementService.bulkUpdateLevels(levelIds, updateData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", updatedCount + "개의 레벨이 성공적으로 수정되었습니다.");
            response.put("updatedCount", updatedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("레벨 일괄 수정 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 레벨 일괄 삭제
     */
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDeleteLevels(@RequestBody List<Integer> levelIds) {
        try {
            if (levelIds == null || levelIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "삭제할 레벨을 선택해주세요."));
            }
            
            int deletedCount = levelManagementService.bulkDeleteLevels(levelIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", deletedCount + "개의 레벨이 성공적으로 삭제되었습니다.");
            response.put("deletedCount", deletedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("레벨 일괄 삭제 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
