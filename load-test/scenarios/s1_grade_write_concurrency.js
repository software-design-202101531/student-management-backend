// 3.1 성적 입력 동시성 (최우선)
// ------------------------------------------------------------------
// 명세서가 명시적으로 요구하는 "다수 교사 동시 데이터 입력" 시나리오.
// 다수 VU 가 소수의 동일 gradeId 에 PUT 을 몰아 낙관적 락(@Version) 경합을 강제한다.
//   - 모든 VU 는 teacher01(국어 담당) 동일 토큰을 공유 → 동일 행 동시 수정 → 409 충돌 유발
//   - 409 는 실패가 아니라 grade_conflicts(관찰)로 집계, 합격 판정은 business_errors 로 한다.
//
// 실행: TEST_TYPE=stress BASE_URL=http://localhost:8080 k6 run s1_grade_write_concurrency.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, TARGET } from '../config/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { TEACHER01 } from '../lib/users.js';
import { recordWrite } from '../lib/metrics.js';
import { buildOptions } from '../config/options.js';
import { WRITE } from '../config/thresholds.js';

export const options = buildOptions(WRITE);

export function setup() {
  const token = login(TEACHER01);
  const params = { headers: authHeaders(token) };

  // 1) 대상 시험 선택 — 발행된 첫 시험
  const examsUrl = `${BASE}/api/exams?academicYear=${TARGET.academicYear}&semester=${TARGET.semester}`;
  const examsRes = http.get(examsUrl, params);
  const exams = examsRes.json('data');
  if (!exams || exams.length === 0) {
    throw new Error('대상 시험이 없습니다 — 데모 시드 적재(app.demo.enabled) 확인');
  }
  const examId = exams[0].examId;

  // 2) 핫스팟 gradeId 수집 — 다수 VU 가 경합할 소수의 행
  const gradesUrl = `${BASE}/api/classrooms/${TARGET.classroomId}/subjects/${TARGET.subjectId}/grades?examId=${examId}`;
  const gradesRes = http.get(gradesUrl, params);
  const grades = gradesRes.json('data.grades') || [];
  const hotGrades = grades
    .filter((g) => g.gradeId != null)
    .map((g) => g.gradeId)
    .slice(0, TARGET.hotGradeCount);
  if (hotGrades.length === 0) {
    throw new Error('경합할 성적이 없습니다 — 성적 입력 시드 확인');
  }

  return { token, examId, hotGrades };
}

export default function (data) {
  const headers = authHeaders(data.token);
  // 의도적 핫스팟 집중: 소수 gradeId 에만 PUT
  const gradeId = data.hotGrades[__ITER % data.hotGrades.length];
  const body = JSON.stringify({
    rawScore: 60 + (__VU % 40),
    reason: `load-test vu${__VU}`,
  });

  const res = http.put(
    `${BASE}/api/classrooms/${TARGET.classroomId}/subjects/${TARGET.subjectId}/grades/${gradeId}`,
    body,
    { headers, tags: { name: 'grade_update', kind: 'write' } }
  );

  recordWrite(res);
  check(res, {
    'update 200 or 409(conflict)': (r) => r.status === 200 || r.status === 409,
  });

  sleep(0.5);
}

// 부하 종료 후 정합성 보정 검증:
// 핫 gradeId 최종 상태가 정상적으로 조회되는지(데드락/손상 없이) 확인한다.
export function teardown(data) {
  const params = { headers: authHeaders(data.token) };
  const url = `${BASE}/api/classrooms/${TARGET.classroomId}/subjects/${TARGET.subjectId}/grades?examId=${data.examId}`;
  const res = http.get(url, params);
  check(res, { 'final read 200': (r) => r.status === 200 });
}

// 종료 시 HTML/JSON 리포트 생성 (CI 아티팩트)
export { handleSummary } from '../lib/summary.js';
