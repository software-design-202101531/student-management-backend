// 행특 다중 접속 부하/경합 테스트 (기획 §5.2) — k6
//
// 전제: 백엔드 스택이 기동돼 있고, 교사 계정이 대상 학생의 담임이어야 한다(예: 시드 teacher01).
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 -e LOGIN_ID=teacher01 -e PASSWORD=test1234! \
//          -e STUDENT_ID=서로다른_학생들은_별도구성 scripts/load-test/behavior-record.js
//
// ⚠️ 운영 DB 대상으로 실행하지 말 것(쓰기 부하). 스테이징/로컬에서만.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const LOGIN_ID = __ENV.LOGIN_ID || 'teacher01';
const PASSWORD = __ENV.PASSWORD || 'test1234!';
const STUDENT_ID = __ENV.STUDENT_ID || '1';

export const options = {
  scenarios: {
    // 시나리오 B: 동일 학생 행특에 다수 교사 동시 입력(경합). 409(정상 경합)는 실패로 치지 않는다.
    contention: {
      executor: 'constant-vus',
      vus: 20,
      duration: '30s',
    },
  },
  thresholds: {
    // 응답시간 p95 < 300ms
    http_req_duration: ['p(95)<300'],
    // 5xx/네트워크성 실패율 < 1% (409 경합은 별도 카운트라 제외)
    server_errors: ['rate<0.01'],
  },
};

import { Rate } from 'k6/metrics';
const serverErrors = new Rate('server_errors');

// 로그인 → access 토큰은 응답 Authorization 헤더로 내려온다(백엔드 계약).
function login() {
  const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    loginId: LOGIN_ID, password: PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } });
  const auth = res.headers['Authorization'] || res.headers['authorization'];
  check(res, { '로그인 200': (r) => r.status === 200, '토큰 수신': () => !!auth });
  return auth; // "Bearer ..."
}

export function setup() {
  return { token: login() };
}

export default function (data) {
  const res = http.post(
    `${BASE_URL}/api/students/${STUDENT_ID}/records/behavior`,
    JSON.stringify({ content: `부하테스트-${__VU}-${__ITER}-${Date.now()}` }),
    { headers: { 'Content-Type': 'application/json', 'Authorization': data.token } },
  );

  // 200/2xx = 정상, 409 = 정상 경합(낙관적 락/유니크), 그 외 5xx = 서버 오류
  check(res, {
    '성공 또는 정상 경합(2xx/409)': (r) => (r.status >= 200 && r.status < 300) || r.status === 409,
  });
  serverErrors.add(res.status >= 500);

  sleep(0.2);
}
