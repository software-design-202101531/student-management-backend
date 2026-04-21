package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "student_grades",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_grade_student_exam_subject",
                        columnNames = {"student_id", "exam_id", "subject_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false)
    private Integer rawScore;

    @Builder
    public StudentGrade(Student student, Exam exam, Subject subject, Integer rawScore) {
        this.student = student;
        this.exam = exam;
        this.subject = subject;
        this.rawScore = rawScore;
    }

    public void updateScore(Integer rawScore) {
        this.rawScore = rawScore;
    }
}
