#!/bin/bash
# validate_service.sh
# 애플리케이션 상태 검증

set -e

APP_DIR="/home/ec2-user/pulse"
PID_FILE="$APP_DIR/application.pid"
LOG_FILE="$APP_DIR/logs/application.log"
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"
MAX_RETRY=30
RETRY_INTERVAL=2

echo "=== Validate Service: 서비스 검증 시작 ==="

if [ ! -f "$PID_FILE" ]; then
    echo "ERROR: PID 파일을 찾을 수 없습니다: $PID_FILE"
    exit 1
fi

PID=$(cat "$PID_FILE")
echo "애플리케이션 PID: $PID"

if ! ps -p "$PID" > /dev/null 2>&1; then
    echo "ERROR: 프로세스가 실행 중이지 않습니다 (PID: $PID)"
    echo "최근 로그:"
    tail -n 50 "$LOG_FILE"
    exit 1
fi

echo "프로세스 실행 확인 완료"