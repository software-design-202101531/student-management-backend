package com.school.studentmanagement.affiliation.entity;


import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.user.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_affiliations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentAffiliation {

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
    public StudentAffiliation(Student student, Integer studentNum, Classroom classroom) {
        this.student = student;
        this.studentNum = studentNum;
        this.classroom = classroom;
    }
}
