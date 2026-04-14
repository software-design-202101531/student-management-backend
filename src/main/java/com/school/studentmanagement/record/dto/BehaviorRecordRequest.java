package com.school.studentmanagement.record.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BehaviorRecordRequest {

    @NotBlank(message = "행특 내용은 비어있을 수 없음")
    private String content;
}
