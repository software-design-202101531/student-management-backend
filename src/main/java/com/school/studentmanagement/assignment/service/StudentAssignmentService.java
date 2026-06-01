package com.school.studentmanagement.assignment.service;

import com.school.studentmanagement.assignment.dto.StudentAssignmentResponse;
import com.school.studentmanagement.assignment.dto.SubmissionCreateRequest;
import com.school.studentmanagement.assignment.entity.Assignment;
import com.school.studentmanagement.assignment.entity.Submission;
import com.school.studentmanagement.assignment.repository.AssignmentRepository;
import com.school.studentmanagement.assignment.repository.SubmissionRepository;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.SubmissionStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 학생 본인의 과제 조회/제출
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final StudentRepository studentRepository;
    private final AcademicCalendarUtil academicCalendarUtil;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // 본인 소속 학급(현재 학기)의 과제 목록 + 각 과제의 내 제출 상태
    public List<StudentAssignmentResponse> getMyAssignments(Long studentId) {
        int year = academicCalendarUtil.getCurrentAcademicYear();
        int semester = academicCalendarUtil.getCurrentSemester();

        var affiliation = studentAffiliationRepository.findWithAllDetails(studentId, year, semester);
        if (affiliation.isEmpty()) {
            return List.of();
        }
        Long classroomId = affiliation.get().getClassroom().getId();

        List<Assignment> assignments = assignmentRepository.findByClassroomWithSubject(classroomId);
        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        Map<Long, Submission> myByAssignment = submissionRepository
                .findByStudentAndAssignments(studentId, assignmentIds).stream()
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), s -> s));

        return assignments.stream()
                .map(a -> {
                    Submission sub = myByAssignment.get(a.getId());
                    return StudentAssignmentResponse.builder()
                            .assignmentId(a.getId())
                            .subjectName(a.getSubject().getName())
                            .title(a.getTitle())
                            .description(a.getDescription())
                            .dueDate(a.getDueDate())
                            .status(sub != null ? sub.getStatus() : SubmissionStatus.NOT_SUBMITTED)
                            .submittedAt(sub != null ? sub.getSubmittedAt() : null)
                            .content(sub != null ? sub.getContent() : null)
                            .score(sub != null ? sub.getScore() : null)
                            .feedback(sub != null ? sub.getFeedback() : null)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void submit(Long studentId, Long assignmentId, SubmissionCreateRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        // 본인이 그 과제의 학급에 속해 있는지 검증
        studentAffiliationRepository.findByStudentIdAndClassroomId(studentId, assignment.getClassroom().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "본인이 속한 학급의 과제만 제출할 수 있습니다"));

        LocalDateTime now = LocalDateTime.now();
        SubmissionStatus status = !now.isAfter(assignment.getDueDate())
                ? SubmissionStatus.SUBMITTED : SubmissionStatus.LATE;

        submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .ifPresentOrElse(
                        existing -> existing.resubmit(request.getContent(), now, status),
                        () -> submissionRepository.save(Submission.builder()
                                .assignment(assignment)
                                .student(studentRepository.getReferenceById(studentId))
                                .content(request.getContent())
                                .submittedAt(now)
                                .status(status)
                                .build())
                );

        // 분석 증분 적재 트리거 — 커밋 이후 RabbitMQ로 중계됨(AnalyticsEventRelay)
        eventPublisher.publishEvent(new com.school.studentmanagement.analytics.event.AnalyticsSourceEvent(
                com.school.studentmanagement.analytics.event.AnalyticsRabbitConfig.RK_SUBMISSION_CREATED,
                new com.school.studentmanagement.analytics.event.AnalyticsEventMessage(
                        studentId, assignment.getSubject().getId())));
    }
}
