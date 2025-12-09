#!/bin/bash
# start_application.sh
# 애플리케이션 시작

set -e

APP_DIR="/home/ec2-user/pulse"
JAR_FILE="$APP_DIR/pulse-0.0.1-SNAPSHOT.jar"
LOG_FILE="$APP_DIR/logs/application.log"
PID_FILE="$APP_DIR/application.pid"
JAVA_OPTS="-Xms256m -Xmx768m -Dspring.profiles.active=prod"

echo "=== Application Start: 애플리케이션 시작 ==="

if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR 파일을 찾을 수 없습니다: $JAR_FILE"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "ERROR: Java가 설치되어 있지 않습니다"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1)
echo "Java 버전: $JAVA_VERSION"

mkdir -p "$(dirname "$LOG_FILE")"

echo "애플리케이션 시작 중..."
echo "JAR 파일: $JAR_FILE"
echo "로그 파일: $LOG_FILE"
echo "Java 옵션: $JAVA_OPTS"

nohup java $JAVA_OPTS -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &

echo $! > "$PID_FILE"
PID=$(cat "$PID_FILE")

echo "애플리케이션 시작됨. PID: $PID"

sleep 5

if ps -p "$PID" > /dev/null 2>&1; then
    echo "애플리케이션이 정상적으로 시작되었습니다"
else
    echo "ERROR: 애플리케이션 시작 실패"
    echo "로그 파일을 확인하세요: $LOG_FILE"
    exit 1
fi

echo "=== Application Start 완료 ==="
