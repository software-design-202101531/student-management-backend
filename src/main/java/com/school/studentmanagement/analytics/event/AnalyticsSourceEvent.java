package com.school.studentmanagement.analytics.event;

// 운영 도메인이 발행하는 Spring 애플리케이션 이벤트. 커밋 이후(AnalyticsEventRelay)에 RabbitMQ로 중계된다.
public record AnalyticsSourceEvent(String routingKey, AnalyticsEventMessage message) {
}
