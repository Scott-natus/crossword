# Laravel → Spring Boot 크로스워드 퍼즐 시스템 포팅 계획

## 📅 작성일: 2025-09-16
## 🎯 목표: Laravel 기반 크로스워드 퍼즐 시스템을 Spring Boot로 완전 마이그레이션

---

## 🔍 현재 Laravel 시스템 분석

### 📊 핵심 기능
1. **퍼즐 생성 시스템**
   - 레벨별 크로스워드 퍼즐 생성
   - 단어 조합 알고리즘 (교차점 기반)
   - 동적 그리드 크기 계산
   - 백트래킹 기반 단어 배치

2. **힌트 관리 시스템**
   - Gemini API 연동 힌트 자동 생성
   - 힌트 난이도 관리 (easy, medium, hard)
   - 힌트 스케줄러 (10분마다 50개씩 생성)

3. **사용자 게임 시스템**
   - React Native 웹앱 연동
   - 상세 플레이 기록 시스템
   - 힌트 사용 기록 추적
   - 정확도 및 통계 관리

4. **관리 시스템**
   - 퍼즐 레벨 관리
   - 단어 및 힌트 관리
   - 게시판 시스템 통합

### 🗄️ 데이터베이스 구조
```sql
-- 핵심 테이블
pz_words              -- 퍼즐 단어 정보
pz_hints              -- 단어 힌트 정보  
puzzle_levels         -- 퍼즐 레벨 관리
game_sessions         -- 게임 세션 정보
game_play_records     -- 상세 플레이 기록
hint_usage_records    -- 힌트 사용 기록
user_wrong_answers    -- 틀린 답변 기록
```

---

## 🚀 Spring Boot 포팅 전략

### 1단계: 프로젝트 구조 설계

#### 📁 Spring Boot 프로젝트 구조
```
/var/www/html/java-projects/
├── src/main/java/com/example/crossword/
│   ├── CrosswordApplication.java
│   ├── config/
│   │   ├── DatabaseConfig.java
│   │   ├── RedisConfig.java
│   │   └── SecurityConfig.java
│   ├── controller/
│   │   ├── CrosswordController.java
│   │   ├── PuzzleController.java
│   │   ├── HintController.java
│   │   └── GameController.java
│   ├── service/
│   │   ├── CrosswordGeneratorService.java
│   │   ├── HintGenerationService.java
│   │   ├── GameSessionService.java
│   │   └── PuzzleLevelService.java
│   ├── repository/
│   │   ├── WordRepository.java
│   │   ├── HintRepository.java
│   │   ├── PuzzleLevelRepository.java
│   │   └── GameSessionRepository.java
│   ├── entity/
│   │   ├── Word.java
│   │   ├── Hint.java
│   │   ├── PuzzleLevel.java
│   │   ├── GameSession.java
│   │   └── GamePlayRecord.java
│   ├── dto/
│   │   ├── CrosswordRequest.java
│   │   ├── CrosswordResponse.java
│   │   ├── GameSessionRequest.java
│   │   └── GameSessionResponse.java
│   └── util/
│       ├── CrosswordAlgorithm.java
│       ├── GridCalculator.java
│       └── WordPlacement.java
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   └── static/
└── build.gradle
```

### 2단계: 데이터베이스 마이그레이션

#### 🔄 PostgreSQL 스키마 변환
```sql
-- JPA Entity 매핑을 위한 테이블 구조
-- 기존 Laravel 테이블을 Spring Boot JPA에 맞게 조정

-- 1. 단어 테이블 (pz_words → Word Entity)
CREATE TABLE words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(255) NOT NULL,
    length INTEGER NOT NULL,
    category VARCHAR(50),
    difficulty INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (word, category)
);

-- 2. 힌트 테이블 (pz_hints → Hint Entity)  
CREATE TABLE hints (
    id BIGSERIAL PRIMARY KEY,
    word_id BIGINT NOT NULL,
    hint_text TEXT NOT NULL,
    hint_type VARCHAR(20) DEFAULT 'TEXT',
    image_url VARCHAR(255),
    audio_url VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    difficulty VARCHAR(20),
    correction_status VARCHAR(1) DEFAULT 'n',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (word_id) REFERENCES words(id) ON DELETE CASCADE
);

-- 3. 퍼즐 레벨 테이블 (puzzle_levels → PuzzleLevel Entity)
CREATE TABLE puzzle_levels (
    id BIGSERIAL PRIMARY KEY,
    level INTEGER NOT NULL,
    level_name VARCHAR(255) NOT NULL,
    word_count INTEGER NOT NULL,
    word_difficulty INTEGER NOT NULL,
    hint_difficulty VARCHAR(255) NOT NULL,
    intersection_count INTEGER NOT NULL,
    time_limit INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255)
);

-- 4. 게임 세션 테이블 (game_sessions → GameSession Entity)
CREATE TABLE game_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    word_id INTEGER NOT NULL,
    session_started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    session_ended_at TIMESTAMP,
    total_play_time INTEGER DEFAULT 0,
    accuracy_rate NUMERIC(5,2) DEFAULT 0,
    total_correct_answers INTEGER DEFAULT 0,
    total_wrong_answers INTEGER DEFAULT 0,
    hints_used_count INTEGER DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (word_id) REFERENCES words(id) ON DELETE CASCADE
);

-- 5. 게임 플레이 기록 테이블 (game_play_records → GamePlayRecord Entity)
CREATE TABLE game_play_records (
    id BIGSERIAL PRIMARY KEY,
    game_session_id BIGINT NOT NULL,
    step_number INTEGER NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_data JSONB,
    is_correct BOOLEAN,
    time_spent_seconds INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id) ON DELETE CASCADE
);
```

