#!/bin/bash
# stop_application.sh
# 애플리케이션 중지

set -e

APP_DIR="/home/ec2-user/pulse"
PID_FILE="$APP_DIR/application.pid"
MAX_WAIT=30

echo "=== Application Stop: 애플리케이션 중지 시작 ==="

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    echo "PID 파일에서 프로세스 ID 확인: $PID"

    if ps -p "$PID" > /dev/null 2>&1; then
        echo "프로세스 종료 시도 (SIGTERM)"
        kill -TERM "$PID"

        WAIT_TIME=0
        while ps -p "$PID" > /dev/null 2>&1 && [ $WAIT_TIME -lt $MAX_WAIT ]; do
            echo "프로세스 종료 대기 중... ($WAIT_TIME/$MAX_WAIT 초)"
            sleep 1
            WAIT_TIME=$((WAIT_TIME + 1))
        done

        if ps -p "$PID" > /dev/null 2>&1; then
            echo "프로세스가 종료되지 않음. 강제 종료 시도 (SIGKILL)"
            kill -KILL "$PID"
            sleep 2
        fi

        echo "프로세스 종료 완료"
    else
        echo "PID $PID에 해당하는 프로세스가 실행 중이지 않음"
    fi

    rm -f "$PID_FILE"
else
    echo "PID 파일이 없음. 실행 중인 애플리케이션 확인"

    PROCESS_ID=$(pgrep -f "pulse-0.0.1-SNAPSHOT.jar" || true)

    if [ -n "$PROCESS_ID" ]; then
        echo "실행 중인 프로세스 발견: $PROCESS_ID"
        echo "프로세스 종료 시도"
        kill -TERM "$PROCESS_ID"
        sleep 5

        if ps -p "$PROCESS_ID" > /dev/null 2>&1; then
            echo "강제 종료 시도"
            kill -KILL "$PROCESS_ID"
        fi
    else
        echo "실행 중인 애플리케이션 없음"
    fi
fi

echo "=== Application Stop 완료 ==="
