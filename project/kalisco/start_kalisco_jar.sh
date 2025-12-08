#!/bin/bash

echo "🚀 캘리스코 서비스 시작 중 (JAR 파일 방식)..."

# 1. 기존 서비스 종료
echo "📍 기존 서비스 확인 및 종료..."
pkill -f "java.*kalisco.*jar" 2>/dev/null
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

# 3. JAR 파일 확인
JAR_FILE="/var/www/html/project/kalisco/build/libs/kalisco-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR 파일이 없습니다. 빌드를 실행합니다..."
    cd /var/www/html/project/kalisco
    ./gradlew clean bootJar
    
    if [ $? -ne 0 ]; then
        echo "❌ 빌드 실패! 서비스를 시작할 수 없습니다."
        exit 1
    fi
    echo "✅ 빌드 완료"
else
    echo "✅ JAR 파일 확인: $JAR_FILE"
fi

# 4. 서비스 시작 (백그라운드)
echo "🚀 서비스 시작 중..."
cd /var/www/html/project/kalisco
nohup java -jar "$JAR_FILE" > logs/kalisco.log 2>&1 &

# 5. 서비스 시작 확인
echo "⏳ 서비스 시작 대기 중..."
sleep 5

PORT_CHECK=$(netstat -tlnp | grep 9091)
if [ ! -z "$PORT_CHECK" ]; then
    echo "✅ 캘리스코 서비스 시작 완료 - 9091 포트 리스닝 중"
    echo "📍 로그 확인: tail -f logs/kalisco.log"
    echo "📍 프로세스 확인: ps aux | grep kalisco"
else
    echo "❌ 서비스 시작 실패. 로그를 확인하세요: tail -f logs/kalisco.log"
    exit 1
fi

echo "🏁 시작 작업 완료"
