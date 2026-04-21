package com.school.studentmanagement.student.dto;

import com.school.studentmanagement.global.enums.Gender;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExcelStudent {
    private Integer enrollmentYear;
    private Integer grade;
    private Integer classNum;
    private Integer studentNum;
    private Gender gender;
    private String studentName;
    private String fatherPhone;
    private String motherPhone;
}
