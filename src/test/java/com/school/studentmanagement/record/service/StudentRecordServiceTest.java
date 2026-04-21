package com.school.studentmanagement.record.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.record.dto.BehaviorRecordRequest;
import com.school.studentmanagement.record.dto.BehaviorRecordResponse;
import com.school.studentmanagement.record.entity.StudentRecord;
import com.school.studentmanagement.record.repository.StudentRecordRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.school.studentmanagement.global.exception.BusinessException;
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
class StudentRecordServiceTest {

    @InjectMocks private StudentRecordService studentRecordService;
    @Mock private StudentRecordRepository studentRecordRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private AcademicCalendarUtil academicCalendarUtil;
    @Mock private TeacherRepository teacherRepository;

    private static final Long TEACHER_ID = 100L;
    private static final Long STUDENT_ID = 1L;

    private Teacher teacher;
    private Student student;
    private StudentAffiliation affiliation;

    @BeforeEach
    void setUp() {
        User teacherUser = User.builder().id(TEACHER_ID).name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        teacher = Teacher.builder().user(teacherUser).employeeNumber("EMP001")
                .officeLocation("본관").officePhoneNumber("02-000").employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);

        Classroom classroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(4)
                .homeroomTeacher(teacher).build();
        ReflectionTestUtils.setField(classroom, "id", 200L);

