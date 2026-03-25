package com.school.studentmanagement.user.entity;

import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parent {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @MapsId // 핵심: User ID와 일치 시킨다
    @JoinColumn(name = "id") // 그리고 그 컬럼의 이름은 id이다
    private User user;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private RelationType relationType;

    // 비즈니스 생성자 User 엔티티를 함께 생성
    public static Parent createParentIdentity(
            String loginId,
            String password,
            String name,
            String phoneNumber,
            RelationType relationType
    ) {
        // User 생성
        User user = User.builder()
                .loginId(loginId)
                .password(password)
                .name(name)
                .role(UserRole.PARENT)
                .status(UserStatus.ACTIVE)
                .build();

        return new Parent(user,phoneNumber, relationType);
    }

    private Parent(User user, String phoneNumber, RelationType relationType) {
        this.user = user;
        this.phoneNumber = phoneNumber;
        this.relationType = relationType;
    }
}
