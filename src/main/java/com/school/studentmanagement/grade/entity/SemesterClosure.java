package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.global.enums.SemesterClosureMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "semester_closures",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_semester_closure_year_semester",
                        columnNames = {"academic_year", "semester"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SemesterClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(nullable = false)
    private Integer semester;

    // 마감 후 reopen하면 해당 row를 삭제하는 정책 — row 존재 자체가 CLOSED를 의미
    @Column(name = "closed_at", nullable = false, updatable = false)
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private SemesterClosureMethod method;

    // 마감 주체 스냅샷 (User 삭제되어도 이력 보존). AUTO일 땐 null 가능.
    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "closed_by_name", nullable = false, length = 50)
    private String closedByName;

    @Column(length = 200)
    private String reason;

    // 채워진 NOT_SUBMITTED row 개수 (감사·운영 정보)
    @Column(name = "filled_count", nullable = false)
    private Integer filledCount;

    @Builder
    public SemesterClosure(Integer academicYear, Integer semester, SemesterClosureMethod method,
                           Long closedByUserId, String closedByName, String reason, Integer filledCount) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.method = method;
        this.closedByUserId = closedByUserId;
        this.closedByName = closedByName;
        this.reason = reason;
        this.filledCount = filledCount != null ? filledCount : 0;
        this.closedAt = LocalDateTime.now();
    }
}
