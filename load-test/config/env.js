// 공통 환경 설정 — 모든 값은 환경변수(__ENV)로 오버라이드 가능하다.
// 기본값은 데모 시드(app.demo.enabled=true) 기준이다.
//   classroom1 = 1학년, subject1 = 국어, teacher01(김국어)이 전 반 국어를 담당.

export const BASE = __ENV.BASE_URL || 'http://localhost:8080';

// 부하 대상 식별자
export const TARGET = {
  classroomId:   Number(__ENV.CLASSROOM_ID   || 1),
  subjectId:     Number(__ENV.SUBJECT_ID     || 1),
  academicYear:  Number(__ENV.ACADEMIC_YEAR  || 2026),
  semester:      Number(__ENV.SEMESTER       || 1),
  // 성적 동시성 시나리오에서 다수 VU가 몰릴 핫스팟 gradeId 개수.
  // 작을수록 @Version 락 경합이 심해진다.
  hotGradeCount: Number(__ENV.HOT_GRADE_COUNT || 5),
};

// 검색 시나리오용 학기 범위 (데모 시드: 2025-2 ~ 2026-1 존재)
export const SEARCH_RANGE = {
  fromYear:     Number(__ENV.FROM_YEAR     || 2025),
  fromSemester: Number(__ENV.FROM_SEMESTER || 1),
  toYear:       Number(__ENV.TO_YEAR       || 2026),
  toSemester:   Number(__ENV.TO_SEMESTER   || 2),
};
