package com.school.studentmanagement.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentActivationRequest {

    // 신원 재검증용 정보 (서버가 이 정보로 가입대기 학생을 직접 찾으므로 클라이언트가 보낸 PK는 신뢰하지 않는다)
    @NotNull(message = "학년도는 필수입니다")
    private Integer academicYear;

    @NotNull(message = "학년은 필수입니다")
    private Integer grade;

    @NotNull(message = "반은 필수입니다")
    private Integer classNum;

    @NotNull(message = "번호는 필수입니다")
    private Integer studentNum;

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    // 계정 자격증명
    @NotBlank(message = "로그인 아이디는 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문/숫자 4~20자여야 합니다")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$",
            message = "비밀번호는 영문/숫자/특수문자를 포함한 8~20자여야 합니다")
    private String password;

    // 학생 상세 정보
    @NotBlank(message = "주소는 필수입니다")
    private String address;

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^01[016-9]\\d{7,8}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;
}
