package com.example.crossword.controller;

import com.example.crossword.service.WordExtractionService;
import com.example.crossword.service.IntersectionCalculationService;
import com.example.crossword.service.TemplateValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 웹 페이지 컨트롤러
 * 정적 파일 서빙을 위한 컨트롤러
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-17
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final WordExtractionService wordExtractionService;
    private final IntersectionCalculationService intersectionCalculationService;
    private final TemplateValidationService templateValidationService;

    /**
     * 메인 페이지 - 정적 리소스 사용 (index.html)
     */
    @GetMapping({"/", "/K-CrossWord/"})
    public ResponseEntity<String> index() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("페이지를 로드할 수 없습니다.");
        }
    }


    /**
     * 퍼즐게임 페이지
     */
    @GetMapping("/puzzle")
    public ResponseEntity<String> puzzle() {
        return index();
    }
    
    /**
     * 인증 상태 확인 API
     */
    @GetMapping("/api/auth/status")
    @ResponseBody
    public Map<String, Object> checkAuthStatus(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Spring Security에서 현재 사용자 정보 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
                
                // 인증된 사용자
                response.put("authenticated", true);
                response.put("userType", "user");
                response.put("username", authentication.getName());
                
            } else {
                // 비인증 사용자
                response.put("authenticated", false);
                response.put("userType", "guest");
                response.put("username", null);
            }
            
        } catch (Exception e) {
            response.put("authenticated", false);
            response.put("userType", "guest");
            response.put("username", null);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    /**
     * 실제 게임 페이지
     */
    @GetMapping("/game.html")
    public ResponseEntity<String> game() {
        try {
            Resource resource = new ClassPathResource("static/game.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * CSS 파일 서빙
     */
    @GetMapping({"/css/{filename}", "/K-CrossWord/css/{filename}"})
    public ResponseEntity<String> css(@PathVariable String filename) {
        try {
            Resource resource = new ClassPathResource("static/css/" + filename);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("text/css"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * JavaScript 파일 서빙
     */
    @GetMapping({"/js/{filename}", "/K-CrossWord/js/{filename}"})
    public ResponseEntity<String> js(@PathVariable String filename) {
        try {
            Resource resource = new ClassPathResource("static/js/" + filename);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/javascript"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Favicon 서빙 (Mixed Content 문제 해결)
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<byte[]> favicon() {
        try {
            Resource resource = new ClassPathResource("static/favicon.ico");
            byte[] content = resource.getInputStream().readAllBytes();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/x-icon"));
            headers.setContentLength(content.length);
            headers.setCacheControl("public, max-age=31536000"); // 1년 캐시
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            // favicon 파일이 없으면 빈 응답
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/x-icon"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new byte[0]);
        }
    }
    
    /**
     * 추가 favicon 경로들 (브라우저 호환성)
     */
    @GetMapping("/K-CrossWord/favicon.ico")
    public ResponseEntity<byte[]> faviconAlternative() {
        return favicon();
    }

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public ResponseEntity<String> login() {
        try {
            // 간단한 로그인 페이지 HTML 생성
            String content = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>로그인 - 크로스워드 퍼즐</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body class="bg-light">
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-header">
                                        <h3 class="text-center">크로스워드 퍼즐 관리자 로그인</h3>
                                    </div>
                                    <div class="card-body">
                                        <form method="post" action="/login">
                                            <div class="mb-3">
                                                <label for="email" class="form-label">이메일</label>
                                                <input type="email" class="form-control" id="email" name="email" required>
                                            </div>
                                            <div class="mb-3">
                                                <label for="password" class="form-label">비밀번호</label>
                                                <input type="password" class="form-control" id="password" name="password" required>
                                            </div>
                                            <div class="d-grid">
                                                <button type="submit" class="btn btn-primary">로그인</button>
                                            </div>
                                        </form>
                                        <div class="mt-3 text-center">
                                            <a href="https://natus250601.viewdns.net/" class="btn btn-secondary">메인으로 돌아가기</a>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 관리자 메인 페이지
     */
    @GetMapping({"/admin/", "/admin"})
    public ResponseEntity<String> adminIndex() {
        try {
            Resource resource = new ClassPathResource("static/admin/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 단어 관리 페이지
     */
    @GetMapping({"/admin/words"})
    public ResponseEntity<String> adminWords() {
        try {
            Resource resource = new ClassPathResource("static/admin/words/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 관리자 통계 API는 WordManagementController로 이동됨

    // 단어 데이터 API는 WordManagementController로 이동됨

    // AI 힌트 생성 페이지는 HintGeneratorWebController로 이동됨

    /**
     * 레벨 관리 페이지
     */
    @GetMapping({"/admin/levels"})
    public ResponseEntity<String> adminLevels() {
        try {
            Resource resource = new ClassPathResource("static/admin/levels/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 그리드 템플릿 관리 페이지
     */
    @GetMapping({"/admin/grid-templates"})
    public ResponseEntity<String> adminGridTemplates() {
        try {
            Resource resource = new ClassPathResource("static/admin/grid-templates/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping({"/admin/grid-templates/create"})
    public ResponseEntity<String> adminGridTemplatesCreate() {
        try {
            Resource resource = new ClassPathResource("static/admin/grid-templates/create.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping({"/admin/grid-templates/detail"})
    public ResponseEntity<String> adminGridTemplatesDetail() {
        try {
            Resource resource = new ClassPathResource("static/admin/grid-templates/detail.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 단어 추출 API
     * Laravel의 extractWords 메서드와 동일한 기능
     */
    @PostMapping("/admin/api/grid-template-management/extract-words")
    public ResponseEntity<Map<String, Object>> extractWords(@RequestBody Map<String, Object> request) {
        try {
            Long templateId = Long.valueOf(request.get("template_id").toString());
            
            Map<String, Object> result = wordExtractionService.extractWords(templateId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("단어 추출 API 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "단어 추출 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 교차점 계산 API
     * 그리드 패턴에서 교차점 정보를 계산
     */
    @PostMapping("/admin/api/grid-template-management/calculate-intersections")
    public ResponseEntity<Map<String, Object>> calculateIntersections(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<List<Integer>> gridPattern = (List<List<Integer>>) request.get("grid_pattern");
            
            if (gridPattern == null || gridPattern.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "그리드 패턴이 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 그리드 패턴 유효성 검증
            Map<String, Object> validation = intersectionCalculationService.validateGridPattern(gridPattern);
            
            if (!(Boolean) validation.get("valid")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "그리드 패턴이 유효하지 않습니다.");
                errorResponse.put("errors", validation.get("errors"));
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 단어 위치 정보 추출
            List<Map<String, Object>> wordPositions = intersectionCalculationService.extractWordPositions(gridPattern);
            
            // 교차점 분석
            Map<Integer, Map<String, Object>> intersectionAnalysis = 
                    intersectionCalculationService.analyzeWordPositions(wordPositions);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("grid_analysis", Map.of(
                    "word_count", validation.get("wordCount"),
                    "intersection_count", validation.get("intersectionCount"),
                    "black_cells", validation.get("blackCells"),
                    "white_cells", validation.get("whiteCells")
            ));
            result.put("word_positions", wordPositions);
            result.put("intersection_analysis", intersectionAnalysis);
            result.put("warnings", validation.get("warnings"));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("교차점 계산 API 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "교차점 계산 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 템플릿 검증 API
     * 템플릿 생성/수정 시 유효성 검증
     */
    @PostMapping("/admin/api/grid-template-management/validate-template")
    public ResponseEntity<Map<String, Object>> validateTemplate(@RequestBody Map<String, Object> request) {
        try {
            String validationType = (String) request.get("type"); // "create" 또는 "update"
            Long templateId = null;
            
            if ("update".equals(validationType)) {
                templateId = Long.valueOf(request.get("template_id").toString());
            }
            
            Map<String, Object> result;
            if ("update".equals(validationType)) {
                result = templateValidationService.validateTemplateUpdate(templateId, request);
            } else {
                result = templateValidationService.validateTemplateCreation(request);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("validation", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("템플릿 검증 API 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "템플릿 검증 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 레벨 조건 조회 API
     * 레벨 선택 시 조건 정보 제공
     */
    @GetMapping("/admin/api/grid-template-management/level-conditions/{levelId}")
    public ResponseEntity<Map<String, Object>> getLevelConditions(@PathVariable Integer levelId) {
        try {
            Map<String, Object> result = templateValidationService.getLevelConditions(levelId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("레벨 조건 조회 API 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "레벨 조건 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 그리드 패턴 실시간 검증 API
     * 프론트엔드에서 실시간으로 그리드 패턴 검증
     */
    @PostMapping("/admin/api/grid-template-management/validate-grid-pattern")
    public ResponseEntity<Map<String, Object>> validateGridPattern(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<List<Integer>> gridPattern = (List<List<Integer>>) request.get("grid_pattern");
            
            if (gridPattern == null || gridPattern.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "그리드 패턴이 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> result = templateValidationService.validateGridPatternRealtime(gridPattern);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("validation", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("그리드 패턴 검증 API 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "그리드 패턴 검증 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }



}
