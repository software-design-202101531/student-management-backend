package com.school.studentmanagement.global.validation;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TeacherStudentRelationValidatorTest {

    @InjectMocks private TeacherStudentRelationValidator validator;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private SubjectAssignmentRepository subjectAssignmentRepository;
    @Mock private AcademicCalendarUtil academicCalendarUtil;

    private static final long TEACHER_ID = 1L;
    private static final long STUDENT_ID = 10L;
    private static final long CLASSROOM_ID = 200L;

    @BeforeEach
    void stubCalendar() {
        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
    }

    @Test
    @DisplayName("학생 미배정 → STUDENT_NOT_ASSIGNED")
    void notAssigned() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1)).willReturn(Optional.empty());
        assertThatThrownBy(() -> validator.validateCanWriteFor(TEACHER_ID, STUDENT_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("담임 교사면 통과 (과목담당 조회 없이)")
    void homeroom_passes() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                .willReturn(Optional.of(affiliation(teacher(TEACHER_ID)))); // 담임 == 요청 교사
        assertThatCode(() -> validator.validateCanWriteFor(TEACHER_ID, STUDENT_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("담임은 아니지만 과목 담당이면 통과")
    void subjectTeacher_passes() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                .willReturn(Optional.of(affiliation(teacher(999L)))); // 담임은 다른 교사
        given(subjectAssignmentRepository.existsByTeacherIdAndClassroomIdAndAcademicYearAndSemester(
                TEACHER_ID, CLASSROOM_ID, 2026, 1)).willReturn(true);
        assertThatCode(() -> validator.validateCanWriteFor(TEACHER_ID, STUDENT_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("담임도 과목담당도 아니면 ACCESS_DENIED")
    void neither_denied() {
        given(studentAffiliationRepository.findWithAllDetails(STUDENT_ID, 2026, 1))
                .willReturn(Optional.of(affiliation(teacher(999L))));
        given(subjectAssignmentRepository.existsByTeacherIdAndClassroomIdAndAcademicYearAndSemester(
                TEACHER_ID, CLASSROOM_ID, 2026, 1)).willReturn(false);
        assertThatThrownBy(() -> validator.validateCanWriteFor(TEACHER_ID, STUDENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("작성 권한이 없습니다");
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────────
    private Teacher teacher(long id) {
        User u = User.builder().id(id).name("교사" + id).gender(Gender.MALE)
                .role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        Teacher t = Teacher.builder().user(u).build();
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private StudentAffiliation affiliation(Teacher homeroom) {
        Classroom classroom = Classroom.builder()
                .academicYear(2026).semester(1).grade(1).classNum(4).homeroomTeacher(homeroom).build();
        ReflectionTestUtils.setField(classroom, "id", CLASSROOM_ID);
        User su = User.builder().id(STUDENT_ID).name("학생").gender(Gender.MALE)
                .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        Student student = Student.builder().id(STUDENT_ID).user(su).enrollmentYear(2026).build();
        return StudentAffiliation.builder().student(student).classroom(classroom).studentNum(1).build();
    }
}
