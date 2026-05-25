package com.school.studentmanagement.global.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserStatus {
    PENDING("가입대기", false),
    ACTIVE("정상활성", true),
    INACTIVE("휴먼 및 탈퇴", false);

    private final String description;
    private final boolean active;


}
