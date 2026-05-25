package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse;
import com.school.studentmanagement.grade.dto.GradeListResponse;
import com.school.studentmanagement.grade.dto.GradeSaveRequest;
import com.school.studentmanagement.grade.dto.GradeUpdateRequest;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.GradeHistory;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.GradeHistoryRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentGradeServiceTest {

    @InjectMocks private StudentGradeService studentGradeService;

    @Mock private StudentGradeRepository studentGradeRepository;
    @Mock private StudentSemesterStatRepository semesterStatRepository;
    @Mock private ExamRepository examRepository;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private ClassRoomRepository classRoomRepository;
    @Mock private GradeHistoryRepository gradeHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private SemesterStatRecalculator semesterStatRecalculator;
    @Mock private SemesterClosureService semesterClosureService;

    private static final Long TEACHER_ID   = 100L;
    private static final Long CLASSROOM_ID = 200L;
    private static final Long SUBJECT_ID   = 1L;
    private static final Long STUDENT_ID   = 1L;
    private static final Long EXAM_ID      = 10L;
    private static final Long GRADE_ID     = 500L;

    private Subject   mathSubject;
    private Exam      midtermExam;
    private Student   student;
    private Classroom classroom;
    private SubjectAssignment  assignment;
    private StudentAffiliation affiliation;

    @BeforeEach
    void setUp() {
        mathSubject = new Subject("수학");
        ReflectionTestUtils.setField(mathSubject, "id", SUBJECT_ID);

        midtermExam = Exam.builder()
                .academicYear(2026).semester(1).examType(ExamType.MIDTERM)
                .name("1학기 중간고사").maxScore(100).weight(0.5)
                .examDate(LocalDate.of(2026, 4, 25)).published(true)
                .build();
        ReflectionTestUtils.setField(midtermExam, "id", EXAM_ID);

        User teacherUser = User.builder()
                .name("최수학").gender(Gender.MALE)
                .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacherUser, "id", TEACHER_ID);

        Teacher teacher = Teacher.builder()
                .user(teacherUser).employeeNumber("EMP001")
                .officeLocation("본관").officePhoneNumber("02-000")
                .employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);

        classroom = Classroom.builder()
                .academicYear(2026).semester(1).grade(1).classNum(4)
                .homeroomTeacher(teacher).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM_ID);

        User studentUser = User.builder()
                .name("1-4학생01").gender(Gender.MALE)
                .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(studentUser, "id", STUDENT_ID);

        student = Student.builder().user(studentUser).enrollmentYear(2026).build();
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);

        assignment = SubjectAssignment.builder()
                .teacher(teacher).classroom(classroom).subject(mathSubject)
                .academicYear(2026).semester(1).build();

        affiliation = StudentAffiliation.builder()
                .student(student).classroom(classroom).studentNum(1).build();

        // 학기는 기본 OPEN 상태
        lenient().when(semesterClosureService.isClosed(anyInt(), anyInt())).thenReturn(false);
    }

    // ==========================================================================
    // saveGrades
    // ==========================================================================

    @Nested
    @DisplayName("성적 일괄 입력 (saveGrades)")
    class SaveGradesTest {

        @Test
        @DisplayName("성공: 신규 성적 저장 + recalculator 호출")
        void saveGrades_Success_NewGrades() {
            GradeSaveRequest request = buildSaveRequest(STUDENT_ID, 85);

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndSubjectIdAndStudentIds(any(), any(), anyList()))
                    .willReturn(List.of());

            studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request);

            verify(studentGradeRepository).save(any(StudentGrade.class));
            verify(semesterStatRecalculator).refresh(eq(student), eq(2026), eq(1));
        }

        @Test
        @DisplayName("성공: 기존 성적이 있으면 updateScore 호출 (새로 저장 X)")
        void saveGrades_Success_UpdatesExistingGrades() {
            StudentGrade existingGrade = StudentGrade.builder()
                    .student(student).exam(midtermExam).subject(mathSubject).rawScore(70).build();
            GradeSaveRequest request = buildSaveRequest(STUDENT_ID, 90);

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndSubjectIdAndStudentIds(any(), any(), anyList()))
                    .willReturn(List.of(existingGrade));

            studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request);

            assertThat(existingGrade.getRawScore()).isEqualTo(90);
            verify(studentGradeRepository, never()).save(any(StudentGrade.class));
            verify(semesterStatRecalculator).refresh(eq(student), eq(2026), eq(1));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 examId → ExamNotFound")
        void saveGrades_Fail_WhenExamNotFound() {
            given(examRepository.findById(EXAM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, buildSaveRequest(STUDENT_ID, 85)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 시험 정보를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패: 마감된 학기는 입력 차단 (SEMESTER_CLOSED)")
        void saveGrades_Fail_WhenSemesterClosed() {
            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(semesterClosureService.isClosed(2026, 1)).willReturn(true);

            assertThatThrownBy(() ->
                    studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID,
                            buildSaveRequest(STUDENT_ID, 85)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("마감되어");
        }

        @Test
        @DisplayName("실패: 담당 교사가 아닌 경우 AccessDeniedException")
        void saveGrades_Fail_WhenTeacherNotAuthorized() {
            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, buildSaveRequest(STUDENT_ID, 85)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 수업에 대한 성적 입력 권한이 없습니다");
        }

        @Test
        @DisplayName("실패: 학급에 속하지 않는 학생이 포함된 경우")
        void saveGrades_Fail_WhenStudentNotInClassroom() {
            Long outsiderStudentId = 999L;
            GradeSaveRequest request = buildSaveRequest(outsiderStudentId, 85);

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));

            assertThatThrownBy(() ->
                    studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("학급에 속하지 않는 학생");
        }

        @Test
        @DisplayName("성공: ABSENT(결시)면 rawScore=null로 강제 저장")
        void saveGrades_AbsentStudent_NullScore() {
            GradeSaveRequest request = GradeSaveRequest.builder()
                    .examId(EXAM_ID)
                    .scores(List.of(GradeSaveRequest.StudentScoreDto.builder()
                            .studentId(STUDENT_ID)
                            .rawScore(99)
                            .attendanceStatus(ExamAttendanceStatus.ABSENT)
                            .build()))
                    .build();

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndSubjectIdAndStudentIds(any(), any(), anyList()))
                    .willReturn(List.of());

            studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request);

            ArgumentCaptor<StudentGrade> captor = ArgumentCaptor.forClass(StudentGrade.class);
            verify(studentGradeRepository).save(captor.capture());
            StudentGrade saved = captor.getValue();
            assertThat(saved.getRawScore()).isNull();
            assertThat(saved.getAttendanceStatus()).isEqualTo(ExamAttendanceStatus.ABSENT);
        }

        @Test
        @DisplayName("성공: CHEATED(부정행위)면 rawScore=0으로 강제 저장")
        void saveGrades_CheatedStudent_ZeroScore() {
            GradeSaveRequest request = GradeSaveRequest.builder()
                    .examId(EXAM_ID)
                    .scores(List.of(GradeSaveRequest.StudentScoreDto.builder()
                            .studentId(STUDENT_ID)
                            .rawScore(85)
                            .attendanceStatus(ExamAttendanceStatus.CHEATED)
                            .build()))
                    .build();

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndSubjectIdAndStudentIds(any(), any(), anyList()))
                    .willReturn(List.of());

            studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request);

            ArgumentCaptor<StudentGrade> captor = ArgumentCaptor.forClass(StudentGrade.class);
            verify(studentGradeRepository).save(captor.capture());
            StudentGrade saved = captor.getValue();
            assertThat(saved.getRawScore()).isEqualTo(0);
            assertThat(saved.getAttendanceStatus()).isEqualTo(ExamAttendanceStatus.CHEATED);
        }

        @Test
        @DisplayName("실패: 시험 만점을 넘는 점수 입력 시 ExamScoreOutOfRange")
        void saveGrades_Fail_WhenScoreExceedsMaxScore() {
            GradeSaveRequest request = buildSaveRequest(STUDENT_ID, 150);

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));

            assertThatThrownBy(() ->
                    studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("점수는 0 ~ 100 범위");
        }
    }

    // ==========================================================================
    // updateGrade
    // ==========================================================================

    @Nested
    @DisplayName("성적 수정 (updateGrade)")
    class UpdateGradeTest {

        @Test
        @DisplayName("성공: 점수 변경 시 grade.update + recalculator 호출")
        void updateGrade_Success() {
            StudentGrade grade = buildGrade(70);
            GradeUpdateRequest request = GradeUpdateRequest.builder().rawScore(95).build();

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));

            studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID, request);

            assertThat(grade.getRawScore()).isEqualTo(95);
            verify(semesterStatRecalculator).refresh(eq(student), eq(2026), eq(1));
        }

        @Test
        @DisplayName("성공: 점수가 변경되면 GradeHistory가 기록됨 (before/after/reason 포함)")
        void updateGrade_RecordsHistoryWhenScoreChanges() {
            StudentGrade grade = buildGrade(70);
            User teacherUser = User.builder().name("최수학").gender(Gender.MALE)
                    .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(userRepository.findById(TEACHER_ID)).willReturn(Optional.of(teacherUser));

            studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                    GradeUpdateRequest.builder().rawScore(90).reason("재채점 결과 반영").build());

            ArgumentCaptor<GradeHistory> captor = ArgumentCaptor.forClass(GradeHistory.class);
            verify(gradeHistoryRepository).save(captor.capture());
            GradeHistory recorded = captor.getValue();
            assertThat(recorded.getBeforeScore()).isEqualTo(70);
            assertThat(recorded.getAfterScore()).isEqualTo(90);
            assertThat(recorded.getChangedByUserId()).isEqualTo(TEACHER_ID);
            assertThat(recorded.getChangedByName()).isEqualTo("최수학");
            assertThat(recorded.getReason()).isEqualTo("재채점 결과 반영");
        }

        @Test
        @DisplayName("성공: 점수가 동일하면 GradeHistory 기록 없음 + recalculator도 호출 안 됨")
        void updateGrade_SkipsHistoryWhenScoreUnchanged() {
            StudentGrade grade = buildGrade(70);

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));

            studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                    GradeUpdateRequest.builder().rawScore(70).build());

            verify(gradeHistoryRepository, never()).save(any(GradeHistory.class));
            verify(semesterStatRecalculator, never()).refresh(any(), any(), any());
        }

        @Test
        @DisplayName("실패: 마감된 학기 grade 수정 차단")
        void updateGrade_Fail_WhenSemesterClosed() {
            StudentGrade grade = buildGrade(70);

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(semesterClosureService.isClosed(2026, 1)).willReturn(true);

            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("마감되어");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 gradeId → GradeNotFound")
        void updateGrade_Fail_WhenGradeNotFound() {
            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("성적 정보를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패: URL 과목 ID와 성적의 과목이 다른 경우")
        void updateGrade_Fail_WhenSubjectMismatch() {
            Subject korSubject = new Subject("국어");
            ReflectionTestUtils.setField(korSubject, "id", 99L);
            StudentGrade grade = StudentGrade.builder()
                    .student(student).exam(midtermExam).subject(korSubject).rawScore(70).build();

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));

            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("성적 과목 정보가 일치하지 않습니다");
        }

        @Test
        @DisplayName("실패: 학생이 해당 학급에 속하지 않는 경우")
        void updateGrade_Fail_WhenStudentNotInClassroom() {
            StudentGrade grade = buildGrade(70);

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 학생은 이 학급에 속하지 않습니다");
        }

        @Test
        @DisplayName("실패: 담당 교사가 아닌 경우")
        void updateGrade_Fail_WhenTeacherNotAuthorized() {
            StudentGrade grade = buildGrade(70);

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 수업에 대한 성적 수정 권한이 없습니다");
        }

        @Test
        @DisplayName("실패: 시험 만점을 넘는 점수로 수정 시 ExamScoreOutOfRange")
        void updateGrade_Fail_WhenScoreExceedsMaxScore() {
            StudentGrade grade = buildGrade(70);

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));

            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(150).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("점수는 0 ~ 100 범위");
        }
    }

    // ==========================================================================
    // getSubjectGrades
    // ==========================================================================

    @Nested
    @DisplayName("과목별 성적 조회 (getSubjectGrades)")
    class GetSubjectGradesTest {

        @Test
        @DisplayName("성공: 입력된 학생은 rawScore, 미입력 학생은 null")
        void getSubjectGrades_Success() {
            StudentGrade grade = buildGrade(85);
            ReflectionTestUtils.setField(grade, "id", GRADE_ID);

            User studentUser2 = User.builder().name("1-4학생02").gender(Gender.FEMALE)
                    .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
            ReflectionTestUtils.setField(studentUser2, "id", 2L);
            Student student2 = Student.builder().user(studentUser2).enrollmentYear(2026).build();
            ReflectionTestUtils.setField(student2, "id", 2L);
            StudentAffiliation affiliation2 = StudentAffiliation.builder()
                    .student(student2).classroom(classroom).studentNum(2).build();

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation, affiliation2));
            given(studentGradeRepository.findByExamIdAndSubjectIdAndStudentIds(any(), any(), anyList()))
                    .willReturn(List.of(grade));

            GradeListResponse response = studentGradeService.getSubjectGrades(
                    CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, EXAM_ID);

            assertThat(response.getSubjectName()).isEqualTo("수학");
            assertThat(response.getExamType()).isEqualTo(ExamType.MIDTERM);
            assertThat(response.getExamName()).isEqualTo("1학기 중간고사");
            assertThat(response.getMaxScore()).isEqualTo(100);
            assertThat(response.getGrades()).hasSize(2);

            GradeListResponse.StudentGradeDto inputtedDto = response.getGrades().get(0);
            assertThat(inputtedDto.getStudentName()).isEqualTo("1-4학생01");
            assertThat(inputtedDto.getRawScore()).isEqualTo(85);
            assertThat(inputtedDto.getGradeId()).isEqualTo(GRADE_ID);

            GradeListResponse.StudentGradeDto notInputtedDto = response.getGrades().get(1);
            assertThat(notInputtedDto.getStudentName()).isEqualTo("1-4학생02");
            assertThat(notInputtedDto.getRawScore()).isNull();
            assertThat(notInputtedDto.getGradeId()).isNull();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 examId → ExamNotFound")
        void getSubjectGrades_Fail_WhenExamNotFound() {
            given(examRepository.findById(EXAM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    studentGradeService.getSubjectGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, EXAM_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 시험 정보를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패: 담당 교사가 아닌 경우")
        void getSubjectGrades_Fail_WhenTeacherNotAuthorized() {
            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    studentGradeService.getSubjectGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, EXAM_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 수업의 성적 조회 권한이 없습니다");
        }
    }

    // ==========================================================================
    // getClassroomGrades
    // ==========================================================================

    @Nested
    @DisplayName("학급 전체 성적 조회 (getClassroomGrades)")
    class GetClassroomGradesTest {

        @Test
        @DisplayName("성공: 담임 교사는 학급 전체 과목 성적 + 학기 누적 통계 + 등급 조회 가능")
        void getClassroomGrades_Success() {
            StudentGrade grade = buildGrade(85);
            StudentSemesterStat stat = StudentSemesterStat.builder()
                    .student(student).academicYear(2026).semester(1)
                    .totalScore(175.0).averageScore(87.5).gradeLevel(GradeLevel.B).build();

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.of(classroom));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndStudentIds(any(), anyList()))
                    .willReturn(List.of(grade));
            given(semesterStatRepository.findByStudentIdsAndYearAndSemester(anyList(), eq(2026), eq(1)))
                    .willReturn(List.of(stat));

            ClassroomGradeResponse response = studentGradeService.getClassroomGrades(
                    CLASSROOM_ID, TEACHER_ID, EXAM_ID);

            assertThat(response.getExamName()).isEqualTo("1학기 중간고사");
            assertThat(response.getStudents()).hasSize(1);
            ClassroomGradeResponse.StudentAllGradesDto studentDto = response.getStudents().get(0);
            assertThat(studentDto.getTotalScore()).isEqualTo(175.0);
            assertThat(studentDto.getAverageScore()).isEqualTo(87.5);
            assertThat(studentDto.getGradeLevel()).isEqualTo(GradeLevel.B);
        }

        @Test
        @DisplayName("성공: 성적 미입력 학생은 totalScore=0.0, gradeLevel=null")
        void getClassroomGrades_Success_StudentWithNoGrades() {
            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.of(classroom));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndStudentIds(any(), anyList()))
                    .willReturn(List.of());
            given(semesterStatRepository.findByStudentIdsAndYearAndSemester(anyList(), eq(2026), eq(1)))
                    .willReturn(List.of());

            ClassroomGradeResponse response = studentGradeService.getClassroomGrades(
                    CLASSROOM_ID, TEACHER_ID, EXAM_ID);

            ClassroomGradeResponse.StudentAllGradesDto studentDto = response.getStudents().get(0);
            assertThat(studentDto.getTotalScore()).isEqualTo(0.0);
            assertThat(studentDto.getGradeLevel()).isNull();
        }

        @Test
        @DisplayName("실패: 담임이 아닌 교사가 다른 학급 조회 시 AccessDenied")
        void getClassroomGrades_Fail_WhenWrongClassroom() {
            Classroom anotherClassroom = Classroom.builder()
                    .academicYear(2026).semester(1).grade(1).classNum(1).build();
            ReflectionTestUtils.setField(anotherClassroom, "id", 999L);

            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.of(anotherClassroom));

            assertThatThrownBy(() ->
                    studentGradeService.getClassroomGrades(CLASSROOM_ID, TEACHER_ID, EXAM_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("담임 교사만 전체 성적을 조회할 수 있습니다");
        }

        @Test
        @DisplayName("실패: 담임 반이 아예 없는 교사인 경우")
        void getClassroomGrades_Fail_WhenNotHomeroomTeacher() {
            given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(midtermExam));
            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    studentGradeService.getClassroomGrades(CLASSROOM_ID, TEACHER_ID, EXAM_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("담임 교사만 전체 성적을 조회할 수 있습니다");
        }
    }

    // ==========================================================================
    // 헬퍼
    // ==========================================================================

    private GradeSaveRequest buildSaveRequest(Long studentId, int rawScore) {
        return GradeSaveRequest.builder()
                .examId(EXAM_ID)
                .scores(List.of(GradeSaveRequest.StudentScoreDto.builder()
                        .studentId(studentId).rawScore(rawScore).build()))
                .build();
    }

    private StudentGrade buildGrade(int rawScore) {
        StudentGrade grade = StudentGrade.builder()
                .student(student).exam(midtermExam).subject(mathSubject).rawScore(rawScore).build();
        ReflectionTestUtils.setField(grade, "id", GRADE_ID);
        return grade;
    }
}
