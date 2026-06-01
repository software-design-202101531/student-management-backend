package com.school.studentmanagement.classroom.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentListResponse {
    private Long studentId;
    private Integer studentNum;
    private String name;
    private String profileImageUrl; // presigned URL (미등록 시 null)
}
