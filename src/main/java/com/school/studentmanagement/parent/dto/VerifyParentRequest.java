package com.school.studentmanagement.parent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyParentRequest {

    @NotNull(message = "학년도는 필수입니다")
    private Integer year;

    @NotNull(message = "학년은 필수입니다")
    private Integer grade;

    @NotNull(message = "반은 필수입니다")
    private Integer classNum;

    @NotNull(message = "번호는 필수입니다")
    private Integer studentNum;

    @NotBlank(message = "학생 이름은 필수입니다")
    private String studentName;

    @NotBlank(message = "학부모 휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^01[016-9]\\d{7,8}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String parentPhone;
}
