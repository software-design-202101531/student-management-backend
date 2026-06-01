#!/bin/sh
# 매일 BACKUP_HOUR(기본 04)시에 db-backup.sh를 실행하는 단순 스케줄 루프.
# (cron 데몬의 환경변수 전달 이슈를 피하기 위해 컨테이너 PID1 루프로 구현)
set -eu

: "${BACKUP_HOUR:=4}"
: "${RETENTION_DAYS:=14}"
: "${BACKUP_BUCKET:=school-backups}"

echo "[db-backup] 기동: 매일 ${BACKUP_HOUR}:00 백업 → MinIO/${BACKUP_BUCKET}, 보존 ${RETENTION_DAYS}일"

while true; do
  now=$(date +%s)
  target=$(date -d "today ${BACKUP_HOUR}:00" +%s)
  if [ "${target}" -le "${now}" ]; then
    target=$(date -d "tomorrow ${BACKUP_HOUR}:00" +%s)
  fi
  wait_sec=$((target - now))
  echo "[db-backup] 다음 백업까지 ${wait_sec}s 대기"
  sleep "${wait_sec}"
  /usr/local/bin/db-backup.sh || echo "[db-backup] 백업 실패 — 다음 주기에 재시도"
done
