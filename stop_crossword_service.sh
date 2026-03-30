#!/bin/bash

echo "🛑 크로스워드 서비스 종료 중..."

# 1. 8081 포트를 사용하는 모든 Java 프로세스 종료
echo "📍 8081 포트 사용 프로세스 확인..."
PIDS=$(ps aux | grep "java.*8081" | grep -v grep | awk '{print $2}')
if [ ! -z "$PIDS" ]; then
    echo "🔍 발견된 프로세스: $PIDS"
    kill -9 $PIDS
    echo "✅ Java 프로세스 종료 완료"
else
    echo "ℹ️ 8081 포트 사용 프로세스 없음"
fi

# 2. Gradle 데몬 프로세스 종료 (8081 관련)
echo "📍 Gradle 데몬 프로세스 확인..."
GRADLE_PIDS=$(ps aux | grep "gradle.*8081" | grep -v grep | awk '{print $2}')
if [ ! -z "$GRADLE_PIDS" ]; then
    echo "🔍 발견된 Gradle 프로세스: $GRADLE_PIDS"
    kill -9 $GRADLE_PIDS
    echo "✅ Gradle 프로세스 종료 완료"
else
    echo "ℹ️ Gradle 8081 관련 프로세스 없음"
fi

# 3. 포트 사용 확인
echo "📍 8081 포트 사용 상태 확인..."
PORT_CHECK=$(netstat -tlnp | grep 8081)
if [ ! -z "$PORT_CHECK" ]; then
    echo "⚠️ 8081 포트가 여전히 사용 중입니다:"
    echo "$PORT_CHECK"
    echo "🔧 강제 종료 시도..."
    # 포트를 사용하는 프로세스 강제 종료
    fuser -k 8081/tcp 2>/dev/null
    sleep 2
else
    echo "✅ 8081 포트 사용 없음"
fi

# 4. 최종 확인
echo "📍 최종 포트 상태 확인..."
FINAL_CHECK=$(netstat -tlnp | grep 8081)
if [ -z "$FINAL_CHECK" ]; then
    echo "✅ 크로스워드 서비스 종료 완료 - 8081 포트 사용 없음"
else
    echo "❌ 8081 포트가 여전히 사용 중입니다:"
    echo "$FINAL_CHECK"
fi

echo "🏁 종료 작업 완료"
