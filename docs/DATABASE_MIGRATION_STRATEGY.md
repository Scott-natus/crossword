# PostgreSQL 데이터베이스 마이그레이션 전략

## 📅 작성일: 2025-09-16
## 🎯 목표: Laravel → Spring Boot 데이터베이스 마이그레이션 전략 수립

---

## 📊 현재 데이터베이스 현황

### 🗄️ 기존 테이블 구조
```sql
-- 핵심 퍼즐 관련 테이블
pz_words                    -- 퍼즐 단어 (36,622개)
pz_hints                    -- 단어 힌트 (109,865개)
puzzle_levels              -- 퍼즐 레벨 관리
puzzle_levels_origin       -- 퍼즐 레벨 원본 데이터

-- 게임 관련 테이블
game_sessions              -- 게임 세션 정보
game_play_records          -- 상세 플레이 기록
hint_usage_records         -- 힌트 사용 기록
user_puzzle_games          -- 사용자 퍼즐 게임
user_puzzle_profiles       -- 사용자 퍼즐 프로필
user_puzzle_achievements   -- 사용자 퍼즐 성취

-- 그리드 템플릿 관련 테이블
puzzle_grid_templates      -- 퍼즐 그리드 템플릿
puzzle_grid_template_word  -- 템플릿 단어 매핑
puzzle_game_records        -- 퍼즐 게임 기록

-- 카테고리 테이블
pz_base_categories         -- 기본 카테고리
```

### 📈 데이터 규모
- **단어 데이터**: 36,622개
- **힌트 데이터**: 109,865개
- **총 관계형 데이터**: 약 150,000개 레코드

---

## 🚀 마이그레이션 전략

### 1단계: 데이터 백업 및 검증

#### 📦 백업 전략
```bash
# 1. 전체 데이터베이스 백업
pg_dump -h 127.0.0.1 -U myuser -d mydb > backup_before_migration_$(date +%Y%m%d_%H%M%S).sql

# 2. 퍼즐 관련 테이블만 백업
pg_dump -h 127.0.0.1 -U myuser -d mydb \
  -t pz_words -t pz_hints -t puzzle_levels \
  -t game_sessions -t game_play_records \
  -t hint_usage_records > puzzle_tables_backup_$(date +%Y%m%d_%H%M%S).sql

# 3. 데이터 무결성 검증
PGPASSWORD=tngkrrhk psql -h 127.0.0.1 -U myuser -d mydb -c "
SELECT 
  'pz_words' as table_name, COUNT(*) as record_count FROM pz_words
UNION ALL
SELECT 
  'pz_hints' as table_name, COUNT(*) as record_count FROM pz_hints
UNION ALL
SELECT 
  'puzzle_levels' as table_name, COUNT(*) as record_count FROM puzzle_levels
UNION ALL
SELECT 
  'game_sessions' as table_name, COUNT(*) as record_count FROM game_sessions;
"
```

#### 🔍 데이터 검증
```sql
-- 1. 외래키 무결성 검증
SELECT 
  h.id, h.word_id, w.id as word_exists
FROM pz_hints h
LEFT JOIN pz_words w ON h.word_id = w.id
WHERE w.id IS NULL;

-- 2. 힌트가 없는 단어 확인
SELECT w.id, w.word, w.difficulty
FROM pz_words w
LEFT JOIN pz_hints h ON w.id = h.word_id
WHERE h.id IS NULL
LIMIT 10;

-- 3. 중복 데이터 확인
SELECT word, category, COUNT(*) as duplicate_count
FROM pz_words
GROUP BY word, category
HAVING COUNT(*) > 1;
```

### 2단계: Spring Boot JPA Entity 설계

