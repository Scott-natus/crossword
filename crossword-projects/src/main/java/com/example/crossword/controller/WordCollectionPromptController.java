package com.example.crossword.controller;

import com.example.crossword.service.WordCollectionPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 단어 수집 프롬프트 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/word-collection-prompt")
@RequiredArgsConstructor
@Slf4j
public class WordCollectionPromptController {
    
    private final WordCollectionPromptService promptService;
    
    /**
     * 현재 프롬프트 타입 조회
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentPromptType() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("currentType", promptService.getCurrentPromptType());
            response.put("availableTypes", promptService.getAvailablePromptTypes());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("현재 프롬프트 타입 조회 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프롬프트 타입 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 프롬프트 타입 변경
     */
    @PostMapping("/set-type")
    public ResponseEntity<Map<String, Object>> setPromptType(@RequestParam String promptType) {
        try {
            log.info("프롬프트 타입 변경 요청: {}", promptType);
            
            promptService.setPromptType(promptType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "프롬프트 타입이 '" + promptType + "'로 변경되었습니다.");
            response.put("currentType", promptService.getCurrentPromptType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("프롬프트 타입 변경 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프롬프트 타입 변경 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 사용 가능한 프롬프트 타입 목록 조회
     */
    @GetMapping("/available-types")
    public ResponseEntity<Map<String, Object>> getAvailablePromptTypes() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("availableTypes", promptService.getAvailablePromptTypes());
            response.put("currentType", promptService.getCurrentPromptType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용 가능한 프롬프트 타입 조회 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프롬프트 타입 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 특정 프롬프트 템플릿 조회
     */
    @GetMapping("/template/{promptType}")
    public ResponseEntity<Map<String, Object>> getPromptTemplate(@PathVariable String promptType) {
        try {
            String template = promptService.getPromptTemplate(promptType);
            
            Map<String, Object> response = new HashMap<>();
            if (template != null) {
                response.put("success", true);
                response.put("promptType", promptType);
                response.put("template", template);
            } else {
                response.put("success", false);
                response.put("message", "존재하지 않는 프롬프트 타입입니다: " + promptType);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("프롬프트 템플릿 조회 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프롬프트 템플릿 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 모든 프롬프트 템플릿 조회
     */
    @GetMapping("/all-templates")
    public ResponseEntity<Map<String, Object>> getAllPromptTemplates() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("templates", promptService.getAllPromptTemplates());
            response.put("currentType", promptService.getCurrentPromptType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("모든 프롬프트 템플릿 조회 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프롬프트 템플릿 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 커스텀 프롬프트 추가
     */
    @PostMapping("/add-custom")
    public ResponseEntity<Map<String, Object>> addCustomPrompt(
            @RequestParam String promptType,
            @RequestParam String promptTemplate) {
        try {
            log.info("커스텀 프롬프트 추가 요청: {}", promptType);
            
            promptService.addCustomPrompt(promptType, promptTemplate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "커스텀 프롬프트가 추가되었습니다: " + promptType);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("커스텀 프롬프트 추가 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "커스텀 프롬프트 추가 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 프롬프트 미리보기 (테스트용)
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewPrompt(
            @RequestParam(defaultValue = "가") String combination,
            @RequestParam(required = false) String promptType) {
        try {
            String originalType = promptService.getCurrentPromptType();
            
            if (promptType != null && !promptType.isEmpty()) {
                promptService.setPromptType(promptType);
            }
            
            String generatedPrompt = promptService.generatePrompt(combination);
            
            // 원래 타입으로 복원
            if (promptType != null && !promptType.isEmpty()) {
                promptService.setPromptType(originalType);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("combination", combination);
            response.put("promptType", promptType != null ? promptType : originalType);
            response.put("generatedPrompt", generatedPrompt);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("프롬프트 미리보기 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프롬프트 미리보기 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 커스텀 프롬프트로 단어 생성 테스트
     */
    @PostMapping("/test-custom")
    public ResponseEntity<Map<String, Object>> testCustomPrompt(
            @RequestParam String combination,
            @RequestParam String category,
            @RequestParam String promptTemplate) {
        try {
            log.info("커스텀 프롬프트 테스트 요청 - 조합: {}, 카테고리: {}", combination, category);
            
            // 임시로 프롬프트 타입 설정
            String originalType = promptService.getCurrentPromptType();
            promptService.addCustomPrompt("temp_test", promptTemplate);
            promptService.setPromptType("temp_test");
            
            // 프롬프트 생성
            String generatedPrompt = promptService.generatePrompt(combination);
            
            // 원래 타입으로 복원
            promptService.setPromptType(originalType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("combination", combination);
            response.put("category", category);
            response.put("generatedPrompt", generatedPrompt);
            response.put("message", "프롬프트 테스트 완료");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("커스텀 프롬프트 테스트 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프롬프트 테스트 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 도움말
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp() {
        Map<String, Object> help = new HashMap<>();
        
        help.put("title", "단어 수집 프롬프트 관리 API");
        help.put("description", "다양한 분야의 단어를 수집하기 위한 프롬프트를 관리합니다.");
        
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("GET /current", "현재 프롬프트 타입 조회");
        endpoints.put("POST /set-type?promptType=science", "프롬프트 타입 변경");
        endpoints.put("GET /available-types", "사용 가능한 프롬프트 타입 목록");
        endpoints.put("GET /template/{promptType}", "특정 프롬프트 템플릿 조회");
        endpoints.put("GET /all-templates", "모든 프롬프트 템플릿 조회");
        endpoints.put("POST /add-custom?promptType=custom&promptTemplate=...", "커스텀 프롬프트 추가");
        endpoints.put("GET /preview?combination=가&promptType=science", "프롬프트 미리보기");
        endpoints.put("GET /help", "API 사용법");
        
        help.put("endpoints", endpoints);
        
        Map<String, Object> promptTypes = new HashMap<>();
        promptTypes.put("general", "일반 단어 (기본값)");
        promptTypes.put("science", "과학/기술 분야");
        promptTypes.put("food", "음식/요리 분야");
        promptTypes.put("sports", "스포츠/운동 분야");
        promptTypes.put("art", "예술/문화 분야");
        promptTypes.put("nature", "동물/자연 분야");
        promptTypes.put("profession", "직업/업무 분야");
        promptTypes.put("travel", "여행/지리 분야");
        promptTypes.put("fashion", "패션/뷰티 분야");
        promptTypes.put("entertainment", "게임/엔터테인먼트 분야");
        
        help.put("promptTypes", promptTypes);
        
        return ResponseEntity.ok(help);
    }
}
