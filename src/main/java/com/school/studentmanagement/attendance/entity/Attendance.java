package com.school.studentmanagement.attendance.entity;

import com.school.studentmanagement.global.enums.AttendanceStatus;
import com.school.studentmanagement.user.entity.Student;
import com.school.studentmanagement.user.entity.Teacher;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "attendances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 출결 대상(학생)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // 선생님
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // 출결 날짜
    @Column(nullable = false)
    private LocalDate date;

    // 출결 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    // 결석, 지각 사유
    @Column(length = 20)
    private String reason;

    @Builder
    public Attendance(Student student, Teacher teacher, LocalDate date, AttendanceStatus status, String reason) {
        this.student = student;
        this.teacher = teacher;
        this.date = date;
        this.status = status;
        this.reason = reason;
    }
}
