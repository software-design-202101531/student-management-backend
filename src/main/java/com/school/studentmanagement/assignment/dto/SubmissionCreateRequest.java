package com.school.studentmanagement.assignment.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubmissionCreateRequest {

    // 제출 내용(선택). 핵심은 제출 행위/시각 기록.
    private String content;
}
