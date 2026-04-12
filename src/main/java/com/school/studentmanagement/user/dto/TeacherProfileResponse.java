package com.school.studentmanagement.user.dto;

import com.school.studentmanagement.global.enums.EmploymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherProfileResponse {

    // 선생님 기본 정보
    private String name;
    private String employeeNumber;
    private String subjectName;
    private EmploymentStatus employmentStatus;

    // 담임 여부
    private Boolean isHomeRoom;

    // 담임 세부 정보(isHomeRoom이 false라면 전부 null)
    private Long homeroomClassId;
    private Integer grade;
    private Integer classNum;
}
