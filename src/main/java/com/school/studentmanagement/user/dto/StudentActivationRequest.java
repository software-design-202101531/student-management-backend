package com.school.studentmanagement.user.dto;

import com.school.studentmanagement.global.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentActivationRequest { // 사진 입력 기능도 추후 추가할 것 lmgBB
    private Long id;
    private String loginId;
    private String password;
    private String address;
    private String phoneNumber;
    // @Size등의 설정도 기준을 마련하면 추가할 것
}
