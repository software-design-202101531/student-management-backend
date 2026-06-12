package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.grade.dto.ExamCreateRequest;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.event.ExamPublishedEvent;
import com.school.studentmanagement.grade.repository.ExamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @InjectMocks private ExamService service;
    @Mock private ExamRepository examRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ExamCreateRequest request() {
        return ExamCreateRequest.builder()
                .academicYear(2026).semester(1).examType(ExamType.MIDTERM).name("중간고사")
                .maxScore(100).weight(0.3).build();
    }

    private Exam exam(boolean published) {
        Exam exam = new Exam(2026, 1, ExamType.MIDTERM, "중간고사", 100, 0.3, null, null, published);
        ReflectionTestUtils.setField(exam, "id", 5L);
        return exam;
    }

    @Test
    @DisplayName("시험 생성: 동일 (학년도/학기/타입/이름) 중복이면 EXAM_NAME_DUPLICATED")
    void create_duplicate() {
        ExamCreateRequest request = request(); // 람다 밖에서 준비 — 예외 발생 호출은 createExam 하나만
        given(examRepository.existsByAcademicYearAndSemesterAndExamTypeAndName(2026, 1, ExamType.MIDTERM, "중간고사"))
                .willReturn(true);
        assertThatThrownBy(() -> service.createExam(request)).isInstanceOf(BusinessException.class);
        verify(examRepository, never()).save(any());
    }

    @Test
    @DisplayName("시험 생성: 정상 → 저장, 발행 이벤트 없음")
    void create_ok() {
        given(examRepository.existsByAcademicYearAndSemesterAndExamTypeAndName(2026, 1, ExamType.MIDTERM, "중간고사"))
                .willReturn(false);
        service.createExam(request());
        verify(examRepository).save(any(Exam.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("발행: 시험 없으면 EXAM_NOT_FOUND")
    void publish_notFound() {
        given(examRepository.findById(5L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.publish(5L)).isInstanceOf(BusinessException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("발행: 이미 발행된 시험이면 EXAM_ALREADY_PUBLISHED (중복 발행 차단)")
    void publish_alreadyPublished() {
        given(examRepository.findById(5L)).willReturn(Optional.of(exam(true)));
        assertThatThrownBy(() -> service.publish(5L)).isInstanceOf(BusinessException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("발행: 정상 → published=true + ExamPublishedEvent 발행")
    void publish_ok() {
        Exam exam = exam(false);
        given(examRepository.findById(5L)).willReturn(Optional.of(exam));

        service.publish(5L);

        assertThat(exam.isPublished()).isTrue();
        verify(eventPublisher).publishEvent(any(ExamPublishedEvent.class));
    }

    @Test
    @DisplayName("발행 취소: 시험 없으면 EXAM_NOT_FOUND")
    void unpublish_notFound() {
        given(examRepository.findById(5L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.unpublish(5L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("발행 취소: 정상 → published=false")
    void unpublish_ok() {
        Exam exam = exam(true);
        given(examRepository.findById(5L)).willReturn(Optional.of(exam));
        service.unpublish(5L);
        assertThat(exam.isPublished()).isFalse();
    }
}
