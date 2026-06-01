package com.school.studentmanagement.classroom.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "student_affiliations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_affiliation_student_classroom",
                columnNames = {"student_id", "classroom_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentAffiliation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(nullable = false)
    private Integer studentNum;

    @Builder
    public StudentAffiliation(Student student, Classroom classroom, Integer studentNum) {
        this.student = student;
        this.classroom = classroom;
        this.studentNum = studentNum;
    }
}
