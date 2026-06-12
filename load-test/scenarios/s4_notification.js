// 3.4 알림 폴링 부하
// ------------------------------------------------------------------
// 인앱 알림은 폴링 방식. 다수 사용자가 배지(unread-count)를 주기적으로 폴링하는 부하.
//   - 미확인 카운트는 Redis 캐시 적중 대상 → 캐시 효과 확인
//   - 사용자별 데이터라 VU 마다 각자 계정으로 로그인(토큰 캐싱)
// 큐 적체(queue depth)·consumer lag 은 RabbitMQ Management(:15672)로 병행 관찰.
// 실행: TEST_TYPE=load k6 run s4_notification.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE } from '../config/env.js';
import { authHeaders, tokenForVU } from '../lib/auth.js';
import { teacherForVU, studentForVU } from '../lib/users.js';
import { buildOptions } from '../config/options.js';
import { READ } from '../config/thresholds.js';

export const options = buildOptions(READ);

export default function () {
  const cred = __VU % 2 === 0 ? teacherForVU(__VU) : studentForVU(__VU);
  const token = tokenForVU(__VU, cred);
  const params = { headers: authHeaders(token), tags: { kind: 'read' } };

  // (a) 배지 폴링 — 미확인 카운트 (가장 빈번)
  const countRes = http.get(
    `${BASE}/api/notifications/unread-count`,
    Object.assign({}, params, { tags: { kind: 'read', name: 'unread_count' } })
  );
  check(countRes, { 'unread-count 200': (r) => r.status === 200 });

  // (b) 가끔 목록 펼치기
  if (__ITER % 5 === 0) {
    const listRes = http.get(
      `${BASE}/api/notifications?page=0&size=20`,
      Object.assign({}, params, { tags: { kind: 'read', name: 'notification_list' } })
    );
    check(listRes, { 'notifications 200': (r) => r.status === 200 });
  }

  sleep(1); // 폴링 간격
}

// 종료 시 HTML/JSON 리포트 생성 (CI 아티팩트)
export { handleSummary } from '../lib/summary.js';
