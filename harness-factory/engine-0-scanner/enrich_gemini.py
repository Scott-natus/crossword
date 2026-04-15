"""
engine0_trends 행에 대해 Gemini로 category, summary_features, hooking_score 보강 (Phase 2).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
처음 쓰는 분께 (순서대로)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1) 터미널에서 engine-0-scanner 폴더로 이동합니다.
2) 한 번만: pip install -r requirements-ai.txt
3) crossword 프로젝트의 .env에 GEMINI_API_KEY=... 가 있어야 합니다.
   (기본 경로: /home/ubuntu/crossword/.env)
   다른 파일을 쓰려면 환경 변수 HARNESS_DOTENV_PATH 로 지정합니다.
4) DB 접속은 crossword/.env 의 NATUS_DB_HOST·PORT·USER·NAME·PASSWORD (Spring 과 동일) 를 씁니다.
5) 실행 예:
     python enrich_gemini.py --dry-run --limit 2   # DB 연결 없이 Gemini만 시험
     python enrich_gemini.py --limit 20           # DB에서 읽어 반영

`crossword/.env` 에 같은 이름의 변수가 있으면, 스크립트 실행 시 그 값이 셸에 export 된 값을 덮어씁니다(GEMINI_API_KEY 포함).
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

환경 변수:
  GEMINI_API_KEY   필수 (보통 crossword/.env 에서 로드)
  GEMINI_MODEL     선택 (기본: gemini-flash-latest — 구 API의 gemini-2.0-flash 는 신규 키에서 404)
  HARNESS_DOTENV_PATH  선택 (.env 파일 경로, 기본 /home/ubuntu/crossword/.env)
  NATUS_DB_* 또는 DATABASE_URL (db_env.py 참고)
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time

from db_env import DEFAULT_DOTENV_PATH, load_harness_env

# ingest_to_postgres와 동일한 연결 방식
from ingest_to_postgres import _connect  # noqa: E402


def _dry_run_sample_rows(limit: int) -> list[tuple]:
    """DB 없이 Gemini만 시험할 때 쓰는 예시 행 (id는 0)."""
    samples = [
        (
            0,
            "youtube",
            "예시: 인기 쇼츠 제목",
            "DemoChannel",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
        ),
        (
            0,
            "instagram",
            "예시 릴스 캡션",
            "demo_user",
            "https://www.instagram.com/reel/AbCdEfGhIjK/",
        ),
    ]
    return samples[: max(1, limit)]


def _parse_json(text: str) -> dict:
    text = text.strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        m = re.search(r"\{[\s\S]*\}", text)
        if m:
            return json.loads(m.group(0))
        raise


def _build_prompt(platform: str, channel: str, title: str, url: str) -> str:
    return f"""당신은 숏폼(YouTube Shorts, Instagram Reels) 트렌드 분석가입니다.
아래는 수집된 메타데이터뿐이며 영상 본문은 없습니다. 이 정보만으로 판단하세요.

- platform: {platform}
- channel: {channel or "(없음)"}
- title: {title or "(없음)"}
- url: {url}

