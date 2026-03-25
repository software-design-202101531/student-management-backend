package com.school.studentmanagement.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyStudentRequest {
    @NotNull(message = "학년도는 필수입니다")Integer academicYear;
    @NotNull(message = "학년은 필수입니다") Integer grade;
    @NotNull(message = "반은 필수입니다") Integer classNum;
    @NotNull(message = "번호는 필수입니다") Integer studentNum;
    @NotNull(message = "이름은 필수입니다") String name;
}
