# Laravel Crossword Puzzle Management System - Workflow

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

