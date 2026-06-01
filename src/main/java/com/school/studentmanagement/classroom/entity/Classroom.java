package com.school.studentmanagement.classroom.entity;


import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "classrooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_classroom_year_semester_grade_class",
                columnNames = {"academic_year", "semester", "grade", "class_num"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classroom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear; // 연도

    @Column(nullable = false)
    private Integer semester; // 학기

    @Column(nullable = false)
    private Integer grade; // 학년

    @Column(name = "class_num", nullable = false)
    private Integer classNum; // 반

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeroom_teacher_id")
    private Teacher homeroomTeacher; // 담임 선생님

    @Builder
    public Classroom(Integer academicYear, Integer semester, Integer grade, Integer classNum, Teacher homeroomTeacher) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.grade = grade;
        this.classNum = classNum;
        this.homeroomTeacher = homeroomTeacher;
    }
}
