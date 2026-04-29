#!/bin/bash

echo "사주 백엔드 서버 중단 중..."

if [ -f logs/server.pid ]; then
    PID=$(cat logs/server.pid)
    if ps -p $PID > /dev/null 2>&1; then
        kill $PID
        echo "서버 중단 완료 (PID: $PID)"
        rm logs/server.pid
    else
        echo "서버가 이미 중단되었습니다."
        rm logs/server.pid
    fi
else
    # PID 파일이 없으면 프로세스 이름으로 찾아서 종료
    PIDS=$(ps aux | grep 'saju-backend.*\.jar' | grep -v grep | awk '{print $2}')
    if [ -n "$PIDS" ]; then
        echo "실행 중인 서버 발견: $PIDS"
        echo "$PIDS" | xargs kill
        echo "서버 중단 완료"
    else
        echo "실행 중인 서버를 찾을 수 없습니다."
    fi
fi
