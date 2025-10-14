package com.example.crossword.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * 8080 포트 게시판 서비스와 동일한 인증 시스템으로 통합
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (개발용)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/K-CrossWord/**").permitAll() // 퍼즐게임 전체 경로 허용
                .requestMatchers("/api/puzzle-game/**").permitAll() // 퍼즐게임 API 허용
                .requestMatchers("/api/auth/**").permitAll() // 인증 상태 확인 API 허용
                .requestMatchers("/admin/api/**").permitAll() // API는 임시로 인증 제외 (테스트용)
                .requestMatchers("/admin/custom-word-collection/**").permitAll() // 커스텀 단어 수집 API 인증 제외
                .requestMatchers("/admin/**").authenticated() // 관리자 페이지는 인증 필수
                .anyRequest().permitAll() // 나머지 모든 요청 허용
            )
            .formLogin(form -> form
                .loginPage("https://natus250601.viewdns.net/login") // 8080 로그인 페이지로 리다이렉트
                .defaultSuccessUrl("/admin/hint-generator", true)
                .permitAll()
            )
            .userDetailsService(userDetailsService)
            .logout(logout -> logout
                .logoutSuccessUrl("https://natus250601.viewdns.net/")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}