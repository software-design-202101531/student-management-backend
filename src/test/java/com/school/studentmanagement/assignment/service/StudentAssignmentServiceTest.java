package com.school.studentmanagement.assignment.service;

import com.school.studentmanagement.assignment.dto.SubmissionCreateRequest;
import com.school.studentmanagement.assignment.entity.Assignment;
import com.school.studentmanagement.assignment.entity.Submission;
import com.school.studentmanagement.assignment.repository.AssignmentRepository;
import com.school.studentmanagement.assignment.repository.SubmissionRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.SubmissionStatus;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentAssignmentServiceTest {

    @InjectMocks private StudentAssignmentService service;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private AcademicCalendarUtil academicCalendarUtil;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static final Long STUDENT_ID = 1L;
    private static final Long ASSIGNMENT_ID = 50L;
    private static final Long CLASSROOM_ID = 200L;

    private Classroom classroom;

    @BeforeEach
    void setUp() {
        classroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(4).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM_ID);
    }

    private Assignment assignmentWithDueDate(LocalDateTime dueDate) {
        Subject subject = new Subject("수학");
        ReflectionTestUtils.setField(subject, "id", 300L);
        Assignment a = Assignment.builder().classroom(classroom).subject(subject).title("과제").dueDate(dueDate).build();
        ReflectionTestUtils.setField(a, "id", ASSIGNMENT_ID);
        return a;
    }

    private void stubSubmitFlow(Assignment assignment) {
        given(assignmentRepository.findById(ASSIGNMENT_ID)).willReturn(Optional.of(assignment));
        given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                .willReturn(Optional.of(StudentAffiliation.builder()
                        .student(Student.builder().id(STUDENT_ID).enrollmentYear(2026).build())
                        .classroom(classroom).studentNum(1).build()));
        given(submissionRepository.findByAssignmentIdAndStudentId(ASSIGNMENT_ID, STUDENT_ID))
                .willReturn(Optional.empty());
    }

    @Test
    @DisplayName("마감 전 제출 → SUBMITTED")
    void submit_beforeDue_isSubmitted() {
        Assignment a = assignmentWithDueDate(LocalDateTime.now().plusDays(1));
        stubSubmitFlow(a);

        service.submit(STUDENT_ID, ASSIGNMENT_ID, SubmissionCreateRequest.builder().content("내용").build());

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("마감 후 제출 → LATE")
    void submit_afterDue_isLate() {
        Assignment a = assignmentWithDueDate(LocalDateTime.now().minusDays(1));
        stubSubmitFlow(a);

        service.submit(STUDENT_ID, ASSIGNMENT_ID, SubmissionCreateRequest.builder().content("내용").build());

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubmissionStatus.LATE);
    }
}
