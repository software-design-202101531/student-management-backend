package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.ExamType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "exams",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_exam_year_semester_type_name",
                        columnNames = {"academic_year", "semester", "exam_type", "name"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(nullable = false)
    private Integer semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 20)
    private ExamType examType;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(nullable = false)
    private Double weight;

    @Column(name = "exam_date")
    private LocalDate examDate;

    // 시험 범위 (예: "1단원 ~ 3단원, 부록 A 포함")
    @Column(length = 500)
    private String coverage;

    @Column(nullable = false)
    private boolean published;

    @Builder
    public Exam(Integer academicYear, Integer semester, ExamType examType, String name,
                Integer maxScore, Double weight, LocalDate examDate, String coverage, Boolean published) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.examType = examType;
        this.name = name;
        this.maxScore = maxScore != null ? maxScore : 100;
        this.weight = weight != null ? weight : 0.0;
        this.examDate = examDate;
        this.coverage = coverage;
        this.published = published != null && published;
    }

    public void publish() { this.published = true; }
    public void unpublish() { this.published = false; }
}
