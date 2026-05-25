package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.global.enums.GradeLevel;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.grade.repository.SubjectScoreAggregation;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SemesterStatRecalculatorTest {

    @InjectMocks private SemesterStatRecalculator recalculator;

    @Mock private StudentGradeRepository studentGradeRepository;
    @Mock private StudentSemesterStatRepository semesterStatRepository;
    @Mock private GradePolicyService gradePolicyService;

    private Student student;

    @BeforeEach
    void setUp() {
        User user = User.builder().name("테스트학생").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        student = Student.builder().user(user).enrollmentYear(2026).build();
        ReflectionTestUtils.setField(student, "id", 1L);

        lenient().when(gradePolicyService.evaluate(anyDouble()))
                .thenAnswer(inv -> GradeLevel.from(inv.<Double>getArgument(0)));
    }

    @Test
    @DisplayName("성공: 새 stat INSERT — 과목 3개 평균 → 등급 산정")
    void refresh_createsNewStat() {
        given(studentGradeRepository.aggregateSubjectScoresByStudentAndSemester(1L, 2026, 1))
                .willReturn(List.of(
                        agg(1L, 90.0),
                        agg(2L, 80.0),
                        agg(3L, 70.0)
                ));
        given(semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(1L, 2026, 1))
                .willReturn(Optional.empty());

        recalculator.refresh(student, 2026, 1);

        ArgumentCaptor<StudentSemesterStat> captor = ArgumentCaptor.forClass(StudentSemesterStat.class);
        verify(semesterStatRepository).save(captor.capture());
        StudentSemesterStat saved = captor.getValue();
        assertThat(saved.getTotalScore()).isEqualTo(240.0);
        assertThat(saved.getAverageScore()).isEqualTo(80.0);
        assertThat(saved.getGradeLevel()).isEqualTo(GradeLevel.B);
    }

    @Test
    @DisplayName("성공: 기존 stat UPDATE — updateStats만 호출, save 호출 안 됨")
    void refresh_updatesExistingStat() {
        StudentSemesterStat existing = StudentSemesterStat.builder()
                .student(student).academicYear(2026).semester(1)
                .totalScore(60.0).averageScore(60.0).gradeLevel(GradeLevel.D)
                .build();

        given(studentGradeRepository.aggregateSubjectScoresByStudentAndSemester(1L, 2026, 1))
                .willReturn(List.of(agg(1L, 95.0), agg(2L, 95.0)));
        given(semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(1L, 2026, 1))
                .willReturn(Optional.of(existing));

        recalculator.refresh(student, 2026, 1);

        assertThat(existing.getTotalScore()).isEqualTo(190.0);
        assertThat(existing.getAverageScore()).isEqualTo(95.0);
        assertThat(existing.getGradeLevel()).isEqualTo(GradeLevel.A);
        verify(semesterStatRepository, never()).save(existing);
    }

    @Test
    @DisplayName("성공: 과목별 점수가 비어있으면 stat 갱신 없음 (early return)")
    void refresh_noSubjectScores_earlyReturn() {
        given(studentGradeRepository.aggregateSubjectScoresByStudentAndSemester(1L, 2026, 1))
                .willReturn(List.of());

        recalculator.refresh(student, 2026, 1);

        verify(semesterStatRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(semesterStatRepository, never())
                .findByStudentIdAndAcademicYearAndSemester(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private SubjectScoreAggregation agg(Long subjectId, Double subjectScore) {
        return new SubjectScoreAggregation() {
            @Override public Long getSubjectId() { return subjectId; }
            @Override public Double getSubjectScore() { return subjectScore; }
        };
    }
}
