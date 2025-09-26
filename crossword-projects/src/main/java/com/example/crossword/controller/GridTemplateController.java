package com.example.crossword.controller;

import com.example.crossword.entity.PzGridTemplate;
import com.example.crossword.service.GridTemplateService;
import com.example.crossword.util.GridRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 그리드 템플릿 관리 REST API 컨트롤러
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-23
 */
@RestController
@RequestMapping("/admin/api/grid-template-management")
@CrossOrigin(origins = "*") // 개발용 CORS 허용
public class GridTemplateController {

    @Autowired
    private GridTemplateService gridTemplateService;
    
    @Autowired
    private GridRenderer gridRenderer;

    /**
     * 그리드 템플릿 관리 API 연결 테스트
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "그리드 템플릿 관리 API 연결 성공");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 템플릿 목록 조회 (DataTables용)
     */
    @GetMapping("/templates-ajax")
    public ResponseEntity<Map<String, Object>> getTemplatesAjax(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(defaultValue = "levelId") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Integer minWordCount,
            @RequestParam(required = false) Integer maxWordCount,
            @RequestParam(required = false) Integer minIntersectionCount,
            @RequestParam(required = false) Integer maxIntersectionCount,
            @RequestParam(defaultValue = "1") int draw) {

        try {
            int page = start / length; // DataTables의 start는 offset이므로 페이지로 변환

            Page<PzGridTemplate> templatePage = gridTemplateService.getAllTemplates(
                    page, length, sortField, sortDir,
                    level, templateName, minWordCount, maxWordCount,
                    minIntersectionCount, maxIntersectionCount
            );

            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", templatePage.getTotalElements());
            response.put("recordsFiltered", templatePage.getTotalElements()); // 필터링 기능 구현 시 변경
            response.put("data", templatePage.getContent());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("draw", draw);
            errorResponse.put("recordsTotal", 0);
            errorResponse.put("recordsFiltered", 0);
            errorResponse.put("data", new Object[0]);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());
            
            // 스택 트레이스도 포함 (개발용)
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            errorResponse.put("stackTrace", sw.toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 템플릿 통계 정보 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTemplateStats() {
        Map<String, Object> stats = gridTemplateService.getTemplateStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 단일 템플릿 조회
     */
    @GetMapping("/template/{id}")
    public ResponseEntity<Map<String, Object>> getTemplateById(@PathVariable Long id) {
        try {
            Optional<PzGridTemplate> templateOpt = gridTemplateService.getTemplateById(id);
            if (templateOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", templateOpt.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "템플릿을 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 템플릿 생성
     */
    @PostMapping("/template")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody PzGridTemplate template) {
        try {
            PzGridTemplate createdTemplate = gridTemplateService.createTemplate(template);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "템플릿이 성공적으로 생성되었습니다.");
            response.put("data", createdTemplate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 템플릿 수정
     */
    @PutMapping("/template/{id}")
    public ResponseEntity<Map<String, Object>> updateTemplate(@PathVariable Long id, @RequestBody Map<String, Object> updateData) {
        try {
            // 기존 템플릿 조회
            Optional<PzGridTemplate> existingTemplateOpt = gridTemplateService.getTemplateById(id);
            if (!existingTemplateOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "템플릿을 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            PzGridTemplate existingTemplate = existingTemplateOpt.get();
            
            // 업데이트할 필드만 적용
            if (updateData.containsKey("levelId")) {
                existingTemplate.setLevelId(Integer.valueOf(updateData.get("levelId").toString()));
            }
            if (updateData.containsKey("templateName")) {
                existingTemplate.setTemplateName((String) updateData.get("templateName"));
            }
            if (updateData.containsKey("gridWidth")) {
                existingTemplate.setGridWidth(Integer.valueOf(updateData.get("gridWidth").toString()));
            }
            if (updateData.containsKey("gridHeight")) {
                existingTemplate.setGridHeight(Integer.valueOf(updateData.get("gridHeight").toString()));
            }
            if (updateData.containsKey("wordCount")) {
                existingTemplate.setWordCount(Integer.valueOf(updateData.get("wordCount").toString()));
            }
            if (updateData.containsKey("intersectionCount")) {
                existingTemplate.setIntersectionCount(Integer.valueOf(updateData.get("intersectionCount").toString()));
            }
            if (updateData.containsKey("wordPositions")) {
                existingTemplate.setWordPositions((String) updateData.get("wordPositions"));
            }
            if (updateData.containsKey("gridPattern")) {
                existingTemplate.setGridPattern((String) updateData.get("gridPattern"));
            }
            if (updateData.containsKey("difficultyRating")) {
                existingTemplate.setDifficultyRating(Integer.valueOf(updateData.get("difficultyRating").toString()));
            }
            if (updateData.containsKey("description")) {
                existingTemplate.setDescription((String) updateData.get("description"));
            }
            
            Optional<PzGridTemplate> updatedTemplateOpt = gridTemplateService.updateTemplate(id, existingTemplate);
            if (!updatedTemplateOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "템플릿 업데이트에 실패했습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
            PzGridTemplate updatedTemplate = updatedTemplateOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "템플릿이 성공적으로 수정되었습니다.");
            response.put("data", updatedTemplate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 템플릿 삭제 (논리 삭제)
     */
    @DeleteMapping("/template/{id}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable Long id) {
        try {
            if (gridTemplateService.deleteTemplate(id)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "템플릿이 성공적으로 삭제되었습니다.");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "템플릿을 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 템플릿 복사
     */
    @PostMapping("/template/{id}/copy")
    public ResponseEntity<Map<String, Object>> copyTemplate(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newTemplateName = request.get("templateName");
            if (newTemplateName == null || newTemplateName.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "새 템플릿 이름을 입력해주세요.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            Optional<PzGridTemplate> copiedTemplate = gridTemplateService.copyTemplate(id, newTemplateName);
            if (copiedTemplate.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "템플릿이 성공적으로 복사되었습니다.");
                response.put("data", copiedTemplate.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "원본 템플릿을 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 레벨별 샘플 템플릿 조회
     */
    @GetMapping("/sample-templates")
    public ResponseEntity<List<PzGridTemplate>> getSampleTemplates() {
        List<PzGridTemplate> sampleTemplates = gridTemplateService.getSampleTemplatesByLevel();
        return ResponseEntity.ok(sampleTemplates);
    }

    /**
     * 특정 레벨의 샘플 템플릿 조회
     */
    @GetMapping("/sample-templates/level/{levelId}")
    public ResponseEntity<List<PzGridTemplate>> getSampleTemplatesByLevel(@PathVariable Integer levelId) {
        List<PzGridTemplate> sampleTemplates = gridTemplateService.getSampleTemplatesBySpecificLevel(levelId);
        return ResponseEntity.ok(sampleTemplates);
    }

    /**
     * 선택된 템플릿 일괄 삭제
     */
    @DeleteMapping("/templates")
    public ResponseEntity<Map<String, Object>> deleteTemplates(@RequestBody List<Object> idsObj) {
        try {
            // Object를 Long으로 변환 (Integer나 Long 모두 처리)
            List<Long> ids = new ArrayList<>();
            for (Object id : idsObj) {
                if (id instanceof Integer) {
                    ids.add(((Integer) id).longValue());
                } else if (id instanceof Long) {
                    ids.add((Long) id);
                } else if (id instanceof String) {
                    ids.add(Long.parseLong((String) id));
                }
            }
            
            int deletedCount = gridTemplateService.bulkDeleteTemplates(ids);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", deletedCount + "개의 템플릿이 삭제되었습니다.");
            response.put("deletedCount", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 선택된 템플릿 일괄 수정
     */
    @PutMapping("/templates/bulk-update")
    public ResponseEntity<Map<String, Object>> bulkUpdateTemplates(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> templateIdsObj = (List<Object>) request.get("templateIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> updateData = (Map<String, Object>) request.get("updateData");

            if (templateIdsObj == null || templateIdsObj.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "수정할 템플릿을 선택해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Object를 Long으로 변환 (Integer나 Long 모두 처리)
            List<Long> templateIds = new ArrayList<>();
            for (Object id : templateIdsObj) {
                if (id instanceof Integer) {
                    templateIds.add(((Integer) id).longValue());
                } else if (id instanceof Long) {
                    templateIds.add((Long) id);
                } else if (id instanceof String) {
                    templateIds.add(Long.parseLong((String) id));
                }
            }

            int updatedCount = gridTemplateService.bulkUpdateTemplates(templateIds, updateData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", updatedCount + "개의 템플릿이 성공적으로 수정되었습니다.");
            response.put("updatedCount", updatedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 복잡한 검색 조건으로 템플릿 조회
     */
    @GetMapping("/search")
    public ResponseEntity<List<PzGridTemplate>> searchTemplates(
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Integer minWordCount,
            @RequestParam(required = false) Integer maxWordCount,
            @RequestParam(required = false) Integer minIntersectionCount,
            @RequestParam(required = false) Integer maxIntersectionCount,
            @RequestParam(required = false) String category) {
        
        List<PzGridTemplate> templates = gridTemplateService.searchTemplatesWithFilters(
                level, templateName, minWordCount, maxWordCount,
                minIntersectionCount, maxIntersectionCount, category
        );
        return ResponseEntity.ok(templates);
    }

    /**
     * 그리드 렌더링 API
     * gridPattern과 wordPositions를 기반으로 HTML 그리드 생성
     */
    @PostMapping("/render-grid")
    public ResponseEntity<Map<String, Object>> renderGrid(@RequestBody Map<String, Object> request) {
        try {
            String gridPattern = (String) request.get("gridPattern");
            String wordPositions = (String) request.get("wordPositions");
            Integer cellSize = (Integer) request.getOrDefault("cellSize", 20);
            Boolean showNumbers = (Boolean) request.getOrDefault("showNumbers", true);
            
            if (gridPattern == null || gridPattern.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "그리드 패턴이 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String html = gridRenderer.renderGrid(gridPattern, wordPositions, cellSize, showNumbers);
            int gridSize = gridRenderer.calculateGridSize(gridPattern);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("html", html);
            response.put("gridSize", gridSize);
            response.put("cellSize", cellSize);
            response.put("showNumbers", showNumbers);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "그리드 렌더링 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 템플릿 ID로 그리드 렌더링
     */
    @GetMapping("/template/{id}/render")
    public ResponseEntity<Map<String, Object>> renderTemplateGrid(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int cellSize,
            @RequestParam(defaultValue = "true") boolean showNumbers) {
        try {
            Optional<PzGridTemplate> templateOpt = gridTemplateService.getTemplateById(id);
            if (!templateOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "템플릿을 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            PzGridTemplate template = templateOpt.get();
            String html = gridRenderer.renderGrid(template.getGridPattern(), template.getWordPositions(), cellSize, showNumbers);
            int gridSize = gridRenderer.calculateGridSize(template.getGridPattern());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("html", html);
            response.put("templateId", id);
            response.put("templateName", template.getTemplateName());
            response.put("gridSize", gridSize);
            response.put("cellSize", cellSize);
            response.put("showNumbers", showNumbers);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "그리드 렌더링 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
