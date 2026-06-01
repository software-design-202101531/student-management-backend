package com.school.studentmanagement.grade.event;

// 시험 성적 발행 도메인 이벤트. 알림 등 다운스트림 처리는 커밋 이후 비동기로 소비한다.
public record ExamPublishedEvent(Long examId) {
}
