package com.school.studentmanagement.assignment.entity;

import com.school.studentmanagement.global.entity.BaseTimeEntity;
import com.school.studentmanagement.global.enums.SubmissionStatus;
import com.school.studentmanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 과제 제출 — 학생당 과제 1건(유니크). 미제출은 row 미존재로 표현한다.
@Entity
@Table(
        name = "submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submission_assignment_student",
                columnNames = {"assignment_id", "student_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    // 채점 결과 — 미채점이면 null. (점수 부여 시점에 함께 피드백 작성 가능)
    @Column(name = "score")
    private Integer score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Builder
    private Submission(Assignment assignment, Student student, String content,
                       LocalDateTime submittedAt, SubmissionStatus status) {
        this.assignment = assignment;
        this.student = student;
        this.content = content;
        this.submittedAt = submittedAt;
        this.status = status;
    }

    // 재제출 — 내용/제출시각/상태 갱신. 재제출하면 기존 채점은 무효화한다.
    public void resubmit(String content, LocalDateTime submittedAt, SubmissionStatus status) {
        this.content = content;
        this.submittedAt = submittedAt;
        this.status = status;
        this.score = null;
        this.feedback = null;
    }

    // 채점 — 점수/피드백 기록
    public void grade(Integer score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }

    public boolean isGraded() {
        return score != null;
    }
}
