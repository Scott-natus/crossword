package com.example.crossword.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 인증 관리 API 컨트롤러
 * 로그인, 회원가입, 로그아웃 등의 REST API를 제공
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    /**
     * 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        log.debug("로그인 요청: {}", credentials.get("email"));
        
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");
            
            // 간단한 인증 로직 (실제로는 데이터베이스에서 사용자 확인 필요)
            if (email != null && password != null) {
                // 테스트 계정 확인
                if ("test@test.com".equals(email) && "123456".equals(password)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "로그인 성공");
                    response.put("data", Map.of(
                        "user", Map.of(
                            "id", 1,
                            "email", email,
                            "name", "테스트 사용자",
                            "is_admin", false
                        ),
                        "authorization", Map.of(
                            "token", "user_token_" + System.currentTimeMillis(),
                            "type", "Bearer"
                        )
                    ));
                    return ResponseEntity.ok(response);
                }
                
                // 관리자 계정 확인
                if ("rainynux@gmail.com".equals(email) && "tngkrrhk".equals(password)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "로그인 성공");
                    response.put("data", Map.of(
                        "user", Map.of(
                            "id", 1,
                            "email", email,
                            "name", "박상우",
                            "is_admin", true
                        ),
                        "authorization", Map.of(
                            "token", "admin_token_" + System.currentTimeMillis(),
                            "type", "Bearer"
                        )
                    ));
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "이메일 또는 비밀번호가 올바르지 않습니다.");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "이메일과 비밀번호를 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("로그인 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "로그인 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 회원가입 API
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> userData) {
        log.debug("회원가입 요청: {}", userData.get("email"));
        
        try {
            String email = userData.get("email");
            String password = userData.get("password");
            String name = userData.get("name");
            
            if (email != null && password != null && name != null) {
                // 간단한 회원가입 로직 (실제로는 데이터베이스에 저장 필요)
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "회원가입 성공");
                response.put("data", Map.of(
                    "user", Map.of(
                        "id", System.currentTimeMillis(), // 임시 ID
                        "email", email,
                        "name", name,
                        "is_admin", false
                    ),
                    "authorization", Map.of(
                        "token", "test_token_" + System.currentTimeMillis(),
                        "type", "Bearer"
                    )
                ));
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "모든 필드를 입력해주세요.");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "회원가입 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 로그아웃 API
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        log.debug("로그아웃 요청");
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그아웃 성공");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "로그아웃 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 사용자 정보 조회 API
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe() {
        log.debug("사용자 정보 조회 요청");
        
        try {
            // 간단한 사용자 정보 반환 (실제로는 토큰에서 사용자 정보 추출 필요)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "id", 1,
                "email", "test@test.com",
                "name", "테스트 사용자",
                "is_admin", false
            ));
            response.put("message", "사용자 정보를 성공적으로 조회했습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "사용자 정보 조회 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 토큰 검증 API
     */
    @PostMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestBody Map<String, String> tokenData) {
        log.debug("토큰 검증 요청");
        
        try {
            String token = tokenData.get("token");
            
            if (token != null && token.startsWith("test_token_")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "토큰이 유효합니다.");
                response.put("data", Map.of(
                    "valid", true,
                    "user", Map.of(
                        "id", 1,
                        "email", "test@test.com",
                        "name", "테스트 사용자",
                        "is_admin", false
                    )
                ));
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "유효하지 않은 토큰입니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("토큰 검증 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "토큰 검증 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
