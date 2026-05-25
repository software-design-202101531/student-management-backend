package com.school.studentmanagement.student.entity;

import com.school.studentmanagement.teacher.entity.Teacher;
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
    private Long id; // 기본키

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user; // 외래키

    // 담임 교사 — 상담/기록 등의 권한 검증에 사용 (미배정 시 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeroom_teacher_id")
    private Teacher homeroomTeacher;

    @Column(length = 255)
    private String address; // 주소

    @Column(length = 20)
    private String phoneNumber; // 휴대폰 번호

    @Column(length = 500)
    private String profileImageUrl; // 사진 url

    @Column(nullable = false)
    private Integer enrollmentYear; // 입학연도

    @Builder
    public Student(Long id, User user, Teacher homeroomTeacher, String address, String phoneNumber, String profileImageUrl, Integer enrollmentYear) {
        this.id = id;
        this.user = user;
        this.homeroomTeacher = homeroomTeacher;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
        this.enrollmentYear = enrollmentYear;
    }

    public void activateStudentInfo(String address, String phoneNumber) {
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    // 담임 교사 배정/변경
    public void assignHomeroomTeacher(Teacher homeroomTeacher) {
        this.homeroomTeacher = homeroomTeacher;
    }

    // 해당 교사가 이 학생의 담임인지 여부
    public boolean isHomeroomTeacher(Long teacherId) {
        return this.homeroomTeacher != null && this.homeroomTeacher.getId().equals(teacherId);
    }
}
