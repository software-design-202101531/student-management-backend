package com.school.studentmanagement.invitation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyParentRequest {
    private Integer year;
    private Integer grade;
    private Integer classNum;
    private Integer studentNum;
    private String studentName;
    private String parentPhone;
}
