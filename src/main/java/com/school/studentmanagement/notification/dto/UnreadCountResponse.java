package com.school.studentmanagement.notification.dto;

import lombok.Getter;

@Getter
public class UnreadCountResponse {

    private final long count;

    private UnreadCountResponse(long count) {
        this.count = count;
    }

    public static UnreadCountResponse of(long count) {
        return new UnreadCountResponse(count);
    }
}
