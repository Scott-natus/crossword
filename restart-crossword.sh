#!/bin/bash
echo "🔄 크로스워드 서비스 재시작 중..."
pkill -f "java.*8081"
sleep 3
cd /var/www/html/crossword-projects && ./gradlew bootRun --args='--server.port=8081' &
echo "✅ 재시작 완료! 잠시 후 https://natus250601.viewdns.net/K-CrossWord/admin/levels 에서 확인하세요."

