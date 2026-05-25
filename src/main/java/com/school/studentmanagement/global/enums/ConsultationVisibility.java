package com.school.studentmanagement.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConsultationVisibility {
    RESTRICTED("제한적 공개"),     // 작성자/담임/관리자만 열람 가능 (기본값)
    ALL_TEACHERS("전체 교사 공개"); // 모든 교사 열람 가능

    private final String description;

    // RESTRICTED <-> ALL_TEACHERS 토글
    public ConsultationVisibility toggle() {
        return this == RESTRICTED ? ALL_TEACHERS : RESTRICTED;
    }
}
