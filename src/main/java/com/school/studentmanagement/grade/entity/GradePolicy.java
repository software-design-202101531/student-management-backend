package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.global.enums.GradeLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grade_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active;

    // 5단계 성취평가 컷오프 (E는 나머지)
    @Column(name = "a_min_score", nullable = false)
    private Double aMinScore;

    @Column(name = "b_min_score", nullable = false)
    private Double bMinScore;

    @Column(name = "c_min_score", nullable = false)
    private Double cMinScore;

    @Column(name = "d_min_score", nullable = false)
    private Double dMinScore;

    @Builder
    public GradePolicy(String name, boolean active,
                       Double aMinScore, Double bMinScore, Double cMinScore, Double dMinScore) {
        this.name = name;
        this.active = active;
        this.aMinScore = aMinScore;
        this.bMinScore = bMinScore;
        this.cMinScore = cMinScore;
        this.dMinScore = dMinScore;
    }

    public GradeLevel evaluate(double averageScore) {
        if (averageScore >= aMinScore) return GradeLevel.A;
        if (averageScore >= bMinScore) return GradeLevel.B;
        if (averageScore >= cMinScore) return GradeLevel.C;
        if (averageScore >= dMinScore) return GradeLevel.D;
        return GradeLevel.E;
    }

    public void activate() { this.active = true; }
    public void deactivate() { this.active = false; }
}
