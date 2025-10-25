package com.example.board.controller;

import com.example.board.entity.User;
import com.example.board.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        log.debug("로그인 요청: {}", credentials.get("email"));
        
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");
            
            // 데이터베이스에서 사용자 정보 조회
            if (email != null && password != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // BCrypt로 암호화된 비밀번호 확인
                    if (user.getPassword() != null && passwordEncoder.matches(password, user.getPassword())) {
                        // 세션에 사용자 정보 저장
                        request.getSession().setAttribute("userEmail", user.getEmail());
                        request.getSession().setAttribute("userName", user.getName());
                        request.getSession().setAttribute("userId", user.getId());
                        request.getSession().setAttribute("isAdmin", user.getIsAdmin() != null ? user.getIsAdmin() : false);
                        
                        // 세션 ID 로그 출력
                        String sessionId = request.getSession().getId();
                        log.info("세션 생성 완료 - Session ID: {}, User: {}", sessionId, user.getEmail());
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "로그인 성공");
                        response.put("data", Map.of(
                            "user", Map.of(
                                "id", user.getId(),
                                "email", user.getEmail(),
                                "name", user.getName(),
                                "is_admin", user.getIsAdmin() != null ? user.getIsAdmin() : false
                            ),
                            "authorization", Map.of(
                                "token", (user.getIsAdmin() != null && user.getIsAdmin()) ? 
                                    "admin_token_" + System.currentTimeMillis() : 
                                    "user_token_" + System.currentTimeMillis(),
                                "type", "Bearer"
                            )
                        ));
                        log.debug("로그인 성공, 세션에 사용자 정보 저장: {}", user.getEmail());
                        return ResponseEntity.ok(response);
                    }
                }
                
                // 로그인 실패
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "이메일 또는 비밀번호가 올바르지 않습니다.");
                return ResponseEntity.badRequest().body(response);
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
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        log.debug("로그아웃 요청");
        
        try {
            // 세션 무효화
            request.getSession().invalidate();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그아웃 성공");
            log.debug("로그아웃 성공, 세션 무효화");
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
     * 인증 상태 확인 API
     */
    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(HttpServletRequest request) {
        log.debug("인증 상태 확인 요청");
        
        try {
            // Spring Security 인증 정보 확인
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            Map<String, Object> response = new HashMap<>();
            
            if (authentication != null && authentication.isAuthenticated() && 
                !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                // 인증된 사용자
                String email = authentication.getName();
                
                // 데이터베이스에서 사용자 정보 조회
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    response.put("authenticated", true);
                    response.put("userType", "user");
                    response.put("user", Map.of(
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "is_admin", user.getIsAdmin() != null ? user.getIsAdmin() : false
                    ));
                    response.put("message", "인증된 사용자");
                    log.debug("인증된 사용자: {}", email);
                } else {
                    response.put("authenticated", false);
                    response.put("userType", "guest");
                    response.put("user", null);
                    response.put("message", "사용자 정보를 찾을 수 없습니다.");
                }
            } else {
                // 비인증 사용자 (게스트 모드)
                response.put("authenticated", false);
                response.put("userType", "guest");
                response.put("user", null);
                response.put("message", "비인증 사용자 (게스트 모드)");
                log.debug("비인증 사용자 (게스트 모드)");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("인증 상태 확인 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("userType", "guest");
            response.put("user", null);
            response.put("message", "인증 상태 확인 중 오류가 발생했습니다.");
            return ResponseEntity.ok(response); // 오류가 발생해도 게스트 모드로 처리
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