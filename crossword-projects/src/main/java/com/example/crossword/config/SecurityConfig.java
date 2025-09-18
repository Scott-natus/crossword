package com.example.crossword.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * API 엔드포인트에 대한 보안 정책을 정의
 * 임시로 모든 요청을 허용하도록 설정 (정적 파일 403 문제 해결용)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF 비활성화
            .authorizeHttpRequests(authz -> authz
                // 모든 요청을 허용 (임시 테스트용)
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> httpBasic.disable()) // HTTP Basic 인증 비활성화
            .formLogin(formLogin -> formLogin.disable()) // 폼 로그인 비활성화
            .headers(headers -> headers.disable()); // 보안 헤더 비활성화

        return http.build();
    }
}