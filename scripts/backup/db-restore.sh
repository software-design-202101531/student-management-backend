#!/bin/sh
# MinIO에서 백업을 받아 PostgreSQL로 복구한다.
# 사용법:
#   db-restore.sh             → 복구 가능한 백업 목록 출력
#   db-restore.sh <파일명>    → 해당 백업으로 복구 (확인 프롬프트 후 진행)
set -eu

mc alias set store "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" >/dev/null

if [ "$#" -lt 1 ]; then
  echo "복구 가능한 백업 목록 (store/${BACKUP_BUCKET}):"
  mc ls "store/${BACKUP_BUCKET}/"
  echo
  echo "복구하려면: db-restore.sh <파일명>"
  exit 0
fi

FILE="$1"
TMP="/tmp/${FILE}"

echo "[db-restore] 다운로드: store/${BACKUP_BUCKET}/${FILE}"
mc cp "store/${BACKUP_BUCKET}/${FILE}" "${TMP}"

echo ""
echo "!! 경고: '${PGDATABASE}' 를 '${FILE}' 내용으로 복구합니다. 기존 객체는 DROP 후 재생성됩니다."
echo "   계속하려면 정확히  RESTORE  를 입력하세요:"
read -r CONFIRM
if [ "${CONFIRM}" != "RESTORE" ]; then
  echo "[db-restore] 취소됨."
  rm -f "${TMP}"
  exit 1
fi

# --clean --if-exists: 기존 객체 제거 후 복구 (멱등)
PGPASSWORD="${PGPASSWORD}" pg_restore -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --clean --if-exists "${TMP}"
rm -f "${TMP}"
echo "[db-restore] 복구 완료. (암호화 컬럼 복호화에는 동일한 APP_ENCRYPTION_PASSWORD/SALT 필요)"
