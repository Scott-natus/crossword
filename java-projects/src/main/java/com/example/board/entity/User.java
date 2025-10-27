package com.example.board.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-10-27
 */
@Entity
@Table(name = "users", schema = "public")
@Getter
@Setter
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status = "active"; // active, inactive, banned
    
    @Column(name = "games_played")
    private Integer gamesPlayed = 0;
    
    @Column(name = "total_score")
    private Integer totalScore = 0;
    
    @Column(name = "wins")
    private Integer wins = 0;
    
    @Column(name = "level")
    private Integer level;
    
    @Column(name = "is_admin")
    private Boolean isAdmin = false;
    
    @Column(name = "is_guest")
    private Boolean isGuest = false;
    
    @Column(name = "guest_id")
    private java.util.UUID guestId;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "total_ddongsun_power")
    private Integer totalDdongsunPower = 0;
    
    @Column(name = "current_level")
    private String currentLevel = "브론즈";
    
    @Column(name = "profile_image")
    private String profileImage;
    
    @Column(name = "nickname")
    private String nickname;
    
    @Column(name = "avatar")
    private String avatar;
    
    @Column(name = "provider")
    private String provider;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;
    
    @Column(name = "remember_token")
    private String rememberToken;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 추가 메서드들
    public String getInitial() {
        return name != null && !name.isEmpty() ? name.charAt(0) + "" : "U";
    }
    
    public boolean isActive() {
        return "active".equals(status);
    }
    
    public boolean isBanned() {
        return "banned".equals(status);
    }
    
    public boolean isInactive() {
        return "inactive".equals(status);
    }
}