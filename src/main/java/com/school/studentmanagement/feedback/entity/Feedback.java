package com.school.studentmanagement.feedback.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.FeedbackCategory;
import com.school.studentmanagement.global.enums.FeedbackStatus;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자(교사)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // 대상(학생)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // 분류 (성적/행동/출결/태도/기타)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackCategory category;

    // 본문 (글자 수 제한 없는 대용량 텍스트) — TEXT로 매핑(@Lob은 oid가 되어 네이티브 SQL·문자열 함수에서 타입 문제 발생).
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 작성 상태 (임시저장/발행 완료) — 데이터 생명주기
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status;

    // 공개 여부 (true: 학생/학부모 공개 가능, false: 교사 전용) — 노출 권한
    @Column(nullable = false)
    private boolean isPublic;

    // createdAt/updatedAt은 BaseTimeEntity(JPA Auditing)가 관리

    private Feedback(Teacher teacher, Student student, FeedbackCategory category, String content, boolean isPublic) {
        this.teacher = teacher;
        this.student = student;
        this.category = category;
        this.content = content;
        this.status = FeedbackStatus.DRAFT; // 최초 작성 시 항상 임시저장 상태
        this.isPublic = isPublic;
    }

    // 생성 (status = DRAFT 고정)
    public static Feedback create(Teacher teacher, Student student, FeedbackCategory category, String content, boolean isPublic) {
        return new Feedback(teacher, student, category, content, isPublic);
    }

    // 본문/분류/공개옵션 수정
    public void update(FeedbackCategory category, String content, boolean isPublic) {
        this.category = category;
        this.content = content;
        this.isPublic = isPublic;
    }

    // 최종 발행 (DRAFT -> PUBLISHED)
    public void publish() {
        this.status = FeedbackStatus.PUBLISHED;
    }

    // 작성자 본인 여부 검증용
    public boolean isAuthor(Long teacherId) {
        return this.teacher.getId().equals(teacherId);
    }

    public boolean isPublished() {
        return this.status == FeedbackStatus.PUBLISHED;
    }
}
