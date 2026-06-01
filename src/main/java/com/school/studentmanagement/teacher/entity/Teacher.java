package com.school.studentmanagement.teacher.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.security.encryption.AesDeterministicStringConverter;
import com.school.studentmanagement.global.security.encryption.AesRandomStringConverter;
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
public class Teacher extends BaseTimeEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // PII + 식별자 — 컬럼 레벨 암호화(deterministic). unique 제약 유지 필요로 같은 평문→같은 암호문.
    @Convert(converter = AesDeterministicStringConverter.class)
    @Column(unique = true, nullable = false, length = 255)
    private String employeeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 50)
    private String officeLocation;

    // PII — 컬럼 레벨 암호화(랜덤 IV). 검색 미사용.
    @Convert(converter = AesRandomStringConverter.class)
    @Column(nullable = false, length = 255)
    private String officePhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmploymentStatus employmentStatus;

    @Column(length = 500)
    private String profileImageKey; // 프로필 사진의 스토리지(MinIO) 객체 key (조회 시 presigned URL 발급)

    @Builder
    private Teacher(User user, String employeeNumber, Subject subject, String officeLocation, String officePhoneNumber, EmploymentStatus employmentStatus) {
        this.user = user;
        this.employeeNumber = employeeNumber;
        this.subject = subject;
        this.officeLocation = officeLocation;
        this.officePhoneNumber = officePhoneNumber;
        this.employmentStatus = employmentStatus;
    }

    // 프로필 사진 객체 key 교체
    public void updateProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }
}
