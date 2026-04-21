package com.school.studentmanagement.classroom.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.dto.StudentListResponse;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ClassroomStudentServiceTest {

    @InjectMocks private ClassroomStudentService classroomStudentService;
    @Mock private ClassRoomRepository classRoomRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private AcademicCalendarUtil academicCalendarUtil;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;

    private static final Long TEACHER_ID   = 100L;
    private static final Long CLASSROOM_ID = 200L;
    private static final Long STUDENT_ID   = 1L;

    private Classroom classroom;
    private StudentAffiliation affiliation;

    @BeforeEach
    void setUp() {
        User teacherUser = User.builder().id(TEACHER_ID).name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        Teacher teacher = Teacher.builder().user(teacherUser).employeeNumber("EMP001")
                .officeLocation("본관").officePhoneNumber("02-000").employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);

        classroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(4)
                .homeroomTeacher(teacher).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM_ID);

        User studentUser = User.builder().id(STUDENT_ID).name("1-4학생01")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        Student student = Student.builder().id(STUDENT_ID).user(studentUser).enrollmentYear(2026).build();
        affiliation = StudentAffiliation.builder().student(student).classroom(classroom).studentNum(1).build();

        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
    }

    // ==========================================================================
    // getMyHomeroomStudents
    // ==========================================================================

    @Nested
    @DisplayName("담임 반 학생 목록 조회 (getMyHomeroomStudents)")
    class GetMyHomeroomStudentsTest {

        @Test
        @DisplayName("성공: 담임 반 학생 목록을 DTO 리스트로 반환")
        void getMyHomeroomStudents_Success() {
            // Given
            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.of(classroom));
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));

            // When
            List<StudentListResponse> result = classroomStudentService.getMyHomeroomStudents(TEACHER_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(result.get(0).getStudentNum()).isEqualTo(1);
            assertThat(result.get(0).getName()).isEqualTo("1-4학생01");
        }

        @Test
        @DisplayName("실패: 담임 반이 없으면 IllegalArgumentException")
        void getMyHomeroomStudents_Fail_NoHomeroomClass() {
            // Given
            given(classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(TEACHER_ID, 2026, 1))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> classroomStudentService.getMyHomeroomStudents(TEACHER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("2026학년도 1학기에는 담임을 담당한 반이 없습니다");
        }
    }

    // ==========================================================================
    // getStudentsInClassroom
    // ==========================================================================

    @Nested
    @DisplayName("수업 담당 반 학생 목록 조회 (getStudentsInClassroom)")
    class GetStudentsInClassroomTest {

        @Test
        @DisplayName("성공: 수업 담당 반 학생 목록을 DTO 리스트로 반환")
        void getStudentsInClassroom_Success() {
            // Given
            given(subjectAssignmentRepository.existsByTeacherIdAndClassroomIdAndYear(TEACHER_ID, CLASSROOM_ID, 2026, 1))
                    .willReturn(true);
            given(studentAffiliationRepository.findAllByClassroomId(CLASSROOM_ID))
                    .willReturn(List.of(affiliation));

            // When
            List<StudentListResponse> result = classroomStudentService.getStudentsInClassroom(CLASSROOM_ID, TEACHER_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(result.get(0).getName()).isEqualTo("1-4학생01");
        }

        @Test
        @DisplayName("실패: 수업 담당 교사가 아닌 경우 → AccessDeniedException")
        void getStudentsInClassroom_Fail_NotSubjectTeacher() {
            // Given
            given(subjectAssignmentRepository.existsByTeacherIdAndClassroomIdAndYear(TEACHER_ID, CLASSROOM_ID, 2026, 1))
                    .willReturn(false);

            // When & Then
            assertThatThrownBy(() -> classroomStudentService.getStudentsInClassroom(CLASSROOM_ID, TEACHER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 반 수업의 담당 교사가 아닙니다");
        }
    }
}
