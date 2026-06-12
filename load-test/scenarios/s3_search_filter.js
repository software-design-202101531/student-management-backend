// 3.3 검색·필터링 부하
// ------------------------------------------------------------------
// 기간(학기 범위)·과목 복합 조건 검색. 데이터가 누적될수록 무거워진다.
//   ⚠️ 데모 시드만으로는 2025-2 ~ 2026-1 두 학기뿐이라 "대용량 누적" 측정이 안 된다.
//      의미 있는 결과를 보려면 data/seed-bulk.sql 로 여러 학년도를 적재한 뒤 돌릴 것.
// 실행: TEST_TYPE=load k6 run s3_search_filter.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, TARGET, SEARCH_RANGE } from '../config/env.js';
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

  const grades = http
    .get(`${BASE}/api/classrooms/${TARGET.classroomId}/subjects/${TARGET.subjectId}/grades?examId=${exams[0].examId}`, params)
    .json('data.grades') || [];
  const studentIds = grades.filter((g) => g.studentId != null).map((g) => g.studentId);
  if (studentIds.length === 0) throw new Error('학생 없음 — 시드 확인');

  return { token, studentIds };
}

export default function (data) {
  const sid = data.studentIds[__ITER % data.studentIds.length];
  const q =
    `subjectId=${TARGET.subjectId}` +
    `&fromYear=${SEARCH_RANGE.fromYear}&fromSemester=${SEARCH_RANGE.fromSemester}` +
    `&toYear=${SEARCH_RANGE.toYear}&toSemester=${SEARCH_RANGE.toSemester}`;

  const res = http.get(
    `${BASE}/api/students/${sid}/grades/search?${q}`,
    { headers: authHeaders(data.token), tags: { kind: 'read', name: 'grade_search' } }
  );
  check(res, { 'search 200': (r) => r.status === 200 });

  sleep(0.3);
}

// 종료 시 HTML/JSON 리포트 생성 (CI 아티팩트)
export { handleSummary } from '../lib/summary.js';
