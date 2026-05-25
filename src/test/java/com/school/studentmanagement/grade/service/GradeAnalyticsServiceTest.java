package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GradeAnalyticsServiceTest {

    @Nested
    @DisplayName("computeRank — 학기 평균 기준 RANK 산출 (동점 동순위, 다음은 건너뜀)")
    class ComputeRank {

        @Test
        @DisplayName("정상: 평균 내림차순 정렬 후 1, 2, 3등 부여")
        void distinctScores() {
            List<StudentSemesterStat> stats = List.of(
                    statFor(1L, 80.0),
                    statFor(2L, 95.0),
                    statFor(3L, 70.0)
            );

            assertThat(GradeAnalyticsService.computeRank(stats, 2L)).isEqualTo(1);
            assertThat(GradeAnalyticsService.computeRank(stats, 1L)).isEqualTo(2);
            assertThat(GradeAnalyticsService.computeRank(stats, 3L)).isEqualTo(3);
        }

        @Test
        @DisplayName("동점자: 같은 평균이면 같은 등수, 다음은 건너뜀 (1, 1, 3, ...)")
        void tiedScores() {
            List<StudentSemesterStat> stats = List.of(
                    statFor(1L, 90.0),
                    statFor(2L, 90.0),
                    statFor(3L, 80.0),
                    statFor(4L, 70.0)
            );

            assertThat(GradeAnalyticsService.computeRank(stats, 1L)).isEqualTo(1);
            assertThat(GradeAnalyticsService.computeRank(stats, 2L)).isEqualTo(1);
            assertThat(GradeAnalyticsService.computeRank(stats, 3L)).isEqualTo(3);
            assertThat(GradeAnalyticsService.computeRank(stats, 4L)).isEqualTo(4);
        }

        @Test
        @DisplayName("학생 ID가 통계 목록에 없으면 null 반환")
        void studentNotInStats() {
            List<StudentSemesterStat> stats = List.of(statFor(1L, 80.0));
            assertThat(GradeAnalyticsService.computeRank(stats, 999L)).isNull();
        }

        @Test
        @DisplayName("빈 통계 목록은 null 반환")
        void emptyStats() {
            assertThat(GradeAnalyticsService.computeRank(List.of(), 1L)).isNull();
        }

        private StudentSemesterStat statFor(Long studentId, Double averageScore) {
            User user = User.builder().name("학생" + studentId).build();
            ReflectionTestUtils.setField(user, "id", studentId);
            Student student = Student.builder().user(user).enrollmentYear(2026).build();
            ReflectionTestUtils.setField(student, "id", studentId);
            return StudentSemesterStat.builder()
                    .student(student).academicYear(2026).semester(1)
                    .totalScore(averageScore * 3).averageScore(averageScore)
                    .gradeLevel(com.school.studentmanagement.global.enums.GradeLevel.from(averageScore))
                    .build();
        }
    }
}