#### 🏗️ Entity 클래스 구조
```java
// 1. Word Entity
@Entity
@Table(name = "words")
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "word", nullable = false, length = 255)
    private String word;
    
    @Column(name = "length", nullable = false)
    private Integer length;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Hint> hints = new ArrayList<>();
    
    // 생성자, getter, setter, equals, hashCode
}

// 2. Hint Entity
@Entity
@Table(name = "hints")
public class Hint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;
    
    @Column(name = "hint_text", nullable = false, columnDefinition = "TEXT")
    private String hintText;
    
    @Column(name = "hint_type", length = 20)
    private String hintType = "TEXT";
    
    @Column(name = "image_url", length = 255)
    private String imageUrl;
    
    @Column(name = "audio_url", length = 255)
    private String audioUrl;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    
    @Column(name = "difficulty", length = 20)
    private String difficulty;
    
    @Column(name = "correction_status", length = 1)
    private String correctionStatus = "n";
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 생성자, getter, setter, equals, hashCode
}

// 3. PuzzleLevel Entity
@Entity
@Table(name = "puzzle_levels")
public class PuzzleLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "level", nullable = false)
    private Integer level;
    
    @Column(name = "level_name", nullable = false, length = 255)
    private String levelName;
    
    @Column(name = "word_count", nullable = false)
    private Integer wordCount;
    
    @Column(name = "word_difficulty", nullable = false)
    private Integer wordDifficulty;
    
    @Column(name = "hint_difficulty", nullable = false, length = 255)
    private String hintDifficulty;
    
    @Column(name = "intersection_count", nullable = false)
    private Integer intersectionCount;
    
    @Column(name = "time_limit", nullable = false)
    private Integer timeLimit;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by", length = 255)
    private String updatedBy;
    
    // 생성자, getter, setter, equals, hashCode
}

// 4. GameSession Entity
@Entity
@Table(name = "game_sessions")
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;
    
    @Column(name = "session_started_at", nullable = false)
    private LocalDateTime sessionStartedAt;
    
    @Column(name = "session_ended_at")
    private LocalDateTime sessionEndedAt;
    
    @Column(name = "total_play_time")
    private Integer totalPlayTime = 0;
    
    @Column(name = "accuracy_rate", precision = 5, scale = 2)
    private BigDecimal accuracyRate = BigDecimal.ZERO;
    
    @Column(name = "total_correct_answers")
    private Integer totalCorrectAnswers = 0;
    
    @Column(name = "total_wrong_answers")
    private Integer totalWrongAnswers = 0;
    
    @Column(name = "hints_used_count")
    private Integer hintsUsedCount = 0;
    
    @Column(name = "is_completed")
    private Boolean isCompleted = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GamePlayRecord> playRecords = new ArrayList<>();
    
    // 생성자, getter, setter, equals, hashCode
}

// 5. GamePlayRecord Entity
@Entity
@Table(name = "game_play_records")
public class GamePlayRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;
    
    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;
    
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;
    
    @Column(name = "action_data", columnDefinition = "JSONB")
    private String actionData;
    
    @Column(name = "is_correct")
    private Boolean isCorrect;
    
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 생성자, getter, setter, equals, hashCode
}
```

### 3단계: Repository 인터페이스 설계

#### 🔍 Repository 클래스 구조
```java
// 1. WordRepository
@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    
    // 난이도별 단어 조회
    List<Word> findByDifficultyAndIsActiveTrue(Integer difficulty);
    
    // 카테고리별 단어 조회
    List<Word> findByCategoryAndIsActiveTrue(String category);
    
    // 힌트가 없는 단어 조회
    @Query("SELECT w FROM Word w LEFT JOIN w.hints h WHERE h.id IS NULL AND w.isActive = true")
    List<Word> findWordsWithoutHints(@Param("limit") int limit);
    
    // 길이별 단어 조회
    List<Word> findByLengthAndIsActiveTrue(Integer length);
    
    // 복합 조건 조회
    @Query("SELECT w FROM Word w WHERE w.difficulty = :difficulty AND w.length = :length AND w.isActive = true")
    List<Word> findByDifficultyAndLength(@Param("difficulty") Integer difficulty, @Param("length") Integer length);
    
    // 랜덤 단어 조회
    @Query(value = "SELECT * FROM words WHERE difficulty = :difficulty AND is_active = true ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Word> findRandomWordsByDifficulty(@Param("difficulty") Integer difficulty, @Param("limit") Integer limit);
}

// 2. HintRepository
@Repository
public interface HintRepository extends JpaRepository<Hint, Long> {
    
    // 단어별 힌트 조회
    List<Hint> findByWordIdAndIsPrimaryTrue(Long wordId);
    
    // 난이도별 힌트 조회
    List<Hint> findByDifficulty(String difficulty);
    
    // 힌트 타입별 조회
    List<Hint> findByHintType(String hintType);
    
    // 단어별 모든 힌트 조회
    List<Hint> findByWordIdOrderByCreatedAtAsc(Long wordId);
}

// 3. PuzzleLevelRepository
@Repository
public interface PuzzleLevelRepository extends JpaRepository<PuzzleLevel, Long> {
    
    // 레벨 번호로 조회
    Optional<PuzzleLevel> findByLevel(Integer level);
    
    // 활성 레벨 조회
    List<PuzzleLevel> findAllByOrderByLevelAsc();
    
    // 난이도별 레벨 조회
    List<PuzzleLevel> findByWordDifficulty(Integer wordDifficulty);
}

// 4. GameSessionRepository
@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    
    // 사용자별 게임 세션 조회
    List<GameSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 완료된 게임 세션 조회
    List<GameSession> findByUserIdAndIsCompletedTrue(Long userId);
    
    // 기간별 게임 세션 조회
    @Query("SELECT gs FROM GameSession gs WHERE gs.userId = :userId AND gs.createdAt BETWEEN :startDate AND :endDate")
    List<GameSession> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    // 통계 조회
    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.userId = :userId AND gs.isCompleted = true")
    Long countCompletedSessionsByUserId(@Param("userId") Long userId);
}
```

