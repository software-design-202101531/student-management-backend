package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.global.enums.ExamType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "exams",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_exam_year_semester_type",
                        columnNames = {"academic_year", "semester", "exam_type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(nullable = false)
    private Integer semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 10)
    private ExamType examType;

    @Builder
    public Exam(Integer academicYear, Integer semester, ExamType examType) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.examType = examType;
    }
}
