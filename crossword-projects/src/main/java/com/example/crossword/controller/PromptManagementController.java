package com.example.crossword.controller;

import com.example.crossword.service.PromptManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 프롬프트 관리 API 컨트롤러
 * 사용자가 재미나이 API 프롬프트를 직접 수정할 수 있도록 지원
 */
@RestController
@RequestMapping("/api/prompt-management")
@RequiredArgsConstructor
@Slf4j
public class PromptManagementController {
    
    private final PromptManagementService promptManagementService;
    
    /**
     * 현재 프롬프트 조회
     * @return 현재 프롬프트 내용
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentPrompt() {
        log.info("현재 프롬프트 조회 API 호출");
        
        try {
            String currentPrompt = promptManagementService.getCurrentPrompt();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prompt", currentPrompt);
            response.put("message", "현재 프롬프트를 성공적으로 조회했습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("프롬프트 조회 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "프롬프트 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 프롬프트 업데이트
     * @param request 프롬프트 업데이트 요청
     * @return 업데이트 결과
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updatePrompt(@RequestBody Map<String, String> request) {
        log.info("프롬프트 업데이트 API 호출");
        
        try {
            String newPrompt = request.get("prompt");
            
            if (newPrompt == null || newPrompt.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "프롬프트 내용이 비어있습니다.");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = promptManagementService.updatePrompt(newPrompt);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.internalServerError().body(result);
            }
            
        } catch (Exception e) {
            log.error("프롬프트 업데이트 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "프롬프트 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 백업 목록 조회
     * @return 백업 파일 목록
     */
    @GetMapping("/backups")
    public ResponseEntity<Map<String, Object>> getBackupList() {
        log.info("백업 목록 조회 API 호출");
        
        try {
            Map<String, Object> result = promptManagementService.getBackupList();
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.internalServerError().body(result);
            }
            
        } catch (Exception e) {
            log.error("백업 목록 조회 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "백업 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 백업에서 프롬프트 복원
     * @param request 복원 요청
     * @return 복원 결과
     */
    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> restoreFromBackup(@RequestBody Map<String, String> request) {
        log.info("백업에서 프롬프트 복원 API 호출");
        
        try {
            String backupFileName = request.get("backupFileName");
            
            if (backupFileName == null || backupFileName.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "백업 파일명이 비어있습니다.");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = promptManagementService.restoreFromBackup(backupFileName);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.internalServerError().body(result);
            }
            
        } catch (Exception e) {
            log.error("백업에서 프롬프트 복원 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "백업에서 프롬프트 복원 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * API 사용법 안내
     * @return API 사용법
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp() {
        Map<String, Object> help = new HashMap<>();
        
        help.put("title", "프롬프트 관리 API 사용법");
        help.put("endpoints", Map.of(
            "GET /api/prompt-management/current", "현재 프롬프트 조회",
            "POST /api/prompt-management/update", "프롬프트 업데이트 (prompt 파라미터 필요)",
            "GET /api/prompt-management/backups", "백업 목록 조회",
            "POST /api/prompt-management/restore", "백업에서 복원 (backupFileName 파라미터 필요)",
            "GET /api/prompt-management/help", "API 사용법 안내"
        ));
        help.put("examples", Map.of(
            "현재 프롬프트 조회", "GET /api/prompt-management/current",
            "프롬프트 업데이트", "POST /api/prompt-management/update {\"prompt\": \"새로운 프롬프트 내용\"}",
            "백업 목록 조회", "GET /api/prompt-management/backups",
            "백업에서 복원", "POST /api/prompt-management/restore {\"backupFileName\": \"backup-file.txt\"}"
        ));
        help.put("features", new String[]{
            "프롬프트 실시간 수정",
            "자동 백업 생성",
            "백업에서 복원 기능",
            "백업 목록 관리"
        });
        
        return ResponseEntity.ok(help);
    }
}
