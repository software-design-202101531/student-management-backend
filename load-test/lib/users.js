// 데모 시드 계정 풀.
//   ADMIN   : admin / admin1234!
//   TEACHER : teacher01~06 / test1234!  (teacher01=김국어, 전 반 국어 담당 + 1반 담임)
//   STUDENT : student01~25 / test1234!
//   PARENT  : parent01~25 / test1234!
// 비밀번호는 환경변수로 오버라이드 가능.

const TEACHER_PW = __ENV.TEACHER_PW || 'test1234!';
const STUDENT_PW = __ENV.STUDENT_PW || 'test1234!';
const ADMIN_PW   = __ENV.ADMIN_PW   || 'admin1234!';

const pad2 = (n) => String(n).padStart(2, '0');

export const ADMIN = { loginId: 'admin', password: ADMIN_PW };

export function teacher(n) {
  return { loginId: `teacher${pad2(n)}`, password: TEACHER_PW };
}
export function student(n) {
  return { loginId: `student${pad2(n)}`, password: STUDENT_PW };
}

export const TEACHER01 = teacher(1);

// VU 를 계정에 분산 매핑 — 단일 계정 토큰 편중을 피한다.
export function teacherForVU(vu) {
  return teacher(((vu - 1) % 6) + 1);   // teacher01~06
}
export function studentForVU(vu) {
  return student(((vu - 1) % 25) + 1);  // student01~25
}
