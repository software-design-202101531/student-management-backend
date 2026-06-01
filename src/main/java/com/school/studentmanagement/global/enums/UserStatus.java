package com.school.studentmanagement.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    PENDING("가입대기", false),
    ACTIVE("정상활성", true),
    INACTIVE("휴면 및 탈퇴", false);

    private final String description;
    private final boolean active;

    // 로그인/권한을 허용하는 활성 상태인지 여부
    public boolean isActive() {
        return this.active;
    }
}
