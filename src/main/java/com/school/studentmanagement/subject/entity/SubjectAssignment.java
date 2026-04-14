package com.school.studentmanagement.subject.entity;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.user.entity.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import org.apache.logging.log4j.util.Lazy;

@Entity
@Table(name = "subject_assignments")
@Getter
public class SubjectAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false)
    private Integer academicYear;

    @Column(nullable = false)
    private Integer semester;
}
