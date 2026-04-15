#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="${CROSSWORD_LOG_DIR:-$ROOT/log}"
mkdir -p "$LOG_DIR"
# Gradle stdout/stderr (애플 로그는 logback 이 /home/ubuntu/crossword/log 에 기록)
LOG_BOARD="${BOARD_BOOT_LOG:-$LOG_DIR/gradle-board-boot.log}"
LOG_CW="${CROSSWORD_BOOT_LOG:-$LOG_DIR/gradle-crossword-boot.log}"

echo "🔄 게시판(8080) + 크로스워드(8081) — 종료 → 빌드 → 기동"

# Gradle 데몬이 Maven Central 접속 시 IPv6 문제가 나는 환경 대비
export GRADLE_OPTS="-Djava.net.preferIPv4Stack=true ${GRADLE_OPTS:-}"

if ! getent hosts services.gradle.org >/dev/null 2>&1; then
  echo "❌ DNS에서 services.gradle.org 를 찾을 수 없습니다. (Gradle 필요)"
  exit 1
fi

echo "📍 8080 / 8081 서비스 종료..."
pkill -f "java.*8080" 2>/dev/null || true
pkill -f "java.*8081" 2>/dev/null || true
fuser -k 8080/tcp 2>/dev/null || true
fuser -k 8081/tcp 2>/dev/null || true
sleep 3

echo "🔨 [1/2] java-projects (게시판) 빌드..."
cd "$ROOT/java-projects"
./gradlew build -x test

echo "🔨 [2/2] crossword-projects (크로스워드) 빌드..."
cd "$ROOT/crossword-projects"
./gradlew build -x test

echo "🚀 서비스 기동 (백그라운드)..."
cd "$ROOT/java-projects"
echo "   게시판(8080) 로그: $LOG_BOARD"
nohup ./gradlew bootRun --args='--server.port=8080' >>"$LOG_BOARD" 2>&1 &

cd "$ROOT/crossword-projects"
echo "   크로스워드(8081) 로그: $LOG_CW"
nohup ./gradlew bootRun --args='--server.port=8081' >>"$LOG_CW" 2>&1 &

echo "✅ 기동 요청을 보냈습니다."
echo "   게시판: http://k-crossword.com/  (또는 :8080 직접)"
echo "   크로스워드: https://k-crossword.com/K-CrossWord/"
echo "   애플리케이션 로그: $LOG_DIR/board-application.log , $LOG_DIR/crossword-application.log"
echo "   Gradle 콘솔 로그: tail -f $LOG_BOARD   /   tail -f $LOG_CW"

# Gradle 부트 로그 등 5일 지난 파일 정리 (logback 이 만든 일별 로그는 maxHistory=5 로 자체 삭제)
find "$LOG_DIR" -maxdepth 1 -type f -name 'gradle-*.log' -mtime +5 -delete 2>/dev/null || true
