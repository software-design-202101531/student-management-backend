package com.school.studentmanagement.assignment.event;

// 과제 부여 도메인 이벤트. 학급 학생 대상 알림은 커밋 이후 비동기로 소비한다.
public record AssignmentCreatedEvent(Long assignmentId) {
}
