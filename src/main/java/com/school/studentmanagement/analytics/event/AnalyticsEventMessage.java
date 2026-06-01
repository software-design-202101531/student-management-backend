package com.school.studentmanagement.analytics.event;

// RabbitMQ로 전달되는 분석 증분 적재 메시지(JSON). subjectId는 성적 등에서만 사용(없으면 null).
public record AnalyticsEventMessage(Long studentId, Long subjectId) {
}
