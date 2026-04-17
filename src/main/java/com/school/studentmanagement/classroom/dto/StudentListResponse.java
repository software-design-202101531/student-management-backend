package com.school.studentmanagement.classroom.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentListResponse {
    private Long studentId;
    private Integer studentNum;
    private String name;
    // 프로필 이미지 url도 나중에 추가할 것
}
