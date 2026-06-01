package com.school.studentmanagement.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyStudentRequest {
    @NotNull(message = "학년도는 필수입니다") private Integer academicYear;
    @NotNull(message = "학년은 필수입니다") private Integer grade;
    @NotNull(message = "반은 필수입니다") private Integer classNum;
    @NotNull(message = "번호는 필수입니다") private Integer studentNum;
    @NotBlank(message = "이름은 필수입니다") private String name;
}
