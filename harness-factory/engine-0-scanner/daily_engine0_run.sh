#!/usr/bin/env bash
#
# Engine-0/1.5 일일 파이프라인:
# YouTube 스캔 → Reels 스캔 → PostgreSQL 적재 → (선택) Gemini 보강
# → (선택) Engine-1.5 시나리오 생성 → 요약 리포트
#
# 사용:
#   chmod +x daily_engine0_run.sh
#   ./daily_engine0_run.sh
#
# 환경 변수 (선택):
#   HARNESS_LOG_DIR       로그 디렉터리 (기본: harness-factory/logs)
#   HARNESS_MAX_SHORTS    scanner.py --max (기본 100)
#   HARNESS_MAX_REELS     reels_scanner.py --max (기본 50)
#   HARNESS_REELS_EXPLORE 1이면 탐색(돋보기) 피드 /explore/ (인기·추천에 가까운 큐레이션, 기본 1). 0이면 해시태그(#reels) 모드
#   HARNESS_ENRICH        1이면 enrich_gemini 실행 (기본 1)
#   HARNESS_ENRICH_LIMIT  enrich_gemini --limit (기본 30)
#   HARNESS_ENGINE15      1이면 Engine-1.5 generate_scenarios 실행 (기본 1)
#   HARNESS_ENGINE15_LIMIT Engine-1.5 생성 건수 상한 (기본 30)
#   HARNESS_DOTENV_PATH   crossword/.env 경로
#

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

VENV_PY="${SCRIPT_DIR}/venv/bin/python"
if [[ ! -x "$VENV_PY" ]]; then
  echo "[!] venv 없음: $VENV_PY" >&2
  exit 1
fi

LOG_DIR="${HARNESS_LOG_DIR:-${SCRIPT_DIR}/../logs}"
mkdir -p "$LOG_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
LOG_FILE="${LOG_DIR}/engine0-run-${STAMP}.log"
REPORT_FILE="${LOG_DIR}/engine0-report-${STAMP}.txt"

MAX_SHORTS="${HARNESS_MAX_SHORTS:-100}"
MAX_REELS="${HARNESS_MAX_REELS:-50}"
HARNESS_REELS_EXPLORE="${HARNESS_REELS_EXPLORE:-1}"
ENRICH="${HARNESS_ENRICH:-1}"
ENRICH_LIMIT="${HARNESS_ENRICH_LIMIT:-30}"
ENGINE15="${HARNESS_ENGINE15:-1}"
ENGINE15_LIMIT="${HARNESS_ENGINE15_LIMIT:-30}"
ENGINE15_SCRIPT="${SCRIPT_DIR}/../engine-1.5-scenarist/generate_scenarios.py"

{
  echo "========================================"
  echo "[*] Engine-0 daily 시작: $(date -Iseconds)"
  echo "    LOG_DIR=$LOG_DIR"
  echo "========================================"

  echo ""
  echo "[1/5] YouTube scanner (max=${MAX_SHORTS})"
  if "$VENV_PY" scanner.py --max "$MAX_SHORTS" --no-ui-filters; then
    echo "[1/5] OK"
  else
    echo "[1/5] 실패 (계속 진행)"
  fi

  echo ""
  echo "[2/5] Instagram reels_scanner (max=${MAX_REELS}, explore=${HARNESS_REELS_EXPLORE})"
  REELS_ARGS=(--max "$MAX_REELS")
  if [[ "$HARNESS_REELS_EXPLORE" == "1" ]]; then
    REELS_ARGS+=(--explore)
  fi
  if "$VENV_PY" reels_scanner.py "${REELS_ARGS[@]}"; then
    echo "[2/5] OK"
  else
    echo "[2/5] 실패 (계속 진행)"
  fi

  echo ""
  echo "[3/5] ingest_to_postgres --auto"
  if "$VENV_PY" ingest_to_postgres.py --auto; then
    echo "[3/5] OK"
  else
    echo "[3/5] 실패"
  fi

  echo ""
  if [[ "$ENRICH" == "1" ]]; then
    echo "[4/5] enrich_gemini (limit=${ENRICH_LIMIT})"
    if "$VENV_PY" enrich_gemini.py --limit "$ENRICH_LIMIT"; then
      echo "[4/5] OK"
    else
      echo "[4/5] 실패"
    fi
  else
    echo "[4/5] enrich_gemini 생략 (HARNESS_ENRICH!=1)"
  fi

  echo ""
  if [[ "$ENGINE15" == "1" ]]; then
    if [[ -f "$ENGINE15_SCRIPT" ]]; then
      echo "[5/5] engine-1.5 generate_scenarios (limit=${ENGINE15_LIMIT})"
      if "$VENV_PY" "$ENGINE15_SCRIPT" --limit "$ENGINE15_LIMIT"; then
        echo "[5/5] OK"
      else
        echo "[5/5] 실패"
      fi
    else
      echo "[5/5] engine-1.5 스크립트 없음: $ENGINE15_SCRIPT (생략)"
    fi
  else
    echo "[5/5] engine-1.5 생략 (HARNESS_ENGINE15!=1)"
  fi

  echo ""
  echo "[*] DB 요약 → $REPORT_FILE"
  "$VENV_PY" report_engine0_summary.py | tee "$REPORT_FILE"

  echo ""
  echo "[*] 종료: $(date -Iseconds)"
} 2>&1 | tee "$LOG_FILE"

echo ""
echo "[*] 전체 로그: $LOG_FILE"
echo "[*] 요약 리포트: $REPORT_FILE"
