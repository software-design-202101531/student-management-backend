package com.school.studentmanagement.classroom.entity;


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
    private Integer academicYear;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false)
    private Integer grade;

    @Column(nullable = false)
    private Integer classNum;

    @Builder
    public Classroom(Integer academicYear, Integer semester, Integer grade, Integer classNum) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.grade = grade;
        this.classNum = classNum;
    }
}
