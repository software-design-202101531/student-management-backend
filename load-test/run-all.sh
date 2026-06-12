#!/usr/bin/env bash
# 전 시나리오를 선택한 부하 패턴으로 순차 실행한다.
#   사용: BASE_URL=http://localhost:8080 TEST_TYPE=smoke ./run-all.sh
#        ./run-all.sh s1_grade_write_concurrency   # 특정 시나리오만
set -uo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
export BASE_URL="${BASE_URL:-http://localhost:8080}"
export TEST_TYPE="${TEST_TYPE:-smoke}"

ALL=(s0_login s1_grade_write_concurrency s2_read_spike s3_search_filter s4_notification s5_olap_dashboard)
if [ "$#" -gt 0 ]; then
  TARGETS=("$@")
else
  TARGETS=("${ALL[@]}")
fi

echo "▶ BASE_URL=$BASE_URL  TEST_TYPE=$TEST_TYPE"
fail=0
for s in "${TARGETS[@]}"; do
  echo ""
  echo "=================== $s ==================="
  if ! k6 run "$DIR/scenarios/${s}.js"; then
    echo "✗ $s 임계값 미달 또는 오류"
    fail=1
  fi
done
exit "$fail"
