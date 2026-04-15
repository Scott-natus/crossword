"""
engine0_shorts.json / engine0_reels.json → PostgreSQL engine0_trends 적재 (UPSERT).

환경 변수:
  기본은 crossword/.env 를 로드하고, Spring 과 동일한 NATUS_DB_* 로 PG 접속을 맞춥니다.
  (HARNESS_DOTENV_PATH 로 .env 경로 변경 가능)
  직접 지정 시: DATABASE_URL 또는 PGHOST·PGPORT·PGUSER·PGPASSWORD·PGDATABASE

사용:
  pip install -r requirements-db.txt
  psql ... -f sql/001_engine0_trends.sql   # 접속 정보는 .env 의 NATUS_* 또는 DATABASE_URL
  python ingest_to_postgres.py engine0_shorts.json engine0_reels.json
  python ingest_to_postgres.py --auto
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

from db_env import load_harness_env


def _connect():
    try:
        import psycopg2
    except ImportError as e:
        print("[!] psycopg2 필요: pip install -r requirements-db.txt", file=sys.stderr)
        raise SystemExit(1) from e

    url = os.environ.get("DATABASE_URL")
    if url:
        return psycopg2.connect(url)
    host = os.environ.get("PGHOST", "localhost")
    port = int(os.environ.get("PGPORT", "5432"))
    user = os.environ.get("PGUSER", "postgres")
    password = os.environ.get("PGPASSWORD", "")
    dbname = os.environ.get("PGDATABASE", "postgres")
    return psycopg2.connect(
        host=host, port=port, user=user, password=password, dbname=dbname
    )


def _platform_for_item(item: dict, url: str, file_hint: str) -> str:
    p = (item.get("platform") or "").strip().lower()
    if p in ("instagram", "youtube"):
        return p
    if "instagram.com" in url:
        return "instagram"
    if "youtube.com" in url or "youtu.be" in url:
        return "youtube"
    if "reel" in file_hint.lower():
        return "instagram"
    return "youtube"


def _load_items(path: Path) -> tuple[list[dict], dict]:
    with open(path, encoding="utf-8") as f:
        root = json.load(f)
    items = root.get("items") or []
    return items, root


def ingest_file(cur, path: Path) -> tuple[int, int]:
    items, _ = _load_items(path)
    hint = path.name
    upsert_count = 0
    insert_sql = """
    INSERT INTO engine0_trends (
        platform, title, channel, view_count, url,
        category, summary_features, hooking_score
    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
    ON CONFLICT (url) DO UPDATE SET
        platform = EXCLUDED.platform,
        title = COALESCE(NULLIF(EXCLUDED.title, ''), engine0_trends.title),
        channel = COALESCE(NULLIF(EXCLUDED.channel, ''), engine0_trends.channel),
        collected_at = CURRENT_TIMESTAMP
    """
    for it in items:
        url = (it.get("url") or "").strip()
        if not url:
            continue
        title = it.get("title") or ""
        channel = it.get("channel") or ""
        platform = _platform_for_item(it, url, hint)
        view_count = it.get("view_count")
        if view_count is not None:
            try:
                view_count = int(view_count)
            except (TypeError, ValueError):
                view_count = None
        cur.execute(
            insert_sql,
            (
                platform,
                title or None,
                channel or None,
                view_count,
                url,
                it.get("category"),
                it.get("summary_features"),
                it.get("hooking_score"),
            ),
        )
        upsert_count += 1
    return upsert_count, len(items)


def main() -> None:
    parser = argparse.ArgumentParser(description="Engine-0 JSON → PostgreSQL engine0_trends")
    parser.add_argument(
        "json_files",
        nargs="*",
        help="engine0_shorts.json 등 경로",
    )
    parser.add_argument(
        "--auto",
        action="store_true",
        help="기본 engine0_shorts.json, engine0_reels.json (있으면)",
    )
    args = parser.parse_args()

    load_harness_env()

    base = Path(__file__).resolve().parent
    paths: list[Path] = []
    if args.auto:
        for name in ("engine0_shorts.json", "engine0_reels.json"):
            p = base / name
            if p.is_file():
                paths.append(p)
        if not paths:
            print("[!] --auto: 기본 JSON 파일이 없습니다.", file=sys.stderr)
            raise SystemExit(1)
    else:
        if not args.json_files:
            print("[!] JSON 파일을 지정하거나 --auto", file=sys.stderr)
            raise SystemExit(1)
        paths = [Path(p).resolve() for p in args.json_files]

    conn = _connect()
    conn.autocommit = False
    total_rows = 0
    try:
        with conn.cursor() as cur:
            for p in paths:
                if not p.is_file():
                    print(f"[!] 건너뜀 (없음): {p}")
                    continue
                n, raw_len = ingest_file(cur, p)
                total_rows += n
                print(f"[+] {p.name}: 항목 {raw_len}개 처리 (UPSERT 실행 {n}회)")
        conn.commit()
        print(f"[*] 완료. 총 UPSERT {total_rows}건 (파일 {len(paths)}개)")
    except Exception as e:
        conn.rollback()
        print(f"[!] DB 오류: {e}", file=sys.stderr)
        raise SystemExit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
