package com.school.studentmanagement.subject.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectRecorndResponse {
    // 과세특 조회 응답 DTO
    private Long recordId;
    private String content;
    private boolean canEdit; // 마감여부
}
