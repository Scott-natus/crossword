package com.example.board.controller;

import com.example.board.entity.User;
import com.example.board.service.UserManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용자 관리 컨트롤러
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-10-27
 */
@RestController
@RequestMapping("/admin/api/user-management")
@CrossOrigin(origins = "*") // 개발용 CORS 허용
@Slf4j
public class UserManagementController {

    @Autowired
    private UserManagementService userManagementService;

    /**
     * 사용자 목록 조회 (DataTables용)
     */
    @GetMapping("/users-ajax")
    public ResponseEntity<Map<String, Object>> getUsersAjax(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int draw) {

        try {
            int page = start / length; // DataTables의 start는 offset이므로 페이지로 변환
            
            // 정렬 설정
            Sort.Direction direction = sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, sortField);
            Pageable pageable = PageRequest.of(page, length, sort);

            // 사용자 목록 조회
            Page<User> userPage = userManagementService.getUsersWithFilters(
                status, dateFilter, search, pageable
            );

            // DataTables 형식으로 변환
            List<Map<String, Object>> userData = userPage.getContent().stream()
                .map(this::convertUserToMap)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", userPage.getTotalElements());
            response.put("recordsFiltered", userPage.getTotalElements());
            response.put("data", userData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 목록 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("draw", draw);
            errorResponse.put("recordsTotal", 0);
            errorResponse.put("recordsFiltered", 0);
            errorResponse.put("data", List.of());
            errorResponse.put("error", "사용자 목록을 불러올 수 없습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        try {
            Map<String, Object> statistics = userManagementService.getUserStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("사용자 통계 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "통계 정보를 불러올 수 없습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 상세 정보 조회
     */
    @GetMapping("/user/{id}")
    public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        try {
            User user = userManagementService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> userDetail = convertUserToDetailMap(user);
            return ResponseEntity.ok(userDetail);
        } catch (Exception e) {
            log.error("사용자 상세 정보 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 정보를 불러올 수 없습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 상태 변경
     */
    @PutMapping("/user/{id}/status")
    public ResponseEntity<Map<String, Object>> changeUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String reason = request.get("reason");
            
            boolean success = userManagementService.changeUserStatus(id, status, reason);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "사용자 상태가 변경되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "사용자 상태 변경에 실패했습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 상태 변경 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자 상태 변경 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 일괄 상태 변경
     */
    @PutMapping("/bulk-status-change")
    public ResponseEntity<Map<String, Object>> bulkStatusChange(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) request.get("userIds");
            String status = (String) request.get("status");
            
            int successCount = userManagementService.bulkChangeUserStatus(userIds, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", successCount + "명의 사용자 상태가 변경되었습니다.");
            response.put("successCount", successCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("일괄 상태 변경 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "일괄 상태 변경 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 삭제
     */
    @DeleteMapping("/user/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        try {
            boolean success = userManagementService.deleteUser(id);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "사용자가 삭제되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "사용자 삭제에 실패했습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자 삭제 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 데이터 내보내기
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportUserData() {
        try {
            // CSV 형식으로 사용자 데이터 내보내기
            String csvData = userManagementService.exportUsersToCsv();
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=users_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv")
                .body(csvData);
        } catch (Exception e) {
            log.error("사용자 데이터 내보내기 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 내보내기에 실패했습니다.");
        }
    }

    /**
     * User 엔티티를 Map으로 변환 (테이블용)
     */
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("status", user.getStatus());
        userMap.put("created_at", user.getCreatedAt());
        userMap.put("last_login_at", user.getLastLoginAt());
        userMap.put("games_played", user.getGamesPlayed());
        return userMap;
    }

    /**
     * User 엔티티를 상세 Map으로 변환 (모달용)
     */
    private Map<String, Object> convertUserToDetailMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("status", user.getStatus());
        userMap.put("created_at", user.getCreatedAt());
        userMap.put("last_login_at", user.getLastLoginAt());
        userMap.put("games_played", user.getGamesPlayed());
        userMap.put("total_score", user.getTotalScore());
        userMap.put("wins", user.getWins());
        userMap.put("level", user.getLevel());
        return userMap;
    }
}
