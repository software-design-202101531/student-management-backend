package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.grade.dto.ClosePreviewResponse;
import com.school.studentmanagement.grade.dto.SemesterClosureResponse;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.SemesterClosure;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.SemesterClosureRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SemesterClosureServiceTest {

    @InjectMocks private SemesterClosureService semesterClosureService;

    @Mock private SemesterClosureRepository semesterClosureRepository;
    @Mock private ExamRepository examRepository;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private StudentGradeRepository studentGradeRepository;
    @Mock private UserRepository userRepository;
    @Mock private SemesterStatRecalculator semesterStatRecalculator;
    @Mock private AcademicCalendarUtil academicCalendarUtil;
    @Mock private ObjectProvider<SemesterClosureService> selfProvider;

    // 픽스처
    private Subject mathSubject;
    private Exam midtermExam;
    private Classroom classroom;
    private Student student1;
    private Student student2;
    private SubjectAssignment assignment;
    private StudentAffiliation aff1;
    private StudentAffiliation aff2;

    @BeforeEach
    void setUp() {
        mathSubject = new Subject("수학");
        ReflectionTestUtils.setField(mathSubject, "id", 1L);

        midtermExam = Exam.builder()
                .academicYear(2025).semester(1).examType(ExamType.MIDTERM)
                .name("1학기 중간고사").maxScore(100).weight(1.0)
                .published(true).build();
        ReflectionTestUtils.setField(midtermExam, "id", 10L);

        Teacher teacher = Teacher.builder()
                .user(User.builder().name("교사").build())
                .employeeNumber("EMP").officeLocation("본관").officePhoneNumber("02-000")
                .employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", 100L);

        classroom = Classroom.builder()
                .academicYear(2025).semester(1).grade(1).classNum(1)
                .homeroomTeacher(teacher).build();
        ReflectionTestUtils.setField(classroom, "id", 200L);

        User u1 = User.builder().name("학생1").build();
        ReflectionTestUtils.setField(u1, "id", 1L);
        student1 = Student.builder().user(u1).enrollmentYear(2025).build();
        ReflectionTestUtils.setField(student1, "id", 1L);

        User u2 = User.builder().name("학생2").build();
        ReflectionTestUtils.setField(u2, "id", 2L);
        student2 = Student.builder().user(u2).enrollmentYear(2025).build();
        ReflectionTestUtils.setField(student2, "id", 2L);

        assignment = SubjectAssignment.builder()
                .teacher(teacher).classroom(classroom).subject(mathSubject)
                .academicYear(2025).semester(1).build();

        aff1 = StudentAffiliation.builder().student(student1).classroom(classroom).studentNum(1).build();
        aff2 = StudentAffiliation.builder().student(student2).classroom(classroom).studentNum(2).build();
    }

    @Nested
    @DisplayName("preview")
    class PreviewTest {

        @Test
        @DisplayName("성공: 학생2만 미입력 → preview에 학생2가 누락으로 잡힘")
        void preview_findsMissingStudent() {
            given(semesterClosureRepository.existsByAcademicYearAndSemester(2025, 1)).willReturn(false);
            given(examRepository.findByAcademicYearAndSemesterAndWeightGreaterThan(2025, 1, 0.0))
                    .willReturn(List.of(midtermExam));
            given(subjectAssignmentRepository.findAllByAcademicYearAndSemester(2025, 1))
                    .willReturn(List.of(assignment));
            given(studentAffiliationRepository.findAllByClassroomId(200L))
                    .willReturn(List.of(aff1, aff2));
            given(studentGradeRepository.findExistingKeysByAcademicYearAndSemester(2025, 1))
                    .willReturn(List.of(existingKey(1L, 10L, 1L))); // 학생1만 입력됨

            ClosePreviewResponse response = semesterClosureService.preview(2025, 1);

            assertThat(response.getTotalMissingCount()).isEqualTo(1);
            assertThat(response.getAffectedStudentCount()).isEqualTo(1);
            assertThat(response.getStudents()).hasSize(1);
            ClosePreviewResponse.StudentMissing missing = response.getStudents().get(0);
            assertThat(missing.getStudentId()).isEqualTo(2L);
            assertThat(missing.getMissing()).hasSize(1);
            assertThat(missing.getMissing().get(0).getSubjectName()).isEqualTo("수학");
        }

        @Test
        @DisplayName("실패: 이미 마감된 학기는 preview 불가")
        void preview_failsWhenAlreadyClosed() {
            given(semesterClosureRepository.existsByAcademicYearAndSemester(2025, 1)).willReturn(true);

            assertThatThrownBy(() -> semesterClosureService.preview(2025, 1))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 마감");
        }
    }

    @Nested
    @DisplayName("close")
    class CloseTest {

        @Test
        @DisplayName("성공: 누락 row INSERT(NOT_SUBMITTED) + 영향받은 학생 stat 재계산 + closure 저장")
        void close_insertsMissingAndRecalculates() {
            given(semesterClosureRepository.existsByAcademicYearAndSemester(2025, 1)).willReturn(false);
            given(examRepository.findByAcademicYearAndSemesterAndWeightGreaterThan(2025, 1, 0.0))
                    .willReturn(List.of(midtermExam));
            given(subjectAssignmentRepository.findAllByAcademicYearAndSemester(2025, 1))
                    .willReturn(List.of(assignment));
            given(studentAffiliationRepository.findAllByClassroomId(200L))
                    .willReturn(List.of(aff1, aff2));
            given(studentGradeRepository.findExistingKeysByAcademicYearAndSemester(2025, 1))
                    .willReturn(List.of(existingKey(1L, 10L, 1L))); // 학생1만 입력
            given(userRepository.findById(999L))
                    .willReturn(Optional.of(User.builder().name("관리자").build()));
            given(semesterClosureRepository.save(any(SemesterClosure.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            SemesterClosureResponse response = semesterClosureService.close(2025, 1, 999L, "수동 마감");

            // 학생2의 누락 row 1개 INSERT
            ArgumentCaptor<StudentGrade> gradeCaptor = ArgumentCaptor.forClass(StudentGrade.class);
            verify(studentGradeRepository).save(gradeCaptor.capture());
            StudentGrade missing = gradeCaptor.getValue();
            assertThat(missing.getStudent().getId()).isEqualTo(2L);
            assertThat(missing.getAttendanceStatus()).isEqualTo(ExamAttendanceStatus.NOT_SUBMITTED);
            assertThat(missing.getRawScore()).isEqualTo(0);

            // 학생2 stat 재계산
            verify(semesterStatRecalculator).refresh(eq(student2), eq(2025), eq(1));

            // closure 저장
            ArgumentCaptor<SemesterClosure> closureCaptor = ArgumentCaptor.forClass(SemesterClosure.class);
            verify(semesterClosureRepository).save(closureCaptor.capture());
            SemesterClosure closure = closureCaptor.getValue();
            assertThat(closure.getMethod()).isEqualTo(SemesterClosureMethod.MANUAL);
            assertThat(closure.getFilledCount()).isEqualTo(1);
            assertThat(closure.getClosedByName()).isEqualTo("관리자");
            assertThat(closure.getReason()).isEqualTo("수동 마감");

            assertThat(response.isClosed()).isTrue();
            assertThat(response.getFilledCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패: 이미 마감된 학기 재마감 불가")
        void close_failsWhenAlreadyClosed() {
            given(semesterClosureRepository.existsByAcademicYearAndSemester(2025, 1)).willReturn(true);

            assertThatThrownBy(() -> semesterClosureService.close(2025, 1, 1L, "x"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 마감");
        }
    }

    @Nested
    @DisplayName("reopen")
    class ReopenTest {

        @Test
        @DisplayName("성공: closure 삭제 (NOT_SUBMITTED row는 그대로)")
        void reopen_deletesClosureOnly() {
            SemesterClosure closure = SemesterClosure.builder()
                    .academicYear(2025).semester(1)
                    .method(SemesterClosureMethod.MANUAL)
                    .closedByUserId(1L).closedByName("관리자")
                    .filledCount(5).build();

            given(semesterClosureRepository.findByAcademicYearAndSemester(2025, 1))
                    .willReturn(Optional.of(closure));

            semesterClosureService.reopen(2025, 1);

            verify(semesterClosureRepository).delete(closure);
            // NOT_SUBMITTED row 삭제 안 함
            verify(studentGradeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 마감되지 않은 학기 reopen 불가")
        void reopen_failsWhenNotClosed() {
            given(semesterClosureRepository.findByAcademicYearAndSemester(2025, 1))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> semesterClosureService.reopen(2025, 1))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("마감되지 않은");
        }
    }

    @Nested
    @DisplayName("autoCloseExpired")
    class AutoCloseTest {

        @Test
        @DisplayName("성공: isModifiable=false + 미마감 학기만 AUTO 마감")
        void autoClose_onlyExpiredAndOpen() {
            // 일괄 마감은 자기 프록시를 통해 학기별 autoClose를 호출한다
            given(selfProvider.getObject()).willReturn(semesterClosureService);
            given(examRepository.findAllDistinctSemesters()).willReturn(List.of(
                    semKey(2024, 1),  // 마감일 지남, 미마감 → close
                    semKey(2025, 1),  // 마감일 안 지남 → skip
                    semKey(2023, 2)   // 마감일 지남, 이미 마감 → skip
            ));
            given(academicCalendarUtil.isModifiable(2024)).willReturn(false);
            given(academicCalendarUtil.isModifiable(2025)).willReturn(true);
            given(academicCalendarUtil.isModifiable(2023)).willReturn(false);

            given(semesterClosureRepository.existsByAcademicYearAndSemester(2024, 1)).willReturn(false);
            given(semesterClosureRepository.existsByAcademicYearAndSemester(2023, 2)).willReturn(true);

            // close 흐름 stub (2024-1만)
            given(examRepository.findByAcademicYearAndSemesterAndWeightGreaterThan(2024, 1, 0.0))
                    .willReturn(List.of());  // 시험 없음 → 누락 0건, closure만 저장
            given(semesterClosureRepository.save(any(SemesterClosure.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            int closedCount = semesterClosureService.autoCloseExpired();

            assertThat(closedCount).isEqualTo(1);
            ArgumentCaptor<SemesterClosure> captor = ArgumentCaptor.forClass(SemesterClosure.class);
            verify(semesterClosureRepository).save(captor.capture());
            assertThat(captor.getValue().getMethod()).isEqualTo(SemesterClosureMethod.AUTO_FALLBACK);
            assertThat(captor.getValue().getClosedByName()).isEqualTo("system");
        }
    }

    private static StudentGradeRepository.ExistingGradeKey existingKey(Long studentId, Long examId, Long subjectId) {
        return new StudentGradeRepository.ExistingGradeKey() {
            @Override public Long getStudentId() { return studentId; }
            @Override public Long getExamId() { return examId; }
            @Override public Long getSubjectId() { return subjectId; }
        };
    }

    private static ExamRepository.SemesterKey semKey(int year, int sem) {
        return new ExamRepository.SemesterKey() {
            @Override public Integer getAcademicYear() { return year; }
            @Override public Integer getSemester() { return sem; }
        };
    }
}
