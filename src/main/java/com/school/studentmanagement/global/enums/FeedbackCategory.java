package com.school.studentmanagement.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedbackCategory {
    GRADE("성적"),
    BEHAVIOR("행동"),
    ATTENDANCE("출결"),
    ATTITUDE("태도"),
    ETC("기타");

    private final String description;
}
