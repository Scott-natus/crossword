package com.example.board.service;

import com.example.board.entity.User;
import com.example.board.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 관리 서비스
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-10-27
 */
@Service
@Slf4j
public class UserManagementService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 필터 조건에 따른 사용자 목록 조회
     */
    public Page<User> getUsersWithFilters(String status, String dateFilter, String search, Pageable pageable) {
        try {
            // 간단한 필터링 로직으로 변경
            if (status != null && !status.isEmpty() && search != null && !search.isEmpty()) {
                // 상태와 검색어 모두 있는 경우
                return userRepository.findByStatusAndNameContainingIgnoreCaseOrStatusAndEmailContainingIgnoreCase(
                    status, search, status, search, pageable);
            } else if (status != null && !status.isEmpty()) {
                // 상태만 있는 경우
                return userRepository.findByStatus(status, pageable);
            } else if (search != null && !search.isEmpty()) {
                // 검색어만 있는 경우
                return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable);
            } else {
                // 필터가 없는 경우 - 모든 사용자 조회
                Page<User> users = userRepository.findAll(pageable);
                log.info("조회된 사용자 수: {}, 첫 번째 사용자 정보: {}", 
                    users.getTotalElements(), 
                    users.getContent().isEmpty() ? "없음" : users.getContent().get(0));
                return users;
            }
        } catch (Exception e) {
            log.error("사용자 목록 조회 중 오류 발생", e);
            throw new RuntimeException("사용자 목록을 불러올 수 없습니다.", e);
        }
    }

    /**
     * 사용자 통계 조회
     */
    public Map<String, Object> getUserStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 전체 사용자 수
            long totalUsers = userRepository.count();
            statistics.put("totalUsers", totalUsers);
            
            // 활성 사용자 수
            long activeUsers = userRepository.countByStatus("active");
            statistics.put("activeUsers", activeUsers);
            
            // 오늘 가입한 사용자 수
            long newUsersToday = userRepository.countNewUsersToday();
            statistics.put("newUsersToday", newUsersToday);
            
            // 현재 온라인 사용자 수 (마지막 로그인이 1시간 이내)
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long onlineUsers = userRepository.countByStatusAndLastLoginAtAfter("active", oneHourAgo);
            statistics.put("onlineUsers", onlineUsers);
            
            return statistics;
        } catch (Exception e) {
            log.error("사용자 통계 조회 중 오류 발생", e);
            throw new RuntimeException("통계 정보를 불러올 수 없습니다.", e);
        }
    }
    
    /**
     * 사용자 게임 통계 업데이트 (puzzle_game_records에서 계산)
     */
    @Transactional
    public void updateUserGameStatistics() {
        try {
            int updatedCount = userRepository.updateUserGameStatistics();
            log.info("사용자 게임 통계 업데이트 완료: {}명의 사용자 통계가 업데이트되었습니다.", updatedCount);
        } catch (Exception e) {
            log.error("사용자 게임 통계 업데이트 중 오류 발생", e);
            throw new RuntimeException("게임 통계 업데이트에 실패했습니다.", e);
        }
    }
    
    /**
     * 사용자 관리자 권한 변경
     */
    @Transactional
    public boolean changeAdminStatus(Long userId, Boolean isAdmin) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("사용자를 찾을 수 없습니다: {}", userId);
                return false;
            }
            
            user.setIsAdmin(isAdmin);
            userRepository.save(user);
            
            log.info("사용자 {}의 관리자 권한이 {}로 변경되었습니다.", user.getEmail(), isAdmin ? "부여" : "제거");
            return true;
        } catch (Exception e) {
            log.error("관리자 권한 변경 중 오류 발생", e);
            return false;
        }
    }

    /**
     * ID로 사용자 조회
     */
    public User getUserById(Long id) {
        try {
            Optional<User> user = userRepository.findById(id);
            return user.orElse(null);
        } catch (Exception e) {
            log.error("사용자 조회 중 오류 발생", e);
            throw new RuntimeException("사용자 정보를 불러올 수 없습니다.", e);
        }
    }

    /**
     * 사용자 상태 변경
     */
    @Transactional
    public boolean changeUserStatus(Long userId, String status, String reason) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }
            
            User user = userOpt.get();
            user.setStatus(status);
            user.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(user);
            
            log.info("사용자 {} 상태 변경: {} -> {} (사유: {})", 
                user.getEmail(), user.getStatus(), status, reason);
            
            return true;
        } catch (Exception e) {
            log.error("사용자 상태 변경 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 일괄 상태 변경
     */
    @Transactional
    public int bulkChangeUserStatus(List<Long> userIds, String status) {
        try {
            List<User> users = userRepository.findByIdIn(userIds);
            int successCount = 0;
            
            for (User user : users) {
                user.setStatus(status);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                successCount++;
            }
            
            log.info("일괄 상태 변경 완료: {}명의 사용자를 {}로 변경", successCount, status);
            return successCount;
        } catch (Exception e) {
            log.error("일괄 상태 변경 중 오류 발생", e);
            return 0;
        }
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    public boolean deleteUser(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }
            
            userRepository.deleteById(userId);
            
            log.info("사용자 삭제 완료: ID {}", userId);
            return true;
        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 사용자 데이터 CSV 내보내기
     */
    public String exportUsersToCsv() {
        try {
            List<User> users = userRepository.findAll();
            
            StringBuilder csv = new StringBuilder();
            csv.append("ID,이름,이메일,상태,가입일,마지막 로그인,게임 플레이,총 점수,승리,레벨\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (User user : users) {
                csv.append(user.getId()).append(",");
                csv.append(escapeCsv(user.getName())).append(",");
                csv.append(escapeCsv(user.getEmail())).append(",");
                csv.append(escapeCsv(user.getStatus())).append(",");
                csv.append(user.getCreatedAt() != null ? user.getCreatedAt().format(formatter) : "").append(",");
                csv.append(user.getLastLoginAt() != null ? user.getLastLoginAt().format(formatter) : "").append(",");
                csv.append(user.getGamesPlayed() != null ? user.getGamesPlayed() : 0).append(",");
                csv.append(user.getTotalScore() != null ? user.getTotalScore() : 0).append(",");
                csv.append(user.getWins() != null ? user.getWins() : 0).append(",");
                csv.append(user.getLevel() != null ? user.getLevel() : 1).append("\n");
            }
            
            return csv.toString();
        } catch (Exception e) {
            log.error("사용자 데이터 내보내기 중 오류 발생", e);
            throw new RuntimeException("데이터 내보내기에 실패했습니다.", e);
        }
    }

    /**
     * CSV 이스케이프 처리
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}
