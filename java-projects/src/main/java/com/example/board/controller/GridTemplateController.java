package com.example.board.controller;

import com.example.board.entity.PzGridTemplate;
import com.example.board.service.GridTemplateService;
import com.example.board.service.TemplateValidationService;
import com.example.board.util.GridRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GridTemplateController {

    @Autowired
    private GridTemplateService gridTemplateService;
    
    @Autowired
    private GridRenderer gridRenderer;
    
    @Autowired
    private TemplateValidationService templateValidationService;

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
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
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
     * 레벨별 샘플 템플릿 조회 (8081 형식) - 실제 데이터베이스에서 조회
     */
    @GetMapping("/sample-templates/level/{levelId}")
    public ResponseEntity<List<Map<String, Object>>> getSampleTemplatesByLevel(@PathVariable Integer levelId) {
        try {
            log.info("레벨별 샘플 템플릿 조회 요청: levelId={}", levelId);
            
            // 실제 데이터베이스에서 템플릿 데이터 조회
            List<PzGridTemplate> templates = gridTemplateService.getTemplatesByLevel(levelId);
            
            // 템플릿 데이터를 Map으로 변환
            List<Map<String, Object>> templateList = templates.stream()
                .map(template -> {
                    Map<String, Object> templateMap = new HashMap<>();
                    templateMap.put("id", template.getId());
                    templateMap.put("templateName", template.getTemplateName());
                    templateMap.put("gridWidth", template.getGridWidth());
                    templateMap.put("gridHeight", template.getGridHeight());
                    templateMap.put("wordCount", template.getWordCount());
                    templateMap.put("intersectionCount", template.getIntersectionCount());
                    templateMap.put("category", template.getCategory());
                    templateMap.put("difficultyRating", template.getDifficultyRating());
                    templateMap.put("isActive", template.getIsActive());
                    templateMap.put("createdAt", template.getCreatedAt());
                    templateMap.put("updatedAt", template.getUpdatedAt());
                    templateMap.put("gridPattern", template.getGridPattern());
                    templateMap.put("wordPositions", template.getWordPositions());
                    return templateMap;
                })
                .toList();
            
            return ResponseEntity.ok(templateList);
            
        } catch (Exception e) {
            log.error("레벨별 샘플 템플릿 조회 중 오류 발생: levelId={}, error={}", levelId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
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
                response.put("template", templateOpt.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "템플릿을 찾을 수 없습니다: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "템플릿 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 템플릿 삭제
     */
    @DeleteMapping("/template/{id}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable Long id) {
        try {
            boolean deleted = gridTemplateService.deleteTemplate(id);
            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("success", true);
                response.put("message", "템플릿이 삭제되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "템플릿 삭제에 실패했습니다.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "템플릿 삭제 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 템플릿 상태 토글 (활성화/비활성화)
     */
    @PostMapping("/template/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleTemplateStatus(@PathVariable Long id) {
        try {
            boolean toggled = gridTemplateService.toggleTemplateStatus(id);
            Map<String, Object> response = new HashMap<>();
            if (toggled) {
                response.put("success", true);
                response.put("message", "템플릿 상태가 변경되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "템플릿 상태 변경에 실패했습니다.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "템플릿 상태 변경 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 레벨별 템플릿 조회
     */
    @GetMapping("/templates/by-level/{levelId}")
    public ResponseEntity<Map<String, Object>> getTemplatesByLevel(@PathVariable Integer levelId) {
        try {
            List<PzGridTemplate> templates = gridTemplateService.getTemplatesByLevel(levelId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templates", templates);
            response.put("count", templates.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "레벨별 템플릿 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 모든 활성화된 템플릿 조회
     */
    @GetMapping("/templates/active")
    public ResponseEntity<Map<String, Object>> getAllActiveTemplates() {
        try {
            List<PzGridTemplate> templates = gridTemplateService.getAllActiveTemplates();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templates", templates);
            response.put("count", templates.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "활성화된 템플릿 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 템플릿 생성
     */
    @PostMapping("/template")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody PzGridTemplate template) {
        try {
            // 템플릿 검증 (라라벨과 동일)
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("level_id", template.getLevelId());
            templateData.put("grid_size", template.getGridWidth()); // gridSize 필드 추가
            templateData.put("grid_pattern", template.getGridPattern());
            templateData.put("word_positions", template.getWordPositions()); // word_positions 필드 추가
            templateData.put("word_count", template.getWordCount());
            templateData.put("intersection_count", template.getIntersectionCount());
            
            Map<String, Object> validationResult = templateValidationService.validateTemplateCreation(templateData);
            
            if (!(Boolean) validationResult.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) validationResult.get("errors");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", String.join("\n", errors));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            PzGridTemplate createdTemplate = gridTemplateService.createTemplate(template);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "템플릿이 성공적으로 생성되었습니다.");
            response.put("data", createdTemplate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("템플릿 생성 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "템플릿 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 단어 추출 API (8081과 동일)
     */
    @PostMapping("/extract-words")
    public ResponseEntity<Map<String, Object>> extractWords(@RequestBody Map<String, Object> request) {
        try {
            Long templateId = Long.valueOf(request.get("template_id").toString());
            log.info("단어 추출 요청: templateId={}", templateId);
            
            // 간단한 단어 추출 결과 반환 (테스트용)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "단어 추출이 완료되었습니다.");
            
            // 추출된 단어 목록 (테스트용)
            Map<String, Object> extractedWords = new HashMap<>();
            extractedWords.put("word_order", List.of(
                Map.of("id", 1, "word", "사과", "type", "no_intersection", "position", Map.of("row", 0, "col", 0, "direction", "horizontal")),
                Map.of("id", 2, "word", "바나나", "type", "intersection", "position", Map.of("row", 1, "col", 0, "direction", "vertical")),
                Map.of("id", 3, "word", "오렌지", "type", "intersection", "position", Map.of("row", 2, "col", 0, "direction", "horizontal"))
            ));
            extractedWords.put("total_words", 3);
            extractedWords.put("independent_words", 1);
            extractedWords.put("connected_words", 2);
            
            response.put("extracted_words", extractedWords);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단어 추출 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "단어 추출 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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
            if (templateOpt.isPresent()) {
            PzGridTemplate template = templateOpt.get();
            String html = gridRenderer.renderGrid(template.getGridPattern(), template.getWordPositions(), cellSize, showNumbers);
            int gridSize = gridRenderer.calculateGridSize(template.getGridPattern());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("html", html);
            response.put("gridSize", gridSize);
            response.put("cellSize", cellSize);
            response.put("showNumbers", showNumbers);
                response.put("template", template);
            
            return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "템플릿을 찾을 수 없습니다: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "그리드 렌더링 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}