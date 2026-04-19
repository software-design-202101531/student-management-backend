package com.school.studentmanagement.user.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.user.dto.TeacherProfileResponse;
import com.school.studentmanagement.user.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TeacherProfileServiceTest {

    @InjectMocks private TeacherProfileService teacherProfileService;
    @Mock private TeacherRepository teacherRepository;
    @Mock private ClassRoomRepository classRoomRepository;

    private Teacher teacher;
    private Classroom homeroom;
    private static final Long TEACHER_ID = 100L;

    @BeforeEach
    void setUp() {
        Subject math = new Subject("수학");
        ReflectionTestUtils.setField(math, "id", 1L);

        User teacherUser = User.builder().id(TEACHER_ID).name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();

        teacher = Teacher.builder()
                .user(teacherUser).employeeNumber("EMP2026003").subject(math)
                .officeLocation("본관 2층").officePhoneNumber("02-123-4569")
                .employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);

        homeroom = Classroom.builder().academicYear(2026).semester(1).grade(1).classNum(4)
                .homeroomTeacher(teacher).build();
        ReflectionTestUtils.setField(homeroom, "id", 200L);
    }

    @Test
    @DisplayName("내 정보 조회 성공: 담임 반이 있는 경우 homeroom 정보 포함")
    void getMyProfile_Success_WithHomeroom() {
        // Given
        given(teacherRepository.findByIdwithDetails(TEACHER_ID)).willReturn(Optional.of(teacher));
        given(classRoomRepository.findClassroomByHomeroomTeacher(teacher)).willReturn(Optional.of(homeroom));

        // When
        TeacherProfileResponse response = teacherProfileService.getMyProfile(TEACHER_ID);

        // Then
        assertThat(response.getName()).isEqualTo("최수학");
        assertThat(response.getEmployeeNumber()).isEqualTo("EMP2026003");
        assertThat(response.getSubjectName()).isEqualTo("수학");
        assertThat(response.getIsHomeRoom()).isTrue();
        assertThat(response.getHomeroomClassId()).isEqualTo(200L);
        assertThat(response.getGrade()).isEqualTo(1);
        assertThat(response.getClassNum()).isEqualTo(4);
    }

    @Test
    @DisplayName("내 정보 조회 성공: 담임 반이 없는 경우 homeroom 필드 null")
    void getMyProfile_Success_WithoutHomeroom() {
        // Given
        given(teacherRepository.findByIdwithDetails(TEACHER_ID)).willReturn(Optional.of(teacher));
        given(classRoomRepository.findClassroomByHomeroomTeacher(teacher)).willReturn(Optional.empty());

        // When
        TeacherProfileResponse response = teacherProfileService.getMyProfile(TEACHER_ID);

        // Then
        assertThat(response.getIsHomeRoom()).isFalse();
        assertThat(response.getHomeroomClassId()).isNull();
        assertThat(response.getGrade()).isNull();
        assertThat(response.getClassNum()).isNull();
    }

    @Test
    @DisplayName("내 정보 조회 실패: 존재하지 않는 교사 ID → IllegalArgumentException")
    void getMyProfile_Fail_TeacherNotFound() {
        // Given
        given(teacherRepository.findByIdwithDetails(TEACHER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> teacherProfileService.getMyProfile(TEACHER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Teacher not found");
    }
}
