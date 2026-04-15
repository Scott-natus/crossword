"""
engine0_trends 테이블 요약을 stdout에 출력 (일일 리포트용).

  python report_engine0_summary.py
"""

from __future__ import annotations

import sys
from datetime import datetime, timezone

from db_env import load_harness_env
from ingest_to_postgres import _connect


def main() -> None:
    load_harness_env()
    try:
        conn = _connect()
    except Exception as e:
        print(f"[!] DB 연결 실패: {e}", file=sys.stderr)
        raise SystemExit(1) from e

    cur = conn.cursor()
    print(f"=== Engine-0 DB 요약 ({datetime.now(timezone.utc).isoformat()} UTC 기준) ===")

    cur.execute(
        """
        SELECT platform, COUNT(*)::bigint,
               MAX(collected_at), MIN(collected_at)
        FROM engine0_trends
        GROUP BY platform
        ORDER BY platform
        """
    )
    rows = cur.fetchall()
    total = 0
    for platform, cnt, mx, mn in rows:
        total += cnt
        print(
            f"  platform={platform!r}  rows={cnt}  "
            f"collected_at min={mn} max={mx}"
        )
    print(f"  합계 행 수: {total}")

    cur.execute(
        """
        SELECT COUNT(*) FROM engine0_trends
        WHERE collected_at >= NOW() - INTERVAL '24 hours'
        """
    )
    (last24,) = cur.fetchone()
    print(f"  최근 24시간 내 수집/갱신( collected_at ): {last24} 건")

    cur.execute(
        """
        SELECT COUNT(*) FROM engine0_trends
        WHERE category IS NOT NULL AND trim(category) <> ''
        """
    )
    (enriched,) = cur.fetchone()
    print(f"  category 가 채워진 행: {enriched} 건")

    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