### 4단계: 데이터 마이그레이션 스크립트

#### 🔄 마이그레이션 SQL 스크립트
```sql
-- 1. Spring Boot용 테이블 생성 (기존 테이블과 호환)
-- 기존 테이블명을 유지하여 데이터 손실 방지

-- 단어 테이블 (pz_words → words로 별칭 생성)
CREATE VIEW words AS SELECT * FROM pz_words;

-- 힌트 테이블 (pz_hints → hints로 별칭 생성)  
CREATE VIEW hints AS SELECT * FROM pz_hints;

-- 퍼즐 레벨 테이블 (puzzle_levels 유지)
-- 게임 세션 테이블 (game_sessions 유지)
-- 게임 플레이 기록 테이블 (game_play_records 유지)

-- 2. 인덱스 최적화
CREATE INDEX IF NOT EXISTS idx_words_difficulty_active ON pz_words(difficulty, is_active);
CREATE INDEX IF NOT EXISTS idx_words_category_active ON pz_words(category, is_active);
CREATE INDEX IF NOT EXISTS idx_words_length_active ON pz_words(length, is_active);
CREATE INDEX IF NOT EXISTS idx_hints_word_id ON pz_hints(word_id);
CREATE INDEX IF NOT EXISTS idx_hints_difficulty ON pz_hints(difficulty);
CREATE INDEX IF NOT EXISTS idx_game_sessions_user_id ON game_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_game_sessions_word_id ON game_sessions(word_id);
CREATE INDEX IF NOT EXISTS idx_game_play_records_session_id ON game_play_records(game_session_id);

-- 3. 데이터 무결성 검증
-- 힌트가 없는 단어 확인
SELECT COUNT(*) as words_without_hints
FROM pz_words w
LEFT JOIN pz_hints h ON w.id = h.word_id
WHERE h.id IS NULL AND w.is_active = true;

-- 중복 힌트 확인
SELECT word_id, COUNT(*) as hint_count
FROM pz_hints
GROUP BY word_id
HAVING COUNT(*) > 1
ORDER BY hint_count DESC
LIMIT 10;

-- 4. 성능 테스트 쿼리
EXPLAIN ANALYZE
SELECT w.*, h.hint_text
FROM pz_words w
JOIN pz_hints h ON w.id = h.word_id
WHERE w.difficulty = 1 AND w.is_active = true
LIMIT 100;
```

### 5단계: 마이그레이션 실행 계획

#### 📋 단계별 실행 순서
```bash
# 1. 백업 생성
./scripts/backup_database.sh

# 2. 데이터 검증
./scripts/validate_data.sh

# 3. Spring Boot 애플리케이션 중지
sudo systemctl stop spring-boot-app

# 4. 데이터베이스 스키마 업데이트
./scripts/update_schema.sh

# 5. Spring Boot 애플리케이션 시작
sudo systemctl start spring-boot-app

# 6. 마이그레이션 검증
./scripts/verify_migration.sh

# 7. 성능 테스트
./scripts/performance_test.sh
```

