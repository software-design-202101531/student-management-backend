package com.school.studentmanagement.record.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectRecordResponse {
    private Long recordId;
    private String content;
    private boolean canEdit;
}
