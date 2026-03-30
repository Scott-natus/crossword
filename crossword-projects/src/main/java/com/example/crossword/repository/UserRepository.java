package com.example.crossword.repository;

import com.example.crossword.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 리포지토리
 * 8080 포트 게시판 서비스와 동일한 구조로 통합
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
    
    /**
     * 게스트 ID로 사용자 조회
     */
    Optional<User> findByGuestId(UUID guestId);
    
    /**
     * 게스트 이메일로 사용자 조회
     */
    Optional<User> findByEmailAndIsGuestTrue(String email);
}
