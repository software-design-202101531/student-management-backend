package com.school.studentmanagement.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedbackStatus {
    DRAFT("임시저장"),
    PUBLISHED("발행 완료");

    private final String description;
}
