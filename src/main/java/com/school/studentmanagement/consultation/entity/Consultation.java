package com.school.studentmanagement.consultation.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
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
public class Consultation extends BaseTimeEntity {

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

    // 주요 내용 — 대용량 텍스트는 PostgreSQL TEXT로 매핑(@Lob은 oid/라지오브젝트가 되어 네이티브 SQL·문자열 함수에서 타입 문제 발생).
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 다음 상담 계획 (선택)
    @Column(columnDefinition = "TEXT")
    private String nextPlan;

    // 공개 범위 (기본값 RESTRICTED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationVisibility visibility;

    // createdAt/updatedAt은 BaseTimeEntity(JPA Auditing)가 관리

    private Consultation(Teacher teacher, Student student, LocalDateTime consultationDate,
                         String content, String nextPlan, ConsultationVisibility visibility) {
        this.teacher = teacher;
        this.student = student;
        this.consultationDate = consultationDate;
        this.content = content;
        this.nextPlan = nextPlan;
        this.visibility = visibility;
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
    }

    // 본문/일시/계획/공개범위 부분 갱신 — 작성자 본인만(서비스에서 검증). null 인자는 변경 없음.
    public void update(LocalDateTime consultationDate, String content, String nextPlan,
                       ConsultationVisibility visibility) {
        if (consultationDate != null) {
            this.consultationDate = consultationDate;
        }
        if (content != null) {
            this.content = content;
        }
        // nextPlan 은 의도적으로 빈 문자열 → null 로 비울 수 있도록 분기.
        if (nextPlan != null) {
            this.nextPlan = nextPlan.isBlank() ? null : nextPlan;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    // 작성자 본인 여부
    public boolean isAuthor(Long teacherId) {
        return this.teacher.getId().equals(teacherId);
    }
}
