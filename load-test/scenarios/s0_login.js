// 인증 — 업무 시작 시간대 로그인 집중
// ------------------------------------------------------------------
// 실행: TEST_TYPE=spike k6 run s0_login.js

import http from 'k6/http';
import { check } from 'k6';
import { BASE } from '../config/env.js';
import { teacherForVU, studentForVU } from '../lib/users.js';
import { buildOptions } from '../config/options.js';
import { AUTH } from '../config/thresholds.js';

export const options = buildOptions(AUTH);

export default function () {
  // VU 절반은 교사, 절반은 학생으로 로그인
  const cred = __VU % 2 === 0 ? teacherForVU(__VU) : studentForVU(__VU);
  const res = http.post(`${BASE}/api/auth/login`, JSON.stringify(cred), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });
  check(res, {
    'login 200': (r) => r.status === 200,
    'has access token': (r) => !!r.headers['Authorization'],
  });
}
