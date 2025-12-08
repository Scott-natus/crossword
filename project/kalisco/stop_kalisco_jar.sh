#!/bin/bash

echo "🛑 캘리스코 서비스 종료 중 (JAR 파일 방식)..."

# 1. JAR 파일로 실행 중인 Java 프로세스 종료
echo "📍 JAR 프로세스 확인..."
PIDS=$(ps aux | grep "java.*kalisco.*jar" | grep -v grep | awk '{print $2}')
if [ ! -z "$PIDS" ]; then
    echo "🔍 발견된 프로세스: $PIDS"
    kill -9 $PIDS 2>/dev/null
    echo "✅ 프로세스 종료 완료"
    sleep 2
else
    echo "ℹ️  JAR 프로세스 없음"
fi

# 2. Gradle 프로세스도 확인 (혹시 모를 경우)
GRADLE_PIDS=$(ps aux | grep "gradle.*kalisco" | grep -v grep | awk '{print $2}')
if [ ! -z "$GRADLE_PIDS" ]; then
    echo "🔍 발견된 Gradle 프로세스: $GRADLE_PIDS"
    kill -9 $GRADLE_PIDS 2>/dev/null
    echo "✅ Gradle 프로세스 종료 완료"
    sleep 2
fi

# 3. 포트 사용 확인
echo "📍 9091 포트 사용 상태 확인..."
PORT_CHECK=$(netstat -tlnp | grep 9091)
if [ ! -z "$PORT_CHECK" ]; then
    echo "⚠️  9091 포트가 여전히 사용 중입니다:"
    echo "$PORT_CHECK"
    echo "🔧 강제 종료 시도..."
    fuser -k 9091/tcp 2>/dev/null
    sleep 2
else
    echo "✅ 9091 포트 사용 없음"
fi

# 4. 최종 확인
echo "📍 최종 포트 상태 확인..."
FINAL_CHECK=$(netstat -tlnp | grep 9091)
if [ -z "$FINAL_CHECK" ]; then
    echo "✅ 캘리스코 서비스 종료 완료 - 9091 포트 사용 없음"
else
    echo "❌ 9091 포트가 여전히 사용 중입니다:"
    echo "$FINAL_CHECK"
fi

echo "🏁 종료 작업 완료"
