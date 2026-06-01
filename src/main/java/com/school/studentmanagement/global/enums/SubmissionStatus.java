package com.school.studentmanagement.global.enums;

// 과제 제출 상태. DB에는 SUBMITTED/LATE만 저장되고(제출 row 존재),
// NOT_SUBMITTED는 제출 row가 없을 때 응답에서만 사용한다(출결 PRESENT 미저장과 동일 패턴).
public enum SubmissionStatus {
    NOT_SUBMITTED,
    SUBMITTED,
    LATE
}
