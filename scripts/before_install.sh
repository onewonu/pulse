#!/bin/bash
# before_install.sh
# 배포 전 준비 작업: 디렉토리 생성, 이전 버전 백업

set -e

APP_DIR="/home/ec2-user/pulse"
LOG_DIR="$APP_DIR/logs"
BACKUP_DIR="$APP_DIR/backup"
JAR_FILE="$APP_DIR/pulse-0.0.1-SNAPSHOT.jar"

echo "=== Before Install: 배포 전 준비 작업 시작 ==="

if [ ! -d "$APP_DIR" ]; then
    echo "애플리케이션 디렉토리 생성: $APP_DIR"
    mkdir -p "$APP_DIR"
fi

if [ ! -d "$LOG_DIR" ]; then
    echo "로그 디렉토리 생성: $LOG_DIR"
    mkdir -p "$LOG_DIR"
fi

if [ ! -d "$BACKUP_DIR" ]; then
    echo "백업 디렉토리 생성: $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"
fi

if [ -f "$JAR_FILE" ]; then
    BACKUP_FILE="$BACKUP_DIR/pulse-0.0.1-SNAPSHOT_$(date +%Y%m%d_%H%M%S).jar"
    echo "기존 JAR 파일 백업: $BACKUP_FILE"
    cp "$JAR_FILE" "$BACKUP_FILE"

    echo "7일 이상 된 백업 파일 삭제"
    find "$BACKUP_DIR" -name "pulse-*.jar" -mtime +7 -delete
fi

if [ -f "$JAR_FILE" ]; then
    echo "이전 JAR 파일 삭제: $JAR_FILE"
    rm -f "$JAR_FILE"
fi

echo "=== Before Install 완료 ==="
