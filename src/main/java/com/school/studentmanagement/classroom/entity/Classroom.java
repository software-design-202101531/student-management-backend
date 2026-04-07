package com.school.studentmanagement.classroom.entity;


import com.school.studentmanagement.user.entity.Teacher;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "classrooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer academicYear; // 연도

    @Column(nullable = false)
    private Integer semester; // 학기

    @Column(nullable = false)
    private Integer grade; // 학년

    @Column(nullable = false)
    private Integer classNum; // 반

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeroom_teacher_id")
    private Teacher homeroomTeacher; // 담임 선생님

    @Builder
    public Classroom(Integer academicYear, Integer semester, Integer grade, Integer classNum) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.grade = grade;
        this.classNum = classNum;
    }
}
