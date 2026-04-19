package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.user.entity.Student;
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
public class StudentSemesterStat {

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

    @Column(nullable = false)
    private Integer totalScore;

    @Column(nullable = false)
    private Double averageScore;

    @Builder
    public StudentSemesterStat(Student student, Integer academicYear, Integer semester,
                               Integer totalScore, Double averageScore) {
        this.student = student;
        this.academicYear = academicYear;
        this.semester = semester;
        this.totalScore = totalScore;
        this.averageScore = averageScore;
    }

    // StudentGrade 추가/수정 시 서비스 레이어에서 호출
    public void updateStats(Integer totalScore, Double averageScore) {
        this.totalScore = totalScore;
        this.averageScore = averageScore;
    }
}
