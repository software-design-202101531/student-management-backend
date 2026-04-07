package com.school.studentmanagement.user.entity;

import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teachers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Teacher {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, nullable = false, length = 50)
    private String employeeNumber; // 교번

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject; // 테이블 생성 후 변경 예정

    @Column(nullable = false, length = 50)
    private String officeLocation; // 소속 위치

    @Column(nullable = false, length = 50)
    private String officePhoneNumber; // 사무실 내선 번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmploymentStatus employmentStatus;

    @Builder
    private Teacher(User user, String employeeNumber, Subject subject, String officeLocation, String officePhoneNumber, EmploymentStatus employmentStatus) {
        this.user = user;
        this.employeeNumber = employeeNumber;
        this.subject = subject;
        this.officeLocation = officeLocation;
        this.officePhoneNumber = officePhoneNumber;
        this.employmentStatus = employmentStatus;
    }
}
