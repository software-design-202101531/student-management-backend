package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.GradeLevel;
import com.school.studentmanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "student_semester_stats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stat_student_year_semester",
                        columnNames = {"student_id", "academic_year", "semester"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentSemesterStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(nullable = false)
    private Integer semester;

    // 학기 총점: 과목별 학기 점수의 단순 합 (가중치 적용 후 100점 환산값들의 합)
    @Column(nullable = false)
    private Double totalScore;

    // 학기 평균: 과목별 학기 점수의 단순 평균
    @Column(nullable = false)
    private Double averageScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = false, length = 1)
    private GradeLevel gradeLevel;

    @Builder
    public StudentSemesterStat(Student student, Integer academicYear, Integer semester,
                               Double totalScore, Double averageScore, GradeLevel gradeLevel) {
        this.student = student;
        this.academicYear = academicYear;
        this.semester = semester;
        this.totalScore = totalScore;
        this.averageScore = averageScore;
        this.gradeLevel = gradeLevel;
    }

    public void updateStats(Double totalScore, Double averageScore, GradeLevel gradeLevel) {
        this.totalScore = totalScore;
        this.averageScore = averageScore;
        this.gradeLevel = gradeLevel;
    }
}