다음 키만 가진 JSON 한 개만 출력하세요. 다른 설명 금지.
- category: 짧은 라벨 (영문 snake_case 또는 한글, 50자 이내)
- summary_features: 후킹 포인트·주제 요약 (한국어 1~2문장)
- hooking_score: 1~100 정수 (제목·채널명만으로 추정한 바이럴·후킹 잠재력)
"""


_DEFAULT_MODEL = "gemini-flash-latest"


def _enrich_row(client, model_name: str, prompt: str) -> dict:
    from google.genai import types

    try:
        resp = client.models.generate_content(
            model=model_name,
            contents=prompt,
            config=types.GenerateContentConfig(
                temperature=0.25,
                response_mime_type="application/json",
            ),
        )
    except Exception:
        resp = client.models.generate_content(
            model=model_name,
            contents=prompt,
            config=types.GenerateContentConfig(temperature=0.25),
        )
    text = (resp.text or "").strip()
    if not text:
        raise RuntimeError("빈 응답")
    return _parse_json(text)


def _clamp_category(s: str, max_len: int = 50) -> str:
    s = (s or "").strip()
    return s[:max_len] if len(s) > max_len else s


def _clamp_score(v) -> int | None:
    if v is None:
        return None
    try:
        n = int(round(float(v)))
    except (TypeError, ValueError):
        return None
    return max(1, min(100, n))


def main() -> None:
    parser = argparse.ArgumentParser(description="engine0_trends Gemini 메타데이터 보강")
    parser.add_argument("--limit", type=int, default=50, help="처리할 최대 행 수")
    parser.add_argument(
        "--sleep", type=float, default=0.35, help="API 호출 간 대기(초)"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="DB 업데이트 없이 프롬프트/응답만 시험",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="category/summary/score가 이미 있어도 다시 생성",
    )
    parser.add_argument(
        "--model",
        default=None,
        help=f"Gemini 모델 ID (기본: 환경변수 GEMINI_MODEL 또는 {_DEFAULT_MODEL})",
    )
    args = parser.parse_args()

    load_harness_env()
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print(
            "[!] GEMINI_API_KEY 가 없습니다. "
            f"{DEFAULT_DOTENV_PATH} 에 GEMINI_API_KEY=... 를 넣거나, export 로 설정하세요.",
            file=sys.stderr,
        )
        raise SystemExit(1)

    model_name = args.model or os.environ.get("GEMINI_MODEL", _DEFAULT_MODEL)

    from google import genai

    client = genai.Client(api_key=api_key)
    print(f"[*] Gemini 모델: {model_name}")

    conn = None
    cur = None

    if args.dry_run:
        rows = _dry_run_sample_rows(args.limit)
        print("[*] dry-run: DB 연결 없음 — 예시 메타데이터로 Gemini만 호출합니다.")
    else:
        conn = _connect()
        conn.autocommit = False
        cur = conn.cursor()

        if args.force:
            cur.execute(
                """
                SELECT id, platform, title, channel, url
                FROM engine0_trends
                ORDER BY id
                LIMIT %s
                """,
                (args.limit,),
            )
        else:
            cur.execute(
                """
                SELECT id, platform, title, channel, url
                FROM engine0_trends
                WHERE (
                    category IS NULL OR trim(category) = ''
                    OR summary_features IS NULL OR trim(summary_features) = ''
                    OR hooking_score IS NULL
                )
                ORDER BY id
                LIMIT %s
                """,
                (args.limit,),
            )

        rows = cur.fetchall()
        if not rows:
            print("[*] 보강할 행이 없습니다. (--force 로 전체 재시도 가능)")
            cur.close()
            conn.close()
            return

    update_sql = """
    UPDATE engine0_trends
    SET category = %s,
        summary_features = %s,
        hooking_score = %s
    WHERE id = %s
    """

    ok = 0
    for row_id, platform, title, channel, url in rows:
        prompt = _build_prompt(platform or "", channel or "", title or "", url or "")
        try:
            data = _enrich_row(client, model_name, prompt)
            cat = _clamp_category(str(data.get("category", "")))
            summ = (str(data.get("summary_features", "")).strip()) or None
            score = _clamp_score(data.get("hooking_score"))
            if args.dry_run:
                print(f"[dry-run] id={row_id} -> {data!r}")
            else:
                cur.execute(update_sql, (cat, summ, score, row_id))
                ok += 1
                print(f"[+] id={row_id} category={cat!r} score={score}")
        except Exception as e:
            print(f"[!] id={row_id} 실패: {e}", file=sys.stderr)
        time.sleep(args.sleep)

    if not args.dry_run:
        conn.commit()
        print(f"[*] 완료: {ok}/{len(rows)}건 업데이트")
    else:
        print(f"[*] dry-run: {len(rows)}건 시뮬레이션")

    if cur is not None:
        cur.close()
    if conn is not None:
        conn.close()


if __name__ == "__main__":
    main()
