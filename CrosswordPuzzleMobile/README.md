# 크로스워드 퍼즐 모바일 앱

## 프로젝트 개요
- **프로젝트 번호**: 5번
- **목적**: React Native를 사용한 크로스워드 퍼즐 모바일 앱
- **API 서버**: 8080 포트 (http://222.100.103.227:8080/api)
- **기술 스택**: React Native, Expo, TypeScript, AsyncStorage

## 주요 기능
- 사용자 인증 (로그인/회원가입)
- 퍼즐 게임 플레이
- 힌트 시스템
- 게임 진행 상태 저장

## 설치 및 실행

### 1. 의존성 설치
```bash
npm install
```

### 2. 개발 서버 실행
```bash
# Android
npm run android

# iOS (macOS 필요)
npm run ios

# 웹
npm run web
```

### 3. 빌드
```bash
# Android APK 빌드
expo build:android

# iOS 빌드 (macOS 필요)
expo build:ios
```

## 프로젝트 구조
```
src/
├── contexts/
│   └── AuthContext.tsx      # 인증 상태 관리
├── screens/
│   ├── LoadingScreen.tsx    # 로딩 화면
│   ├── LoginScreen.tsx      # 로그인 화면
│   ├── RegisterScreen.tsx   # 회원가입 화면
│   └── GameScreen.tsx       # 게임 화면
├── services/
│   └── apiService.ts        # API 연동 서비스
└── components/              # 재사용 컴포넌트
```

## API 연동
- **기본 URL**: http://222.100.103.227:8080/api
- **인증**: Bearer 토큰 방식
- **토큰 저장**: AsyncStorage 사용

## 주요 특징
- **AsyncStorage**: 토큰 및 사용자 정보 안전한 저장
- **네비게이션**: 인증 상태에 따른 화면 분기
- **에러 처리**: 네트워크 오류 및 인증 실패 처리
- **반응형 UI**: 다양한 화면 크기 지원

## 개발 환경
- **Node.js**: 18.x 이상
- **Expo CLI**: 최신 버전
- **React Native**: 0.73.x
- **TypeScript**: 5.x

## 주의사항
- API 서버(8080 포트)가 실행 중이어야 함
- 네트워크 연결이 필요함
- AsyncStorage는 디바이스별로 독립적으로 저장됨

## 문제 해결
- **빌드 오류**: `expo doctor` 실행 후 문제 해결
- **네트워크 오류**: API 서버 상태 확인
- **토큰 문제**: AsyncStorage 클리어 후 재로그인









