package com.example.crossword.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (개발용)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/api/**").permitAll() // API는 허용
                .requestMatchers("/K-CrossWord/admin/api/**").permitAll() // 관리자 API는 허용
                .requestMatchers("/K-CrossWord/admin/**").authenticated() // 관리자 페이지는 인증 필요
                .anyRequest().permitAll() // 나머지는 허용
            )
            .formLogin(form -> form
                .loginPage("https://natus250601.viewdns.net/login") // 8080 포트 로그인 페이지로 리다이렉트
                .defaultSuccessUrl("https://natus250601.viewdns.net/K-CrossWord/admin/hint-generator", true)
                .permitAll()
            )
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