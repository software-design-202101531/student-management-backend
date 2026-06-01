package com.school.studentmanagement.assignment.entity;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 과제 — 과목 담당 교사가 특정 학급에 부여한다. 부여 학기는 classroom의 학년도/학기를 따른다.
@Entity
@Table(name = "assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Builder
    private Assignment(Classroom classroom, Subject subject, Teacher teacher,
                       String title, String description, LocalDateTime dueDate) {
        this.classroom = classroom;
        this.subject = subject;
        this.teacher = teacher;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
    }

    // 내용 수정 — 학급/과목/담당교사는 불변, 제목·설명·마감일만 변경한다.
    public void update(String title, String description, LocalDateTime dueDate) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
    }
}
