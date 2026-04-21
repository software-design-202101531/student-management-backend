package com.school.studentmanagement.record.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubjectRecordRequest {
    @NotBlank(message = "세특 내용은 비어있을 수 없습니다")
    private String content;
}
