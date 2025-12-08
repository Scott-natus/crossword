#!/bin/bash

echo "🔄 캘리스코 서비스 재시작 중 (JAR 파일 방식)..."

# 1. 서비스 종료
echo "🛑 서비스 종료 중..."
/var/www/html/project/kalisco/stop_kalisco_jar.sh

# 2. 잠시 대기
echo "⏳ 포트 해제 대기 중..."
sleep 3

# 3. 서비스 시작
echo "🚀 서비스 시작 중..."
/var/www/html/project/kalisco/start_kalisco_jar.sh

echo "🏁 재시작 작업 완료"
