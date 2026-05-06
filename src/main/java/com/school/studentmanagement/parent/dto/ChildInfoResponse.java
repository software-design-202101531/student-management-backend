package com.school.studentmanagement.parent.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChildInfoResponse {

    private Long studentId;
    private String name;
    private Integer academicYear;
    private Integer semester;
    private Integer grade;      // 학년 (학급 미배정 시 null)
    private Integer classNum;   // 반 (학급 미배정 시 null)
    private Integer studentNum; // 번호 (학급 미배정 시 null)
}
