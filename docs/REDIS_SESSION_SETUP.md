# Redis 세션 관리 시스템 설정 및 테스트

## 📅 작성일: 2025-09-16
## 🎯 목표: Spring Boot 애플리케이션을 위한 Redis 세션 관리 시스템 구축

---

## 📊 현재 Redis 상태

### 🔧 Redis 서버 정보
- **버전**: Redis 7.0.15
- **상태**: 정상 실행 중 (Active: active (running))
- **포트**: 6379
- **바인딩**: 127.0.0.1 -::1
- **메모리 사용량**: 1.12M
- **PID**: 49946

### 📋 Redis 설정 정보
```bash
# 기본 설정
port: 6379
bind: 127.0.0.1 -::1
databases: 16
maxclients: 10000
timeout: 0
tcp-keepalive: 300

# 메모리 설정
maxmemory: 0 (무제한)
maxmemory-policy: noeviction

# 지속성 설정
save: 3600 1 300 100 60 10000
appendonly: no
rdbcompression: yes

# 로그 설정
loglevel: notice
logfile: /var/log/redis/redis-server.log
```

---

## 🚀 Spring Boot Redis 설정

### 1단계: 의존성 추가

#### 📦 build.gradle 설정
```gradle
dependencies {
    // 기존 의존성들...
    
    // Redis 의존성
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.session:spring-session-data-redis'
    
    // Lettuce 연결 풀
    implementation 'org.apache.commons:commons-pool2'
    
    // JSON 직렬화
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
}
```

### 2단계: Redis 설정 클래스

#### ⚙️ RedisConfig.java
```java
package com.example.crossword.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

@Configuration
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 1800, // 30분
    redisNamespace = "crossword:session"
)
public class RedisConfig {

    @Value("${spring.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.timeout:2000}")
    private int redisTimeout;

    @Value("${spring.redis.lettuce.pool.max-active:8}")
    private int maxActive;

    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.redis.lettuce.pool.min-idle:0}")
    private int minIdle;

    @Value("${spring.redis.lettuce.pool.max-wait:2000}")
    private long maxWait;

    /**
     * Redis 연결 팩토리 설정
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis 서버 설정
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        serverConfig.setDatabase(redisDatabase);
        if (!redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        // 연결 풀 설정
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWaitMillis(maxWait);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        // Lettuce 클라이언트 설정
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofMillis(redisTimeout))
            .shutdownTimeout(Duration.ofMillis(100))
            .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    /**
     * RedisTemplate 설정
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON 직렬화 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 키 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 값 직렬화
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 기본 직렬화
        template.setDefaultSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 세션 RedisTemplate 설정
     */
    @Bean("sessionRedisTemplate")
    public RedisTemplate<String, Object> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 세션용 직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
```

### 3단계: application.properties 설정

#### ⚙️ Redis 설정
```properties
# Redis 설정
spring.redis.host=127.0.0.1
spring.redis.port=6379
spring.redis.database=0
spring.redis.password=
spring.redis.timeout=2000ms

# Lettuce 연결 풀 설정
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=0
spring.redis.lettuce.pool.max-wait=2000ms

# Spring Session 설정
spring.session.store-type=redis
spring.session.redis.namespace=crossword:session
spring.session.redis.flush-mode=on_save
spring.session.timeout=1800s

# 로깅 설정
logging.level.org.springframework.session=DEBUG
logging.level.org.springframework.data.redis=DEBUG
```

### 4단계: 세션 관리 서비스

#### 🎮 SessionService.java
```java
package com.example.crossword.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_PREFIX = "crossword:session:";
    private static final String USER_SESSION_PREFIX = "crossword:user:";
    private static final String GAME_SESSION_PREFIX = "crossword:game:";

    /**
     * 사용자 세션 정보 저장
     */
    public void setUserSession(String sessionId, Object userData) {
        String key = USER_SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, userData, Duration.ofMinutes(30));
    }

    /**
     * 사용자 세션 정보 조회
     */
    public Object getUserSession(String sessionId) {
        String key = USER_SESSION_PREFIX + sessionId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 게임 세션 정보 저장
     */
    public void setGameSession(String sessionId, Object gameData) {
        String key = GAME_SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, gameData, Duration.ofHours(2));
    }

    /**
     * 게임 세션 정보 조회
     */
    public Object getGameSession(String sessionId) {
        String key = GAME_SESSION_PREFIX + sessionId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 세션 만료 시간 연장
     */
    public void extendSession(String sessionId, Duration duration) {
        String userKey = USER_SESSION_PREFIX + sessionId;
        String gameKey = GAME_SESSION_PREFIX + sessionId;
        
        redisTemplate.expire(userKey, duration);
        redisTemplate.expire(gameKey, duration);
    }

    /**
     * 세션 삭제
     */
    public void deleteSession(String sessionId) {
        String userKey = USER_SESSION_PREFIX + sessionId;
        String gameKey = GAME_SESSION_PREFIX + sessionId;
        
        redisTemplate.delete(userKey);
        redisTemplate.delete(gameKey);
    }

    /**
     * 세션 존재 여부 확인
     */
    public boolean hasSession(String sessionId) {
        String userKey = USER_SESSION_PREFIX + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
    }

    /**
     * 세션 TTL 조회
     */
    public long getSessionTTL(String sessionId) {
        String userKey = USER_SESSION_PREFIX + sessionId;
        Long ttl = redisTemplate.getExpire(userKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
}
```

