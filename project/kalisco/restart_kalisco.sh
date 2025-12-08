#!/bin/bash

# 캘리스코 서비스 재시작 스크립트 (표준 프로세스)
# 1. 서비스 다운 → 2. 빌드 → 3. JAR 파일로 서비스 시작

echo "🔄 캘리스코 서비스 재시작 (표준 프로세스)"
echo "=========================================="

# 1. 서비스 다운
echo ""
echo "📌 1단계: 서비스 다운"
echo "----------------------------------------"
echo "📍 기존 서비스 종료 중..."

# systemd 서비스 중지
echo "dlrhkeo8453" | sudo -S systemctl stop kalisco-service 2>/dev/null

# JAR 프로세스 종료
pkill -f "java.*kalisco.*jar" 2>/dev/null
pkill -f "gradle.*kalisco" 2>/dev/null

# 포트 해제 확인
PORT_CHECK=$(netstat -tlnp | grep 9091)
if [ ! -z "$PORT_CHECK" ]; then
    echo "⚠️  9091 포트가 여전히 사용 중입니다. 강제 종료 시도..."
    fuser -k 9091/tcp 2>/dev/null
    sleep 2
fi

# 최종 확인
FINAL_CHECK=$(netstat -tlnp | grep 9091)
if [ -z "$FINAL_CHECK" ]; then
    echo "✅ 서비스 종료 완료 - 9091 포트 사용 없음"
else
    echo "⚠️  포트가 여전히 사용 중이지만 계속 진행합니다."
fi

# 2. 빌드
echo ""
echo "📌 2단계: 프로젝트 빌드"
echo "----------------------------------------"
cd /var/www/html/project/kalisco

echo "🔨 Gradle 빌드 실행 중..."
./gradlew clean bootJar

if [ $? -ne 0 ]; then
    echo "❌ 빌드 실패! 서비스를 시작할 수 없습니다."
    exit 1
fi

# JAR 파일 확인
JAR_FILE="build/libs/kalisco-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR 파일이 생성되지 않았습니다: $JAR_FILE"
    exit 1
fi

JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
echo "✅ 빌드 성공 - JAR 파일: $JAR_FILE ($JAR_SIZE)"

# 3. JAR 파일로 서비스 시작
echo ""
echo "📌 3단계: JAR 파일로 서비스 시작"
echo "----------------------------------------"

# 로그 디렉토리 확인
mkdir -p logs

# systemd 서비스 시작
echo "🚀 systemd 서비스 시작 중..."
echo "dlrhkeo8453" | sudo -S systemctl start kalisco-service

# 서비스 시작 확인
echo "⏳ 서비스 시작 대기 중..."
sleep 5

# 상태 확인 (여러 번 시도)
echo "📍 서비스 상태 확인 중..."
for i in {1..5}; do
    sleep 2
    PORT_CHECK=$(netstat -tlnp 2>/dev/null | grep 9091)
    SERVICE_STATUS=$(systemctl is-active kalisco-service 2>/dev/null)
    
    if [ ! -z "$PORT_CHECK" ] && [ "$SERVICE_STATUS" = "active" ]; then
        echo "✅ 서비스 시작 완료 - 9091 포트 리스닝 중"
        echo ""
        echo "📍 서비스 상태:"
        systemctl status kalisco-service --no-pager | head -10
        echo ""
        echo "📍 로그 확인: tail -f logs/kalisco.log"
        echo "📍 또는: sudo journalctl -u kalisco-service -f"
        exit 0
    fi
    echo "⏳ 대기 중... ($i/5)"
done

# 최종 확인
PORT_CHECK=$(netstat -tlnp 2>/dev/null | grep 9091)
SERVICE_STATUS=$(systemctl is-active kalisco-service 2>/dev/null)

if [ ! -z "$PORT_CHECK" ] && [ "$SERVICE_STATUS" = "active" ]; then
    echo "✅ 서비스 시작 완료"
else
    echo "⚠️  서비스 상태 확인 필요"
    echo "📍 서비스 상태: $SERVICE_STATUS"
    echo "📍 포트 상태: $([ -z "$PORT_CHECK" ] && echo "사용 안 함" || echo "사용 중")"
    echo ""
    echo "📍 상세 상태 확인: sudo systemctl status kalisco-service"
    echo "📍 로그 확인: sudo journalctl -u kalisco-service -n 50"
fi

echo ""
echo "🏁 재시작 작업 완료"
echo "=========================================="
