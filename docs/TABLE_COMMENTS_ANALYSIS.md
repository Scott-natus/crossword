# 테이블 코멘트 분석 및 추출 (2025-10-31)

## 현재 데이터베이스 상태

### 코멘트가 있는 테이블
- `pz_hints`: 퍼즐 힌트 테이블 (인코딩 문제로 깨져 보임)

### 코멘트가 없는 테이블 (docs/DATABASE_SCHEMA.md 기준)

#### 퍼즐 관련 테이블
1. **puzzle_words** (또는 pz_words)
   - **설명**: 퍼즐 단어 테이블
   - **용도**: 십자낱말 퀴즈에 사용되는 단어 저장

2. **puzzle_levels**
   - **설명**: 퍼즐 레벨 테이블
   - **용도**: 레벨별 난이도, 단어 수, 교차점 수, 시간 제한 등 설정

3. **puzzle_grid_templates**
   - **설명**: 퍼즐 그리드 템플릿 테이블
   - **용도**: 레벨별 그리드 패턴 및 단어 위치 정보 저장

#### 크로스워드 관련 테이블
4. **crossword_puzzles**
   - **설명**: 크로스워드 퍼즐 테이블
   - **용도**: 생성된 크로스워드 퍼즐 정보 저장

5. **crossword_words**
   - **설명**: 크로스워드 단어 테이블
   - **용도**: 퍼즐에 포함된 단어와 위치 정보 저장

6. **crossword_grid**
   - **설명**: 크로스워드 그리드 테이블
   - **용도**: 그리드의 각 셀 정보 저장

#### 게시판 관련 테이블
7. **boards**
   - **설명**: 게시판 테이블
   - **용도**: 게시글 저장

8. **board_types**
   - **설명**: 게시판 타입 테이블
   - **용도**: 게시판 유형 관리 (bbs1, talk, proj 등)

9. **board_comments**
   - **설명**: 게시판 댓글 테이블
   - **용도**: 게시글 댓글 저장

10. **board_attachments**
    - **설명**: 게시판 첨부파일 테이블
    - **용도**: 게시글 첨부파일 저장

#### 사용자 관련 테이블
11. **users**
    - **설명**: 사용자 테이블
    - **용도**: 시스템 사용자 정보 저장

12. **user_puzzle_games**
    - **설명**: 사용자 퍼즐 게임 테이블
    - **용도**: 사용자의 퍼즐 게임 진행 상황 저장

13. **user_puzzle_completions**
    - **설명**: 사용자 퍼즐 완료 기록 테이블
    - **용도**: 퍼즐 완료 기록 저장

14. **user_puzzle_achievements**
    - **설명**: 사용자 퍼즐 업적 테이블
    - **용도**: 사용자 업적 정보 저장

15. **user_puzzle_profiles**
    - **설명**: 사용자 퍼즐 프로필 테이블
    - **용도**: 사용자 프로필 정보 저장

16. **user_wrong_answers**
    - **설명**: 사용자 오답 기록 테이블
    - **용도**: 사용자의 오답 기록 저장

#### 게임 관련 테이블
17. **puzzle_game_records**
    - **설명**: 퍼즐 게임 기록 테이블
    - **용도**: 게임 플레이 기록 및 통계 저장

18. **game_sessions**
    - **설명**: 게임 세션 테이블
    - **용도**: 게임 세션 정보 저장

19. **game_play_records**
    - **설명**: 게임 플레이 기록 테이블
    - **용도**: 게임 플레이 상세 기록 저장

#### 힌트 관련 테이블
20. **hint_usage_records**
    - **설명**: 힌트 사용 기록 테이블
    - **용도**: 힌트 사용 이력 저장

#### 템플릿 관련 테이블
21. **puzzle_grid_template_word**
    - **설명**: 퍼즐 그리드 템플릿 단어 관계 테이블
    - **용도**: 템플릿과 단어의 관계 저장 (pivot 테이블)

#### 일일 퍼즐 테이블
22. **theme_daily_puzzles**
    - **설명**: 테마별 일일 퍼즐 테이블
    - **용도**: 테마별 일일 퍼즐 관리

#### 임시 테이블
23. **tmp_for_make_words**
    - **설명**: 단어 생성 임시 테이블
    - **용도**: 재미나이 API 기반 단어 생성용 임시 테이블

24. **tmp_pz_word_difficulty**
    - **설명**: 단어 난이도 업데이트 임시 테이블
    - **용도**: 단어 난이도 일괄 업데이트용 임시 테이블

#### 로그/기록 테이블
25. **failed_word_extractions**
    - **설명**: 단어 추출 실패 로그 테이블
    - **용도**: 템플릿 로드 시 단어 추출 실패 정보 저장

26. **failed_jobs**
    - **설명**: 실패한 작업 로그 테이블
    - **용도**: 배치 작업 실패 기록 저장

#### 뉴스레터/알림 테이블
27. **newsletter_subscriptions**
    - **설명**: 뉴스레터 구독 테이블
    - **용도**: 뉴스레터 구독 정보 저장

28. **notification_settings**
    - **설명**: 알림 설정 테이블
    - **용도**: 사용자 알림 설정 저장

29. **mobile_push_settings**
    - **설명**: 모바일 푸시 설정 테이블
    - **용도**: 모바일 푸시 알림 설정 저장

