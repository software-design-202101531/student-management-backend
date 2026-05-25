package com.school.studentmanagement.grade.dto;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CloseSemesterRequest {

    @Size(max = 200)
    private String reason;

    @Builder
    public CloseSemesterRequest(String reason) {
        this.reason = reason;
    }
}
