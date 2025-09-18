# Spring Boot 게시판 시스템 아키텍처 문서

## 📅 작성일: 2025-09-16
## 🏗️ 시스템 구성: Spring Boot + Apache + PostgreSQL + Redis

---

## 🎯 시스템 개요

### 도메인 정보
- **프로덕션 도메인**: https://natus250601.viewdns.net
- **SSL 인증서**: Let's Encrypt (자동 갱신 설정 완료)
- **서버 IP**: 222.100.103.227

### 기술 스택
- **Backend**: Spring Boot 3.5.5 (Java 17)
- **Web Server**: Apache 2.4 (리버스 프록시)
- **Database**: PostgreSQL (mydb)
- **Session Store**: Redis
- **Build Tool**: Gradle 8.14.3
- **Template Engine**: Thymeleaf
- **Security**: Spring Security 6.5.3

---

## 🔧 네트워크 아키텍처

### 포트 구성
```
외부 요청 → Apache (80/443) → Spring Boot (8080)
```

### 상세 포트 정보
- **80 (HTTP)**: Apache 리버스 프록시
- **443 (HTTPS)**: Apache SSL 터미네이션
- **8080 (내부)**: Spring Boot 애플리케이션
- **8000**: 기타 서비스 (미사용)

### SSL/TLS 설정
- **인증서**: Let's Encrypt
- **자동 갱신**: certbot 설정 완료
- **리다이렉션**: HTTP → HTTPS 자동 리다이렉션

---

## 📁 프로젝트 구조

### Spring Boot 프로젝트
```
/var/www/html/java-projects/
├── src/main/java/com/example/board/
│   ├── BoardSystemApplication.java
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── config/
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   └── static/
├── build.gradle
└── gradlew
```

### Apache 설정 파일
```
/etc/apache2/sites-available/
├── spring-boot-proxy.conf          # HTTP 설정
└── spring-boot-proxy-le-ssl.conf   # HTTPS 설정
```

### SSL 인증서 위치
```
/etc/letsencrypt/live/natus250601.viewdns.net/
├── fullchain.pem
├── privkey.pem
├── cert.pem
└── chain.pem
```

---

## 🗄️ 데이터베이스 구성

### PostgreSQL 설정
- **호스트**: 127.0.0.1 (로컬)
- **데이터베이스**: mydb
- **사용자**: myuser
- **비밀번호**: tngkrrhk

### 연결 정보
```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/mydb
spring.datasource.username=myuser
spring.datasource.password=tngkrrhk
```

---

## 🔄 세션 관리

### Redis 설정
- **호스트**: 127.0.0.1
- **포트**: 6379 (기본)
- **세션 저장소**: Spring Session Data Redis

### 세션 설정
```properties
spring.session.store-type=redis
spring.redis.host=127.0.0.1
spring.redis.port=6379
```

---

## 🚀 배포 및 실행

### Spring Boot 실행
```bash
cd /var/www/html/java-projects
nohup ./gradlew bootRun > spring_boot.log 2>&1 &
```

### Apache 서비스 관리
```bash
# Apache 시작/중지/재시작
sudo systemctl start apache2
sudo systemctl stop apache2
sudo systemctl restart apache2

# 상태 확인
sudo systemctl status apache2
```

### SSL 인증서 갱신
```bash
# 수동 갱신
sudo certbot renew

# 자동 갱신 확인
sudo crontab -l
```

---

## 📊 모니터링

### 로그 파일 위치
- **Spring Boot**: `/var/www/html/java-projects/spring_boot.log`
- **Apache**: `/var/log/apache2/`
- **SSL**: `/var/log/letsencrypt/`

### 프로세스 모니터링
```bash
# Java 프로세스 확인
ps aux | grep java

# 포트 사용 현황
netstat -tlnp | grep -E ':(80|443|8080)'

# 서비스 상태
systemctl status apache2
```

---

## 🔒 보안 설정

### 방화벽 설정
- **80/tcp**: HTTP 허용
- **443/tcp**: HTTPS 허용
- **22/tcp**: SSH 허용

### SSL 보안
- **TLS 1.2+**: 지원
- **HSTS**: 활성화
- **자동 리다이렉션**: HTTP → HTTPS

---

## 🎯 다음 단계

### 1단계: 크로스워드 퍼즐 시스템 포팅
- Laravel → Spring Boot 마이그레이션
- 퍼즐 생성/관리 시스템
- 힌트 관리 시스템
- 사용자 인증 통합

### 2단계: 똥손 프로젝트 포팅
- 로또 번호 분석 시스템
- 똥손 파워 계산 알고리즘
- 통계 및 랭킹 시스템

---

## 📝 변경 이력

### 2025-09-16
- Spring Boot 게시판 시스템 도메인 설정 완료
- Apache 리버스 프록시 구성
- SSL 인증서 설치 및 설정
- 시스템 아키텍처 문서화

---

## 🔧 문제 해결

### 일반적인 문제
1. **포트 충돌**: `netstat -tlnp`로 포트 사용 현황 확인
2. **SSL 인증서**: `certbot certificates`로 인증서 상태 확인
3. **Apache 설정**: `apache2ctl configtest`로 설정 파일 검증
4. **Spring Boot 로그**: `tail -f spring_boot.log`로 실시간 로그 확인

### 서비스 재시작 순서
1. Spring Boot 애플리케이션 중지
2. Apache 서비스 재시작
3. Spring Boot 애플리케이션 시작
4. 서비스 상태 확인