### 3단계: 핵심 서비스 구현

#### 🧩 CrosswordGeneratorService
```java
@Service
@Transactional
public class CrosswordGeneratorService {
    
    @Autowired
    private WordRepository wordRepository;
    
    @Autowired
    private PuzzleLevelRepository puzzleLevelRepository;
    
    /**
     * 레벨에 맞는 크로스워드 퍼즐 생성
     */
    public CrosswordResponse generateCrossword(Long levelId) {
        // 1. 레벨 정보 조회
        PuzzleLevel level = puzzleLevelRepository.findById(levelId)
            .orElseThrow(() -> new EntityNotFoundException("레벨을 찾을 수 없습니다."));
        
        // 2. 단어 후보군 선정
        List<Word> wordCombination = selectWordCombination(level);
        
        // 3. 동적 그리드 크기 계산
        int gridSize = calculateGridSize(wordCombination, level.getIntersectionCount());
        
        // 4. 백트래킹 기반 단어 배치
        WordPlacement placement = placeWordsWithBacktracking(
            wordCombination, gridSize, level.getIntersectionCount());
        
        // 5. 힌트 생성
        List<Hint> hints = generateHints(wordCombination);
        
        return CrosswordResponse.builder()
            .level(level)
            .grid(placement.getGrid())
            .words(placement.getWords())
            .wordPositions(placement.getWordPositions())
            .hints(hints)
            .stats(calculateStats(wordCombination, level))
            .build();
    }
    
    /**
     * 단어 조합 선택 알고리즘 (Laravel 로직 포팅)
     */
    private List<Word> selectWordCombination(PuzzleLevel level) {
        // Laravel의 selectWordCombination 로직을 Java로 포팅
        // 1. 출제 가능한 단어 풀 생성
        // 2. 첫 번째 단어 선택
        // 3. 교차점 기반 단어 조합 생성
        // 4. 조건 만족 검증
    }
    
    /**
     * 백트래킹 기반 단어 배치
     */
    private WordPlacement placeWordsWithBacktracking(
        List<Word> words, int gridSize, int intersectionCount) {
        // Laravel의 백트래킹 알고리즘을 Java로 포팅
    }
}
```

#### 🎯 HintGenerationService
```java
@Service
public class HintGenerationService {
    
    @Autowired
    private HintRepository hintRepository;
    
    @Autowired
    private WordRepository wordRepository;
    
    /**
     * Gemini API를 통한 힌트 자동 생성
     */
    public void generateHintsForWords(List<Word> words) {
        for (Word word : words) {
            try {
                // Gemini API 호출
                String hintText = callGeminiAPI(word.getWord());
                
                // 힌트 저장
                Hint hint = Hint.builder()
                    .wordId(word.getId())
                    .hintText(hintText)
                    .hintType("TEXT")
                    .difficulty("medium")
                    .isPrimary(true)
                    .build();
                    
                hintRepository.save(hint);
                
            } catch (Exception e) {
                log.error("힌트 생성 실패: word={}, error={}", word.getWord(), e.getMessage());
            }
        }
    }
    
    /**
     * 스케줄러를 통한 자동 힌트 생성
     */
    @Scheduled(fixedRate = 600000) // 10분마다
    public void scheduledHintGeneration() {
        // 힌트가 없는 단어 50개 조회
        List<Word> wordsWithoutHints = wordRepository.findWordsWithoutHints(50);
        
        if (!wordsWithoutHints.isEmpty()) {
            generateHintsForWords(wordsWithoutHints);
            log.info("스케줄러 힌트 생성 완료: {}개", wordsWithoutHints.size());
        }
    }
}
```

