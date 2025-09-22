# 프로젝트U (프로젝트 번호 4번) - 관리 시스템 작업/운영/구조 정리

## 프로젝트 개요
- 명칭: **프로젝트U**
- 프로젝트 번호: 4번
- 목적: 아파트 분양/홍보/상담/설문/갤러리 등 통합 관리 시스템
- 주요 기능: 관리자 페이지(분양정보, 단지배치도, 입지환경, 프로젝트 개요, 갤러리, 온라인상담, 설문, 팝업 등) + 프론트엔드

## 서버 및 폴더 구조
- 운영 서버: 222.100.103.227 (Ubuntu Linux)
- 작업 경로: `/var/www/html/uljin-apartment-website/`
- 관리자 소스: `uljin_admin/main/`
- 관리자 템플릿: `uljin_admin/main/templates/manage/`
- 주요 문서: `/var/www/html/docs/WORKFLOW.md` (이 파일)
- 백업: `/var/www/html/backups/source_backup_YYYYMMDD_HHMMSS.tar.gz`

## 주요 기술스택
- Python 3.12, Django 5.x
- PostgreSQL
- HTML5, CSS3, JS (jQuery 일부)
- Bootstrap 기반 반응형
- Git, systemd, Nginx

## 관리자 기능 구조
- **프로젝트 개요**: 섹션(타이틀/서브타이틀), 카드(제목/서브제목/아이콘), 카드별 이미지
- **입지환경**: 섹션(타이틀/서브타이틀), 카드(제목/서브제목/아이콘), 카드별 이미지
- **단지배치도**: 섹션(타이틀/서브타이틀), 카드(제목/서브제목/아이콘), [이미지보기] 버튼용 이미지(card=None)만 관리
- **분양정보**: 섹션(타이틀/서브타이틀), 타입별 정보(가격/면적/세대수/층수/주차 등), 타입별 평면도 이미지
- **갤러리**: 카드(타이틀/서브타이틀), 카드별 이미지
- **온라인상담**: 리스트, 상세, 답변, 삭제(소프트딜리트)
- **설문조사/팝업**: 설문/팝업 관리 기능(확장 예정)
- **좌측 메뉴**: `_sidebar.html`로 모든 관리자 메뉴 통일, 동적 active 처리

## 작업/백업/운영 규칙
- 모든 소스/DB 변경 전 **백업 필수** (backups/)
- git 커밋/푸시 필수, 커밋 메시지에 작업 내역 명확히 기록
- DB 조작(INSERT/UPDATE/DELETE 등)은 반드시 대화형 승인
- 스키마 변경 시 puzzle_db_schema.sql, docs/DATABASE_SCHEMA.md에 기록
- 운영 서버는 systemd 등 백그라운드 서비스만 사용(포그라운드 실행 금지)
- 주요 변경/문제해결은 docs/ 폴더에 기록

## 참고 URL/명령어
- 관리자: http://222.100.103.227:8000/manage/
- 프론트: http://222.100.103.227:8000/
- DB 백업: `pg_dump -h 127.0.0.1 -U myuser mydb > backup.sql`
- 소스 백업: `tar czf backups/source_backup_$(date +%Y%m%d_%H%M%S).tar.gz ...`
- git 커밋/푸시: `git add . && git commit -m "메시지" && git push`

## 다음 작업시 참고
- 이 문서( /var/www/html/docs/WORKFLOW.md )를 항상 최신화
- 관리자 템플릿/구조/운영 규칙/백업 정책 등 반드시 준수
- 신규 기능/수정/문제해결 시 반드시 이 문서에 기록 후 커밋/푸시

---

# 프로젝트 5번 - 크로스워드 퍼즐 모바일앱

