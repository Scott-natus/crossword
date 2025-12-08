#!/bin/bash

echo "🚀 캘리스코 서비스 시작 중..."

# 1. 기존 서비스 종료 (혹시 실행 중인 경우)
echo "📍 기존 서비스 확인 및 종료..."
pkill -f "java.*9091" 2>/dev/null
pkill -f "gradle.*kalisco" 2>/dev/null
sleep 2

# 2. 포트 해제 확인
echo "📍 9091 포트 사용 상태 확인..."
PORT_CHECK=$(netstat -tlnp | grep 9091)
if [ ! -z "$PORT_CHECK" ]; then
    echo "⚠️  9091 포트가 여전히 사용 중입니다. 강제 종료 시도..."
    fuser -k 9091/tcp 2>/dev/null
    sleep 2
fi

# 3. 빌드 실행 (오류 확인)
echo "🔨 Gradle 빌드 실행 중..."
cd /var/www/html/project/kalisco
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo "❌ 빌드 실패! 서비스를 시작할 수 없습니다."
    exit 1
fi

echo "✅ 빌드 성공"

# 4. 서비스 시작 (백그라운드)
echo "🚀 서비스 시작 중..."
nohup ./gradlew bootRun > logs/kalisco.log 2>&1 &

# 5. 서비스 시작 확인
echo "⏳ 서비스 시작 대기 중..."
sleep 5

PORT_CHECK=$(netstat -tlnp | grep 9091)
if [ ! -z "$PORT_CHECK" ]; then
    echo "✅ 캘리스코 서비스 시작 완료 - 9091 포트 리스닝 중"
    echo "📍 로그 확인: tail -f logs/kalisco.log"
else
    echo "❌ 서비스 시작 실패. 로그를 확인하세요: tail -f logs/kalisco.log"
    exit 1
fi

echo "🏁 시작 작업 완료"