#### 🔧 마이그레이션 스크립트
```bash
#!/bin/bash
# backup_database.sh

echo "데이터베이스 백업 시작..."

# 전체 백업
pg_dump -h 127.0.0.1 -U myuser -d mydb > "backup_full_$(date +%Y%m%d_%H%M%S).sql"

# 퍼즐 관련 테이블 백업
pg_dump -h 127.0.0.1 -U myuser -d mydb \
  -t pz_words -t pz_hints -t puzzle_levels \
  -t game_sessions -t game_play_records \
  -t hint_usage_records > "backup_puzzle_$(date +%Y%m%d_%H%M%S).sql"

echo "백업 완료"
```

```bash
#!/bin/bash
# validate_data.sh

echo "데이터 검증 시작..."

# 데이터 개수 확인
PGPASSWORD=tngkrrhk psql -h 127.0.0.1 -U myuser -d mydb -c "
SELECT 
  'pz_words' as table_name, COUNT(*) as record_count FROM pz_words
UNION ALL
SELECT 
  'pz_hints' as table_name, COUNT(*) as record_count FROM pz_hints
UNION ALL
SELECT 
  'puzzle_levels' as table_name, COUNT(*) as record_count FROM puzzle_levels;
"

# 외래키 무결성 검증
PGPASSWORD=tngkrrhk psql -h 127.0.0.1 -U myuser -d mydb -c "
SELECT COUNT(*) as orphaned_hints
FROM pz_hints h
LEFT JOIN pz_words w ON h.word_id = w.id
WHERE w.id IS NULL;
"

echo "데이터 검증 완료"
```

### 6단계: 롤백 전략

#### 🔄 롤백 계획
```bash
#!/bin/bash
# rollback_migration.sh

echo "마이그레이션 롤백 시작..."

# 1. Spring Boot 애플리케이션 중지
sudo systemctl stop spring-boot-app

# 2. 최신 백업으로 복원
LATEST_BACKUP=$(ls -t backup_full_*.sql | head -n1)
echo "복원할 백업 파일: $LATEST_BACKUP"

# 3. 데이터베이스 복원
PGPASSWORD=tngkrrhk psql -h 127.0.0.1 -U myuser -d mydb < "$LATEST_BACKUP"

# 4. Spring Boot 애플리케이션 시작
sudo systemctl start spring-boot-app

echo "롤백 완료"
```

---

## 📊 마이그레이션 검증 체크리스트

### ✅ 데이터 무결성 검증
- [ ] 모든 테이블의 레코드 수 일치
- [ ] 외래키 관계 정상 작동
- [ ] 인덱스 정상 생성
- [ ] 제약조건 정상 적용

### ✅ 성능 검증
- [ ] 단어 조회 쿼리 성능 (100ms 이하)
- [ ] 힌트 조회 쿼리 성능 (50ms 이하)
- [ ] 게임 세션 조회 성능 (200ms 이하)
- [ ] 복합 쿼리 성능 (500ms 이하)

### ✅ 기능 검증
- [ ] Spring Boot 애플리케이션 정상 시작
- [ ] JPA Entity 매핑 정상 작동
- [ ] Repository 메서드 정상 작동
- [ ] API 엔드포인트 정상 응답

### ✅ 사용자 경험 검증
- [ ] React Native 웹앱 정상 작동
- [ ] 기존 API 호출 정상 작동
- [ ] 게임 기능 정상 작동
- [ ] 관리자 기능 정상 작동

---

## 🎯 마이그레이션 성공 지표

### 📈 성능 지표
- **API 응답 시간**: 500ms 이하
- **데이터베이스 쿼리 시간**: 100ms 이하
- **시스템 가용성**: 99.9% 이상
- **에러율**: 0.1% 이하

### 📊 데이터 지표
- **데이터 손실률**: 0%
- **데이터 무결성**: 100%
- **마이그레이션 성공률**: 100%
- **롤백 가능성**: 100%

### 🎮 사용자 경험 지표
- **기능 호환성**: 100%
- **성능 저하**: 0%
- **사용자 불만**: 0건
- **서비스 중단**: 0분

---

## 📝 다음 단계

1. **Redis 세션 관리 시스템 설정**
2. **Spring Boot 프로젝트 기본 구조 생성**
3. **JPA Entity 클래스 구현**
4. **Repository 인터페이스 구현**
5. **마이그레이션 스크립트 작성 및 테스트**

이 전략을 통해 안전하고 효율적인 데이터베이스 마이그레이션을 수행할 수 있을 것입니다.