#### 🎮 GameSessionService
```java
@Service
@Transactional
public class GameSessionService {
    
    @Autowired
    private GameSessionRepository gameSessionRepository;
    
    @Autowired
    private GamePlayRecordRepository gamePlayRecordRepository;
    
    /**
     * 게임 세션 시작
     */
    public GameSessionResponse startGameSession(GameSessionRequest request) {
        GameSession session = GameSession.builder()
            .userId(request.getUserId())
            .wordId(request.getWordId())
            .sessionStartedAt(LocalDateTime.now())
            .isCompleted(false)
            .build();
            
        GameSession savedSession = gameSessionRepository.save(session);
        
        return GameSessionResponse.from(savedSession);
    }
    
    /**
     * 게임 플레이 기록 저장
     */
    public void recordGamePlay(Long sessionId, GamePlayRecordRequest request) {
        GamePlayRecord record = GamePlayRecord.builder()
            .gameSessionId(sessionId)
            .stepNumber(request.getStepNumber())
            .actionType(request.getActionType())
            .actionData(request.getActionData())
            .isCorrect(request.getIsCorrect())
            .timeSpentSeconds(request.getTimeSpentSeconds())
            .build();
            
        gamePlayRecordRepository.save(record);
    }
    
    /**
     * 게임 세션 종료
     */
    public void endGameSession(Long sessionId, GameSessionEndRequest request) {
        GameSession session = gameSessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("게임 세션을 찾을 수 없습니다."));
            
        session.setSessionEndedAt(LocalDateTime.now());
        session.setTotalPlayTime(request.getTotalPlayTime());
        session.setAccuracyRate(request.getAccuracyRate());
        session.setTotalCorrectAnswers(request.getTotalCorrectAnswers());
        session.setTotalWrongAnswers(request.getTotalWrongAnswers());
        session.setHintsUsedCount(request.getHintsUsedCount());
        session.setCompleted(true);
        
        gameSessionRepository.save(session);
    }
}
```

### 4단계: API 엔드포인트 구현

#### 🌐 REST API 설계
```java
@RestController
@RequestMapping("/api/crossword")
public class CrosswordController {
    
    @Autowired
    private CrosswordGeneratorService crosswordGeneratorService;
    
    /**
     * 크로스워드 퍼즐 생성
     */
    @GetMapping("/generate/{levelId}")
    public ResponseEntity<CrosswordResponse> generateCrossword(@PathVariable Long levelId) {
        try {
            CrosswordResponse response = crosswordGeneratorService.generateCrossword(levelId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
    
    /**
     * 레벨별 단어 조합 조회
     */
    @GetMapping("/words/{levelId}")
    public ResponseEntity<List<WordResponse>> getWordsByLevel(@PathVariable Long levelId) {
        List<Word> words = wordService.getWordsByLevel(levelId);
        List<WordResponse> response = words.stream()
            .map(WordResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}

@RestController
@RequestMapping("/api/game")
public class GameController {
    
    @Autowired
    private GameSessionService gameSessionService;
    
    /**
     * 게임 세션 시작
     */
    @PostMapping("/session/start")
    public ResponseEntity<GameSessionResponse> startGameSession(
        @RequestBody GameSessionRequest request) {
        GameSessionResponse response = gameSessionService.startGameSession(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 게임 플레이 기록
     */
    @PostMapping("/session/{sessionId}/record")
    public ResponseEntity<Void> recordGamePlay(
        @PathVariable Long sessionId,
        @RequestBody GamePlayRecordRequest request) {
        gameSessionService.recordGamePlay(sessionId, request);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 게임 세션 종료
     */
    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<Void> endGameSession(
        @PathVariable Long sessionId,
        @RequestBody GameSessionEndRequest request) {
        gameSessionService.endGameSession(sessionId, request);
        return ResponseEntity.ok().build();
    }
}
```

### 5단계: 프론트엔드 연동

