// 3.2 조회 트래픽 스파이크
// ------------------------------------------------------------------
// 성적 발표 직후 조회가 짧은 시간에 몰리는 상황. 캐시 미적용 경로라 DB 직격 →
// HikariCP 커넥션 풀(기본 10) 포화와 p95 유지 여부를 본다.
//   - 과목 성적 목록 조회 + 학생 종합 뷰(overview) 를 섞어 부하
// 실행: TEST_TYPE=spike k6 run s2_read_spike.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, TARGET } from '../config/env.js';
import { login, authHeaders } from '../lib/auth.js';
import { TEACHER01 } from '../lib/users.js';
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
  if (studentIds.length === 0) throw new Error('학생 없음 — 시드 확인');

  return { token, examId, studentIds };
}

export default function (data) {
  const params = { headers: authHeaders(data.token), tags: { kind: 'read' } };

  if (__ITER % 2 === 0) {
    // (a) 과목 성적 목록
    const res = http.get(
      `${BASE}/api/classrooms/${TARGET.classroomId}/subjects/${TARGET.subjectId}/grades?examId=${data.examId}`,
      Object.assign({}, params, { tags: { kind: 'read', name: 'grade_list' } })
    );
    check(res, { 'grade_list 200': (r) => r.status === 200 });
  } else {
    // (b) 학생 종합 뷰
    const sid = data.studentIds[__ITER % data.studentIds.length];
    const res = http.get(
      `${BASE}/api/teachers/students/${sid}/grades/overview?academicYear=${TARGET.academicYear}&semester=${TARGET.semester}`,
      Object.assign({}, params, { tags: { kind: 'read', name: 'student_overview' } })
    );
    check(res, { 'overview 200': (r) => r.status === 200 });
  }

  sleep(0.3);
}

// 종료 시 HTML/JSON 리포트 생성 (CI 아티팩트)
export { handleSummary } from '../lib/summary.js';
