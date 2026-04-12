package com.school.studentmanagement.classroom.dto;

import com.school.studentmanagement.user.entity.Student;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeroomStudentResponse {
    private Long studentId;
    private Integer studentNum;
    private String name;
    private String gender;
    // 프로필 이미지 url도 나중에 추가할 것
}
