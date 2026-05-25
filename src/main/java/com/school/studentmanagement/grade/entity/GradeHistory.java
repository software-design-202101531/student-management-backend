package com.school.studentmanagement.grade.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "grade_histories",
        indexes = {
                @Index(name = "idx_history_grade_id", columnList = "student_grade_id, changed_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_grade_id", nullable = false)
    private StudentGrade studentGrade;

    @Column(name = "before_score")
    private Integer beforeScore;   // 신규 입력 또는 ABSENT 시 null

    @Column(name = "after_score")
    private Integer afterScore;    // ABSENT 변경 시 null

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @Column(name = "changed_by_name", nullable = false, length = 50)
    private String changedByName;

    @Column(length = 200)
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Builder
    public GradeHistory(StudentGrade studentGrade, Integer beforeScore, Integer afterScore,
                        Long changedByUserId, String changedByName, String reason) {
        this.studentGrade = studentGrade;
        this.beforeScore = beforeScore;
        this.afterScore = afterScore;
        this.changedByUserId = changedByUserId;
        this.changedByName = changedByName;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }
}