#### 🔗 React Native 웹앱 연동
```javascript
// API 서비스 클래스
class CrosswordApiService {
    constructor() {
        this.baseUrl = 'https://natus250601.viewdns.net/api/crossword';
    }
    
    // 크로스워드 퍼즐 생성
    async generateCrossword(levelId) {
        const response = await fetch(`${this.baseUrl}/generate/${levelId}`);
        return response.json();
    }
    
    // 게임 세션 시작
    async startGameSession(userId, wordId) {
        const response = await fetch(`${this.baseUrl}/game/session/start`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ userId, wordId })
        });
        return response.json();
    }
    
    // 게임 플레이 기록
    async recordGamePlay(sessionId, playData) {
        const response = await fetch(`${this.baseUrl}/game/session/${sessionId}/record`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(playData)
        });
        return response.ok;
    }
}
```

---

## 📋 구현 단계별 계획

### Phase 1: 기본 구조 설정 (1주)
- [ ] Spring Boot 프로젝트 구조 생성
- [ ] 데이터베이스 스키마 마이그레이션
- [ ] JPA Entity 클래스 구현
- [ ] 기본 Repository 인터페이스 구현

### Phase 2: 핵심 서비스 구현 (2주)
- [ ] CrosswordGeneratorService 구현
- [ ] HintGenerationService 구현
- [ ] GameSessionService 구현
- [ ] 단어 조합 알고리즘 포팅

### Phase 3: API 엔드포인트 구현 (1주)
- [ ] REST API 컨트롤러 구현
- [ ] 요청/응답 DTO 클래스 구현
- [ ] 예외 처리 및 에러 응답 구현
- [ ] API 문서화 (Swagger)

### Phase 4: 프론트엔드 연동 (1주)
- [ ] React Native 웹앱 API 연동
- [ ] 기존 Laravel API 호출을 Spring Boot로 변경
- [ ] 인증 시스템 통합
- [ ] 테스트 및 디버깅

### Phase 5: 고급 기능 구현 (2주)
- [ ] 힌트 자동 생성 스케줄러
- [ ] 게임 통계 및 분석 기능
- [ ] 관리자 페이지 연동
- [ ] 성능 최적화

### Phase 6: 테스트 및 배포 (1주)
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] 성능 테스트
- [ ] 프로덕션 배포

---

## 🔧 기술적 고려사항

### 1. 데이터베이스 마이그레이션
- **기존 데이터 보존**: Laravel 데이터를 Spring Boot로 안전하게 이전
- **스키마 호환성**: JPA Entity와 기존 테이블 구조 매핑
- **인덱스 최적화**: 성능을 위한 인덱스 재설계

### 2. 알고리즘 포팅
- **단어 조합 알고리즘**: Laravel의 복잡한 로직을 Java로 정확히 포팅
- **백트래킹 알고리즘**: 그리드 배치 알고리즘의 성능 최적화
- **메모리 관리**: 대용량 데이터 처리 시 메모리 효율성

### 3. API 호환성
- **기존 API 유지**: React Native 웹앱의 기존 API 호출 방식 유지
- **응답 형식**: Laravel API와 동일한 JSON 응답 구조
- **에러 처리**: 기존 에러 코드 및 메시지 형식 유지

### 4. 성능 최적화
- **캐싱 전략**: Redis를 활용한 단어 및 힌트 캐싱
- **데이터베이스 최적화**: 쿼리 성능 개선 및 인덱스 최적화
- **비동기 처리**: 힌트 생성 등 시간이 오래 걸리는 작업의 비동기 처리

---

## 🎯 성공 지표

### 기능적 지표
- [ ] 모든 Laravel 기능이 Spring Boot에서 정상 작동
- [ ] React Native 웹앱과의 API 연동 성공
- [ ] 기존 데이터의 100% 마이그레이션
- [ ] 모든 게임 기능의 정상 작동

### 성능 지표
- [ ] API 응답 시간: 500ms 이하
- [ ] 퍼즐 생성 시간: 2초 이하
- [ ] 힌트 생성 성공률: 95% 이상
- [ ] 시스템 가용성: 99.9% 이상

### 사용자 경험 지표
- [ ] 기존 사용자 경험의 100% 유지
- [ ] 새로운 기능의 직관적인 사용성
- [ ] 모바일 및 웹 환경에서의 일관된 경험

---

## 📝 다음 단계

1. **데이터베이스 마이그레이션 전략 수립**
2. **Redis 세션 관리 시스템 설정**
3. **Spring Boot 프로젝트 기본 구조 생성**
4. **핵심 Entity 클래스 구현**
5. **단어 조합 알고리즘 포팅 시작**

이 계획을 통해 Laravel 기반의 크로스워드 퍼즐 시스템을 Spring Boot로 성공적으로 마이그레이션할 수 있을 것입니다.
