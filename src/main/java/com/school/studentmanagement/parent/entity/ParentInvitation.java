package com.school.studentmanagement.parent.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.security.encryption.AesDeterministicStringConverter;
import com.school.studentmanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parent_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParentInvitation extends BaseTimeEntity {

    // 초대장 유효기간(일). 발급 후 이 기간이 지나면 검증/가입에 사용할 수 없다.
    public static final long INVITATION_VALID_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // PII — 학부모 가입 검증에서 exact match 매칭(findValidInvitation)에 사용 → deterministic 필요.
    @Convert(converter = AesDeterministicStringConverter.class)
    @Column(nullable = false, length = 255)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelationType relationType;

    // 발급 시각(createdAt)은 BaseTimeEntity(JPA Auditing)가 관리하며, 만료 검증(INVITATION_VALID_DAYS)에 사용된다.

    @Builder
    public ParentInvitation(Student student, String phoneNumber, RelationType relationType) {
        this.student = student;
        this.phoneNumber = phoneNumber;
        this.relationType = relationType;
    }
}
