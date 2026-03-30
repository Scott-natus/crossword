package com.example.board.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/index.html", "/game.html", "/puzzle", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/Korean/**", "/K-pop/**", "/K-POP/**", "/K-movie/**", "/K-MOVIE/**", "/K-Drama/**", "/K-DRAMA/**", "/K-Culture/**", "/K-CULTURE/**").permitAll()
                .requestMatchers("/board/**").permitAll()
                .requestMatchers("/api/puzzle/**", "/api/auth/**", "/api/theme-puzzle/**").permitAll()
                .requestMatchers("/admin/api/**").permitAll() // 관리자 API는 인증 제외 (페이지는 인증)
                .requestMatchers("/admin/templates/create").permitAll() // 템플릿 생성 페이지는 인증 제외
                .requestMatchers("/login").permitAll()
                .requestMatchers("/admin/**").authenticated() // 관리자 메뉴 페이지는 인증 필요
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .userDetailsService(userDetailsService)
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
        delegate.setDefaultTargetUrl("/");
        delegate.setAlwaysUseDefaultTargetUrl(false);

        return (request, response, authentication) -> {
            String redirect = request.getParameter("redirect");
            boolean safe = redirect != null && !redirect.isBlank() && redirect.startsWith("/");
            if (safe) {
                response.sendRedirect(redirect);
                return;
            }
            try {
                delegate.onAuthenticationSuccess(request, response, authentication);
            } catch (Exception e) {
                response.sendRedirect("/");
            }
        };
    }
}