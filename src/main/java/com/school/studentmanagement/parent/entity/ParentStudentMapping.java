package com.school.studentmanagement.parent.entity;

import com.school.studentmanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parent_student_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParentStudentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    public ParentStudentMapping(Parent parent, Student student) {
        this.parent = parent;
        this.student = student;
    }
}