30. **mobile_push_tokens**
    - **설명**: 모바일 푸시 토큰 테이블
    - **용도**: 모바일 기기 푸시 토큰 저장

#### 인증/세션 테이블
31. **personal_access_tokens**
    - **설명**: 개인 액세스 토큰 테이블
    - **용도**: API 인증 토큰 저장

32. **password_resets**
    - **설명**: 비밀번호 재설정 테이블
    - **용도**: 비밀번호 재설정 토큰 저장

33. **password_reset_tokens**
    - **설명**: 비밀번호 재설정 토큰 테이블
    - **용도**: 비밀번호 재설정 토큰 저장

#### Django 관련 테이블 (다른 프로젝트)
- auth_group, auth_group_permissions, auth_permission, auth_user, auth_user_groups, auth_user_user_permissions
- django_admin_log, django_content_type, django_migrations, django_session

#### 기타 프로젝트 테이블
- apartments_info, consult_requests, consultations, ddongsun_rankings
- faqs, gallery_images, lotto_results, lotto_tickets
- main_*, number_statistics, pricing_info, posts
- reservations, unit_types

## 권장 SQL 코멘트 추가 쿼리

```sql
-- 퍼즐 관련 테이블
COMMENT ON TABLE pz_words IS '퍼즐 단어 테이블 - 십자낱말 퀴즈에 사용되는 단어 저장';
COMMENT ON TABLE puzzle_levels IS '퍼즐 레벨 테이블 - 레벨별 난이도, 단어 수, 교차점 수, 시간 제한 등 설정';
COMMENT ON TABLE puzzle_grid_templates IS '퍼즐 그리드 템플릿 테이블 - 레벨별 그리드 패턴 및 단어 위치 정보 저장';

-- 크로스워드 관련 테이블
COMMENT ON TABLE crossword_puzzles IS '크로스워드 퍼즐 테이블 - 생성된 크로스워드 퍼즐 정보 저장';
COMMENT ON TABLE crossword_words IS '크로스워드 단어 테이블 - 퍼즐에 포함된 단어와 위치 정보 저장';
COMMENT ON TABLE crossword_grid IS '크로스워드 그리드 테이블 - 그리드의 각 셀 정보 저장';

-- 게시판 관련 테이블
COMMENT ON TABLE boards IS '게시판 테이블 - 게시글 저장';
COMMENT ON TABLE board_types IS '게시판 타입 테이블 - 게시판 유형 관리 (bbs1, talk, proj 등)';
COMMENT ON TABLE board_comments IS '게시판 댓글 테이블 - 게시글 댓글 저장';
COMMENT ON TABLE board_attachments IS '게시판 첨부파일 테이블 - 게시글 첨부파일 저장';

-- 사용자 관련 테이블
COMMENT ON TABLE users IS '사용자 테이블 - 시스템 사용자 정보 저장';
COMMENT ON TABLE user_puzzle_games IS '사용자 퍼즐 게임 테이블 - 사용자의 퍼즐 게임 진행 상황 저장';
COMMENT ON TABLE user_puzzle_completions IS '사용자 퍼즐 완료 기록 테이블 - 퍼즐 완료 기록 저장';
COMMENT ON TABLE user_wrong_answers IS '사용자 오답 기록 테이블 - 사용자의 오답 기록 저장';

-- 게임 관련 테이블
COMMENT ON TABLE puzzle_game_records IS '퍼즐 게임 기록 테이블 - 게임 플레이 기록 및 통계 저장';
COMMENT ON TABLE game_sessions IS '게임 세션 테이블 - 게임 세션 정보 저장';

-- 힌트 관련 테이블
COMMENT ON TABLE pz_hints IS '퍼즐 힌트 테이블 - 퍼즐 단어에 대한 힌트 저장';

-- 템플릿 관련 테이블
COMMENT ON TABLE puzzle_grid_template_word IS '퍼즐 그리드 템플릿 단어 관계 테이블 - 템플릿과 단어의 관계 저장 (pivot 테이블)';

-- 일일 퍼즐 테이블
COMMENT ON TABLE theme_daily_puzzles IS '테마별 일일 퍼즐 테이블 - 테마별 일일 퍼즐 관리';

-- 임시 테이블
COMMENT ON TABLE tmp_for_make_words IS '단어 생성 임시 테이블 - 재미나이 API 기반 단어 생성용 임시 테이블';
COMMENT ON TABLE tmp_pz_word_difficulty IS '단어 난이도 업데이트 임시 테이블 - 단어 난이도 일괄 업데이트용 임시 테이블';

-- 로그/기록 테이블
COMMENT ON TABLE failed_word_extractions IS '단어 추출 실패 로그 테이블 - 템플릿 로드 시 단어 추출 실패 정보 저장';

-- 뉴스레터/알림 테이블
COMMENT ON TABLE newsletter_subscriptions IS '뉴스레터 구독 테이블 - 뉴스레터 구독 정보 저장';
COMMENT ON TABLE notification_settings IS '알림 설정 테이블 - 사용자 알림 설정 저장';
COMMENT ON TABLE mobile_push_settings IS '모바일 푸시 설정 테이블 - 모바일 푸시 알림 설정 저장';
COMMENT ON TABLE mobile_push_tokens IS '모바일 푸시 토큰 테이블 - 모바일 기기 푸시 토큰 저장';
```

