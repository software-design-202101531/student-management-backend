package com.school.studentmanagement.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    PRESENT("출결"),
    ABSENT("결석"),
    LATE("지각"),
    EARLY_LEAVE("조퇴");

    private final String description;
}
