#!/bin/sh
# PostgreSQL 백업 → MinIO 업로드 → 보존기간 초과분 정리.
# 환경변수: PGHOST PGUSER PGPASSWORD PGDATABASE
#           MINIO_ENDPOINT MINIO_ACCESS_KEY MINIO_SECRET_KEY BACKUP_BUCKET RETENTION_DAYS
set -eu

TS=$(date +%Y%m%d_%H%M%S)
FILE="${PGDATABASE}_${TS}.dump"
TMP="/tmp/${FILE}"

echo "[db-backup] pg_dump 시작: ${PGDATABASE} → ${FILE}"
# 커스텀 포맷(-Fc): 압축 + pg_restore의 선택 복구/병렬 복구 지원
PGPASSWORD="${PGPASSWORD}" pg_dump -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" -Fc -f "${TMP}"

# MinIO 별칭 등록 + 버킷 보장(없으면 생성)
mc alias set store "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" >/dev/null
mc mb --ignore-existing "store/${BACKUP_BUCKET}" >/dev/null

echo "[db-backup] 업로드: store/${BACKUP_BUCKET}/${FILE}"
mc cp "${TMP}" "store/${BACKUP_BUCKET}/${FILE}"
rm -f "${TMP}"

# 보존기간 정리: RETENTION_DAYS 초과 백업 삭제
mc rm --recursive --force --older-than "${RETENTION_DAYS}d" "store/${BACKUP_BUCKET}/" >/dev/null 2>&1 || true

echo "[db-backup] 완료. (보존 ${RETENTION_DAYS}일 초과분 정리)"
