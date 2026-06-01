package com.school.studentmanagement.user.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    @Column(unique = true, length = 50)
    private String loginId; // 로그인 id (PENDING 상태에서는 null)

    @JsonIgnore
    @Column(length = 255)
    private String password; // 로그인 비밀번호 (암호화 저장, PENDING 상태에서는 null)

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
    private UserStatus status; // 계정 상태

    @Builder
    private User(Long id, String loginId, String password, String name, Gender gender, UserRole role, UserStatus status) {
        this.id = id;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role;
        this.gender = gender;
        this.status = status;
    }

    // 자격증명 없이 가입 대기(PENDING) 상태의 유저 생성
    public static User createPending(String name, Gender gender, UserRole role) {
        return User.builder()
                .name(name)
                .gender(gender)
                .role(role)
                .status(UserStatus.PENDING)
                .build();
    }

    // 자격증명을 갖춘 활성(ACTIVE) 유저 생성 (password는 암호화된 값을 전달)
    public static User createActive(String loginId, String encodedPassword, String name, Gender gender, UserRole role) {
        return User.builder()
                .loginId(loginId)
                .password(encodedPassword)
                .name(name)
                .gender(gender)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // PENDING 계정에 자격증명을 부여하고 활성화 (password는 암호화된 값을 전달)
    public void activateAccount(String loginId, String encodedPassword) {
        if (this.status != UserStatus.PENDING) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVE);
        }
        this.loginId = loginId;
        this.password = encodedPassword;
        this.status = UserStatus.ACTIVE;
    }

    // 계정이 활성 상태인지 여부
    public boolean isActive() {
        return this.status.isActive();
    }
}