        User studentUser = User.builder().id(STUDENT_ID).name("1-4학생01")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        student = Student.builder().id(STUDENT_ID).user(studentUser).enrollmentYear(2026).build();
        affiliation = StudentAffiliation.builder().student(student).classroom(classroom).studentNum(1).build();

        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
    }

    // 담임 권한 검증 통과 stubbing 헬퍼
    private void stubValidAuthority() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                .willReturn(Optional.of(affiliation));
    }

    // ==========================================================================
    // getBehaviorRecord
    // ==========================================================================

    @Nested
    @DisplayName("행특 조회 (getBehaviorRecord)")
    class GetBehaviorRecordTest {

        @Test
        @DisplayName("성공: 기존 행특이 있으면 내용과 canEdit 반환")
        void getBehaviorRecord_Success_WithExistingRecord() {
            // Given
            StudentRecord existingRecord = StudentRecord.createBehaviorOpinion(
                    student, teacher, 2026, 1, "성실하고 모범적인 학생입니다");
            ReflectionTestUtils.setField(existingRecord, "id", 10L);

            stubValidAuthority();
            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                    STUDENT_ID, RecordCategory.BEHAVIOR_OPINION, 2026, 1))
                    .willReturn(Optional.of(existingRecord));

            // When
            BehaviorRecordResponse response = studentRecordService.getBehaviorRecord(STUDENT_ID, TEACHER_ID);

            // Then
            assertThat(response.getRecordId()).isEqualTo(10L);
            assertThat(response.getContent()).isEqualTo("성실하고 모범적인 학생입니다");
            assertThat(response.isCanEdit()).isTrue();
        }

        @Test
        @DisplayName("성공: 행특이 없으면 빈 내용과 recordId=null 반환")
        void getBehaviorRecord_Success_NoRecord() {
            // Given
            stubValidAuthority();
            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                    STUDENT_ID, RecordCategory.BEHAVIOR_OPINION, 2026, 1))
                    .willReturn(Optional.empty());

            // When
            BehaviorRecordResponse response = studentRecordService.getBehaviorRecord(STUDENT_ID, TEACHER_ID);

            // Then
            assertThat(response.getRecordId()).isNull();
            assertThat(response.getContent()).isEqualTo("");
        }

        @Test
        @DisplayName("실패: 담임이 아닌 교사 → AccessDeniedException")
        void getBehaviorRecord_Fail_NotHomeroomTeacher() {
            // Given - 학급의 담임은 다른 선생님
            User otherUser = User.builder().id(999L).name("다른선생").gender(Gender.MALE)
                    .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
            Teacher otherTeacher = Teacher.builder().user(otherUser).employeeNumber("EMP999")
                    .officeLocation("본관").officePhoneNumber("02-999").employmentStatus(EmploymentStatus.ACTIVE).build();
            ReflectionTestUtils.setField(otherTeacher, "id", 999L);
            Classroom classroomWithOtherTeacher = Classroom.builder()
                    .academicYear(2026).semester(1).grade(1).classNum(4).homeroomTeacher(otherTeacher).build();
            StudentAffiliation otherAffiliation = StudentAffiliation.builder()
                    .student(student).classroom(classroomWithOtherTeacher).studentNum(1).build();

            given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.of(otherAffiliation));

            // When & Then
            assertThatThrownBy(() -> studentRecordService.getBehaviorRecord(STUDENT_ID, TEACHER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("담당 학생이 아니기에 접근할 수 없습니다");
        }
    }

    // ==========================================================================
    // saveBehaviorRecord
    // ==========================================================================

    @Nested
    @DisplayName("행특 저장/수정 (saveBehaviorRecord)")
    class SaveBehaviorRecordTest {

        @Test
        @DisplayName("성공: 기존 행특이 없으면 신규 생성 후 save 호출")
        void saveBehaviorRecord_Success_CreatesNew() {
            // Given
            BehaviorRecordRequest request = buildRequest("모범적인 학생입니다");
            stubValidAuthority();
            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                    STUDENT_ID, RecordCategory.BEHAVIOR_OPINION, 2026, 1))
                    .willReturn(Optional.empty());
            given(teacherRepository.getReferenceById(TEACHER_ID)).willReturn(teacher);

            // When
            studentRecordService.saveBehaviorRecord(STUDENT_ID, TEACHER_ID, request);

            // Then
            verify(studentRecordRepository).save(any(StudentRecord.class));
        }

        @Test
        @DisplayName("성공: 기존 행특이 있으면 updateContent 호출 (save 없음)")
        void saveBehaviorRecord_Success_UpdatesExisting() {
            // Given
            StudentRecord existingRecord = StudentRecord.createBehaviorOpinion(student, teacher, 2026, 1, "기존 내용");
            BehaviorRecordRequest request = buildRequest("수정된 내용");

            stubValidAuthority();
            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentRecordRepository.findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
                    STUDENT_ID, RecordCategory.BEHAVIOR_OPINION, 2026, 1))
                    .willReturn(Optional.of(existingRecord));
            given(teacherRepository.getReferenceById(TEACHER_ID)).willReturn(teacher);

            // When
            studentRecordService.saveBehaviorRecord(STUDENT_ID, TEACHER_ID, request);

            // Then
            assertThat(existingRecord.getContent()).isEqualTo("수정된 내용");
            verify(studentRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 마감 기한이 지난 경우 → IllegalArgumentException")
        void saveBehaviorRecord_Fail_PastDeadline() {
            // Given
            given(academicCalendarUtil.isModifiable(2026)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> studentRecordService.saveBehaviorRecord(
                    STUDENT_ID, TEACHER_ID, buildRequest("내용")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 학년도의 행특 작성 및 수정 기간은 마감되었습니다");
        }

        @Test
        @DisplayName("실패: 담임이 아닌 교사 → AccessDeniedException")
        void saveBehaviorRecord_Fail_NotHomeroomTeacher() {
            // Given
            User otherUser = User.builder().id(999L).name("다른선생").gender(Gender.MALE)
                    .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
            Teacher otherTeacher = Teacher.builder().user(otherUser).employeeNumber("EMP999")
                    .officeLocation("본관").officePhoneNumber("02-999").employmentStatus(EmploymentStatus.ACTIVE).build();
            ReflectionTestUtils.setField(otherTeacher, "id", 999L);
            Classroom otherClassroom = Classroom.builder()
                    .academicYear(2026).semester(1).grade(1).classNum(1).homeroomTeacher(otherTeacher).build();
            StudentAffiliation otherAffiliation = StudentAffiliation.builder()
                    .student(student).classroom(otherClassroom).studentNum(1).build();

            given(academicCalendarUtil.isModifiable(2026)).willReturn(true);
            given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.of(otherAffiliation));

            // When & Then
            assertThatThrownBy(() -> studentRecordService.saveBehaviorRecord(
                    STUDENT_ID, TEACHER_ID, buildRequest("내용")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("담당 학생이 아니기에 접근할 수 없습니다");
        }
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────────
    private BehaviorRecordRequest buildRequest(String content) {
        try {
            Constructor<BehaviorRecordRequest> ctor = ReflectionUtils.accessibleConstructor(BehaviorRecordRequest.class);
            BehaviorRecordRequest req = ctor.newInstance();
            ReflectionTestUtils.setField(req, "content", content);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
