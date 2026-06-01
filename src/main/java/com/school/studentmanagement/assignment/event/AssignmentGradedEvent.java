package com.school.studentmanagement.assignment.event;

// 과제 채점 도메인 이벤트. 대상 학생 알림은 커밋 이후 비동기로 소비한다.
public record AssignmentGradedEvent(Long submissionId) {
}
