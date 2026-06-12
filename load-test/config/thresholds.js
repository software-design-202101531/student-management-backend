// 합격/불합격을 자동 판정하는 공통 임계값 모음.
// 값은 출발점이며 실제 학교 규모·목표 동시 사용자 수에 맞춰 조정한다.
//
// ⚠️ 핵심: 쓰기(성적 입력) 시나리오는 http_req_failed 를 합격 기준으로 쓰면 안 된다.
//    낙관적 락 충돌(409)은 정상적인 비즈니스 결과인데 k6 는 status>=400 을
//    기본적으로 실패로 집계하기 때문이다. 대신 business_errors(=200/409 외)로 판정한다.
//    (lib/metrics.js 의 recordWrite() 참고)

// 읽기 시나리오: 표준 실패율 + 응답시간
export const READ = {
  http_req_failed: ['rate<0.01'],
  'http_req_duration{kind:read}': ['p(95)<500', 'p(99)<2000'],
  checks: ['rate>0.99'],
};

// 쓰기 시나리오: 409 를 제외한 진짜 실패율로 판정 + 충돌률 관찰
export const WRITE = {
  business_errors: ['rate<0.01'],     // 200/409 외 응답 = 진짜 실패
  grade_conflicts: ['rate<0.30'],     // 409 충돌률 관찰 기준선(과하면 설계 재검토 신호)
  'http_req_duration{kind:write}': ['p(95)<1000', 'p(99)<2000'],
  checks: ['rate>0.99'],
};

// 인증 시나리오
export const AUTH = {
  http_req_failed: ['rate<0.01'],
  'http_req_duration{name:login}': ['p(95)<800'],
  checks: ['rate>0.99'],
};
