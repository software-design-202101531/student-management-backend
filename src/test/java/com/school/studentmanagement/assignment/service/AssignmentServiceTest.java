package com.school.studentmanagement.assignment.service;

import com.school.studentmanagement.assignment.dto.AssignmentCreateRequest;
import com.school.studentmanagement.assignment.dto.SubmissionGradeRequest;
import com.school.studentmanagement.assignment.entity.Assignment;
import com.school.studentmanagement.assignment.entity.Submission;
import com.school.studentmanagement.assignment.event.AssignmentGradedEvent;
import com.school.studentmanagement.assignment.repository.AssignmentRepository;
import com.school.studentmanagement.assignment.repository.SubmissionRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.SubmissionStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @InjectMocks private AssignmentService service;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;
    @Mock private ClassRoomRepository classRoomRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private static final Long TEACHER = 1L, CLASSROOM = 10L, SUBJECT = 20L, ASSIGNMENT = 30L, STUDENT = 40L;

    private Classroom classroom;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        classroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(1).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM);
        Subject subject = new Subject("수학");
        ReflectionTestUtils.setField(subject, "id", SUBJECT);
        assignment = Assignment.builder().classroom(classroom).subject(subject)
                .title("과제").description("설명").dueDate(LocalDateTime.now().plusDays(3)).build();
        ReflectionTestUtils.setField(assignment, "id", ASSIGNMENT);
    }

    // validateSubjectTeacher 통과 경로 stub
    private void stubTeacher() {
        given(classRoomRepository.findById(CLASSROOM)).willReturn(Optional.of(classroom));
        given(subjectAssignmentRepository.findValidAssignment(TEACHER, CLASSROOM, SUBJECT, 2026, 1))
                .willReturn(Optional.of(mock(SubjectAssignment.class)));
    }

    private Submission submittedSubmission() {
        return Submission.builder().assignment(assignment).student(mock(Student.class))
                .content("제출 내용").submittedAt(LocalDateTime.now()).status(SubmissionStatus.SUBMITTED).build();
    }

    @Test
    @DisplayName("채점 성공: 점수/피드백 기록 + 채점 이벤트 발행")
    void grade_success() {
        stubTeacher();
        given(assignmentRepository.findById(ASSIGNMENT)).willReturn(Optional.of(assignment));
        Submission sub = submittedSubmission();
        given(submissionRepository.findByAssignmentIdAndStudentId(ASSIGNMENT, STUDENT)).willReturn(Optional.of(sub));

        service.gradeSubmission(TEACHER, CLASSROOM, SUBJECT, ASSIGNMENT, STUDENT,
                SubmissionGradeRequest.builder().score(95).feedback("잘했어요").build());

        assertThat(sub.getScore()).isEqualTo(95);
        assertThat(sub.getFeedback()).isEqualTo("잘했어요");
        assertThat(sub.isGraded()).isTrue();
        verify(eventPublisher).publishEvent(any(AssignmentGradedEvent.class));
    }

    @Test
    @DisplayName("채점 실패: 제출이 없으면 SUBMISSION_NOT_FOUND")
    void grade_noSubmission() {
        stubTeacher();
        given(assignmentRepository.findById(ASSIGNMENT)).willReturn(Optional.of(assignment));
        given(submissionRepository.findByAssignmentIdAndStudentId(ASSIGNMENT, STUDENT)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.gradeSubmission(TEACHER, CLASSROOM, SUBJECT, ASSIGNMENT, STUDENT,
                SubmissionGradeRequest.builder().score(80).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUBMISSION_NOT_FOUND);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("수정 실패: 마감일이 과거면 ASSIGNMENT_DUE_DATE_INVALID")
    void update_pastDueDate() {
        stubTeacher();

        assertThatThrownBy(() -> service.updateAssignment(TEACHER, CLASSROOM, SUBJECT, ASSIGNMENT,
                AssignmentCreateRequest.builder().title("t").dueDate(LocalDateTime.now().minusDays(1)).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ASSIGNMENT_DUE_DATE_INVALID);
    }

    @Test
    @DisplayName("수정 성공: 제목/설명/마감일 갱신")
    void update_success() {
        stubTeacher();
        given(assignmentRepository.findById(ASSIGNMENT)).willReturn(Optional.of(assignment));
        LocalDateTime newDue = LocalDateTime.now().plusDays(7);

        service.updateAssignment(TEACHER, CLASSROOM, SUBJECT, ASSIGNMENT,
                AssignmentCreateRequest.builder().title("수정제목").description("수정설명").dueDate(newDue).build());

        assertThat(assignment.getTitle()).isEqualTo("수정제목");
        assertThat(assignment.getDescription()).isEqualTo("수정설명");
        assertThat(assignment.getDueDate()).isEqualTo(newDue);
    }

    @Test
    @DisplayName("삭제 성공: 제출들 먼저 제거 후 과제 삭제")
    void delete_success() {
        stubTeacher();
        given(assignmentRepository.findById(ASSIGNMENT)).willReturn(Optional.of(assignment));

        service.deleteAssignment(TEACHER, CLASSROOM, SUBJECT, ASSIGNMENT);

        verify(submissionRepository).deleteByAssignmentId(ASSIGNMENT);
        verify(assignmentRepository).delete(assignment);
    }
}