## 프로젝트 개요
- **명칭**: 크로스워드 퍼즐 모바일앱
- **프로젝트 번호**: 5번
- **목적**: React Native를 사용한 크로스워드 퍼즐 모바일 앱
- **API 서버**: 8080 포트 (http://222.100.103.227:8080/api)
- **기술 스택**: React Native, Expo, TypeScript, AsyncStorage

## 서버 및 폴더 구조
- **작업 경로**: `/var/www/html/CrosswordPuzzleMobile/`
- **API 서버**: 8080 포트 (기존 Laravel API 서버 활용)
- **웹앱과 분리**: 기존 웹앱(CrosswordPuzzleApp)과 완전히 독립적인 프로젝트

## 주요 기능
- **사용자 인증**: 로그인/회원가입 (AsyncStorage로 토큰 저장)
- **퍼즐 게임**: 퍼즐 플레이, 정답 확인, 힌트 시스템
- **게임 상태**: 진행 상태 저장 및 복원
- **반응형 UI**: 다양한 모바일 화면 크기 지원

## 기술적 특징
- **AsyncStorage**: 토큰 및 사용자 정보 안전한 저장
- **네비게이션**: 인증 상태에 따른 화면 분기
- **에러 처리**: 네트워크 오류 및 인증 실패 처리
- **Expo**: 빌드 환경 단순화

## 이전 문제 해결 방안
### 1. AsyncStorage 저장 실패 문제
- **해결**: Expo 환경에서 안정적인 AsyncStorage 사용
- **구현**: @react-native-async-storage/async-storage 패키지 사용
- **테스트**: 토큰 저장/복원 기능 검증

### 2. CMake 빌드 실패 문제
- **해결**: Expo를 사용하여 네이티브 빌드 환경 단순화
- **장점**: Expo가 네이티브 모듈 컴파일을 자동으로 처리
- **빌드**: expo build:android 명령으로 간단한 APK 빌드

### 3. 버전 호환성 문제
- **해결**: 최신 안정 버전 사용 (React Native 0.73.x)
- **Expo**: 버전 관리 자동화
- **의존성**: 명시적 버전 지정으로 호환성 보장

### 4. 반복적인 빌드 실패
- **해결**: Expo 개발 환경 사용
- **개발**: npm run android/ios/web로 간단한 개발 서버 실행

---

# 2025-08-26 작업 내역 - 라라벨 게시판 인증 시스템 구축

## 작업 개요
- **목적**: 특정 게시판([입학자료], [프로젝트U])을 인증된 사용자만 접근 가능하도록 제한
- **대상 게시판**: 입학자료(bbs1), 프로젝트U(proj2)
- **기술**: Laravel 인증 시스템, 데이터베이스 스키마 확장

## 데이터베이스 변경사항
### 1. board_types 테이블 스키마 확장
```sql
-- 인증 요구 컬럼 추가
ALTER TABLE board_types ADD COLUMN requires_auth BOOLEAN DEFAULT false;

-- 특정 게시판에 인증 요구 설정
UPDATE board_types SET requires_auth = true WHERE slug IN ('bbs1', 'proj2');
```

### 2. 현재 설정 상태
- **입학자료 (bbs1)**: requires_auth = true (로그인 필요)
- **자유게시판 (talk)**: requires_auth = false (비회원 접근 가능)
- **프로젝트 (proj)**: requires_auth = false (비회원 접근 가능)
- **라이브러리 (library)**: requires_auth = false (비회원 접근 가능)
- **프로젝트U (proj2)**: requires_auth = true (로그인 필요)

## 코드 변경사항

### 1. BoardType 모델 업데이트
- **파일**: `app/Models/BoardType.php`
- **변경**: `requires_auth` 컬럼을 fillable과 casts에 추가

### 2. BoardController 인증 체크 추가
- **파일**: `app/Http/Controllers/BoardController.php`
- **변경**: 
  - `index()` 메서드: 게시판 목록 접근 시 인증 체크
  - `show()` 메서드: 게시글 상세 접근 시 인증 체크

### 3. 프론트엔드 UI 개선
- **파일**: `resources/views/layouts/app.blade.php`, `resources/views/welcome.blade.php`
- **변경**: 인증이 필요한 게시판에 자물쇠 아이콘 표시

## 보안 기능
### 1. 접근 제어
- **게시판 목록**: 인증이 필요한 게시판 접근 시 로그인 페이지로 리다이렉트
- **게시글 상세**: 인증이 필요한 게시판의 게시글 접근 시 로그인 페이지로 리다이렉트
- **에러 메시지**: "이 게시판은 로그인이 필요합니다." 표시

### 2. UI 표시
- **자물쇠 아이콘**: 인증이 필요한 게시판에 노란색 자물쇠 아이콘 표시
- **툴팁**: "로그인 필요" 툴팁 제공

## 테스트 방법
1. **비로그인 상태**: 입학자료, 프로젝트U 게시판 접근 시 로그인 페이지로 리다이렉트
2. **로그인 상태**: 모든 게시판 정상 접근 가능
3. **UI 확인**: 게시판 목록에서 자물쇠 아이콘 표시 확인

## 파일 변경 목록
- `app/Models/BoardType.php`: 모델 업데이트
- `app/Http/Controllers/BoardController.php`: 인증 체크 로직 추가
- `resources/views/layouts/app.blade.php`: UI 개선
- `resources/views/welcome.blade.php`: UI 개선
- `puzzle_db_schema.sql`: 스키마 변경사항 기록
- **배포**: Expo EAS Build 또는 expo build 사용

## 프로젝트 구조
```
CrosswordPuzzleMobile/
├── src/
│   ├── contexts/
│   │   └── AuthContext.tsx      # 인증 상태 관리
│   ├── screens/
│   │   ├── LoadingScreen.tsx    # 로딩 화면
│   │   ├── LoginScreen.tsx      # 로그인 화면
│   │   ├── RegisterScreen.tsx   # 회원가입 화면
│   │   └── GameScreen.tsx       # 게임 화면
│   ├── services/
│   │   └── apiService.ts        # API 연동 서비스
│   └── components/              # 재사용 컴포넌트
├── App.tsx                      # 메인 앱 컴포넌트
├── app.json                     # Expo 설정
└── package.json                 # 의존성 관리
```

## 설치 및 실행 명령어
```bash
# 의존성 설치
cd CrosswordPuzzleMobile && npm install

# 개발 서버 실행
npm run android    # Android
npm run ios        # iOS (macOS 필요)
npm run web        # 웹

# 빌드
expo build:android  # Android APK
expo build:ios      # iOS (macOS 필요)
```

## API 연동
- **기본 URL**: http://222.100.103.227:8080/api
- **인증**: Bearer 토큰 방식
- **토큰 저장**: AsyncStorage 사용
- **에러 처리**: 401 오류 시 자동 토큰 제거

## 개발 환경 설정
- **Node.js**: 18.x 이상
- **Expo CLI**: 최신 버전
- **React Native**: 0.73.x
- **TypeScript**: 5.x

## 주의사항
- API 서버(8080 포트)가 실행 중이어야 함
- 네트워크 연결이 필요함
- AsyncStorage는 디바이스별로 독립적으로 저장됨
- Expo 개발 환경 사용으로 빌드 문제 최소화

## 문제 해결 가이드
- **빌드 오류**: `expo doctor` 실행 후 문제 해결
- **네트워크 오류**: API 서버 상태 확인
- **토큰 문제**: AsyncStorage 클리어 후 재로그인
- **버전 충돌**: package.json의 의존성 버전 확인

---

# Spring Boot 기반 크로스워드 퍼즐 시스템 - Workflow

## 현재 서버 구성 (2025-09-19 업데이트)

### 🏗️ **서버 아키텍처**
- **운영 서버**: 222.100.103.227 (Ubuntu Linux)
- **웹 서버**: Apache (80 포트) - 리버스 프록시 역할
- **백엔드**: Spring Boot 애플리케이션들
- **데이터베이스**: PostgreSQL

### 📡 **포트 구성**
- **80 포트**: Apache (리버스 프록시)
  - `/api/board/` → 8080 포트 (게시판 시스템)
  - `/K-CrossWord/` → 8081 포트 (크로스워드 게임)
  - `/api/puzzle/` → 8081 포트 (크로스워드 게임 API)
- **8080 포트**: Spring Boot 게시판 시스템
- **8081 포트**: Spring Boot 크로스워드 게임 시스템
- **3001 포트**: React Native 웹앱 (현재 서비스 정지 상태)

### 🚀 **실행 중인 서비스**
- ✅ **Apache**: 80 포트에서 리버스 프록시로 실행 중
- ✅ **Spring Boot 게시판**: 8080 포트에서 실행 중
- ✅ **Spring Boot 크로스워드**: 8081 포트에서 실행 중
- ❌ **React Native 웹앱**: 3001 포트 서비스 정지 상태

### 📁 **프로젝트 구조**
- **크로스워드 게임**: `/var/www/html/crossword-projects/`
- **게시판 시스템**: `/var/www/html/java-projects/` (추정)
- **React Native 웹앱**: `/var/www/html/CrosswordPuzzleApp/` (서비스 정지)
- **문서**: `/var/www/html/docs/`

### 🔧 **서비스 관리 명령어**
```bash
# 크로스워드 게임 재시작
cd /var/www/html/crossword-projects && ./gradlew bootRun --args='--server.port=8081' &

# 게시판 시스템 재시작 (추정)
cd /var/www/html/java-projects && ./gradlew bootRun --args='--server.port=8080' &

# 실행 중인 Java 프로세스 확인
ps aux | grep -E "(java|gradle)" | grep -v grep

# 포트 사용 상태 확인
netstat -tlnp | grep -E ":(8080|8081)"
```

---

## 2025-09-19 작업 내역 (서버 다운 복구 및 구성 정리)

### 문제 상황
- Spring Boot 크로스워드 게임 시스템(8081 포트)과 게시판 시스템(8080 포트)이 모두 중지
- 503 Service Unavailable 에러 발생
- 사용자가 크로스워드 게임에 접근할 수 없는 상황

### 해결 과정
1. **서비스 상태 확인**: 두 Spring Boot 애플리케이션 모두 중지 상태 확인
2. **Spring Boot 애플리케이션 재시작**: 백그라운드에서 정상 시작
3. **포트 충돌 해결**: 3001 포트 사용 중인 불필요한 프로세스 정리
4. **systemd 서비스 정리**: React Native 웹앱 관련 서비스 비활성화

### 현재 상태
- ✅ **크로스워드 게임**: 8081 포트에서 정상 실행 중
- ✅ **게시판 시스템**: 8080 포트에서 정상 실행 중
- ✅ **503 에러 해결**: 두 시스템 모두 정상 접근 가능
- ✅ **Apache 리버스 프록시**: 80 포트에서 정상 작동

---

## 2025-07-31 작업 내역 (React Native 모바일 앱 localStorage 에러 해결)

### 문제 상황
- React Native 모바일 앱에서 `Property 'localStorage' doesn't exist` 에러 발생
- 모바일 환경에서 웹 브라우저 API인 localStorage를 사용하려고 해서 발생한 문제
- 로그인 후 게임 페이지로 전환되지 않는 인증 문제

### 해결 과정

#### 1. localStorage 사용 위치 파악
- `CrosswordPuzzleMobileApp/src/services/api.ts`: API 서비스에서 토큰 저장/로드
- `CrosswordPuzzleMobileApp/src/contexts/AuthContext.tsx`: 인증 컨텍스트에서 토큰 관리
- 두 파일 모두 localStorage를 직접 사용하여 모바일 환경에서 에러 발생

#### 2. 환경별 스토리지 처리 로직 구현
- **React Native 환경**: AsyncStorage 사용
- **웹 환경**: localStorage 사용 (fallback)
- **AsyncStorage 로드 실패 시**: 메모리 스토리지 사용

#### 3. 수정된 파일들

**CrosswordPuzzleMobileApp/src/services/api.ts**
```typescript
const getStorage = () => {
  try {
    const AsyncStorage = require('@react-native-async-storage/async-storage').default;
    console.log('✅ CrosswordPuzzleMobileApp AsyncStorage 로드 성공');
    return AsyncStorage;
  } catch (error) {
    console.log('❌ CrosswordPuzzleMobileApp AsyncStorage 로드 실패, 메모리 스토리지 사용');
    const memoryStorage: any = {};
    return {
      getItem: (key: string) => Promise.resolve(memoryStorage[key] || null),
      setItem: (key: string, value: string) => Promise.resolve(memoryStorage[key] = value),
      removeItem: (key: string) => Promise.resolve(delete memoryStorage[key]),
    };
  }
};
```

**CrosswordPuzzleMobileApp/src/contexts/AuthContext.tsx**
```typescript
const getStorage = () => {
  try {
    const AsyncStorage = require('@react-native-async-storage/async-storage').default;
    console.log('✅ CrosswordPuzzleMobileApp AuthContext AsyncStorage 로드 성공');
    return AsyncStorage;
  } catch (error) {
    console.log('❌ CrosswordPuzzleMobileApp AuthContext AsyncStorage 로드 실패, 메모리 스토리지 사용');
    const memoryStorage: any = {};
    return {
      getItem: (key: string) => Promise.resolve(memoryStorage[key] || null),
      setItem: (key: string, value: string) => Promise.resolve(memoryStorage[key] = value),
      removeItem: (key: string) => Promise.resolve(delete memoryStorage[key]),
    };
  }
};
```

#### 4. 디버깅 로그 추가
- 각 파일에 `CrosswordPuzzleMobileApp` 접두사를 사용한 로그 추가
- AsyncStorage 로드 성공/실패 로그
- API 요청/응답 로그
- 인증 상태 변경 로그

#### 5. 빌드 및 테스트
- Android APK 빌드: `./build-android.sh`
- 빌드 시간: 2025-07-31 15:30:00
- 테스트 계정: test@test.com / 123456

### 예상 결과
- localStorage 에러 해결
- 모바일 환경에서 AsyncStorage 정상 동작
- 로그인 후 게임 페이지로 정상 전환
- 인증 토큰 저장/로드 정상 동작

### 다음 단계
1. 새 APK 설치 및 테스트
2. 로그인 플로우 확인
3. 인증 상태 유지 확인
4. 게임 페이지 전환 확인

---

## 2025-07-18 작업 내역 (Wordle 스타일 게임 상태 유지 시스템 - 정답 표시 문제 해결)

### 문제 상황
- 새로고침 후 맞춘 정답들이 `***` 별표로 표시됨
- 사용자가 맞춘 정답은 보여야 하는데 보안상 정답을 클라이언트에 전송하지 않아서 발생한 문제

### 해결 과정

#### 1. 서버 측 수정 (PuzzleGameController.php)
- `getTemplate()` 메서드에서 맞춘 정답 단어 정보를 안전하게 전송하는 로직 추가
- `answered_words_with_answers` 필드 추가: 맞춘 단어의 정답만 서버에서 전송
- 보안 고려사항: 맞춘 단어만 전송하여 보안 유지

#### 2. 클라이언트 측 수정 (index.blade.php)
- `restoreGameState()` 함수 수정: 서버에서 받은 정답 정보 사용
- `answeredWordsWithAnswers` 파라미터 추가하여 정답 복원 로직 개선
- 정답 정보가 없으면 별표로 표시하는 fallback 로직 유지

#### 3. 구현된 기능
- 새로고침 시 맞춘 정답들이 정상적으로 표시됨
- 보안 유지: 맞춘 단어만 전송, 미완성 단어는 전송하지 않음
- 사용자 경험 개선: Wordle처럼 게임 진행 상태 완전 복원

### 수정된 파일
- `app/Http/Controllers/PuzzleGameController.php`: 정답 단어 정보 전송 로직 추가
- `resources/views/puzzle/game/index.blade.php`: 정답 복원 로직 수정

### 테스트 방법
1. 퍼즐게임에서 몇 개 단어를 맞춤
2. 브라우저 새로고침
3. 맞춘 정답들이 정상적으로 표시되는지 확인

---

## 2025-07-18 작업 내역 (Wordle 스타일 게임 상태 유지 시스템 구축)

### 문제 상황
- 새로고침할 때마다 새로운 퍼즐이 생성되어 사용자가 어려운 퍼즐을 피해갈 수 있는 문제
- Wordle처럼 새로고침해도 게임 진행 상태가 유지되어야 함

### 해결 과정

#### 1. 데이터베이스 스키마 확장
- `user_puzzle_games` 테이블에 퍼즐 세션 정보 저장 컬럼 추가
- `current_puzzle_data`: 현재 퍼즐의 템플릿, 단어, 그리드 정보
- `current_game_state`: 현재 게임 상태 (정답/오답, 힌트 사용 등)
- `current_puzzle_started_at`: 현재 퍼즐 시작 시간
- `has_active_puzzle`: 활성 퍼즐 존재 여부

#### 2. UserPuzzleGame 모델 확장
- 퍼즐 세션 관리용 메서드 추가:
  - `startNewPuzzle()`: 새 퍼즐 시작
  - `endCurrentPuzzle()`: 현재 퍼즐 종료
  - `updateGameState()`: 게임 상태 업데이트
  - `hasActivePuzzle()`: 활성 퍼즐 존재 확인
  - `getCurrentPuzzleData()`: 현재 퍼즐 데이터 조회
  - `getCurrentGameState()`: 현재 게임 상태 조회

#### 3. PuzzleGameController 수정
- `getTemplate()` 메서드: 활성 퍼즐 세션이 있으면 기존 퍼즐과 게임 상태 복원
- `checkAnswer()` 메서드: 게임 상태 업데이트 로직 추가
- `completeLevel()` 메서드: 퍼즐 세션 종료 로직 추가

#### 4. 프론트엔드 JavaScript 수정
- `loadTemplate()` 함수: 서버에서 받은 게임 상태 복원
- `restoreGameState()` 함수: 정답 상태 복원
- `restoreAnsweredCells()` 함수: 그리드에 정답 상태 표시
- `updateGridWithAnswer()` 함수: 게임 상태 업데이트 로그 추가

#### 5. 구현된 기능
- Wordle처럼 게임 진행 상태를 데이터베이스에 저장
- 새로고침 시 동일한 퍼즐과 진행 상태 복원
- 정답/오답 상태, 힌트 사용 여부 등 추적
- 정답 단어는 보안상 서버에만 저장하고 클라이언트에는 표시용으로만 복원

### 수정된 파일
- `database/migrations/2025_07_18_102239_add_current_puzzle_session_to_user_puzzle_games_table.php`: 마이그레이션 파일
- `app/Models/UserPuzzleGame.php`: 퍼즐 세션 관리 메서드 추가
- `app/Http/Controllers/PuzzleGameController.php`: 게임 상태 관리 로직 추가
- `resources/views/puzzle/game/index.blade.php`: 프론트엔드 게임 상태 복원 로직 추가

### 테스트 방법
1. 퍼즐게임에서 몇 개 단어를 맞춤
2. 브라우저 새로고침
3. 동일한 퍼즐과 진행 상태가 복원되는지 확인
4. 정답/오답 상태가 정상적으로 표시되는지 확인

---

## 2025-07-11 작업 내역 (React Native 웹앱 빌드 및 서비스 설정)

### 문제 상황
- React Native 웹앱이 개발 서버 모드로 실행 중
- 정식 빌드 및 정적 파일 서비스가 아닌 상태
- UI 변경사항이 브라우저에 반영되지 않는 문제

### 해결 과정

#### 1. 빌드 시스템 확인
- `package.json`에서 `build-web` 스크립트 확인
- `npm run build-web` 명령으로 프로덕션 빌드 실행
- `dist/` 폴더에 정적 파일 생성 확인

#### 2. Systemd 서비스 설정
- `/etc/systemd/system/crossword-puzzle-app.service` 생성
- Python HTTP 서버로 3001 포트에서 정적 파일 서비스
- 서비스 활성화 및 시작

#### 3. 현재 상태
- ✅ **빌드 완료**: `dist/` 폴더에 `index.html`, `bundle.js` 생성
- ✅ **서비스 생성**: systemd 서비스 파일 생성 완료
- ⚠️ **서비스 시작 실패**: Python HTTP 서버 시작 시 오류 발생
- ⚠️ **권한 문제**: www-data 사용자 권한 설정 필요

# 2025-09-22 작업 내역 (Spring Boot 크로스워드 퍼즐 시스템 API 경로 통일)

## 인증 시스템 통합 구조
- **Spring Boot 게시판(8080 포트)**: 기존 게시판 시스템
- **Spring Boot 퍼즐게임(8081 포트)**: 크로스워드 퍼즐 게임 시스템
- **인증 통합**: Redis 세션 공유로 두 시스템 간 인증 통합
- **관리자 인증**: 퍼즐게임 관리자 페이지는 반드시 인증 필요 (게스트는 게임만 가능)
- **경로 통일**: 퍼즐게임 관련 모든 프로그램은 `/K-CrossWord/` 하위에 위치

## API 경로 통일 작업
- **기존 문제**: 힌트 생성 페이지에서 `/K-CrossWord/api/` 경로 사용
- **단어 관리 페이지**: `/K-CrossWord/admin/api/` 경로 사용
- **해결 방안**: 모든 관리자 API를 `/K-CrossWord/admin/api/` 경로로 통일
- **SecurityConfig 수정**: `/K-CrossWord/admin/api/**` 경로 허용 설정 추가
- **컨텍스트 경로**: `server.servlet.context-path=/K-CrossWord` 설정으로 Apache 리버스 프록시와 연동

## 수정된 파일들
- `hint-generator/index.html`: 모든 API 호출을 `/K-CrossWord/admin/api/` 경로로 변경
- `SecurityConfig.java`: `/K-CrossWord/admin/api/**` 경로 허용 설정 추가
- `application.properties`: 컨텍스트 경로 설정 추가

## 현재 진행 중인 문제
- **관리자 페이지 404 문제**: `https://natus250601.viewdns.net/K-CrossWord/admin/` 접근 시 404 에러 발생
- **원인 분석**: Spring Boot 애플리케이션이 `/K-CrossWord` 컨텍스트 경로로 시작되지만 `application.properties`에서 컨텍스트 경로 설정이 제거된 상태
- **해결 방안**: 컨텍스트 경로 설정과 WebController 매핑 경로 일치 필요

### 다음 작업 예정
1. **서비스 권한 문제 해결**: www-data 사용자 권한 설정
2. **서비스 재시작**: 정상적인 정적 파일 서비스 확인
3. **브라우저 캐시 클리어**: 하드 리프레시로 새 빌드 반영 확인
4. **UI 기능 테스트**: 로그인, 게임 화면, 퍼즐 기능 정상 작동 확인

### 중요 파일 위치
- **빌드 파일**: `/var/www/html/CrosswordPuzzleApp/dist/`
- **서비스 설정**: `/etc/systemd/system/crossword-puzzle-app.service`
- **소스 코드**: `/var/www/html/CrosswordPuzzleApp/src/`

### 디버깅 명령어
- **서비스 상태 확인**: `sudo systemctl status crossword-puzzle-app`
- **서비스 로그 확인**: `sudo journalctl -u crossword-puzzle-app -f`
- **빌드 재실행**: `cd CrosswordPuzzleApp && npm run build-web`
- **수동 서비스 테스트**: `cd CrosswordPuzzleApp/dist && python3 -m http.server 3001`

---

## 2025-07-10 작업 내역 (React Native 웹앱 인증 시스템 구축)

### 문제 상황
- React Native 웹앱에서 퍼즐게임 진입 시 인증 없이 바로 게임 화면 표시
- Laravel API 호출 시 404 Not Found 에러 발생
- 기존 라라벨 웹서비스(쿠키&세션)와 React 앱(토큰 기반)의 인증 구조 차이

### 해결 과정

#### 1. 인증 구조 분석
- **라라벨 기존 웹 로그인**: 쿠키 + 세션 기반 (Auth::routes())
- **React용 API 로그인**: Sanctum 토큰 기반 (AuthController)
- **퍼즐게임 API**: 인증 토큰이 있어야 접근 가능

#### 2. API 서버 분리
- **기존 라라벨 웹서비스**: http://222.100.103.227/ (포트 80)
- **React Native 웹앱**: http://222.100.103.227:3001/ (포트 3001)
- **API 서버**: http://222.100.103.227:8080/api (포트 8080, 새로 분리)

#### 3. 인증 시스템 구현
- **AuthContext 생성**: 인증 상태 전역 관리
- **App.tsx 수정**: 인증 상태에 따른 화면 분기
- **LoginScreen/RegisterScreen**: AuthContext 사용하도록 수정
- **GameScreen**: 인증 체크 추가, 로그아웃 기능 구현

#### 4. 스토리지 호환성 해결
- **웹 환경**: localStorage 사용
- **모바일 환경**: AsyncStorage 사용
- **통합 스토리지 인터페이스**: 환경에 따라 자동 선택

#### 5. 서버 설정
- **API 서버 실행**: `cd public && php -S 0.0.0.0:8080`
- **방화벽 설정**: `sudo ufw allow 8080/tcp`
- **포트포워딩**: 8080 포트 외부 접근 허용 필요

#### 6. 테스트 계정 생성
- **이메일**: test@test.com
- **비밀번호**: 123456
- **API 테스트**: 회원가입/로그인 정상 작동 확인

### 현재 상태
- ✅ **API 서버**: 8080 포트에서 정상 실행
- ✅ **방화벽**: 8080 포트 허용 완료
- ✅ **인증 API**: 회원가입/로그인/토큰 발급 정상 작동
- ✅ **React Native 웹앱**: 인증 상태에 따른 화면 분기 구현
- ⚠️ **포트포워딩**: 8080 포트 외부 접근 설정 필요
- ⚠️ **인증 화면 전환**: 브라우저에서 아직 로그인 화면으로 전환 안 됨

### 다음 작업 예정
1. **포트포워딩 설정**: 8080 포트 외부 접근 허용
2. **인증 화면 전환 디버깅**: AuthContext 로그 확인 및 수정
3. **퍼즐게임 API 연동**: 인증된 사용자의 퍼즐 데이터 로드
4. **실제 로그인 테스트**: 브라우저에서 전체 인증 흐름 확인

### 중요 파일 위치
- **React Native 웹앱**: /var/www/html/CrosswordPuzzleApp/
- **AuthContext**: CrosswordPuzzleApp/src/contexts/AuthContext.tsx
- **API 서비스**: CrosswordPuzzleApp/src/services/api.ts
- **App.tsx**: CrosswordPuzzleApp/App.tsx
- **API 서버**: public/api.php (새로 생성)

### 디버깅 명령어
- **API 서버 상태 확인**: `curl -X POST http://127.0.0.1:8080/api/login -H "Content-Type: application/json" -d '{"email":"test@test.com","password":"123456"}'`
- **React 앱 재시작**: `cd CrosswordPuzzleApp && npm run web`
- **포트 충돌 해결**: `pkill -f "webpack-dev-server" && pkill -f "react-native start"`

---

## 2025-07-09 작업 내역
- 단어 난이도 보정 스케줄러 개선 및 임시테이블 동기화 로직 개선
- Gemini API 프롬프트 구조 단순화 및 모델 버전별(1.5/2.5 flash) 실험
- 난이도 분포 집계 및 신규/재측정 방식 차이 분석
- Gemini API 쿼터 초과(429) 이슈 및 대응
- docs/WORKFLOW.md, docs/DATABASE_SCHEMA.md, docs/TROUBLESHOOTING.md에 상세 내역 기록 및 커밋/푸시 완료

---

## 2025-07-01 작업 내역
- 교차점 개수 validation을 '최소값' 기준으로 변경 (프론트엔드, 백엔드 모두)
- 조건 확인 div 안내 문구를 '최소 N개 필요'로 수정
- 삭제 기능 JS 이벤트 연결을 DOMContentLoaded 이후로 이동하여 DOM 로딩 타이밍 문제 해결
- Blade 템플릿 중복 @endpush 태그 제거
- git 커밋 및 원격 저장소 푸시, 병합 충돌 해결

---

## 2025-06-30 작업 내역
- 서버 OS 재설치 후 크론탭(`*/10 * * * * cd /var/www/html && php artisan schedule:run >> /dev/null 2>&1`) 재등록
- storage 및 storage/logs 디렉토리, 로그 파일 권한/소유자(www-data)로 변경
- 배치 스케줄러 정상 동작 및 로그 권한 이슈 대응
- 단어 추출 로직에서 난이도 조건(단어 난이도, 힌트 난이도) 제거하여 더 다양한 단어 추출 가능
- 단어 추출 실패 시 확정된 단어를 초기화하고 최대 5회까지 재시도하는 로직 구현
- 크론 배치 작업 권한 문제 해결: natus-server를 www-data 그룹에 추가하여 로그 파일 쓰기 권한 확보

---

## 2025-01-27: React Native 웹앱 퍼즐 그리드 렌더링 문제 해결

### 문제 상황
- React Native 웹앱에서 퍼즐 그리드와 번호(배지)가 전혀 표시되지 않음
- API에서 데이터는 정상적으로 받아오지만 화면에 렌더링되지 않는 현상
- 사용자가 웹 서비스(Blade/Laravel)와 동일한 구조로 검은칸(2)에만 번호 표시 요청

### 원인 분석
- React Native for Web 환경에서는 HTML 태그(`<div>`, `<span>`)가 렌더링되지 않음
- 반드시 RN 컴포넌트(`<View>`, `<Text>`)를 사용해야 함
- CrosswordGrid.tsx에서 HTML 태그를 사용하여 렌더링 실패

### 해결 과정
1. **전체 소스 백업**: 작업 전 안전을 위해 백업 진행
2. **CrosswordGrid.tsx 수정**: 
   - HTML 태그를 RN 컴포넌트로 완전 교체
   - `<div>` → `<View>`, `<span>` → `<Text>` 변경
   - 검은칸(2)에만 번호 표시하는 로직 구현
   - RN 스타일 적용 (flexbox, 스타일 객체 등)
3. **빌드 및 서비스 재시작**: 수정사항 반영
4. **테스트**: 브라우저에서 새로고침 후 정상 표시 확인

### 결과
- ✅ 퍼즐 그리드 정상 표시
- ✅ 검은칸(2)에만 번호(배지) 표시
- ✅ 웹 서비스와 동일한 구조 구현 완료
- ✅ React Native for Web 환경 호환성 확보

### 기술적 교훈
- React Native for Web에서는 반드시 RN 컴포넌트 사용
- HTML 태그는 웹 환경에서도 렌더링되지 않음
- 스타일링도 RN 방식으로 적용해야 함

### 관련 파일
- `CrosswordPuzzleApp/src/components/CrosswordGrid.tsx`: 메인 수정 파일
- 백업 파일: `backups/CrosswordPuzzleApp_20250127_143000.tar.gz`

---

## 2025-07-01 작업 내역
- 교차점 개수 validation을 '최소값' 기준으로 변경 (프론트엔드, 백엔드 모두)
- 조건 확인 div 안내 문구를 '최소 N개 필요'로 수정
- 삭제 기능 JS 이벤트 연결을 DOMContentLoaded 이후로 이동하여 DOM 로딩 타이밍 문제 해결
- Blade 템플릿 중복 @endpush 태그 제거
- git 커밋 및 원격 저장소 푸시, 병합 충돌 해결

---

## 2025-06-30 작업 내역
- 서버 OS 재설치 후 크론탭(`*/10 * * * * cd /var/www/html && php artisan schedule:run >> /dev/null 2>&1`) 재등록
- storage 및 storage/logs 디렉토리, 로그 파일 권한/소유자(www-data)로 변경
- 배치 스케줄러 정상 동작 및 로그 권한 이슈 대응
- 단어 추출 로직에서 난이도 조건(단어 난이도, 힌트 난이도) 제거하여 더 다양한 단어 추출 가능
- 단어 추출 실패 시 확정된 단어를 초기화하고 최대 5회까지 재시도하는 로직 구현
- 크론 배치 작업 권한 문제 해결: natus-server를 www-data 그룹에 추가하여 로그 파일 쓰기 권한 확보

---

## 2025-01-27 작업 내역
- 단어 난이도 업데이트 방식 개선: 힌트 생성과 단어 난이도 업데이트 완전 분리
- 임시 테이블(tmp_pz_word_difficulty) 생성하여 기존 단어 ID 복사 및 update_yn 관리
- 힌트 생성 API에서 단어 난이도 업데이트 코드 제거
- 단어 생성 API는 단어만 생성하고, 난이도는 별도 Gemini API 호출로 평가
- 단어 난이도 업데이트 API 별도 구현: update_yn='n'인 단어 50개씩 가져와 Gemini API에 일괄 평가
- UpdateWordDifficultyScheduler artisan 명령어 생성 및 Laravel 스케줄러에 10분마다 실행 등록
- GeminiService.php의 난이도 평가 프롬프트를 십자낱말 퀴즈 관점에서 1~5 숫자로 평가하도록 수정
- 테스트 모드와 실제 업데이트 모두 정상 작동 확인

---

## 2025-07-18 작업 내역 (퍼즐 단어 정리 및 비활성화 스케줄러 구축)

### 문제 상황
- `pz_words` 테이블에 "단어,숫자" 형태로 저장된 데이터들이 존재
- 영문이나 숫자가 포함된 단어들이 활성 상태로 남아있음
- 매일 정기적으로 단어 데이터를 정리하고 비활성화할 필요

### 해결 과정

#### 1. Artisan 명령어 생성
- `CleanupPuzzleWords` 명령어 생성: `php artisan make:command CleanupPuzzleWords`
- 두 가지 기능 구현:
  1. 쉼표와 숫자가 포함된 단어 정리
  2. 영문이나 숫자가 포함된 단어 비활성화

#### 2. 명령어 기능 구현
- `cleanupCommaNumberWords()`: 쉼표와 숫자 제거, length 업데이트
- `deactivateEnglishNumberWords()`: 영문/숫자 포함 단어 비활성화
- 로그 기록 및 에러 처리 구현

#### 3. 스케줄러 등록
- `app/Console/Kernel.php`에 매일 새벽 1시 실행 스케줄 등록
- `storage/logs/word-cleanup.log`에 로그 기록
- `withoutOverlapping()` 설정으로 중복 실행 방지

#### 4. 테스트 결과
- 쉼표 포함 단어: 0개 (이미 정리 완료)
- 영문/숫자 포함 단어: 22개 비활성화
- 비활성화된 단어 예시: "PR전략", "의료AI", "MZ세대", "NFT아트", "UI/UX", "3D프린터" 등

### 구현된 기능
- ✅ 매일 새벽 1시 자동 실행
- ✅ 쉼표와 숫자 제거하여 순수 단어만 남김
- ✅ 영문이나 숫자가 포함된 단어 자동 비활성화
- ✅ 로그 기록 및 에러 처리
- ✅ 중복 실행 방지

### 수정된 파일
- `app/Console/Commands/CleanupPuzzleWords.php`: 단어 정리 명령어
- `app/Console/Kernel.php`: 스케줄러 등록

### 테스트 방법
```bash
# 수동 실행
php artisan puzzle:cleanup-words

# 로그 확인
tail -f storage/logs/word-cleanup.log
cat storage/logs/laravel.log | grep "cleanup"
```

---

## 주요 작업 원칙

### 1. 데이터베이스 변경 시 필수 절차
- **변경 전**: 데이터에 미치는 영향 분석 및 설명
- **사용자 확인**: 변경 작업 진행 여부 및 백업 진행 여부 확인
- **스키마 기록**: /var/www/html/puzzle_db_schema.sql에 변경사항 추가

### 2. 데이터베이스 조작 작업 규칙 (매우 중요!)
- **대화형 진행 필수**: INSERT, UPDATE, DELETE, TRUNCATE, DROP 작업 시 반드시 사용자와 대화형으로 진행
- **작업 전 확인**: 실행할 SQL 문과 예상 결과를 먼저 보여주고 승인 요청
- **단계별 진행**: 복잡한 작업은 단계별로 나누어 진행
- **실행 후 확인**: 작업 완료 후 결과 확인 및 보고

### 3. 라이브 서버 주의사항 (222.100.103.227 전용)
- 데이터베이스 변경 시 항상 백업 후 진행
- Laravel 파일 수정 시 서비스 영향도 분석
- React 기능 추가/수정 시 Laravel 서비스 영향 고려

### 4. 환경 설정 확인
- 문제 발생 시 즉시 `cat .env`로 환경 설정 확인
- 불필요한 중간 단계 건너뛰고 직접적인 방법 사용

### 5. 문서화 원칙
- 모든 주요 변경사항은 docs/ 폴더에 기록
- 변경 후 실제 동작 확인

### 6. 로그 시스템 관리
- 로그 설정은 config/logging.php에서 관리
- 모든 로그 채널이 NullHandler로 설정되지 않도록 주의
- 디버깅 로그는 \Log::info() 사용하여 기록
- 로그 레벨은 env('LOG_LEVEL', 'debug')로 설정

### 7. 게시판 시스템 관리
- 각 게시판별 글쓰기 버튼은 현재 URL의 boardType 파라미터를 직접 읽어 생성
- AppServiceProvider에서 전역 변수 공유 시 주의 (boardType 등)

---

## 2025-01-27 작업 내역 (DataTable 체크박스 상태 유지 문제 해결)

### 문제 상황
- DataTable 무한 스크롤링으로 인해 체크박스 상태가 사라지는 문제
- 스크롤할 때마다 새로운 데이터를 서버에서 가져와서 DOM이 재생성됨
- 일괄변경을 위해 체크한 항목들이 스크롤 후 사라지는 문제

### 해결 과정

#### 1. 전역 변수로 체크박스 상태 관리
- `selectedCheckboxes` Set 객체로 선택된 체크박스 ID 관리
- 페이지 변경 시에도 선택 상태 유지
- DOM 재생성과 무관하게 상태 보존

#### 2. 서버 사이드에서 클라이언트 사이드로 변경
- `serverSide: false` 설정으로 클라이언트 사이드 처리로 변경
- 모든 데이터를 한 번에 로드하여 스크롤링 시 추가 요청 없음
- 체크박스 상태가 안정적으로 유지됨

#### 3. DataTable 설정 개선
- `stateSave: true` 설정으로 테이블 상태 저장 기능 활성화
- `xhr` 이벤트 핸들러 추가로 스크롤링 시 체크박스 상태 복원
- 체크박스 이벤트 관리 및 전체 선택/해제 기능 개선

#### 4. 서버 사이드 컨트롤러 수정
- `getData()` 메서드를 클라이언트 사이드에 맞게 수정
- 페이징 로직 제거하고 모든 데이터를 한 번에 반환
- 난이도 필터는 서버에서 처리

### 구현된 기능
- ✅ 무한 스크롤 시에도 체크박스 상태 유지
- ✅ 페이지 변경 시 선택된 항목 보존
- ✅ 전체 선택/해제 기능 정상 작동
- ✅ 일괄변경 시 선택된 항목 정확히 처리
- ✅ 선택된 개수 실시간 업데이트

### 수정된 파일
- `resources/views/puzzle/words/index.blade.php`: DataTable 설정 및 체크박스 상태 관리 로직 개선
- `app/Http/Controllers/PzWordController.php`: 클라이언트 사이드 처리로 변경

### 테스트 방법
1. 단어 관리 화면에서 여러 항목 체크
2. 스크롤하여 페이지 변경
3. 체크된 항목들이 그대로 유지되는지 확인
4. 일괄변경 기능 정상 작동 확인
- 레이아웃에서 동적 링크 생성 시 현재 컨텍스트 확인

### 8. 그리드 템플릿 시스템 관리
- 그리드 템플릿 저장 시 사용자가 "단어 위치 정보"에서 선택한 번호가 그대로 저장됨
- PuzzleGridTemplateService의 saveTemplate()에서 sortWordPositionsWithPriority() 호출 제거
- 사용자 입력 번호 우선 보존, 자동 넘버링 비활성화
- 템플릿 수정 시 사용자가 선택한 번호로 word_positions의 id 값 업데이트
- 단어 추출 시 word_positions를 id 순서대로 정렬하여 표시 (백엔드에서 처리)
- 문제가 있는 템플릿(#14, #15)은 비활성화 처리

### 9. React Native 웹앱 인증 시스템 관리 (2025-07-10 추가)
- **인증 구조**: 토큰 기반 인증 (Sanctum)
- **인증 흐름**: 앱 진입 → 토큰 없으면 로그인 화면 → 로그인 성공 시 퍼즐게임 화면
- **API 서버**: 8080 포트로 분리 (기존 라라벨 웹서비스와 독립)
- **스토리지**: 웹 환경에서는 localStorage, 모바일에서는 AsyncStorage 사용
- **포트 구성**:
  - React Native 웹앱: http://222.100.103.227:3001/
  - API 서버: http://222.100.103.227:8080/api
  - 라라벨 웹서비스: http://222.100.103.227/
- **방화벽**: 8080 포트 허용 완료

### 10. React Native 웹앱 빌드 및 서비스 관리 (2025-07-11 추가)
- **빌드 시스템**: `npm run build-web` 명령으로 프로덕션 빌드
- **정적 파일**: `dist/` 폴더에 `index.html`, `bundle.js` 생성
- **서비스 관리**: systemd 서비스로 Python HTTP 서버 실행
- **포트 구성**: 3001 포트에서 정적 파일 서비스
- **권한 관리**: www-data 사용자로 서비스 실행

## 자주 사용하는 명령어
- 데이터베이스 백업: `pg_dump -h 127.0.0.1 -U myuser mydb > backup.sql`
- 테이블 구조 확인: `\d table_name`
- Laravel 설정 확인: `php artisan tinker --execute="echo config('database.default');"`
- 환경 설정 확인: `cat .env`
- 로그 확인: `tail -f storage/logs/laravel.log`
- 설정 캐시 클리어: `php artisan config:clear`
- React Native 웹앱 실행: `cd CrosswordPuzzleApp && npm run web`
- API 서버 실행: `cd public && php -S 0.0.0.0:8080`
- React 앱 빌드: `cd CrosswordPuzzleApp && npm run build-web`
- 서비스 상태 확인: `sudo systemctl status crossword-puzzle-app`

## 주의사항
- **원격 개발 환경**: SSH로 222.100.103.227에 직접 연결하여 작업
- **데이터베이스 호스트**: 127.0.0.1 (서버 내부에서 같은 서버의 DB 접근)
- **모든 설명**: 한글로 진행
- **스키마 변경**: puzzle_db_schema.sql에 반드시 기록
- **DB 조작**: INSERT/UPDATE/DELETE/TRUNCATE/DROP 시 반드시 대화형 진행 
- **로그 설정**: config/logging.php 수정 후 반드시 config:clear 실행
- **React Native 웹앱**: 인증 토큰이 없으면 반드시 로그인 화면으로 이동해야 함
- **빌드 후 서비스**: 빌드 완료 후 반드시 systemd 서비스 재시작 필요 

## 2025-07-12 오답 초과 안내박스 UI/UX 반복 개선 및 배포

- 오답 5회 초과 시 안내 모달(재도전) 위치/동작/클릭 가능성 20회 이상 반복 수정
- 안내박스가 오버레이 위에 정확히 뜨도록 구조 개선 (zIndex, 렌더링 순서, pointerEvents)
- 안내박스가 입력/버튼 박스와 정확히 겹치게 ref+absolute 방식으로 위치 조정 시도
- transform, top, px, %, calc 등 다양한 CSS/React Native Web 스타일 실험
- 오버레이와 안내박스를 같은 부모에서, 오버레이 먼저, 안내박스 나중에, zIndex로 분리하여 최종 구조 확정
- 빌드(npm run build-web) 및 서비스 재시작(systemctl restart crossword-puzzle-app) 반복
- 실제 서비스에서 위치/동작/클릭 가능성 등 실사용 피드백 반영
- 서버 소스 전체 풀백업 진행 (source_backup_YYYYMMDD_HHMMSS.tar.gz) 
## 2025-07-12 한글 음절별 단어 수집 자동화 및 장애 대응

- temp_hangul_syllables 테이블 구조 개선(초성, processed_at 컬럼 추가)
- syllable별 단어 수집 프롬프트/자동화 설계 및 CollectWordsFromSyllables artisan 커맨드 구현
- 1분마다 스케줄러 자동 실행, Gemini API 연동, 결과 임시테이블 저장
- 장애(503 등) 발생 시 롤백 처리 및 로그 기록 개선
- 운영 정책 및 DB 작업 규칙 철저 준수
- 전체 플로우/운영 정책 docs/WORKFLOW.md, docs/DATABASE_SCHEMA.md, docs/TROUBLESHOOTING.md에 반영

### 2025-07-17 작업내역
- 퍼즐게임 클리어 조건 실시간 반영 및 중복 API 호출 방지 개선
- 클리어 기록 항상 저장, 클리어 조건 충족 시 다음 레벨 이동 정상화
- 프론트엔드: 다음 레벨 버튼 중복 호출 방지, 실시간 카운트 표시
- DB/소스/문서 백업 및 git 커밋

# 백업 정책
- 데이터베이스는 **풀백업** 진행 (pg_dump 등)
- 소스코드는 **핵심 소스만** 백업 (app, resources, routes, config, public, database, docs 등) 

# 첨부파일(이미지) 미노출 문제 발생 시
- storage/app/public/attachments에 파일이 있는데 웹에서 보이지 않으면 `php artisan storage:link` 상태를 반드시 점검
- 심볼릭 링크가 없으면 웹에서 /storage/attachments/ 경로로 접근 불가
- 서버 이전/OS 재설치/퍼미션 변경 후 반드시 storage:link 재확인

# 2025-07-09 작업 내역
- 단어 난이도 보정 스케줄러 개선 및 임시테이블 동기화 로직 개선
- Gemini API 프롬프트 구조 단순화 및 모델 버전별(1.5/2.5 flash) 실험
- 난이도 분포 집계 및 신규/재측정 방식 차이 분석
- Gemini API 쿼터 초과(429) 이슈 및 대응
- docs/WORKFLOW.md, docs/DATABASE_SCHEMA.md, docs/TROUBLESHOOTING.md에 상세 내역 기록 및 커밋/푸시 완료 

# 작업 규칙(중요)
- 모든 서버와 서비스는 반드시 서버사이드(systemd 등 백그라운드)에서만 실행한다.
- 터미널에서 직접 실행(포그라운드)은 금지. 

# 2025-01-28 작업 내역 (모바일 최적화)
## 퍼즐게임 모바일 반응형 개선
### 1. 상단 배너 모바일 숨김 처리
- **파일**: `resources/views/layouts/app.blade.php`
- **변경사항**: 모바일 화면(768px 이하)에서 상단 hero-banner 숨김
- **CSS 추가**: `@media (max-width: 768px) { .hero-banner { display: none !important; } }`

### 2. 퍼즐게임 모바일 최적화
- **파일**: `resources/views/puzzle/game/index.blade.php`
- **변경사항**: 모바일 화면에서 퍼즐 셀 크기 및 UI 요소 최적화
- **적용된 최적화**:
  - 컨테이너 패딩 축소 (0 10px)
  - 카드 헤더 폰트 크기 축소 (h3: 1.5rem, h5: 1rem)
  - 배지 크기 축소 (font-size: 0.75rem, padding: 0.25rem 0.5rem)
  - 퍼즐 셀 크기 축소 (35px x 35px, 320px 이하에서는 30px x 30px)
  - 셀 번호 및 단어 번호 폰트 크기 축소
  - 버튼 및 입력 필드 크기 최적화

### 3. 반응형 디자인 적용
- **768px 이하**: 일반 모바일 화면 최적화
- **320px 이하**: 매우 작은 모바일 화면 추가 최적화
- **터치 친화적**: 터치 영역 확보 및 가독성 향상

## 적용된 개선사항
- ✅ **상단 배너 숨김**: 모바일에서 불필요한 공간 절약
- ✅ **퍼즐 셀 크기 조정**: 모바일 화면에 맞는 적절한 크기
- ✅ **폰트 크기 최적화**: 가독성 향상
- ✅ **레이아웃 개선**: 모바일에서 더 나은 사용자 경험
- ✅ **터치 친화적**: 터치 영역 확보

## 테스트 필요사항
1. **다양한 모바일 기기**: iPhone, Android 다양한 화면 크기에서 테스트
2. **가로/세로 모드**: 화면 회전 시 레이아웃 확인
3. **터치 반응성**: 퍼즐 셀 클릭 및 버튼 터치 확인
4. **가독성**: 작은 화면에서 텍스트 가독성 확인
\n2025-07-22 index.ejs 기반 상담모달/갤러리/분양정보/모든 UI/스타일 1:1 복원 및 반응형 개선\n- 상담하기(consult-modal, consult-complete-modal) 모달 구조/스타일/동작 100% 복원 (HTML, CSS, JS)\n- 갤러리 카드형 구조 및 오버레이, 모달 1:1 복원\n- 분양정보 평면도 버튼, 모달, 스타일 1:1 복원\n- 모든 input/label/안내문구/버튼/라디오/체크박스 등 폼 요소 스타일 index.ejs와 동일하게 적용\n- 모바일/반응형(600px 이하)에서 모달/폼/버튼/텍스트/스크롤 완벽 복원\n- main.js: 모든 모달/트리거/공통 함수/상담 fetch 등 index.ejs와 동일하게 동작\n- custom.css: index.ejs의 <style> 및 반응형, 폼, 모달, 버튼, 안내문구 등 1:1 복사\n- index.html: consult-modal, gallery, sales-info 등 모든 구조/클래스/텍스트 1:1 복원\n- git 커밋 및 원격 저장소 푸시 완료\n

## 2025-07-24 작업 내역 (Django /manage/ 관리자 인증 및 계정 관리 UI 구축)

### 목적
- 기존 Django admin이 아닌 별도 관리자 페이지(/manage/)에 세션 기반 인증 및 계정 관리 기능 도입

### 주요 구현 내용
- /manage/ 하위 모든 뷰에 @login_required 데코레이터 적용(비로그인 시 접근 불가)
- /manage/login/ (로그인), /manage/logout/ (로그아웃), /manage/password_change/ (비밀번호 변경) URL 및 뷰 추가
- 로그인/비밀번호 변경 폼 템플릿(manage/login.html, manage/password_change.html) 및 공통 레이아웃(manage/base.html) 생성
- 로그인 성공 시 /manage/로 리다이렉트되도록 LOGIN_REDIRECT_URL 설정
- 템플릿 경로 및 권한 문제 해결, 서버 재시작 및 정상 동작 확인
- 관리자 계정 생성/삭제 등 추가 기능은 추후 구현 예정

### 적용 파일
- uljin_admin/main/urls.py
- uljin_admin/main/views.py
- uljin_admin/main/templates/manage/login.html
- uljin_admin/main/templates/manage/password_change.html
- uljin_admin/main/templates/manage/base.html
- uljin_admin/uljin_admin/settings.py
- docs/WORKFLOW.md (본 문서)

### 테스트 방법
1. /manage/login/에서 로그인 시도 → 성공 시 /manage/로 이동
2. 비밀번호 변경, 로그아웃 등 정상 동작 확인
3. 로그인하지 않으면 /manage/ 하위 접근 불가 확인

---

## 2025-09-19 작업 내역 (Spring Boot 크로스워드 게임 시스템 서비스 재시작 및 포트 관리)

### 문제 상황
- Spring Boot 크로스워드 게임 시스템(8081 포트)과 게시판 시스템(8080 포트)이 모두 중지된 상태
- 503 Service Unavailable 에러 발생
- 사용자가 크로스워드 게임에 접근할 수 없는 상황

### 해결 과정

#### 1. 서비스 상태 확인
- **크로스워드 게임**: 8081 포트에서 실행되어야 함 (`com.example.crossword.CrosswordApplication`)
- **게시판 시스템**: 8080 포트에서 실행되어야 함 (`com.example.board.BoardSystemApplication`)
- 두 시스템 모두 중지된 상태 확인

#### 2. Spring Boot 애플리케이션 재시작
- **크로스워드 게임 재시작**: `cd /var/www/html/crossword-projects && ./gradlew bootRun`
- **게시판 시스템 재시작**: `cd /var/www/html/java-projects && ./gradlew bootRun`
- 두 애플리케이션 모두 백그라운드에서 정상 시작

#### 3. 서비스 상태 확인
- **크로스워드 게임**: 8081 포트에서 정상 실행 중
- **게시판 시스템**: 8080 포트에서 정상 실행 중
- 두 시스템 모두 Tomcat 서버로 정상 시작됨

### 현재 상태
- ✅ **크로스워드 게임**: 8081 포트에서 정상 실행 중
- ✅ **게시판 시스템**: 8080 포트에서 정상 실행 중
- ✅ **503 에러 해결**: 두 시스템 모두 정상 접근 가능
- ✅ **서비스 안정성**: 백그라운드에서 안정적으로 실행 중

### 포트 구성
- **80 포트**: Apache (리버스 프록시)
  - `/api/board/` → 8080 포트 (게시판 시스템)
  - `/K-CrossWord/` → 8081 포트 (크로스워드 게임)
  - `/api/puzzle/` → 8081 포트 (크로스워드 게임 API)
- **8080 포트**: 게시판 시스템 (Spring Boot)
- **8081 포트**: 크로스워드 게임 (Spring Boot)
- **3001 포트**: React Native 웹앱 (정적 파일 서비스)

### 중요 파일 위치
- **크로스워드 게임**: `/var/www/html/crossword-projects/`
- **게시판 시스템**: `/var/www/html/java-projects/`
- **Apache 설정**: `/etc/apache2/sites-available/000-default.conf`
- **Laravel 소스**: `/var/www/html/` (현재 미실행)

### 서비스 관리 명령어
```bash
# 크로스워드 게임 재시작
cd /var/www/html/crossword-projects && ./gradlew bootRun

# 게시판 시스템 재시작
cd /var/www/html/java-projects && ./gradlew bootRun

# 실행 중인 Java 프로세스 확인
ps aux | grep -E "(java|gradle)" | grep -v grep

# 포트 사용 상태 확인
netstat -tlnp | grep -E ":(8080|8081)"
```

### 다음 작업 예정
1. **서비스 자동 시작 설정**: systemd 서비스로 등록하여 서버 재부팅 시 자동 시작
2. **모니터링 시스템**: 서비스 상태 모니터링 및 자동 재시작 설정
3. **로그 관리**: 각 서비스별 로그 파일 관리 및 로테이션 설정

---

## 2025-07-31 작업 내역 (React Native 모바일 앱 AuthContext 토큰 저장 및 API 인증 문제 해결)

### 문제 상황
- React Native 모바일 앱에서 로그인은 성공하지만 AuthContext 함수가 실행되지 않는 문제
- 로그인 후 메인 화면에서 "Unauthenticated" 에러 발생
- 토큰이 제대로 저장되지 않거나 API 호출 시 전달되지 않는 문제
- 로그가 너무 빨리 지나가서 디버깅이 어려운 상황

### 해결 과정

#### 1. AuthContext 연결 문제 해결
- **문제 발견**: LoginScreen에서 `apiService.login()`과 `AuthContext.login()` 두 번의 로그인 시도
- **해결**: `apiService.login()` 호출 제거하고 AuthContext의 `login()` 함수만 사용
- **수정 파일**: `CrosswordPuzzleMobileApp/src/screens/LoginScreen.tsx`

#### 2. 토큰 저장 로직 개선
- **기존 문제**: 복잡한 응답 구조 처리로 토큰 추출 실패
- **개선 사항**: 
  - 4가지 주요 응답 구조 지원 (response.authorization.token, response.data.authorization.token, response.token, response.data.token)
  - 저장 후 즉시 확인하는 로직 추가
  - 상세한 JSON 로그 출력
- **수정 파일**: `CrosswordPuzzleMobileApp/src/contexts/AuthContext.tsx`

#### 3. API 서비스 토큰 전달 로그 강화
- **추가된 로그**:
  - API 요청 시작/메서드/헤더 로그
  - 토큰 저장 상태 및 값 확인
  - 최종 요청 헤더 확인
- **수정 파일**: `CrosswordPuzzleMobileApp/src/services/api.ts`

#### 4. MainScreen API 호출 로그 개선
- **추가된 로그**:
  - API 호출 전 토큰 확인
  - 응답 구조 상세 로그
  - 성공/실패 상태 명확한 구분
- **수정 파일**: `CrosswordPuzzleMobileApp/src/screens/MainScreen.tsx`

#### 5. 로그 확인 시스템 개선
- **로그인 화면**: 로그인 성공 후 3초간 대기 (로그 확인용)
- **메인 화면**: 로그 화면 자동 표시, 2초간 대기 후 데이터 로드
- **개선 사항**:
  - "⏳ 3초간 대기 중... (로그 확인용)" 메시지
  - "🚀 메인 화면으로 이동합니다..." 메시지
  - 로그 화면 자동 표시 (`showLogs = true`)

### 수정된 파일들

**CrosswordPuzzleMobileApp/src/screens/LoginScreen.tsx**
```typescript
// AuthContext의 login 함수만 사용
const loginSuccess = await login(email, password);
addLog('🔍 AuthContext 로그인 결과: ' + loginSuccess);

if (loginSuccess) {
  addLog('✅ AuthContext 로그인 성공');
  addLog('⏳ 3초간 대기 중... (로그 확인용)');
  
  // 3초간 대기하여 로그 확인 가능
  await new Promise(resolve => setTimeout(resolve, 3000));
  
  addLog('🚀 메인 화면으로 이동합니다...');
}
```

**CrosswordPuzzleMobileApp/src/contexts/AuthContext.tsx**
```typescript
// 다양한 응답 구조 처리
const responseAny = response as any;
if (response && typeof response === 'object') {
  // 1. response.authorization.token
  if (responseAny.authorization && responseAny.authorization.token) {
    token = responseAny.authorization.token;
    userData = responseAny.user;
    console.log('✅ CrosswordPuzzleMobileApp AuthContext: response.authorization.token 구조');
  }
  // 2. response.data.authorization.token
  else if (responseAny.data && responseAny.data.authorization && responseAny.data.authorization.token) {
    token = responseAny.data.authorization.token;
    userData = responseAny.data.user;
    console.log('✅ CrosswordPuzzleMobileApp AuthContext: response.data.authorization.token 구조');
  }
  // 3. response.token (직접 토큰)
  else if (responseAny.token) {
    token = responseAny.token;
    userData = responseAny.user;
    console.log('✅ CrosswordPuzzleMobileApp AuthContext: response.token 구조');
  }
  // 4. response.data.token
  else if (responseAny.data && responseAny.data.token) {
    token = responseAny.data.token;
    userData = responseAny.data.user;
    console.log('✅ CrosswordPuzzleMobileApp AuthContext: response.data.token 구조');
  }
  else {
    console.log('❌ CrosswordPuzzleMobileApp AuthContext: 토큰을 찾을 수 없음');
    console.log('❌ CrosswordPuzzleMobileApp AuthContext: response 구조:', JSON.stringify(response, null, 2));
  }
}

if (token && userData) {
  console.log('✅ CrosswordPuzzleMobileApp AuthContext: 토큰 발견:', token);
  console.log('✅ CrosswordPuzzleMobileApp AuthContext: 사용자 데이터:', userData);
  
  // 토큰 저장
  try {
    await storage.setItem('auth_token', token);
    console.log('✅ AuthContext: 토큰 저장 성공');
    
    // 저장된 토큰 확인
    const savedToken = await storage.getItem('auth_token');
    console.log('🔍 AuthContext: 저장된 토큰 확인:', savedToken);
  } catch (error) {
    console.error('❌ AuthContext: 토큰 저장 실패:', error);
  }
}
```

**CrosswordPuzzleMobileApp/src/services/api.ts**
```typescript
// Request interceptor
api.interceptors.request.use(
  async (config) => {
    console.log('🔧 CrosswordPuzzleMobileApp API 요청 시작:', config.url);
    console.log('🔧 CrosswordPuzzleMobileApp 요청 메서드:', config.method);
    console.log('🔧 CrosswordPuzzleMobileApp 요청 헤더:', config.headers);
    
    const token = await storage.getItem('auth_token');
    console.log('🔑 CrosswordPuzzleMobileApp 저장된 토큰:', token ? '있음' : '없음');
    console.log('🔑 CrosswordPuzzleMobileApp 토큰 값:', token);
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('🔑 CrosswordPuzzleMobileApp 토큰 추가됨:', `Bearer ${token}`);
      console.log('🔧 CrosswordPuzzleMobileApp 최종 요청 헤더:', config.headers);
    } else {
      console.log('❌ CrosswordPuzzleMobileApp 토큰 없음');
    }
    
    return config;
  },
  (error) => {
    console.error('❌ CrosswordPuzzleMobileApp 요청 인터셉터 에러:', error);
    return Promise.reject(error);
  }
);
```

**CrosswordPuzzleMobileApp/src/screens/MainScreen.tsx**
```typescript
useEffect(() => {
  addLog('🚀 MainScreen 컴포넌트 마운트됨');
  addLog('⏳ 2초 후 데이터 로드 시작...');
  
  // 2초간 대기 후 데이터 로드 (로그 확인용)
  const timer = setTimeout(() => {
    loadUserData();
  }, 2000);
  
  return () => clearTimeout(timer);
}, []);

const [showLogs, setShowLogs] = useState(true); // 자동으로 로그 표시
```

### 빌드 결과
- ✅ **APK 빌드 성공**: 45M 크기, 2025-08-01 02:08:31 빌드
- ✅ **AuthContext 연결**: LoginScreen에서 AuthContext login 함수 정상 호출
- ✅ **토큰 저장 로직**: 다양한 응답 구조 지원 및 저장 확인
- ✅ **로그 확인 시스템**: 로그인/메인 화면에서 충분한 대기 시간 제공
- ✅ **API 호출 로그**: 토큰 전달 과정 상세 추적 가능

### 현재 상태
- ✅ **AuthContext 함수 호출**: 로그인 시 AuthContext login 함수 정상 실행
- ✅ **토큰 저장**: 다양한 응답 구조에서 토큰 추출 및 저장
- ✅ **로그 확인**: 3초/2초 대기로 로그 충분히 확인 가능
- ⚠️ **API 인증**: 메인 화면에서 "Unauthenticated" 에러 여전히 발생 가능성

### 다음 작업 예정
1. **새 APK 테스트**: 토큰 저장 및 API 호출 과정 로그 확인
2. **인증 문제 해결**: "Unauthenticated" 에러 원인 파악 및 수정
3. **API 서버 확인**: Laravel API 서버에서 토큰 인증 처리 확인
4. **최종 테스트**: 전체 로그인 → 메인 화면 → API 호출 플로우 확인

### 중요 파일 위치
- **모바일 앱**: `/var/www/html/CrosswordPuzzleMobileApp/`
- **AuthContext**: `CrosswordPuzzleMobileApp/src/contexts/AuthContext.tsx`
- **API 서비스**: `CrosswordPuzzleMobileApp/src/services/api.ts`
- **로그인 화면**: `CrosswordPuzzleMobileApp/src/screens/LoginScreen.tsx`
- **메인 화면**: `CrosswordPuzzleMobileApp/src/screens/MainScreen.tsx`

### 디버깅 명령어
- **APK 빌드**: `cd CrosswordPuzzleMobileApp && ./build-android.sh`
- **API 테스트**: `curl -X POST http://222.100.103.227:8080/api/login -H "Content-Type: application/json" -d '{"email":"test@test.com","password":"123456"}'`
- **로그 확인**: 앱에서 "로그 보기" 버튼 클릭 또는 자동 표시된 로그 확인

---

## 2025-09-22 작업 내역 (Spring Boot 인증 시스템 통합 완료)

### 문제 상황
- 8080 포트 게시판 서비스와 8081 포트 퍼즐 게임 서비스가 별도의 인증 시스템을 사용
- 8080 포트에서 로그인 후 8081 포트 관리자 페이지 접근 시 로그인 페이지로 리다이렉트
- 두 서비스 간 세션 공유가 되지 않는 문제

### 해결 과정

#### 1. Redis 세션 네임스페이스 통일
- **문제**: 8081 포트 서비스가 `crossword:session` 네임스페이스 사용
- **해결**: `CrosswordApplication.java`에서 하드코딩된 `redisNamespace = "crossword:session"` 제거
- **결과**: `application.properties`의 `spring.session.redis.namespace=spring:session` 설정 적용

#### 2. 인증 시스템 통합
- **User 엔티티 복제**: 8081 포트 서비스에 8080 포트와 동일한 User 엔티티 생성
- **UserRepository 생성**: 공유 users 테이블 접근을 위한 Repository 생성
- **CustomUserDetailsService 생성**: 8080 포트와 동일한 인증 로직 구현
- **SecurityConfig 수정**: 8080 포트와 동일한 인증 흐름으로 설정

#### 3. 프론트엔드 링크 업데이트
- **메인 페이지 링크**: 8080 포트 메인 페이지의 퍼즐 관리 시스템 링크를 HTTPS 도메인으로 업데이트
- **동적 로그인/로그아웃**: 로그인 상태에 따른 동적 버튼 표시 구현
- **Mixed Content 해결**: 모든 API 호출을 HTTPS로 변경

#### 4. Redis 세션 초기화 및 테스트
- **Redis 초기화**: `redis-cli FLUSHALL`로 모든 세션 데이터 삭제
- **새 세션 생성**: 로그인 후 `spring:session` 네임스페이스로 세션 생성 확인
- **세션 공유 확인**: 두 서비스가 동일한 세션을 인식하는지 테스트

### 구현된 기능
- ✅ **세션 공유**: 8080 포트와 8081 포트 서비스가 동일한 Redis 세션 사용
- ✅ **인증 통합**: 8080 포트에서 로그인 후 8081 포트 관리자 페이지 접근 가능
- ✅ **동적 UI**: 로그인 상태에 따른 동적 로그인/로그아웃 버튼 표시
- ✅ **HTTPS 지원**: Mixed Content 오류 해결 및 HTTPS 도메인 사용
- ✅ **관리자 페이지 접근**: 로그인 후 퍼즐 관리 시스템 정상 접근

### 수정된 파일들
- **CrosswordApplication.java**: 하드코딩된 Redis 네임스페이스 제거
- **User.java**: 8081 포트 서비스에 User 엔티티 생성
- **UserRepository.java**: 공유 users 테이블 접근을 위한 Repository 생성
- **CustomUserDetailsService.java**: 8080 포트와 동일한 인증 로직 구현
- **SecurityConfig.java**: 8080 포트와 동일한 인증 흐름으로 설정
- **index.html**: 8080 포트 메인 페이지 링크 및 동적 버튼 구현
- **admin/index.html**: HTTPS API 호출로 Mixed Content 오류 해결
- **admin/words/index.html**: HTTPS API 호출로 Mixed Content 오류 해결

### 현재 상태
- ✅ **8080 포트 게시판**: 정상 실행 중, 로그인 기능 작동
- ✅ **8081 포트 퍼즐 게임**: 정상 실행 중, 관리자 페이지 접근 가능
- ✅ **인증 시스템 통합**: 두 서비스 간 세션 공유 완료
- ✅ **관리자 페이지**: 로그인 후 정상 접근 가능

### Redis 세션 상태
```
spring:session:sessions:2712f37a-9bc2-4a05-bcef-3a9793d81238  ← 8080 포트 로그인 세션
spring:session:sessions:ff0b41cd-780e-46bd-8b1d-525d4f7e7d77  ← 8081 포트 접근 세션
spring:session:sessions:a8cbd598-032b-4af6-8795-f2785ccd3634  ← 추가 접근 세션
```

# 2025-01-27 작업 내역 (힌트 추가 기능 UI 구조 개선)

## 문제 상황
- 상단의 "[AI 힌트생성]" 버튼이 체크박스로 선택된 복수 단어에 동시 적용되는 구조
- 힌트는 개별 단어별로 고유한 내용이어야 하는데, 같은 힌트를 여러 단어에 추가하는 것은 논리적으로 맞지 않음

## 해결 과정

### 1. UI 구조 개선
- **상단 버튼 제거**: "[AI 힌트생성]" 버튼 제거
- **개별 단어별 힌트 추가**: 각 단어의 "관리" 컬럼에 "힌트 추가" 아이콘 추가
- **논리적 일관성**: 개별 단어에만 해당하는 힌트를 추가하는 올바른 구조

### 2. 관리 컬럼 버튼 구성
1. **수정** (파란색) - 단어 정보 수정
2. **힌트 추가** (초록색) - 해당 단어에 힌트 추가 ← **새로 추가**
3. **힌트 보기** (파란색) - 해당 단어의 힌트 목록 보기
4. **삭제** (빨간색) - 단어 삭제

### 3. 기능 구현
- `openAddHintModalForWord(wordId)` 함수 추가
- 개별 단어 ID를 받아서 해당 단어에만 힌트 추가
- 기존 `openAddHintModal()` 함수는 경고 메시지로 변경

### 4. 사용자 경험 개선
- **직관적 인터페이스**: 아이콘 기반의 명확한 기능 구분
- **효율성**: 필요한 단어에만 정확히 힌트 추가 가능
- **논리적 일관성**: 개별 단어별로 고유한 힌트 추가

## 수정된 파일
- `/var/www/html/crossword-projects/src/main/resources/static/admin/words/index.html`
  - 상단 "[AI 힌트생성]" 버튼 제거
  - 관리 컬럼에 "힌트 추가" 아이콘 추가
  - `openAddHintModalForWord()` 함수 구현
  - 힌트 추가 모달 제목 아이콘 변경 (마법봉 → 플러스)

## 결과
- ✅ **논리적 일관성**: 개별 단어별로 고유한 힌트 추가
- ✅ **사용자 경험**: 직관적인 아이콘 기반 인터페이스
- ✅ **기능 분리**: 단어 추가, 일괄변경, 개별 힌트 추가가 명확히 구분
- ✅ **효율성**: 필요한 단어에만 정확히 힌트 추가 가능

### 다음 작업 예정
1. **AI 힌트 생성**: Gemini API를 활용한 자동 힌트 생성
2. **레벨 관리**: 퍼즐 레벨 관리 기능 구현
3. **그리드 템플릿 관리**: 그리드 템플릿 관리 기능 구현

### 중요 파일 위치
- **8080 포트 게시판**: `/var/www/html/java-projects/`
- **8081 포트 퍼즐 게임**: `/var/www/html/crossword-projects/`
- **인증 설정**: `crossword-projects/src/main/java/com/example/crossword/config/`
- **관리자 페이지**: `crossword-projects/src/main/resources/static/admin/`

### 디버깅 명령어
- **Redis 세션 확인**: `redis-cli keys "*session*"`
- **서비스 상태 확인**: `ps aux | grep -E "(java|gradle)" | grep -v grep`
- **포트 사용 확인**: `netstat -tlnp | grep -E ":(8080|8081)"`
- **Redis 초기화**: `redis-cli FLUSHALL`

---
