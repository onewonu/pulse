#!/bin/bash
# after_install.sh
# 배포 후 설정 작업: 파일 권한 설정

set -e

APP_DIR="/home/ec2-user/pulse"
JAR_FILE="$APP_DIR/pulse-0.0.1-SNAPSHOT.jar"

echo "=== After Install: 배포 후 설정 작업 시작 ==="

if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR 파일을 찾을 수 없습니다: $JAR_FILE"
    exit 1
fi

echo "JAR 파일 실행 권한 설정"
chmod +x "$JAR_FILE"

echo "디렉토리 소유권 확인 및 설정"
chown -R ec2-user:ec2-user "$APP_DIR"

echo "=== After Install 완료 ==="
