package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.validation.TeacherStudentRelationValidator;
import com.school.studentmanagement.grade.dto.GradeSearchResponse;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.student.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("성적 검색 (GradeSearchService)")
class GradeSearchServiceTest {

    @InjectMocks private GradeSearchService gradeSearchService;
    @Mock private StudentGradeRepository studentGradeRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherStudentRelationValidator teacherStudentRelationValidator;

    private static final Long STUDENT_ID = 10L;
    private static final Long TEACHER_ID = 1L;

    @Test
    @DisplayName("교사 검색: 관계 검증 호출 + 미발행 포함(includeUnpublished=true) + 학기 키 변환")
    void teacher_includesUnpublished_andComputesKeys() {
        given(studentRepository.existsById(STUDENT_ID)).willReturn(true);
        given(studentGradeRepository.searchByStudentAndFilters(any(), any(), any(), any(), anyBoolean()))
                .willReturn(List.of());

        gradeSearchService.searchForTeacher(STUDENT_ID, TEACHER_ID, 3L, 2026, 1, 2026, 2);

        verify(teacherStudentRelationValidator).validateCanWriteFor(TEACHER_ID, STUDENT_ID);
        verify(studentGradeRepository).searchByStudentAndFilters(
                eq(STUDENT_ID), eq(3L), eq(20261), eq(20262), eq(true));
    }

    @Test
    @DisplayName("학생 본인 검색: 발행분만(includeUnpublished=false)")
    void student_onlyPublished() {
        given(studentRepository.existsById(STUDENT_ID)).willReturn(true);
        given(studentGradeRepository.searchByStudentAndFilters(any(), any(), any(), any(), anyBoolean()))
                .willReturn(List.of());

        gradeSearchService.searchForStudent(STUDENT_ID, null, 2026, 1, 2026, 2);

        verify(studentGradeRepository).searchByStudentAndFilters(
                eq(STUDENT_ID), eq(null), eq(20261), eq(20262), eq(false));
    }

    @Test
    @DisplayName("학기 범위 미지정 시 양방향 무한대(null) 키로 전달")
    void openEndedRange_nullKeys() {
        given(studentRepository.existsById(STUDENT_ID)).willReturn(true);
        given(studentGradeRepository.searchByStudentAndFilters(any(), any(), any(), any(), anyBoolean()))
                .willReturn(List.of());

        gradeSearchService.searchForStudent(STUDENT_ID, null, null, null, null, null);

        verify(studentGradeRepository).searchByStudentAndFilters(
                eq(STUDENT_ID), eq(null), eq(null), eq(null), eq(false));
    }

    @Test
    @DisplayName("연도만 지정 시 학기는 경계 기본값(시작=1, 끝=2)으로 보정")
    void yearOnly_defaultsSemesterBoundaries() {
        given(studentRepository.existsById(STUDENT_ID)).willReturn(true);
        given(studentGradeRepository.searchByStudentAndFilters(any(), any(), any(), any(), anyBoolean()))
                .willReturn(List.of());

        gradeSearchService.searchForStudent(STUDENT_ID, null, 2025, null, 2026, null);

        ArgumentCaptor<Integer> fromKey = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> toKey = ArgumentCaptor.forClass(Integer.class);
        verify(studentGradeRepository).searchByStudentAndFilters(
                eq(STUDENT_ID), eq(null), fromKey.capture(), toKey.capture(), eq(false));
        assertThat(fromKey.getValue()).isEqualTo(20251); // 2025 + 1학기
        assertThat(toKey.getValue()).isEqualTo(20262);    // 2026 + 2학기
    }

    @Test
    @DisplayName("빈 결과는 count=0 으로 매핑")
    void emptyResult_countZero() {
        given(studentRepository.existsById(STUDENT_ID)).willReturn(true);
        given(studentGradeRepository.searchByStudentAndFilters(any(), any(), any(), any(), anyBoolean()))
                .willReturn(List.of());

        GradeSearchResponse res = gradeSearchService.searchForStudent(STUDENT_ID, null, null, null, null, null);

        assertThat(res.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(res.getCount()).isZero();
        assertThat(res.getGrades()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 학생이면 STUDENT_NOT_FOUND")
    void studentNotFound() {
        given(studentRepository.existsById(STUDENT_ID)).willReturn(false);

        assertThatThrownBy(() ->
                gradeSearchService.searchForStudent(STUDENT_ID, null, null, null, null, null))
                .isInstanceOf(BusinessException.class);
    }
}
