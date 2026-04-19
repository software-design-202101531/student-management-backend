package com.school.studentmanagement.record.service;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.StudentAffiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.record.entity.StudentRecord;
import com.school.studentmanagement.record.repository.StudentRecordRepository;
import com.school.studentmanagement.subject.dto.SubjectRecordRequest;
import com.school.studentmanagement.subject.dto.SubjectRecordResponse;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.user.entity.Student;
import com.school.studentmanagement.user.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubjectRecordServiceTest {

    @InjectMocks private SubjectRecordService subjectRecordService;
    @Mock private StudentRecordRepository studentRecordRepository;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private AcademicCalendarUtil academicCalendarUtil;

    private static final Long TEACHER_ID   = 100L;
    private static final Long CLASSROOM_ID = 200L;
    private static final Long STUDENT_ID   = 1L;
    private static final Long SUBJECT_ID   = 1L;

    private Teacher teacher;
    private Student student;
    private Subject mathSubject;
    private Classroom classroom;
    private SubjectAssignment assignment;
    private StudentAffiliation affiliation;

    @BeforeEach
    void setUp() {
        mathSubject = new Subject("수학");
        ReflectionTestUtils.setField(mathSubject, "id", SUBJECT_ID);

        User teacherUser = User.builder().id(TEACHER_ID).name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        teacher = Teacher.builder().user(teacherUser).employeeNumber("EMP001")
                .officeLocation("본관").officePhoneNumber("02-000").employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);

        classroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(4).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM_ID);

        User studentUser = User.builder().id(STUDENT_ID).name("1-4학생01")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        student = Student.builder().id(STUDENT_ID).user(studentUser).enrollmentYear(2026).build();
        affiliation = StudentAffiliation.builder().student(student).classroom(classroom).studentNum(1).build();

        assignment = SubjectAssignment.builder()
                .teacher(teacher).classroom(classroom).subject(mathSubject).academicYear(2026).semester(1).build();

        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
    }

    // 권한 검증 통과 stubbing 헬퍼
    private void stubValidContext() {
        given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                .willReturn(Optional.of(assignment));
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                .willReturn(Optional.of(affiliation));
    }

    // ==========================================================================
    // getSubjectRecord
    // ==========================================================================

    @Nested
    @DisplayName("과세특 조회 (getSubjectRecord)")
    class GetSubjectRecordTest {

        @Test
        @DisplayName("성공: 기존 과세특이 있으면 내용과 canEdit 반환")
        void getSubjectRecord_Success_WithRecord() {
            // Given
            StudentRecord record = StudentRecord.createSubjectOpinion(
                    student, teacher, 2026, 1, mathSubject, "수학에 탁월한 이해력을 보임");
            ReflectionTestUtils.setField(record, "id", 10L);

            stubValidContext();
            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentRecordRepository.findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
                    STUDENT_ID, RecordCategory.SUBJECT_OPINION, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(record));

            // When
            SubjectRecordResponse response = subjectRecordService.getSubjectRecord(
                    CLASSROOM_ID, STUDENT_ID, SUBJECT_ID, TEACHER_ID);

            // Then
            assertThat(response.getRecordId()).isEqualTo(10L);
            assertThat(response.getContent()).isEqualTo("수학에 탁월한 이해력을 보임");
            assertThat(response.isCanEdit()).isTrue();
        }

        @Test
        @DisplayName("성공: 과세특이 없으면 빈 내용과 recordId=null 반환")
        void getSubjectRecord_Success_NoRecord() {
            // Given
            stubValidContext();
            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentRecordRepository.findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
                    STUDENT_ID, RecordCategory.SUBJECT_OPINION, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When
            SubjectRecordResponse response = subjectRecordService.getSubjectRecord(
                    CLASSROOM_ID, STUDENT_ID, SUBJECT_ID, TEACHER_ID);

            // Then
            assertThat(response.getRecordId()).isNull();
            assertThat(response.getContent()).isEqualTo("");
        }

        @Test
        @DisplayName("실패: 과목 담당 교사가 아닌 경우 → AccessDeniedException")
        void getSubjectRecord_Fail_NotSubjectTeacher() {
            // Given
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> subjectRecordService.getSubjectRecord(
                    CLASSROOM_ID, STUDENT_ID, SUBJECT_ID, TEACHER_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("해당 반의 과목 담당 교사가 아닙니다");
        }

        @Test
        @DisplayName("실패: 학생이 해당 반 소속이 아닌 경우 → AccessDeniedException")
        void getSubjectRecord_Fail_StudentNotInClassroom() {
            // Given - 학생이 다른 반(999L) 소속
            Classroom otherClassroom = Classroom.builder()
                    .academicYear(2026).semester(1).grade(2).classNum(1).build();
            ReflectionTestUtils.setField(otherClassroom, "id", 999L);
            StudentAffiliation otherAffiliation = StudentAffiliation.builder()
                    .student(student).classroom(otherClassroom).studentNum(1).build();

            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.of(otherAffiliation));

            // When & Then
            assertThatThrownBy(() -> subjectRecordService.getSubjectRecord(
                    CLASSROOM_ID, STUDENT_ID, SUBJECT_ID, TEACHER_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("해당 학생은 해당 반의 소속이 아닙니다");
        }
    }

    // ==========================================================================
    // saveSubjectRecord
    // ==========================================================================

    @Nested
    @DisplayName("과세특 저장/수정 (saveSubjectRecord)")
    class SaveSubjectRecordTest {

        @Test
        @DisplayName("성공: 기존 과세특이 없으면 신규 생성 후 save 호출")
        void saveSubjectRecord_Success_CreatesNew() {
            // Given
            SubjectRecordRequest request = buildRequest("수학 심화 학습 능력 탁월");
            stubValidContext();
            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentRecordRepository.findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
                    STUDENT_ID, RecordCategory.SUBJECT_OPINION, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When
            subjectRecordService.saveSubjectRecord(CLASSROOM_ID, STUDENT_ID, SUBJECT_ID, TEACHER_ID, request);

            // Then
            verify(studentRecordRepository).save(any(StudentRecord.class));
        }

        @Test
        @DisplayName("성공: 기존 과세특이 있으면 updateContent 호출 (save 없음)")
        void saveSubjectRecord_Success_UpdatesExisting() {
            // Given
            StudentRecord existingRecord = StudentRecord.createSubjectOpinion(
                    student, teacher, 2026, 1, mathSubject, "기존 내용");
            SubjectRecordRequest request = buildRequest("수정된 내용");

            stubValidContext();
            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentRecordRepository.findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
                    STUDENT_ID, RecordCategory.SUBJECT_OPINION, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(existingRecord));

            // When
            subjectRecordService.saveSubjectRecord(CLASSROOM_ID, STUDENT_ID, SUBJECT_ID, TEACHER_ID, request);

            // Then
            assertThat(existingRecord.getContent()).isEqualTo("수정된 내용");
            verify(studentRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 마감 기한이 지난 경우 → IllegalStateException")
        void saveSubjectRecord_Fail_PastDeadline() {
            // Given
            given(academicCalendarUtil.isModifiable(2026)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> subjectRecordService.saveSubjectRecord(
                    CLASSROOM_ID, STUDENT_ID, SUBJECT_ID, TEACHER_ID, buildRequest("내용")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("해당 학년도의 과세특 작성 및 수정 기간은 마감되었습니다");
        }
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────────
    private SubjectRecordRequest buildRequest(String content) {
        try {
            Constructor<SubjectRecordRequest> ctor = ReflectionUtils.accessibleConstructor(SubjectRecordRequest.class);
            SubjectRecordRequest req = ctor.newInstance();
            ReflectionTestUtils.setField(req, "content", content);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
