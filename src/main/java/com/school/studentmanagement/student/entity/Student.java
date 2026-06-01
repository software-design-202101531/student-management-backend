package com.school.studentmanagement.student.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.security.encryption.AesRandomStringConverter;
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
public class Student extends BaseTimeEntity {

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

    // PII — 컬럼 레벨 암호화(AES, 랜덤 IV). 검색 미사용.
    // 암호문 길이는 평문 hex의 2배 + IV/MAC 오버헤드 → 보수적으로 1024.
    @Convert(converter = AesRandomStringConverter.class)
    @Column(length = 1024)
    private String address; // 주소

    // PII — 컬럼 레벨 암호화(AES, 랜덤 IV). 검색 미사용.
    @Convert(converter = AesRandomStringConverter.class)
    @Column(length = 255)
    private String phoneNumber; // 휴대폰 번호

    @Column(length = 500)
    private String profileImageKey; // 프로필 사진의 스토리지(MinIO) 객체 key (조회 시 presigned URL 발급)

    @Column(nullable = false)
    private Integer enrollmentYear; // 입학연도

    @Builder
    public Student(Long id, User user, Teacher homeroomTeacher, String address, String phoneNumber, String profileImageKey, Integer enrollmentYear) {
        this.id = id;
        this.user = user;
        this.homeroomTeacher = homeroomTeacher;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.profileImageKey = profileImageKey;
        this.enrollmentYear = enrollmentYear;
    }

    public void activateStudentInfo(String address, String phoneNumber) {
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    // 활성화 이후 담임이 연락처를 부분 갱신 (null은 변경하지 않음)
    public void updateContactInfo(String address, String phoneNumber) {
        if (address != null) {
            this.address = address;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }

    // 프로필 사진 객체 key 교체
    public void updateProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
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
