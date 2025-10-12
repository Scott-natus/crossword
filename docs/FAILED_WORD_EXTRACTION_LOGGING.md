# 실패한 단어 추출 로깅 시스템

## 개요
템플릿 로드 실패 시 어떤 단어들을 추출하다 실패했는지 상세한 정보를 데이터베이스에 저장하는 시스템입니다.

## 구현 일자
2025-10-12

## 구현 목적
- 퍼즐 생성 시 단어 추출 실패 원인 분석
- 실패한 단어들의 패턴 파악
- 향후 단어 데이터베이스 개선을 위한 기초 자료 수집

## 구현된 기능

### 1. 데이터베이스 테이블
```sql
CREATE TABLE failed_word_extractions (
    id SERIAL PRIMARY KEY,
    template_id INTEGER,
    level INTEGER,
    word_difficulty INTEGER,
    hint_difficulty INTEGER,
    intersection_count INTEGER,
    failed_word_id INTEGER,
    failed_word_position JSONB,
    failure_reason TEXT,
    confirmed_words JSONB,
    intersection_requirements JSONB,
    retry_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2. JPA 엔티티 및 Repository
- **FailedWordExtraction**: 실패한 단어 추출 정보 엔티티
- **FailedWordExtractionRepository**: 다양한 조회 메서드 제공
- **FailedWordExtractionService**: 비즈니스 로직 처리

### 3. 실패 로깅 로직
**PuzzleGridTemplateService에서 다음 경우에 실패 정보 저장:**

#### 3.1 독립 단어 추출 실패
```java
if ("추출 실패".equals(extractedWord.get("word"))) {
    failedWordExtractionService.saveIndependentWordFailure(
        template.getId(), level.getLevel(), level.getWordDifficulty(),
        level.getHintDifficulty(), level.getIntersectionCount(),
        wordId, word, confirmedWords, retryCount);
}
```

#### 3.2 교차점 단어 추출 실패
```java
if (!(Boolean) extractedWord.get("success")) {
    String failureReason = String.format("교차점 단어 추출 실패 - 길이: %s, 난이도: %s, 교차점 개수: %d", 
        word.get("length"), level.getWordDifficulty(), intersections.size());
    
    failedWordExtractionService.saveIntersectionWordFailure(
        template.getId(), level.getLevel(), level.getWordDifficulty(),
        level.getHintDifficulty(), level.getIntersectionCount(),
        wordId, word, failureReason, confirmedWords,
        confirmedIntersectionSyllables, retryCount);
}
```

#### 3.3 5회 시도 후 최종 실패
```java
if (extractionFailed || extractedWords.size() != wordPositions.size()) {
    for (Map<String, Object> word : wordPositions) {
        Integer wordId = (Integer) word.get("id");
        if (!confirmedWords.containsKey(wordId)) {
            String failureReason = String.format("5회 시도 후 최종 실패 - 추출된 단어: %d/%d", 
                extractedWords.size(), wordPositions.size());
            
            failedWordExtractionService.saveFailedExtraction(
                template.getId(), level.getLevel(), level.getWordDifficulty(),
                level.getHintDifficulty(), level.getIntersectionCount(),
                wordId, word, failureReason, confirmedWords,
                null, maxRetries);
        }
    }
}
```

### 4. API 엔드포인트
```
GET /K-CrossWord/api/failed-extractions/recent?limit=5
GET /K-CrossWord/api/failed-extractions/level/{level}
GET /K-CrossWord/api/failed-extractions/template/{templateId}
GET /K-CrossWord/api/failed-extractions/date-range?startDate=2025-10-01T00:00:00&endDate=2025-10-31T23:59:59
GET /K-CrossWord/api/failed-extractions/reason?reason=독립
GET /K-CrossWord/api/failed-extractions/word/{wordId}
GET /K-CrossWord/api/failed-extractions/stats/summary
GET /K-CrossWord/api/failed-extractions/stats/level
GET /K-CrossWord/api/failed-extractions/stats/reason
GET /K-CrossWord/api/failed-extractions/stats/most-failed-words/{level}
```

## 저장되는 정보

### 기본 정보
- **template_id**: 템플릿 ID
- **level**: 퍼즐 레벨
- **word_difficulty**: 단어 난이도
- **hint_difficulty**: 힌트 난이도
- **intersection_count**: 교차점 개수

### 실패 정보
- **failed_word_id**: 실패한 단어 ID
- **failed_word_position**: 실패한 단어 위치 정보 (JSON)
- **failure_reason**: 실패 이유 (상세한 실패 원인)
- **retry_count**: 재시도 횟수

### 컨텍스트 정보
- **confirmed_words**: 확정된 단어들 (JSON)
- **intersection_requirements**: 교차점 요구사항 (JSON)
- **created_at**: 생성 시간

## 사용 예시

### 1. 최근 실패 기록 조회
```bash
curl -X GET "http://127.0.0.1:8081/K-CrossWord/api/failed-extractions/recent?limit=10"
```

### 2. 특정 레벨의 실패 기록 조회
```bash
curl -X GET "http://127.0.0.1:8081/K-CrossWord/api/failed-extractions/level/1"
```

### 3. 실패 통계 요약 조회
```bash
curl -X GET "http://127.0.0.1:8081/K-CrossWord/api/failed-extractions/stats/summary"
```

### 4. 특정 실패 이유로 검색
```bash
curl -X GET "http://127.0.0.1:8081/K-CrossWord/api/failed-extractions/reason?reason=교차점"
```

## 데이터 분석 활용

### 1. 실패 패턴 분석
- 레벨별 실패 빈도
- 실패 이유별 분포
- 특정 단어 ID의 반복 실패

### 2. 단어 데이터베이스 개선
- 자주 실패하는 단어 길이/난이도 패턴 파악
- 교차점 조건을 만족하지 못하는 단어들 식별
- 단어 데이터베이스 보완 방향 제시

### 3. 퍼즐 생성 알고리즘 개선
- 5회 재시도 로직의 효과성 분석
- 템플릿별 실패율 비교
- 최적화된 단어 선택 전략 개발

## 주의사항
- 실패 로그는 디버깅 및 분석 목적으로만 사용
- 개인정보는 포함되지 않음
- 대용량 데이터 축적 시 정기적인 정리 필요

## 향후 개선 계획
1. 실패 로그 시각화 대시보드
2. 자동화된 실패 패턴 분석
3. 실패 예측 모델 개발
4. 단어 데이터베이스 자동 보완 시스템
