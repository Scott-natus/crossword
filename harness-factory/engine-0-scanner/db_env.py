"""
crossword/.env 로드 후 PostgreSQL 접속 정보 정리.

우선순위:
  1) DATABASE_URL (이미 있으면 그대로 사용)
  2) PGHOST, PGPORT, PGUSER, PGDATABASE, PGPASSWORD
  3) Spring 과 동일한 NATUS_DB_* (.env에 정의) → PG* 로 보완

환경 변수 HARNESS_DOTENV_PATH 로 .env 경로 변경 가능 (기본: /home/ubuntu/crossword/.env)
"""

from __future__ import annotations

import os
from pathlib import Path

DEFAULT_DOTENV_PATH = "/home/ubuntu/crossword/.env"


def load_harness_env() -> None:
    """.env 로드 + DB 관련 변수 보완."""
    try:
        from dotenv import load_dotenv
    except ImportError:
        return

    path = Path(os.environ.get("HARNESS_DOTENV_PATH", DEFAULT_DOTENV_PATH))
    if path.is_file():
        load_dotenv(path, override=True)
        print(f"[*] .env 로드: {path}")
    else:
        print(f"[!] .env 없음: {path} (HARNESS_DOTENV_PATH 확인)")

    _apply_natus_to_postgres_env()


def _apply_natus_to_postgres_env() -> None:
    """DATABASE_URL 이 없을 때 NATUS_DB_* 또는 기본값으로 PG* 채움."""
    if (os.environ.get("DATABASE_URL") or "").strip():
        return

    mapping = [
        ("PGHOST", "NATUS_DB_HOST", "127.0.0.1"),
        ("PGPORT", "NATUS_DB_PORT", "5432"),
        ("PGUSER", "NATUS_DB_USER", "natus_user"),
        ("PGDATABASE", "NATUS_DB_NAME", "natus_db"),
    ]
    for pg_key, natus_key, default in mapping:
        if not (os.environ.get(pg_key) or "").strip():
            val = (os.environ.get(natus_key) or "").strip() or default
            os.environ[pg_key] = val

    # 셸에 남아 있는 예전 PGPASSWORD 때문에 인증 실패하는 경우 방지 — .env 의 NATUS 가 우선
    pw = (os.environ.get("NATUS_DB_PASSWORD") or "").strip()
    if pw:
        os.environ["PGPASSWORD"] = pw
