package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse;
import com.school.studentmanagement.grade.dto.GradeListResponse;
import com.school.studentmanagement.grade.dto.GradeSaveRequest;
import com.school.studentmanagement.grade.dto.GradeUpdateRequest;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
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

    // ─── 공통 상수 ───────────────────────────────────────────────────────────────
    private static final Long TEACHER_ID   = 100L;
    private static final Long CLASSROOM_ID = 200L;
    private static final Long SUBJECT_ID   = 1L;
    private static final Long STUDENT_ID   = 1L;
    private static final Long EXAM_ID      = 10L;
    private static final Long GRADE_ID     = 500L;

    // ─── 공통 픽스처 ─────────────────────────────────────────────────────────────
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
                .academicYear(2026).semester(1).examType(ExamType.MIDTERM).build();
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
    }

    // ==========================================================================
    // saveGrades
    // ==========================================================================

    @Nested
    @DisplayName("성적 일괄 입력 (saveGrades)")
    class SaveGradesTest {

        @Test
        @DisplayName("성공: 기존 시험 없으면 Exam 새로 생성 후 StudentGrade 저장, 통계 초기화")
        void saveGrades_Success_NewExamAndGrades() {
            // Given
            GradeSaveRequest request = buildSaveRequest(STUDENT_ID, 85);

            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(examRepository.findByAcademicYearAndSemesterAndExamType(2026, 1, ExamType.MIDTERM))
                    .willReturn(Optional.empty());
            given(examRepository.save(any())).willReturn(midtermExam);
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndSubjectIdAndStudentIds(any(), any(), anyList()))
                    .willReturn(List.of());
            given(studentGradeRepository.sumTotalScoreByStudentAndSemester(STUDENT_ID, 2026, 1)).willReturn(85);
            given(studentGradeRepository.countByStudentAndSemester(STUDENT_ID, 2026, 1)).willReturn(1L);
            given(semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When
            studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request);

            // Then
            verify(examRepository).save(any(Exam.class));
            verify(studentGradeRepository).save(any(StudentGrade.class));
            verify(semesterStatRepository).save(any(StudentSemesterStat.class));
        }

        @Test
        @DisplayName("성공: 기존 성적이 있으면 updateScore 호출 (새로 저장 X), 통계 갱신")
        void saveGrades_Success_UpdatesExistingGrades() {
            // Given
            StudentGrade existingGrade = StudentGrade.builder()
                    .student(student).exam(midtermExam).subject(mathSubject).rawScore(70).build();
            GradeSaveRequest request = buildSaveRequest(STUDENT_ID, 90);

            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(examRepository.findByAcademicYearAndSemesterAndExamType(2026, 1, ExamType.MIDTERM))
                    .willReturn(Optional.of(midtermExam));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndSubjectIdAndStudentIds(any(), any(), anyList()))
                    .willReturn(List.of(existingGrade));
            given(studentGradeRepository.sumTotalScoreByStudentAndSemester(STUDENT_ID, 2026, 1)).willReturn(90);
            given(studentGradeRepository.countByStudentAndSemester(STUDENT_ID, 2026, 1)).willReturn(1L);
            given(semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When
            studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request);

            // Then
            assertThat(existingGrade.getRawScore()).isEqualTo(90);
            verify(studentGradeRepository, never()).save(any(StudentGrade.class));
        }

        @Test
        @DisplayName("실패: 담당 교사가 아닌 경우 AccessDeniedException")
        void saveGrades_Fail_WhenTeacherNotAuthorized() {
            // Given
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, buildSaveRequest(STUDENT_ID, 85)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 수업에 대한 성적 입력 권한이 없습니다");
        }

        @Test
        @DisplayName("실패: 학급에 속하지 않는 학생이 포함된 경우 IllegalArgumentException")
        void saveGrades_Fail_WhenStudentNotInClassroom() {
            // Given
            Long outsiderStudentId = 999L;
            GradeSaveRequest request = buildSaveRequest(outsiderStudentId, 85);

            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(examRepository.findByAcademicYearAndSemesterAndExamType(2026, 1, ExamType.MIDTERM))
                    .willReturn(Optional.of(midtermExam));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation)); // student.id=1L 만 포함

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.saveGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("학급에 속하지 않는 학생");
        }
    }

    // ==========================================================================
    // updateGrade
    // ==========================================================================

    @Nested
    @DisplayName("성적 수정 (updateGrade)")
    class UpdateGradeTest {

        @Test
        @DisplayName("성공: 점수 변경 후 학기 통계 신규 생성")
        void updateGrade_Success_CreateNewStat() {
            // Given
            StudentGrade grade = buildGrade(70);
            GradeUpdateRequest request = GradeUpdateRequest.builder().rawScore(95).build();

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentGradeRepository.sumTotalScoreByStudentAndSemester(STUDENT_ID, 2026, 1)).willReturn(95);
            given(studentGradeRepository.countByStudentAndSemester(STUDENT_ID, 2026, 1)).willReturn(1L);
            given(semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When
            studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID, request);

            // Then
            assertThat(grade.getRawScore()).isEqualTo(95);
            verify(semesterStatRepository).save(any(StudentSemesterStat.class));
        }

        @Test
        @DisplayName("성공: 기존 학기 통계가 있으면 updateStats 호출 (새로 저장 X)")
        void updateGrade_Success_UpdatesExistingSemesterStat() {
            // Given
            StudentGrade grade = buildGrade(70);
            StudentSemesterStat existingStat = StudentSemesterStat.builder()
                    .student(student).academicYear(2026).semester(1)
                    .totalScore(70).averageScore(70.0).build();

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(studentGradeRepository.sumTotalScoreByStudentAndSemester(STUDENT_ID, 2026, 1)).willReturn(90);
            given(studentGradeRepository.countByStudentAndSemester(STUDENT_ID, 2026, 1)).willReturn(1L);
            given(semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(STUDENT_ID, 2026, 1))
                    .willReturn(Optional.of(existingStat));

            // When
            studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                    GradeUpdateRequest.builder().rawScore(90).build());

            // Then
            assertThat(existingStat.getTotalScore()).isEqualTo(90);
            assertThat(existingStat.getAverageScore()).isEqualTo(90.0);
            verify(semesterStatRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 gradeId → IllegalArgumentException")
        void updateGrade_Fail_WhenGradeNotFound() {
            // Given
            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("성적 정보를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패: URL 과목 ID와 성적의 과목이 다른 경우 IllegalArgumentException")
        void updateGrade_Fail_WhenSubjectMismatch() {
            // Given - grade의 과목은 국어(id=99), URL의 subjectId는 수학(1L)
            Subject korSubject = new Subject("국어");
            ReflectionTestUtils.setField(korSubject, "id", 99L);
            StudentGrade grade = StudentGrade.builder()
                    .student(student).exam(midtermExam).subject(korSubject).rawScore(70).build();

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("성적 과목 정보가 일치하지 않습니다");
        }

        @Test
        @DisplayName("실패: 학생이 해당 학급에 속하지 않는 경우 IllegalArgumentException")
        void updateGrade_Fail_WhenStudentNotInClassroom() {
            // Given
            StudentGrade grade = buildGrade(70);

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 학생은 이 학급에 속하지 않습니다");
        }

        @Test
        @DisplayName("실패: 담당 교사가 아닌 경우 AccessDeniedException")
        void updateGrade_Fail_WhenTeacherNotAuthorized() {
            // Given
            StudentGrade grade = buildGrade(70);

            given(studentGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
            given(studentAffiliationRepository.findByStudentIdAndClassroomId(STUDENT_ID, CLASSROOM_ID))
                    .willReturn(Optional.of(affiliation));
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.updateGrade(CLASSROOM_ID, SUBJECT_ID, GRADE_ID, TEACHER_ID,
                            GradeUpdateRequest.builder().rawScore(90).build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 수업에 대한 성적 수정 권한이 없습니다");
        }
    }

    // ==========================================================================
    // getSubjectGrades
    // ==========================================================================

    @Nested
    @DisplayName("과목별 성적 조회 (getSubjectGrades)")
    class GetSubjectGradesTest {

        @Test
        @DisplayName("성공: 성적이 입력된 학생은 rawScore 반환, 미입력 학생은 null 반환")
        void getSubjectGrades_Success() {
            // Given
            StudentGrade grade = buildGrade(85);
            ReflectionTestUtils.setField(grade, "id", GRADE_ID);

            // student2: 미입력
            User studentUser2 = User.builder().name("1-4학생02").gender(Gender.FEMALE)
                    .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
            ReflectionTestUtils.setField(studentUser2, "id", 2L);
            Student student2 = Student.builder().user(studentUser2).enrollmentYear(2026).build();
            ReflectionTestUtils.setField(student2, "id", 2L);
            StudentAffiliation affiliation2 = StudentAffiliation.builder()
                    .student(student2).classroom(classroom).studentNum(2).build();

            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(examRepository.findByAcademicYearAndSemesterAndExamType(2026, 1, ExamType.MIDTERM))
                    .willReturn(Optional.of(midtermExam));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation, affiliation2));
            given(studentGradeRepository.findByExamIdAndSubjectIdAndStudentIds(any(), any(), anyList()))
                    .willReturn(List.of(grade)); // student1만 입력됨

            // When
            GradeListResponse response = studentGradeService.getSubjectGrades(
                    CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, 2026, 1, ExamType.MIDTERM);

            // Then
            assertThat(response.getSubjectName()).isEqualTo("수학");
            assertThat(response.getExamType()).isEqualTo(ExamType.MIDTERM);
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
        @DisplayName("실패: 담당 교사가 아닌 경우 AccessDeniedException")
        void getSubjectGrades_Fail_WhenTeacherNotAuthorized() {
            // Given
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.getSubjectGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, 2026, 1, ExamType.MIDTERM))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 수업의 성적 조회 권한이 없습니다");
        }

        @Test
        @DisplayName("실패: 시험 정보가 없는 경우 IllegalArgumentException")
        void getSubjectGrades_Fail_WhenExamNotFound() {
            // Given
            given(subjectAssignmentRepository.findValidAssignment(TEACHER_ID, CLASSROOM_ID, SUBJECT_ID, 2026, 1))
                    .willReturn(Optional.of(assignment));
            given(examRepository.findByAcademicYearAndSemesterAndExamType(2026, 1, ExamType.MIDTERM))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.getSubjectGrades(CLASSROOM_ID, SUBJECT_ID, TEACHER_ID, 2026, 1, ExamType.MIDTERM))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 시험 정보를 찾을 수 없습니다");
        }
    }

    // ==========================================================================
    // getClassroomGrades
    // ==========================================================================

    @Nested
    @DisplayName("학급 전체 성적 조회 (getClassroomGrades)")
    class GetClassroomGradesTest {

        @Test
        @DisplayName("성공: 담임 교사는 학급 전체 과목 성적 + 학기 누적 통계를 조회 가능")
        void getClassroomGrades_Success() {
            // Given
            StudentGrade grade = buildGrade(85);
            StudentSemesterStat stat = StudentSemesterStat.builder()
                    .student(student).academicYear(2026).semester(1)
                    .totalScore(175).averageScore(87.5).build();

            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.of(classroom)); // classroom.id == CLASSROOM_ID(200L) → filter 통과
            given(examRepository.findByAcademicYearAndSemesterAndExamType(2026, 1, ExamType.MIDTERM))
                    .willReturn(Optional.of(midtermExam));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndStudentIds(any(), anyList()))
                    .willReturn(List.of(grade));
            given(semesterStatRepository.findByStudentIdsAndYearAndSemester(anyList(), eq(2026), eq(1)))
                    .willReturn(List.of(stat));

            // When
            ClassroomGradeResponse response = studentGradeService.getClassroomGrades(
                    CLASSROOM_ID, TEACHER_ID, 2026, 1, ExamType.MIDTERM);

            // Then
            assertThat(response.getStudents()).hasSize(1);
            ClassroomGradeResponse.StudentAllGradesDto studentDto = response.getStudents().get(0);
            assertThat(studentDto.getStudentName()).isEqualTo("1-4학생01");
            assertThat(studentDto.getTotalScore()).isEqualTo(175);
            assertThat(studentDto.getAverageScore()).isEqualTo(87.5);
            assertThat(studentDto.getSubjectScores()).hasSize(1);
            assertThat(studentDto.getSubjectScores().get(0).getSubjectName()).isEqualTo("수학");
            assertThat(studentDto.getSubjectScores().get(0).getRawScore()).isEqualTo(85);
        }

        @Test
        @DisplayName("성공: 성적 미입력 학생은 totalScore=0, subjectScores=[] 으로 반환")
        void getClassroomGrades_Success_StudentWithNoGrades() {
            // Given
            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.of(classroom));
            given(examRepository.findByAcademicYearAndSemesterAndExamType(2026, 1, ExamType.MIDTERM))
                    .willReturn(Optional.of(midtermExam));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));
            given(studentGradeRepository.findByExamIdAndStudentIds(any(), anyList()))
                    .willReturn(List.of()); // 성적 없음
            given(semesterStatRepository.findByStudentIdsAndYearAndSemester(anyList(), eq(2026), eq(1)))
                    .willReturn(List.of()); // 통계 없음

            // When
            ClassroomGradeResponse response = studentGradeService.getClassroomGrades(
                    CLASSROOM_ID, TEACHER_ID, 2026, 1, ExamType.MIDTERM);

            // Then
            ClassroomGradeResponse.StudentAllGradesDto studentDto = response.getStudents().get(0);
            assertThat(studentDto.getTotalScore()).isEqualTo(0);
            assertThat(studentDto.getAverageScore()).isEqualTo(0.0);
            assertThat(studentDto.getSubjectScores()).isEmpty();
        }

        @Test
        @DisplayName("실패: 담임이 아닌 교사가 다른 학급 조회 시 AccessDeniedException")
        void getClassroomGrades_Fail_WhenWrongClassroom() {
            // Given - 교사의 담임 반은 999L, 요청한 classroomId는 200L
            Classroom anotherClassroom = Classroom.builder()
                    .academicYear(2026).semester(1).grade(1).classNum(1).build();
            ReflectionTestUtils.setField(anotherClassroom, "id", 999L);

            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.of(anotherClassroom));

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.getClassroomGrades(CLASSROOM_ID, TEACHER_ID, 2026, 1, ExamType.MIDTERM))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("담임 교사만 전체 성적을 조회할 수 있습니다");
        }

        @Test
        @DisplayName("실패: 담임 반이 아예 없는 교사인 경우 AccessDeniedException")
        void getClassroomGrades_Fail_WhenNotHomeroomTeacher() {
            // Given
            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    studentGradeService.getClassroomGrades(CLASSROOM_ID, TEACHER_ID, 2026, 1, ExamType.MIDTERM))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("담임 교사만 전체 성적을 조회할 수 있습니다");
        }
    }

    // ==========================================================================
    // 테스트 헬퍼
    // ==========================================================================

    private GradeSaveRequest buildSaveRequest(Long studentId, int rawScore) {
        return GradeSaveRequest.builder()
                .academicYear(2026).semester(1).examType(ExamType.MIDTERM)
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
