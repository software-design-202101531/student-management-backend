package com.school.studentmanagement.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ActivateAccountRequest {
    Long id;
    @NotBlank(message = "사용할 아이디를 입력하세요") String loginId;
    @NotBlank(message = "사용할 비밀번호를 입력하세요") String password;
    // @Size등의 설정도 기준을 마련하면 추가할 것
}
