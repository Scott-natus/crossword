#!/bin/bash

# 윈도우 원격데스크탑에서 파이어폭스 실행 스크립트

# 원격데스크탑 환경에서는 DISPLAY가 자동으로 설정됨
# X11 관련 설정은 제거

# 파이어폭스 실행 (원격데스크탑 환경에 최적화)
/snap/bin/firefox --no-remote --new-instance "$@"
