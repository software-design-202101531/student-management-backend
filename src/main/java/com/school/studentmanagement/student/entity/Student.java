package com.school.studentmanagement.student.entity;

import com.school.studentmanagement.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private Integer enrollmentYear;

    @Builder
    public Student(Long id, User user, String address, String phoneNumber, String profileImageUrl, Integer enrollmentYear) {
        this.id = id;
        this.user = user;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
        this.enrollmentYear = enrollmentYear;
    }

    public void activateStudentInfo(String address, String phoneNumber) {
        this.address = address;
        this.phoneNumber = phoneNumber;
    }
}
