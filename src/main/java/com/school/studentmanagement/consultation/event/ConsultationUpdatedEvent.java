package com.school.studentmanagement.consultation.event;

// 상담 내역 수정 도메인 이벤트. 알림 등 다운스트림 처리는 커밋 이후 비동기로 소비한다.
public record ConsultationUpdatedEvent(Long consultationId) {
}
