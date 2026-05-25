package com.school.studentmanagement.grade.entity;

import com.school.studentmanagement.global.enums.GradeLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GradePolicyTest {

    @Test
    @DisplayName("기본 5단계 성취평가 컷오프(90/80/70/60)로 평가")
    void evaluate_defaultCutoffs() {
        GradePolicy policy = GradePolicy.builder()
                .name("default").active(true)
                .aMinScore(90.0).bMinScore(80.0).cMinScore(70.0).dMinScore(60.0)
                .build();

        assertThat(policy.evaluate(95.0)).isEqualTo(GradeLevel.A);
        assertThat(policy.evaluate(90.0)).isEqualTo(GradeLevel.A);  // 경계
        assertThat(policy.evaluate(89.99)).isEqualTo(GradeLevel.B);
        assertThat(policy.evaluate(80.0)).isEqualTo(GradeLevel.B);
        assertThat(policy.evaluate(75.0)).isEqualTo(GradeLevel.C);
        assertThat(policy.evaluate(65.0)).isEqualTo(GradeLevel.D);
        assertThat(policy.evaluate(60.0)).isEqualTo(GradeLevel.D);
        assertThat(policy.evaluate(59.99)).isEqualTo(GradeLevel.E);
        assertThat(policy.evaluate(0.0)).isEqualTo(GradeLevel.E);
    }

    @Test
    @DisplayName("정책 변경: 컷오프 다른 정책은 다른 등급을 반환")
    void evaluate_customCutoffs() {
        // A=95, B=85, C=75, D=65 (엄격한 정책)
        GradePolicy strict = GradePolicy.builder()
                .name("strict").active(true)
                .aMinScore(95.0).bMinScore(85.0).cMinScore(75.0).dMinScore(65.0)
                .build();

        assertThat(strict.evaluate(90.0)).isEqualTo(GradeLevel.B);  // 기본 정책에선 A
        assertThat(strict.evaluate(80.0)).isEqualTo(GradeLevel.C);  // 기본 정책에선 B
    }
}
