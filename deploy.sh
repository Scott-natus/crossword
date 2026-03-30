#!/bin/bash

cd /var/www/html/CrosswordPuzzleApp || exit 1

echo "🔨 [1/2] 웹앱 빌드 시작..."
npm run build-web

if [ $? -ne 0 ]; then
  echo "❌ 빌드 실패!"
  exit 1
fi

echo "🚀 [2/2] 서비스 재시작..."
sudo systemctl restart crossword-puzzle-app

if [ $? -ne 0 ]; then
  echo "❌ 서비스 재시작 실패!"
  exit 1
fi

echo "✅ 빌드 및 배포 완료!" 