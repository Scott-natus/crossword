#!/bin/bash

# 실패한 자음-모음 조합 재처리 스크립트
# 총 61개 조합을 30초 간격으로 처리

echo "🚀 실패한 자음-모음 조합 재처리 시작"
echo "📅 시작 시간: $(date)"
echo "⏱️  예상 소요 시간: 약 30분 (61개 × 30초)"
echo ""

# 실패한 조합들 배열
declare -a combinations=(
    "ㄱ:ㅓ" "ㄱ:ㅗ" "ㄱ:ㅘ" "ㄱ:ㅡ" "ㄱ:ㅢ"
    "ㄴ:ㅓ" "ㄴ:ㅗ" "ㄴ:ㅜ" "ㄴ:ㅠ"
    "ㄷ:ㅏ" "ㄷ:ㅗ" "ㄷ:ㅛ" "ㄷ:ㅠ"
    "ㄹ:ㅑ" "ㄹ:ㅕ" "ㄹ:ㅠ" "ㄹ:ㅣ"
    "ㅁ:ㅏ" "ㅁ:ㅓ" "ㅁ:ㅕ" "ㅁ:ㅗ" "ㅁ:ㅘ" "ㅁ:ㅠ" "ㅁ:ㅡ"
    "ㅂ:ㅏ" "ㅂ:ㅢ"
    "ㅅ:ㅓ" "ㅅ:ㅘ" "ㅅ:ㅙ" "ㅅ:ㅛ" "ㅅ:ㅜ" "ㅅ:ㅞ" "ㅅ:ㅠ" "ㅅ:ㅡ"
    "ㅇ:ㅏ" "ㅇ:ㅑ" "ㅇ:ㅕ" "ㅇ:ㅘ" "ㅇ:ㅜ"
    "ㅈ:ㅑ" "ㅈ:ㅗ" "ㅈ:ㅙ" "ㅈ:ㅛ" "ㅈ:ㅜ" "ㅈ:ㅠ" "ㅈ:ㅣ"
    "ㅊ:ㅏ" "ㅊ:ㅑ" "ㅊ:ㅘ" "ㅊ:ㅞ" "ㅊ:ㅠ"
    "ㅋ:ㅢ"
    "ㅌ:ㅏ" "ㅌ:ㅜ"
    "ㅍ:ㅕ" "ㅍ:ㅗ" "ㅍ:ㅘ" "ㅍ:ㅛ" "ㅍ:ㅠ" "ㅍ:ㅢ" "ㅍ:ㅣ"
    "ㅎ:ㅞ"
)

total=${#combinations[@]}
success_count=0
failure_count=0

echo "📊 총 처리할 조합: $total개"
echo ""

for i in "${!combinations[@]}"; do
    combination="${combinations[$i]}"
    consonant=$(echo "$combination" | cut -d':' -f1)
    vowel=$(echo "$combination" | cut -d':' -f2)
    
    current=$((i + 1))
    echo "📍 [$current/$total] 처리 중: $consonant + $vowel"
    
    # URL 인코딩 (Python을 사용하여 정확한 UTF-8 인코딩)
    consonant_encoded=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$consonant'))")
    vowel_encoded=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$vowel'))")
    
    # API 호출
    response=$(curl -s -X POST "http://127.0.0.1:8081/K-CrossWord/api/consonant-vowel-batch/process-combination?consonant=$consonant_encoded&vowel=$vowel_encoded")
    
    # 응답 파싱
    success=$(echo "$response" | grep -o '"success":[^,]*' | cut -d':' -f2 | tr -d ' "')
    
    if [ "$success" = "true" ]; then
        generated_count=$(echo "$response" | grep -o '"generated_count":[^,]*' | cut -d':' -f2 | tr -d ' "')
        saved_count=$(echo "$response" | grep -o '"saved_count":[^,]*' | cut -d':' -f2 | tr -d ' "')
        echo "✅ 성공: $generated_count개 생성, $saved_count개 저장"
        ((success_count++))
    else
        echo "❌ 실패: $response"
        ((failure_count++))
    fi
    
    # 마지막 조합이 아닌 경우에만 대기
    if [ $current -lt $total ]; then
        echo "⏳ 30초 대기 중... (다음: $((current + 1))/$total)"
        sleep 30
    fi
    
    echo ""
done

echo "🏁 배치 완료!"
echo "📅 완료 시간: $(date)"
echo "📊 결과: 성공 $success_count개, 실패 $failure_count개"
echo ""

# 최종 통계 확인
echo "🔍 최종 통계 확인 중..."
final_response=$(curl -s "http://127.0.0.1:8081/K-CrossWord/api/consonant-vowel-batch/status")
echo "$final_response" | python3 -c "
import json, sys
data = json.load(sys.stdin)
print(f'총 조합: {data[\"totalCombinations\"]}개')
print(f'처리 완료: {data[\"processedCount\"]}개')
print(f'성공: {data[\"successCount\"]}개')
print(f'실패: {data[\"failureCount\"]}개')
"
