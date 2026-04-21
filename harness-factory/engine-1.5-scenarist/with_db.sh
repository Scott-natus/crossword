#!/usr/bin/env bash
# crossword/.env 의 NATUS_DB_* 로 psql DATABASE_URL 구성 후 실행
set -euo pipefail

ENV_FILE="${HARNESS_DOTENV_PATH:-/home/ubuntu/crossword/.env}"
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

export DATABASE_URL="postgresql://${NATUS_DB_USER}:${NATUS_DB_PASSWORD}@${NATUS_DB_HOST}:${NATUS_DB_PORT}/${NATUS_DB_NAME}"
exec psql "$DATABASE_URL" "$@"
