package com.school.studentmanagement.assignment.entity;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.user.entity.Teacher;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teacher_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 담당 선생님
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // 담당 반
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classroom classroom;

    // 담당 과목
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Builder
    private TeacherAssignment(Teacher teacher, Classroom classroom, Subject subject) {
        this.teacher = teacher;
        this.classroom = classroom;
        this.subject = subject;
    }
}
