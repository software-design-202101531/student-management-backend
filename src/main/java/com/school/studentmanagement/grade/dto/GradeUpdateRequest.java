package com.school.studentmanagement.grade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeUpdateRequest {

    @NotNull
    @Min(0) @Max(100)
    private Integer rawScore;

    @Builder
    public GradeUpdateRequest(Integer rawScore) {
        this.rawScore = rawScore;
    }
}
