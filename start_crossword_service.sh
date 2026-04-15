#!/bin/bash

echo "🚀 크로스워드 서비스 시작 중..."

# 1. 기존 서비스 완전 종료
echo "📍 기존 서비스 종료..."
./stop_crossword_service.sh

# 2. 잠시 대기 (포트 해제 대기)
echo "⏳ 포트 해제 대기 중..."
sleep 5

# 3. 포트 사용 상태 재확인
echo "📍 포트 상태 재확인..."
PORT_CHECK=$(netstat -tlnp | grep 8081)
if [ ! -z "$PORT_CHECK" ]; then
    echo "❌ 8081 포트가 여전히 사용 중입니다. 수동으로 해결해주세요:"
    echo "$PORT_CHECK"
    exit 1
fi

# 4. 빌드 확인
echo "🔨 프로젝트 빌드 확인..."
cd /var/www/html/crossword-projects

echo "📍 Gradle 빌드 실행..."
if ./gradlew build -x test; then
    echo "✅ 빌드 성공"
else
    echo "❌ 빌드 실패 - 서비스 시작 중단"
    echo "📋 빌드 로그를 확인하고 오류를 수정해주세요"
    exit 1
fi

# 5. 서비스 시작
echo "🚀 크로스워드 서비스 시작..."
nohup ./gradlew bootRun --args='--server.port=8081' > /dev/null 2>&1 &

# 6. 시작 확인
echo "⏳ 서비스 시작 대기 중..."
sleep 10

# 7. 서비스 상태 확인
echo "📍 서비스 상태 확인..."
if netstat -tlnp | grep 8081 > /dev/null; then
    echo "✅ 크로스워드 서비스 시작 완료 - 8081 포트에서 실행 중"
    echo "🌐 접속 URL: http://222.100.103.227:8081/K-CrossWord/"
else
    echo "❌ 서비스 시작 실패"
    echo "📋 로그 확인: tail -f /home/ubuntu/crossword/log/crossword-application.log"
fi

echo "🏁 시작 작업 완료"
