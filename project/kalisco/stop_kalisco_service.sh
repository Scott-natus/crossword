#!/bin/bash

echo "🛑 캘리스코 서비스 종료 중..."

# 1. 9091 포트를 사용하는 모든 Java 프로세스 종료
echo "📍 9091 포트 사용 프로세스 확인..."
PIDS=$(ps aux | grep "java.*9091\|gradle.*kalisco" | grep -v grep | awk '{print $2}')
if [ ! -z "$PIDS" ]; then
    echo "🔍 발견된 프로세스: $PIDS"
    kill -9 $PIDS 2>/dev/null
    echo "✅ 프로세스 종료 완료"
    sleep 2
else
    echo "ℹ️  9091 포트 사용 프로세스 없음"
fi

# 2. 포트 사용 확인
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

# 3. 최종 확인
echo "📍 최종 포트 상태 확인..."
FINAL_CHECK=$(netstat -tlnp | grep 9091)
if [ -z "$FINAL_CHECK" ]; then
    echo "✅ 캘리스코 서비스 종료 완료 - 9091 포트 사용 없음"
else
    echo "❌ 9091 포트가 여전히 사용 중입니다:"
    echo "$FINAL_CHECK"
fi

echo "🏁 종료 작업 완료"
