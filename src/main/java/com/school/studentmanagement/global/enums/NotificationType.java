package com.school.studentmanagement.global.enums;

// 알림 종류 — 발행 이벤트 단위로 구분한다.
public enum NotificationType {
    GRADE_PUBLISHED,        // 시험 성적 발행
    FEEDBACK_PUBLISHED,     // 공개 피드백 발행
    ASSIGNMENT_CREATED,     // 과제 부여
    ASSIGNMENT_GRADED,      // 과제 채점 완료
    CONSULTATION_CREATED,   // 상담 내역 등록 (담임 교사 대상)
    CONSULTATION_UPDATED    // 상담 내역 수정 (담임 교사 대상)
}
