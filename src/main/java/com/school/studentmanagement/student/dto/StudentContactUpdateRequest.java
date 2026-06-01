package com.school.studentmanagement.student.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 담임이 학생의 연락처(주소/전화)를 부분 갱신할 때 사용.
 * 두 필드 모두 선택적. null이면 해당 항목은 변경하지 않는다(엔티티 메서드가 부분 업데이트).
 */
@Getter
@NoArgsConstructor
public class StudentContactUpdateRequest {

    @Size(max = 255, message = "주소는 최대 255자입니다")
    private String address;

    @Pattern(regexp = "^01[016-9]\\d{7,8}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;
}
