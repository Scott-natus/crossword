"""
Engine-1 v1: engine0_trends → 테마 코드(K-POP, K-DRAMA, …) 규칙 기반 매핑.

  cd harness-factory/engine-1-architect
  ../engine-0-scanner/venv/bin/python map_trend_themes.py
  ../engine-0-scanner/venv/bin/python map_trend_themes.py --dry-run --limit 10
  ../engine-0-scanner/venv/bin/python map_trend_themes.py --force --limit 100
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

# engine-0-scanner 의 db_env, ingest
_ROOT = Path(__file__).resolve().parent.parent
_SCANNER = _ROOT / "engine-0-scanner"
if str(_SCANNER) not in sys.path:
    sys.path.insert(0, str(_SCANNER))

from db_env import load_harness_env  # noqa: E402
from ingest_to_postgres import _connect  # noqa: E402

# 문서 100 §8.1 과 동일한 테마 코드
THEMES = ("K-POP", "K-DRAMA", "K-MOVIE", "K-CULTURE", "Korean")

# 키워드 → 테마 (소문자·원문 혼합, 부분 일치)
KEYWORD_WEIGHTS: list[tuple[str, str, float]] = [
    # (theme, needle, weight)
    ("K-POP", "k-pop", 3.0),
    ("K-POP", "kpop", 3.0),
    ("K-POP", "아이돌", 2.5),
    ("K-POP", "가수", 2.0),
    ("K-POP", "노래", 2.0),
    ("K-POP", "음악", 1.5),
    ("K-POP", "mv", 1.5),
    ("K-POP", "comeback", 1.5),
    ("K-DRAMA", "드라마", 3.0),
    ("K-DRAMA", "drama", 2.5),
    ("K-DRAMA", "kdrama", 2.5),
    ("K-DRAMA", "ost", 1.5),
    ("K-DRAMA", "방영", 1.0),
    ("K-MOVIE", "영화", 3.0),
    ("K-MOVIE", "movie", 2.5),
    ("K-MOVIE", "film", 2.0),
    ("K-MOVIE", "감독", 2.0),
    ("K-MOVIE", "개봉", 1.5),
    ("K-CULTURE", "문화", 2.0),
    ("K-CULTURE", "전통", 2.0),
    ("K-CULTURE", "한복", 2.5),
    ("K-CULTURE", "김치", 1.5),
    ("K-CULTURE", "food", 1.0),
    ("K-CULTURE", "culture", 1.5),
]


def _normalize_text(title: str, summary: str, category: str) -> str:
    parts = [title or "", summary or "", category or ""]
    return " ".join(parts).lower()


def _score_themes(blob: str) -> tuple[str, float, str]:
    scores: dict[str, float] = {t: 0.0 for t in THEMES if t != "Korean"}
    reasons: list[str] = []

    for theme, needle, w in KEYWORD_WEIGHTS:
        if needle.lower() in blob:
            scores[theme] = scores.get(theme, 0.0) + w
            reasons.append(f"{needle}:{theme}")

    # 영문 단어 경계(간단): drama vs other
    if re.search(r"\bdrama\b", blob):
        scores["K-DRAMA"] = scores.get("K-DRAMA", 0.0) + 1.0

    best_theme = max(scores, key=scores.get) if scores else "Korean"
    best_score = scores.get(best_theme, 0.0)

    if best_score <= 0:
        return "Korean", 0.35, "default:no_keyword_hit"

    # confidence: 0.4 ~ 0.95 구간으로 스케일 (최대 가중 합 대략 10 가정)
    conf = min(0.95, 0.4 + min(best_score / 10.0, 0.55))
    reason = "keyword:" + "|".join(reasons[:8]) if reasons else "keyword"
    return best_theme, conf, reason


def main() -> None:
    parser = argparse.ArgumentParser(description="Engine-1: 트렌드 → 테마 매핑")
    parser.add_argument("--limit", type=int, default=500, help="처리 행 상한")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="DB에 쓰지 않고 결과만 출력",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="이미 매핑된 trend_id도 다시 계산",
    )
    args = parser.parse_args()

    load_harness_env()

    conn = _connect()
    cur = conn.cursor()

    if args.force:
        cur.execute(
            """
            SELECT t.id, t.title, t.summary_features, t.category, t.hooking_score
            FROM engine0_trends t
            ORDER BY t.id
            LIMIT %s
            """,
            (args.limit,),
        )
    else:
        cur.execute(
            """
            SELECT t.id, t.title, t.summary_features, t.category, t.hooking_score
            FROM engine0_trends t
            WHERE NOT EXISTS (
                SELECT 1 FROM engine1_trend_theme_map m WHERE m.engine0_trend_id = t.id
            )
            ORDER BY t.id
            LIMIT %s
            """,
            (args.limit,),
        )

    rows = cur.fetchall()
    if not rows:
        print("[*] 매핑할 행이 없습니다. (--force 또는 engine0_trends 적재 확인)")
        cur.close()
        conn.close()
        return

    upsert = """
    INSERT INTO engine1_trend_theme_map (engine0_trend_id, theme_code, confidence, match_reason)
    VALUES (%s, %s, %s, %s)
    ON CONFLICT (engine0_trend_id) DO UPDATE SET
        theme_code = EXCLUDED.theme_code,
        confidence = EXCLUDED.confidence,
        match_reason = EXCLUDED.match_reason,
        created_at = CURRENT_TIMESTAMP
    """

    n = 0
    for tid, title, summary, category, _hook in rows:
        blob = _normalize_text(title or "", summary or "", category or "")
        if not blob.strip():
            theme, conf, reason = "Korean", 0.25, "skip:empty_text"
        else:
            theme, conf, reason = _score_themes(blob)

        if args.dry_run:
            print(f"[dry-run] id={tid} theme={theme} conf={conf:.2f} reason={reason[:80]}")
        else:
            cur.execute(upsert, (tid, theme, conf, reason[:2000]))
            n += 1
            print(f"[+] id={tid} → {theme} ({conf:.2f})")

    if not args.dry_run:
        conn.commit()
        print(f"[*] 완료: {n}건 UPSERT")
    else:
        print(f"[*] dry-run: {len(rows)}건")

    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
