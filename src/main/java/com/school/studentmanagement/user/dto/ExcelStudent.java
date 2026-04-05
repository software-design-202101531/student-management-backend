package com.school.studentmanagement.user.dto;

import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.RelationType;
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
