package com.school.studentmanagement.teacher.entity;

import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.user.entity.User;
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
    private String employeeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, length = 50)
    private String officeLocation;

    @Column(nullable = false, length = 50)
    private String officePhoneNumber;

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
