package com.school.studentmanagement.user.dto;

import com.school.studentmanagement.global.enums.RelationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExcelStudent {
    private int enrollmentYear;
    private int grade;
    private int classNum;
    private int studentNum;
    private String studentName;
    private String fatherPhone;
    private String motherPhone;
}
