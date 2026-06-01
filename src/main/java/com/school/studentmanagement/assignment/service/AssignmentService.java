package com.school.studentmanagement.assignment.service;

import com.school.studentmanagement.assignment.dto.AssignmentCreateRequest;
import com.school.studentmanagement.assignment.dto.AssignmentResponse;
import com.school.studentmanagement.assignment.dto.SubmissionGradeRequest;
import com.school.studentmanagement.assignment.dto.SubmissionStatusResponse;
import com.school.studentmanagement.assignment.entity.Assignment;
import com.school.studentmanagement.assignment.entity.Submission;
import com.school.studentmanagement.assignment.event.AssignmentCreatedEvent;
import com.school.studentmanagement.assignment.event.AssignmentGradedEvent;
import com.school.studentmanagement.assignment.repository.AssignmentRepository;
import com.school.studentmanagement.assignment.repository.SubmissionRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.SubmissionStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 과제 부여/조회/제출현황 (과목 담당 교사 전용)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AssignmentResponse createAssignment(Long teacherId, Long classroomId, Long subjectId,
                                               AssignmentCreateRequest request) {
        SubjectAssignment sa = validateSubjectTeacher(teacherId, classroomId, subjectId);
        validateDueDate(request.getDueDate());

        Assignment assignment = Assignment.builder()
                .classroom(sa.getClassroom())
                .subject(sa.getSubject())
                .teacher(sa.getTeacher())
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .build();
        assignmentRepository.save(assignment);

        // 학급 학생(+학부모) 대상 알림 — 커밋 이후 비동기 처리
        eventPublisher.publishEvent(new AssignmentCreatedEvent(assignment.getId()));
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long teacherId, Long classroomId, Long subjectId,
                                               Long assignmentId, AssignmentCreateRequest request) {
        validateSubjectTeacher(teacherId, classroomId, subjectId);
        validateDueDate(request.getDueDate());

        Assignment assignment = findAssignmentIn(assignmentId, classroomId, subjectId);
        assignment.update(request.getTitle(), request.getDescription(), request.getDueDate());
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public void deleteAssignment(Long teacherId, Long classroomId, Long subjectId, Long assignmentId) {
        validateSubjectTeacher(teacherId, classroomId, subjectId);
        Assignment assignment = findAssignmentIn(assignmentId, classroomId, subjectId);

        submissionRepository.deleteByAssignmentId(assignment.getId()); // 제출들 먼저 제거(FK)
        assignmentRepository.delete(assignment);
    }

    @Transactional
    public void gradeSubmission(Long teacherId, Long classroomId, Long subjectId,
                                Long assignmentId, Long studentId, SubmissionGradeRequest request) {
        validateSubjectTeacher(teacherId, classroomId, subjectId);
        findAssignmentIn(assignmentId, classroomId, subjectId); // 권한·소속 검증

        Submission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));
        submission.grade(request.getScore(), request.getFeedback());

        eventPublisher.publishEvent(new AssignmentGradedEvent(submission.getId()));
    }

    public List<AssignmentResponse> getAssignments(Long teacherId, Long classroomId, Long subjectId) {
        validateSubjectTeacher(teacherId, classroomId, subjectId);
        return assignmentRepository.findByClassroomAndSubject(classroomId, subjectId).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    public SubmissionStatusResponse getSubmissionStatus(Long teacherId, Long classroomId, Long subjectId,
                                                        Long assignmentId) {
        validateSubjectTeacher(teacherId, classroomId, subjectId);

        Assignment assignment = findAssignmentIn(assignmentId, classroomId, subjectId);

        Map<Long, Submission> submissionByStudentId = submissionRepository.findByAssignmentId(assignmentId).stream()
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        int submittedCount = 0;
        int lateCount = 0;
        List<SubmissionStatusResponse.StudentSubmissionDto> rows = new java.util.ArrayList<>();
        for (StudentAffiliation aff : affiliations) {
            Submission sub = submissionByStudentId.get(aff.getStudent().getId());
            SubmissionStatus status = sub != null ? sub.getStatus() : SubmissionStatus.NOT_SUBMITTED;
            if (status != SubmissionStatus.NOT_SUBMITTED) submittedCount++;
            if (status == SubmissionStatus.LATE) lateCount++;
            rows.add(SubmissionStatusResponse.StudentSubmissionDto.builder()
                    .studentId(aff.getStudent().getId())
                    .studentNum(aff.getStudentNum())
                    .studentName(aff.getStudent().getUser().getName())
                    .status(status)
                    .submittedAt(sub != null ? sub.getSubmittedAt() : null)
                    .content(sub != null ? sub.getContent() : null)
                    .score(sub != null ? sub.getScore() : null)
                    .feedback(sub != null ? sub.getFeedback() : null)
                    .build());
        }

        return SubmissionStatusResponse.builder()
                .assignmentId(assignment.getId())
                .title(assignment.getTitle())
                .dueDate(assignment.getDueDate())
                .totalStudents(affiliations.size())
                .submittedCount(submittedCount)
                .lateCount(lateCount)
                .students(rows)
                .build();
    }

    // 과제가 해당 학급/과목 소속인지 확인하며 조회(경로 변조 방지). 불일치는 NOT_FOUND로 통일.
    private Assignment findAssignmentIn(Long assignmentId, Long classroomId, Long subjectId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        if (!assignment.getClassroom().getId().equals(classroomId)
                || !assignment.getSubject().getId().equals(subjectId)) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND);
        }
        return assignment;
    }

    // 마감일은 현재 이후여야 한다(이미 지난 시각으로 부여/수정 방지).
    private void validateDueDate(LocalDateTime dueDate) {
        if (dueDate == null || !dueDate.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_DUE_DATE_INVALID);
        }
    }

    // 과목 담당 교사 검증 — 학기는 해당 학급의 학년도/학기를 따른다. 통과 시 SubjectAssignment 반환.
    private SubjectAssignment validateSubjectTeacher(Long teacherId, Long classroomId, Long subjectId) {
        Classroom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASSROOM_NOT_FOUND));
        return subjectAssignmentRepository.findValidAssignment(
                        teacherId, classroomId, subjectId, classroom.getAcademicYear(), classroom.getSemester())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "해당 수업 담당 교사가 아닙니다"));
    }
}
