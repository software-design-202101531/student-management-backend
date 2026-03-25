package com.school.studentmanagement.invitation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParentRegisterRequest {
    private Long id;
    private String loginId;
    private String password;
    private String name;
}