### 5단계: 세션 테스트 컨트롤러

#### 🧪 SessionTestController.java
```java
package com.example.crossword.controller;

import com.example.crossword.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionTestController {

    @Autowired
    private SessionService sessionService;

    /**
     * 세션 정보 저장 테스트
     */
    @PostMapping("/test/set")
    public ResponseEntity<Map<String, Object>> setSessionData(
            HttpSession session,
            @RequestBody Map<String, Object> data) {
        
        String sessionId = session.getId();
        
        // 세션 데이터 저장
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", data.get("userId"));
        sessionData.put("username", data.get("username"));
        sessionData.put("loginTime", LocalDateTime.now());
        sessionData.put("testData", data);
        
        sessionService.setUserSession(sessionId, sessionData);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("message", "세션 데이터 저장 완료");
        response.put("data", sessionData);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 정보 조회 테스트
     */
    @GetMapping("/test/get")
    public ResponseEntity<Map<String, Object>> getSessionData(HttpSession session) {
        String sessionId = session.getId();
        
        Object sessionData = sessionService.getUserSession(sessionId);
        long ttl = sessionService.getSessionTTL(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("hasSession", sessionService.hasSession(sessionId));
        response.put("ttl", ttl);
        response.put("data", sessionData);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 게임 세션 테스트
     */
    @PostMapping("/test/game")
    public ResponseEntity<Map<String, Object>> setGameSession(
            HttpSession session,
            @RequestBody Map<String, Object> gameData) {
        
        String sessionId = session.getId();
        
        Map<String, Object> gameSessionData = new HashMap<>();
        gameSessionData.put("gameId", gameData.get("gameId"));
        gameSessionData.put("level", gameData.get("level"));
        gameSessionData.put("startTime", LocalDateTime.now());
        gameSessionData.put("score", 0);
        gameSessionData.put("hintsUsed", 0);
        
        sessionService.setGameSession(sessionId, gameSessionData);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("message", "게임 세션 저장 완료");
        response.put("gameData", gameSessionData);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 삭제 테스트
     */
    @DeleteMapping("/test/delete")
    public ResponseEntity<Map<String, Object>> deleteSession(HttpSession session) {
        String sessionId = session.getId();
        
        sessionService.deleteSession(sessionId);
        session.invalidate();
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("message", "세션 삭제 완료");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Redis 연결 테스트
     */
    @GetMapping("/test/redis")
    public ResponseEntity<Map<String, Object>> testRedisConnection() {
        try {
            // Redis 연결 테스트
            String testKey = "test:connection:" + System.currentTimeMillis();
            String testValue = "Redis connection test";
            
            sessionService.setUserSession(testKey, testValue);
            Object retrievedValue = sessionService.getUserSession(testKey);
            sessionService.deleteSession(testKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Redis 연결 정상");
            response.put("testValue", retrievedValue);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Redis 연결 실패: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
```

---

## 🧪 Redis 세션 테스트

### 1단계: 기본 연결 테스트

#### 🔍 Redis 연결 확인
```bash
# Redis 서버 상태 확인
redis-cli ping

# Redis 정보 확인
redis-cli info server | grep redis_version
redis-cli info memory | grep used_memory_human

# Redis 설정 확인
redis-cli config get port
redis-cli config get bind
redis-cli config get databases
```

### 2단계: Spring Boot 애플리케이션 테스트

#### 🚀 애플리케이션 시작 및 테스트
```bash
# Spring Boot 애플리케이션 시작
cd /var/www/html/java-projects
./gradlew bootRun

# API 테스트
curl -X GET http://localhost:8080/api/session/test/redis
curl -X POST http://localhost:8080/api/session/test/set \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "username": "testuser"}'
```

### 3단계: 세션 기능 테스트

#### 📋 테스트 시나리오
```bash
# 1. 세션 데이터 저장
curl -X POST http://localhost:8080/api/session/test/set \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "username": "testuser", "email": "test@example.com"}'

# 2. 세션 데이터 조회
curl -X GET http://localhost:8080/api/session/test/get

# 3. 게임 세션 저장
curl -X POST http://localhost:8080/api/session/test/game \
  -H "Content-Type: application/json" \
  -d '{"gameId": "game001", "level": 1, "difficulty": "easy"}'

# 4. 세션 TTL 확인
curl -X GET http://localhost:8080/api/session/test/get

# 5. 세션 삭제
curl -X DELETE http://localhost:8080/api/session/test/delete
```

