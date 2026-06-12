// 3.5 OLAP 적재 파이프라인 / 대시보드
// ------------------------------------------------------------------
// 두 가지를 본다:
//   (1) 대시보드 집계 조회(analytics 스키마) 부하 — student/subject overview
//   (2) [선택] end-to-end ETL lag 프로브 — 성적을 쓴 뒤 analytics 반영까지의 시간(etl_lag)
//       ETL_LAG_PROBE=1 일 때만 VU1 이 측정. 큐 적체/consumer lag 은 RabbitMQ(:15672)도 병행.
// 실행: TEST_TYPE=load k6 run s5_olap_dashboard.js
//      TEST_TYPE=smoke ETL_LAG_PROBE=1 k6 run s5_olap_dashboard.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, TARGET } from '../config/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { TEACHER01 } from '../lib/users.js';
import { etlLag, recordWrite } from '../lib/metrics.js';
import { buildOptions } from '../config/options.js';
import { READ } from '../config/thresholds.js';

export const options = buildOptions(READ);

export function setup() {
  const token = login(TEACHER01);
  const params = { headers: authHeaders(token) };

  const exams = http
    .get(`${BASE}/api/exams?academicYear=${TARGET.academicYear}&semester=${TARGET.semester}`, params)
    .json('data');
  if (!exams || exams.length === 0) throw new Error('대상 시험 없음 — 데모 시드 확인');
  const examId = exams[0].examId;

  const grades = http
    .get(`${BASE}/api/classrooms/${TARGET.classroomId}/subjects/${TARGET.subjectId}/grades?examId=${examId}`, params)
    .json('data.grades') || [];
  const studentIds = grades.filter((g) => g.studentId != null).map((g) => g.studentId);
  const firstGradeId = (grades.find((g) => g.gradeId != null) || {}).gradeId;
  if (studentIds.length === 0) throw new Error('학생 없음 — 시드 확인');

  return { token, examId, studentIds, firstGradeId };
}

export default function (data) {
  const headers = authHeaders(data.token);

  if (__ENV.ETL_LAG_PROBE && __VU === 1 && data.firstGradeId) {
    probeEtlLag(data, headers);
    return;
  }

  // 대시보드 집계 조회 부하
  const sid = data.studentIds[__ITER % data.studentIds.length];
  if (__ITER % 2 === 0) {
    const res = http.get(
      `${BASE}/api/analytics/students/${sid}/overview?academicYear=${TARGET.academicYear}&semester=${TARGET.semester}`,
      { headers, tags: { kind: 'read', name: 'analytics_student' } }
    );
    check(res, { 'analytics student 200': (r) => r.status === 200 });
  } else {
    const res = http.get(
      `${BASE}/api/analytics/subjects/${TARGET.subjectId}/overview?academicYear=${TARGET.academicYear}&semester=${TARGET.semester}`,
      { headers, tags: { kind: 'read', name: 'analytics_subject' } }
    );
    check(res, { 'analytics subject 200': (r) => r.status === 200 });
  }

  sleep(0.3);
}

// 성적 1건 수정 → analytics subject overview 의 응답이 바뀔 때까지 폴링하며 지연 측정.
// 집계값이 정확히 어떤 필드인지에 의존하지 않도록, 응답 바디 전체의 변화를 감지한다(best-effort).
function probeEtlLag(data, headers) {
  const subjUrl = `${BASE}/api/analytics/subjects/${TARGET.subjectId}/overview?academicYear=${TARGET.academicYear}&semester=${TARGET.semester}`;
  const before = http.get(subjUrl, { headers }).body;

  const newScore = 50 + (__ITER % 50);
  const putRes = http.put(
    `${BASE}/api/classrooms/${TARGET.classroomId}/subjects/${TARGET.subjectId}/grades/${data.firstGradeId}`,
    JSON.stringify({ rawScore: newScore, reason: 'etl-lag-probe' }),
    { headers, tags: { name: 'grade_update', kind: 'write' } }
  );
  recordWrite(putRes);

  const t0 = Date.now();
  let reflected = false;
  for (let i = 0; i < 40 && !reflected; i++) {
    const now = http.get(subjUrl, { headers }).body;
    reflected = now !== before;
    if (!reflected) sleep(0.25);
  }
  if (reflected) etlLag.add(Date.now() - t0);
  check(null, { 'etl reflected within window': () => reflected });
}

// 종료 시 HTML/JSON 리포트 생성 (CI 아티팩트)
export { handleSummary } from '../lib/summary.js';
