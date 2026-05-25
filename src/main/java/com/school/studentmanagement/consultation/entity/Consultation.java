package com.school.studentmanagement.consultation.entity;

import com.school.studentmanagement.global.enums.ConsultationVisibility;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상담을 진행하고 기록한 작성자 교사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // 대상 학생
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // 상담 일시
    @Column(nullable = false)
    private LocalDateTime consultationDate;

    // 주요 내용
    @Lob
    @Column(nullable = false)
    private String content;

    // 다음 상담 계획 (선택)
    @Lob
    private String nextPlan;

    // 공개 범위 (기본값 RESTRICTED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationVisibility visibility;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Consultation(Teacher teacher, Student student, LocalDateTime consultationDate,
                         String content, String nextPlan, ConsultationVisibility visibility) {
        this.teacher = teacher;
        this.student = student;
        this.consultationDate = consultationDate;
        this.content = content;
        this.nextPlan = nextPlan;
        this.visibility = visibility;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // 생성 — visibility 미지정 시 기본값 RESTRICTED
    public static Consultation create(Teacher teacher, Student student, LocalDateTime consultationDate,
                                      String content, String nextPlan, ConsultationVisibility visibility) {
        return new Consultation(
                teacher, student, consultationDate, content, nextPlan,
                visibility != null ? visibility : ConsultationVisibility.RESTRICTED
        );
    }

    // 공개 범위 토글 (RESTRICTED <-> ALL_TEACHERS)
    public void toggleVisibility() {
        this.visibility = this.visibility.toggle();
        this.updatedAt = LocalDateTime.now();
    }

    // 작성자 본인 여부
    public boolean isAuthor(Long teacherId) {
        return this.teacher.getId().equals(teacherId);
    }
}
