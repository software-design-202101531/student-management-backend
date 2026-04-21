package com.school.studentmanagement.teacher.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.global.enums.*;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.teacher.dto.TeacherAssignmentResponse;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TeacherAssignmentServiceTest {

    @InjectMocks private TeacherAssignmentService teacherAssignmentService;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;
    @Mock private AcademicCalendarUtil academicCalendarUtil;

    private static final Long TEACHER_ID    = 100L;
    private static final Long CLASSROOM_ID  = 200L;
    private static final Long SUBJECT_ID    = 1L;
    private static final Long ASSIGNMENT_ID = 50L;

    private SubjectAssignment assignment;

    @BeforeEach
    void setUp() {
        Subject mathSubject = new Subject("수학");
        ReflectionTestUtils.setField(mathSubject, "id", SUBJECT_ID);

        User teacherUser = User.builder().id(TEACHER_ID).name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        Teacher teacher = Teacher.builder().user(teacherUser).employeeNumber("EMP001")
                .officeLocation("본관").officePhoneNumber("02-000").employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);

        Classroom classroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(4).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM_ID);

        assignment = SubjectAssignment.builder()
                .teacher(teacher).classroom(classroom).subject(mathSubject).academicYear(2026).semester(1).build();
        ReflectionTestUtils.setField(assignment, "id", ASSIGNMENT_ID);

        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
    }

    @Nested
    @DisplayName("담당 수업 반 조회 (getMyAssignments)")
    class GetMyAssignmentsTest {

        @Test
        @DisplayName("성공: 담당 과목 배정이 있으면 DTO 리스트 반환")
        void getMyAssignments_Success_ReturnsMappedDTOs() {
            given(subjectAssignmentRepository.findAllMyAssignments(TEACHER_ID, 2026, 1))
                    .willReturn(List.of(assignment));

            List<TeacherAssignmentResponse> result = teacherAssignmentService.getMyAssignments(TEACHER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssignmentId()).isEqualTo(ASSIGNMENT_ID);
            assertThat(result.get(0).getClassroomId()).isEqualTo(CLASSROOM_ID);
            assertThat(result.get(0).getGrade()).isEqualTo(1);
            assertThat(result.get(0).getClassNum()).isEqualTo(4);
            assertThat(result.get(0).getSubjectId()).isEqualTo(SUBJECT_ID);
            assertThat(result.get(0).getSubjectName()).isEqualTo("수학");
        }

        @Test
        @DisplayName("성공: 담당 과목 배정이 없으면 빈 리스트 반환")
        void getMyAssignments_Success_EmptyList() {
            given(subjectAssignmentRepository.findAllMyAssignments(TEACHER_ID, 2026, 1))
                    .willReturn(List.of());

            List<TeacherAssignmentResponse> result = teacherAssignmentService.getMyAssignments(TEACHER_ID);

            assertThat(result).isEmpty();
        }
    }
}