### 4단계: Redis 모니터링

#### 📊 Redis 모니터링 명령어
```bash
# 실시간 모니터링
redis-cli monitor

# 키 조회
redis-cli keys "crossword:*"

# 특정 패턴 키 조회
redis-cli keys "crossword:session:*"
redis-cli keys "crossword:user:*"
redis-cli keys "crossword:game:*"

# 키 정보 조회
redis-cli info keyspace

# 메모리 사용량 확인
redis-cli info memory

# 연결 정보 확인
redis-cli info clients
```

---

## 📊 성능 최적화

### 1단계: Redis 설정 최적화

#### ⚙️ Redis 설정 파일 수정
```bash
# Redis 설정 파일 위치
sudo nano /etc/redis/redis.conf
```

```conf
# 메모리 최적화
maxmemory 256mb
maxmemory-policy allkeys-lru

# 지속성 최적화
save 900 1
save 300 10
save 60 10000

# 네트워크 최적화
tcp-keepalive 60
timeout 300

# 로그 최적화
loglevel notice
```

### 2단계: Spring Boot 설정 최적화

#### ⚙️ application.properties 최적화
```properties
# Redis 연결 풀 최적화
spring.redis.lettuce.pool.max-active=20
spring.redis.lettuce.pool.max-idle=10
spring.redis.lettuce.pool.min-idle=5
spring.redis.lettuce.pool.max-wait=3000ms

# 세션 최적화
spring.session.redis.flush-mode=immediate
spring.session.timeout=3600s

# 직렬화 최적화
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.default-property-inclusion=non_null
```

---

## 🔧 문제 해결

### 일반적인 문제들

#### 1. Redis 연결 실패
```bash
# Redis 서비스 상태 확인
sudo systemctl status redis-server

# Redis 서비스 재시작
sudo systemctl restart redis-server

# 방화벽 확인
sudo ufw status
```

#### 2. 세션 데이터 손실
```bash
# Redis 데이터 확인
redis-cli keys "*"
redis-cli info memory

# 세션 TTL 확인
redis-cli ttl "crossword:session:sessionId"
```

#### 3. 성능 문제
```bash
# Redis 성능 모니터링
redis-cli --latency-history -i 1

# 메모리 사용량 모니터링
redis-cli info memory | grep used_memory_human
```

---

## 📈 모니터링 및 로깅

### 1단계: 로깅 설정

#### 📝 logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- Redis 관련 로깅 -->
    <logger name="org.springframework.data.redis" level="DEBUG"/>
    <logger name="org.springframework.session" level="DEBUG"/>
    <logger name="io.lettuce.core" level="INFO"/>
    
    <!-- 애플리케이션 로깅 -->
    <logger name="com.example.crossword" level="DEBUG"/>
    
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### 2단계: 모니터링 대시보드

#### 📊 Redis 모니터링 스크립트
```bash
#!/bin/bash
# redis_monitor.sh

echo "=== Redis 모니터링 정보 ==="
echo "시간: $(date)"
echo ""

echo "1. Redis 서버 상태:"
redis-cli ping
echo ""

echo "2. 메모리 사용량:"
redis-cli info memory | grep -E "(used_memory_human|maxmemory_human)"
echo ""

echo "3. 연결 정보:"
redis-cli info clients | grep -E "(connected_clients|blocked_clients)"
echo ""

echo "4. 키 개수:"
redis-cli dbsize
echo ""

echo "5. 세션 키 개수:"
redis-cli keys "crossword:*" | wc -l
echo ""

echo "6. 최근 명령어:"
redis-cli slowlog get 5
```

---

## 🎯 성공 지표

### 📊 성능 지표
- **Redis 응답 시간**: 1ms 이하
- **세션 저장/조회 시간**: 10ms 이하
- **메모리 사용량**: 100MB 이하
- **연결 풀 사용률**: 80% 이하

### 🔧 안정성 지표
- **Redis 가용성**: 99.9% 이상
- **세션 데이터 손실률**: 0%
- **연결 실패율**: 0.1% 이하
- **에러 발생률**: 0.1% 이하

### 🎮 사용자 경험 지표
- **세션 유지 시간**: 30분 이상
- **게임 세션 지속성**: 100%
- **로그인 상태 유지**: 100%
- **세션 복구 성공률**: 100%

---

## 📝 다음 단계

1. **Spring Boot 프로젝트 기본 구조 생성**
2. **JPA Entity 클래스 구현**
3. **Repository 인터페이스 구현**
4. **크로스워드 퍼즐 서비스 구현**
5. **API 엔드포인트 구현 및 테스트**

Redis 세션 관리 시스템이 성공적으로 구축되어 Spring Boot 애플리케이션의 세션 관리가 안정적으로 작동할 것입니다.
