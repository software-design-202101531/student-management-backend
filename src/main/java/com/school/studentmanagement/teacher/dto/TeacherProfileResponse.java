package com.school.studentmanagement.teacher.dto;

import com.school.studentmanagement.global.enums.EmploymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherProfileResponse {
    private String name;
    private String employeeNumber;
    private String subjectName;
    private EmploymentStatus employmentStatus;
    private Long subjectId;
    private Boolean isHomeRoom;
    private Long homeroomClassId;
    private Integer grade;
    private Integer classNum;
}
