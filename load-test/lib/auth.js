import http from 'k6/http';
import { check } from 'k6';
import { BASE } from '../config/env.js';

// 이 프로젝트 특수성: access 토큰은 응답 바디가 아니라 Authorization 헤더("Bearer xxx")로 온다.
// refresh 토큰은 httpOnly 쿠키라 부하 테스트에서는 사용하지 않는다.
export function login(cred) {
  const res = http.post(`${BASE}/api/auth/login`, JSON.stringify(cred), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });
  const ok = check(res, { 'login 200': (r) => r.status === 200 });
  if (!ok) {
    throw new Error(`login failed for ${cred.loginId}: status=${res.status} body=${res.body}`);
  }
  return res.headers['Authorization']; // "Bearer xxx"
}

// 인증 요청 헤더. token 은 login() 이 돌려준 "Bearer xxx" 전체 문자열.
export function authHeaders(token) {
  return { Authorization: token, 'Content-Type': 'application/json' };
}

// VU 별로 토큰을 1회만 발급해 캐싱하는 헬퍼(알림 폴링처럼 사용자별 데이터가 필요할 때).
const _tokenCache = {};
export function tokenForVU(vu, cred) {
  if (!_tokenCache[vu]) {
    _tokenCache[vu] = login(cred);
  }
  return _tokenCache[vu];
}
