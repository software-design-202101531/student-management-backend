package com.school.studentmanagement.invitation.entity;

import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.user.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "parent_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParentInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelationType relationType;

    @Column(nullable = false)
    private LocalDate createdAt;

    @Builder
    public ParentInvitation(Student student, String phoneNumber, RelationType relationType) {
        this.student = student;
        this.phoneNumber = phoneNumber;
        this.relationType = relationType;
        this.createdAt = LocalDate.now();
    }
}
