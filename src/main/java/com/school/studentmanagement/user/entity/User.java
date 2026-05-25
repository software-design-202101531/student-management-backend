package com.school.studentmanagement.user.entity;


import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    @Column(unique = true, length = 50)
    private String loginId; // 로그인 id

    @Column(length = 255)
    private String password; // 로그인 비밀번호

    @Column(nullable = false, length = 50)
    private String name; // 사용자 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Gender gender; // 성별(남/여)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role; // 역할(학생/학부모/선생/관리자)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status; // 상태()

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 계정 활성화 변경
    public void activateAccount(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
        this.status = UserStatus.ACTIVE;
    }

    @Builder
    private User(Long id, String loginId, String password, String name, Gender gender, UserRole role, UserStatus status) {
        this.id = id;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role;
        this.gender = gender;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}
