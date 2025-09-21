package com.example.crossword;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 크로스워드 퍼즐 시스템 메인 애플리케이션
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-16
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 1800 // 30분
    // redisNamespace는 application.properties에서 설정
)
public class CrosswordApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrosswordApplication.class, args);
    }
}
