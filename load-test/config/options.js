// 5가지 표준 부하 패턴을 TEST_TYPE 환경변수로 스위칭한다.
//   TEST_TYPE=smoke|load|stress|spike|soak  (기본 smoke)
//
// 시나리오 스크립트는 부하 패턴을 직접 정의하지 않고 buildOptions(thresholds)만 호출한다.
// 같은 스크립트를 패턴만 바꿔 재사용하기 위함이다.

const PROFILES = {
  // 스크립트/기본 동작 검증 — 최소 부하
  smoke: { vus: 1, duration: '1m' },

  // 정상 업무 시간대 동시 사용자 — 기준선
  load: {
    stages: [
      { duration: '1m', target: 50 },
      { duration: '3m', target: 50 },
      { duration: '1m', target: 0 },
    ],
  },

  // 한계점(breaking point) 탐색 — 점증
  stress: {
    stages: [
      { duration: '2m', target: 50 },
      { duration: '2m', target: 150 },
      { duration: '2m', target: 300 },
      { duration: '2m', target: 450 },
      { duration: '1m', target: 0 },
    ],
  },

  // 순간 급증 대응 — 성적 발표 직후 조회 러시
  spike: {
    stages: [
      { duration: '10s', target: 5 },
      { duration: '10s', target: 300 },
      { duration: '1m', target: 300 },
      { duration: '10s', target: 5 },
      { duration: '20s', target: 0 },
    ],
  },

  // 장시간 지속 부하 — 커넥션 풀 누수/메모리 누수 탐지 (약 1시간)
  soak: {
    stages: [
      { duration: '2m', target: 40 },
      { duration: '56m', target: 40 },
      { duration: '2m', target: 0 },
    ],
  },
};

const selected = PROFILES[__ENV.TEST_TYPE || 'smoke'];
if (!selected) {
  throw new Error(`unknown TEST_TYPE: ${__ENV.TEST_TYPE} (smoke|load|stress|spike|soak)`);
}

export const profile = selected;

// 선택된 부하 패턴 + 시나리오별 임계값을 합쳐 k6 options 객체를 만든다.
export function buildOptions(thresholds = {}) {
  return Object.assign({}, selected, { thresholds });
}
