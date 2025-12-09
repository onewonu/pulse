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

echo "Health Check 엔드포인트 검증 시작: $HEALTH_CHECK_URL"

RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL" || echo "000")

    if [ "$HTTP_STATUS" == "200" ]; then
        echo "Health Check 성공! (HTTP $HTTP_STATUS)"
        echo "애플리케이션이 정상적으로 실행 중입니다"
        echo "=== Validate Service 완료 ==="
        exit 0
    fi

    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Health Check 재시도 중... ($RETRY_COUNT/$MAX_RETRY) - HTTP $HTTP_STATUS"
    sleep $RETRY_INTERVAL
done

echo "ERROR: Health Check 실패 (최대 재시도 횟수 초과)"
echo "HTTP Status: $HTTP_STATUS"
echo "최근 로그:"
tail -n 100 "$LOG_FILE"
exit 1
