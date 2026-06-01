package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.grade.dto.ExamCreateRequest;
import com.school.studentmanagement.grade.dto.ExamResponse;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.event.ExamPublishedEvent;
import com.school.studentmanagement.grade.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamService {

    private final ExamRepository examRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ExamResponse createExam(ExamCreateRequest request) {
        boolean duplicated = examRepository.existsByAcademicYearAndSemesterAndExamTypeAndName(
                request.getAcademicYear(), request.getSemester(), request.getExamType(), request.getName());
        if (duplicated) {
            throw new BusinessException(ErrorCode.EXAM_NAME_DUPLICATED);
        }

        Exam exam = Exam.builder()
                .academicYear(request.getAcademicYear())
                .semester(request.getSemester())
                .examType(request.getExamType())
                .name(request.getName())
                .maxScore(request.getMaxScore())
                .weight(request.getWeight())
                .examDate(request.getExamDate())
                .coverage(request.getCoverage())
                .published(false)
                .build();
        examRepository.save(exam);
        return ExamResponse.from(exam);
    }

    public List<ExamResponse> listExams(Integer academicYear, Integer semester) {
        return examRepository.findByAcademicYearAndSemesterOrderByExamDateAscIdAsc(academicYear, semester)
                .stream()
                .map(ExamResponse::from)
                .toList();
    }

    @Transactional
    public void publish(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        if (exam.isPublished()) {
            throw new BusinessException(ErrorCode.EXAM_ALREADY_PUBLISHED);
        }
        exam.publish();
        // 커밋 이후(AFTER_COMMIT) 비동기로 학생/학부모에게 성적 발행 알림을 생성한다.
        eventPublisher.publishEvent(new ExamPublishedEvent(exam.getId()));
    }

    @Transactional
    public void unpublish(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        exam.unpublish();
    }
}
