package com.school.studentmanagement.assignment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 교사 채점 요청 — 점수(0~100) + 선택 피드백
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionGradeRequest {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer score;

    @Size(max = 2000)
    private String feedback;
}
