package com.school.studentmanagement.student.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentActivationRequest {
    private Long id;
    private String loginId;
    private String password;
    private String address;
    private String phoneNumber;
}
