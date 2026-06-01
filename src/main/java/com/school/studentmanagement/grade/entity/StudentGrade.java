package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "student_grades",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_grade_student_exam_subject",
                        columnNames = {"student_id", "exam_id", "subject_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentGrade extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 낙관적 락 — 여러 교사가 같은 학생/시험/과목 성적을 동시 수정할 때의 갱신 손실(Lost Update) 방지.
    // 충돌 시 ObjectOptimisticLockingFailureException → GlobalExceptionHandler 가 409(RECORD_CONFLICT)로 변환.
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    // ABSENT 시 null. PRESENT/CHEATED 시 점수.
    @Column(name = "raw_score")
    private Integer rawScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 10)
    private ExamAttendanceStatus attendanceStatus;

    @Builder
    public StudentGrade(Student student, Exam exam, Subject subject,
                        Integer rawScore, ExamAttendanceStatus attendanceStatus) {
        this.student = student;
        this.exam = exam;
        this.subject = subject;
        ExamAttendanceStatus status = attendanceStatus != null ? attendanceStatus : ExamAttendanceStatus.PRESENT;
        this.attendanceStatus = status;
        this.rawScore = normalizeScore(rawScore, status);
    }

    public void update(Integer rawScore, ExamAttendanceStatus status) {
        ExamAttendanceStatus s = status != null ? status : ExamAttendanceStatus.PRESENT;
        this.attendanceStatus = s;
        this.rawScore = normalizeScore(rawScore, s);
    }

    // 단순 점수 변경 (status 유지) — 기존 호출처 호환용
    public void updateScore(Integer rawScore) {
        update(rawScore, this.attendanceStatus);
    }

    private static Integer normalizeScore(Integer raw, ExamAttendanceStatus status) {
        return switch (status) {
            case ABSENT -> null;
            case CHEATED, NOT_SUBMITTED -> 0;
            case PRESENT -> raw;
        };
    }
}
