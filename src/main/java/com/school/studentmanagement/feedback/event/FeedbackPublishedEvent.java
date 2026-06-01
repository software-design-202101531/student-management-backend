package com.school.studentmanagement.feedback.event;

// 피드백 발행 도메인 이벤트. 알림 등 다운스트림 처리는 커밋 이후 비동기로 소비한다.
public record FeedbackPublishedEvent(Long feedbackId) {
}
