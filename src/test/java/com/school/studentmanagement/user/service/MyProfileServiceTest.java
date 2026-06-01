package com.school.studentmanagement.user.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.storage.FileStorageService;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.parent.entity.Parent;
import com.school.studentmanagement.parent.repository.ParentRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.teacher.repository.TeacherRepository;
import com.school.studentmanagement.user.dto.MyProfileResponse;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyProfileServiceTest {

    @InjectMocks private MyProfileService myProfileService;
    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentAffiliationRepository studentAffiliationRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private AcademicCalendarUtil academicCalendarUtil;

    private static final Long USER_ID = 10L;
    private static final int YEAR = 2026;
    private static final int SEMESTER = 1;

    private User user(Long id, String name, UserRole role) {
        // 민감정보(loginId/password)를 일부러 채워, 응답 DTO 구조상 노출 불가함을 확인
        return User.builder().id(id).loginId("secret-login").password("ENCODED")
                .name(name).gender(Gender.MALE).role(role).status(UserStatus.ACTIVE).build();
    }

    private Teacher teacher(Long id, String name, String imageKey) {
        Subject math = new Subject("수학");
        ReflectionTestUtils.setField(math, "id", 1L);
        Teacher teacher = Teacher.builder()
                .user(user(id, name, UserRole.TEACHER)).employeeNumber("EMP001").subject(math)
                .officeLocation("본관 2층").officePhoneNumber("02-000-0000")
                .employmentStatus(EmploymentStatus.ACTIVE).build();
        ReflectionTestUtils.setField(teacher, "id", id);
        ReflectionTestUtils.setField(teacher, "profileImageKey", imageKey);
        return teacher;
    }

    @Test
    @DisplayName("학생: 사진·현재 학급·담임 이름을 포함하고 공통 정보가 채워진다")
    void student_withClassAndHomeroom() {
        Teacher homeroom = teacher(99L, "김담임", null);
        Student student = Student.builder().id(USER_ID).user(user(USER_ID, "이학생", UserRole.STUDENT))
                .homeroomTeacher(homeroom).address("서울시 강남구").phoneNumber("010-1111-2222")
                .profileImageKey("profiles/students/abc.png").enrollmentYear(2024).build();

        Classroom classroom = Classroom.builder().academicYear(YEAR).semester(SEMESTER)
                .grade(3).classNum(5).build();
        StudentAffiliation affiliation = StudentAffiliation.builder()
                .student(student).classroom(classroom).studentNum(7).build();

        given(studentRepository.findByIdWithUserAndHomeroom(USER_ID)).willReturn(Optional.of(student));
        given(fileStorageService.presignedGetUrl("profiles/students/abc.png")).willReturn("https://minio/presigned");
        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(YEAR);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(SEMESTER);
        given(studentAffiliationRepository.findWithAllDetails(USER_ID, YEAR, SEMESTER))
                .willReturn(Optional.of(affiliation));

        MyProfileResponse response = myProfileService.getMyProfile(USER_ID, UserRole.STUDENT);

        assertThat(response.name()).isEqualTo("이학생");
        assertThat(response.gender()).isEqualTo(Gender.MALE);
        assertThat(response.role()).isEqualTo(UserRole.STUDENT);
        assertThat(response.profileImageUrl()).isEqualTo("https://minio/presigned");
        assertThat(response.parent()).isNull();
        assertThat(response.teacher()).isNull();
        assertThat(response.student()).isNotNull();
        assertThat(response.student().address()).isEqualTo("서울시 강남구");
        assertThat(response.student().phoneNumber()).isEqualTo("010-1111-2222");
        assertThat(response.student().enrollmentYear()).isEqualTo(2024);
        assertThat(response.student().grade()).isEqualTo(3);
        assertThat(response.student().classNum()).isEqualTo(5);
        assertThat(response.student().homeroomTeacherName()).isEqualTo("김담임");
    }

    @Test
    @DisplayName("학생: 미배정·담임 미지정·사진 미등록이면 학급/담임/사진이 null")
    void student_withoutClassHomeroomImage() {
        Student student = Student.builder().id(USER_ID).user(user(USER_ID, "이학생", UserRole.STUDENT))
                .homeroomTeacher(null).address(null).phoneNumber(null)
                .profileImageKey(null).enrollmentYear(2024).build();

        given(studentRepository.findByIdWithUserAndHomeroom(USER_ID)).willReturn(Optional.of(student));
        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(YEAR);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(SEMESTER);
        given(studentAffiliationRepository.findWithAllDetails(USER_ID, YEAR, SEMESTER))
                .willReturn(Optional.empty());

        MyProfileResponse response = myProfileService.getMyProfile(USER_ID, UserRole.STUDENT);

        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.student().grade()).isNull();
        assertThat(response.student().classNum()).isNull();
        assertThat(response.student().homeroomTeacherName()).isNull();
    }

    @Test
    @DisplayName("학부모: 사진 없이 연락처·관계가 채워지고 다른 역할 상세는 null")
    void parent_success() {
        Parent parent = Parent.builder().user(user(USER_ID, "박학부모", UserRole.PARENT))
                .phoneNumber("010-3333-4444").relationType(RelationType.MOTHER).build();
        ReflectionTestUtils.setField(parent, "id", USER_ID);

        given(parentRepository.findByIdWithUser(USER_ID)).willReturn(Optional.of(parent));

        MyProfileResponse response = myProfileService.getMyProfile(USER_ID, UserRole.PARENT);

        assertThat(response.name()).isEqualTo("박학부모");
        assertThat(response.role()).isEqualTo(UserRole.PARENT);
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.student()).isNull();
        assertThat(response.teacher()).isNull();
        assertThat(response.parent().phoneNumber()).isEqualTo("010-3333-4444");
        assertThat(response.parent().relationType()).isEqualTo(RelationType.MOTHER);
    }

    @Test
    @DisplayName("교사: 사진·재직정보가 채워진다")
    void teacher_success() {
        given(teacherRepository.findByIdWithDetails(USER_ID))
                .willReturn(Optional.of(teacher(USER_ID, "최교사", "profiles/teachers/x.png")));
        given(fileStorageService.presignedGetUrl("profiles/teachers/x.png")).willReturn("https://minio/teacher");

        MyProfileResponse response = myProfileService.getMyProfile(USER_ID, UserRole.TEACHER);

        assertThat(response.name()).isEqualTo("최교사");
        assertThat(response.profileImageUrl()).isEqualTo("https://minio/teacher");
        assertThat(response.student()).isNull();
        assertThat(response.parent()).isNull();
        assertThat(response.teacher().employeeNumber()).isEqualTo("EMP001");
        assertThat(response.teacher().subjectName()).isEqualTo("수학");
        assertThat(response.teacher().officeLocation()).isEqualTo("본관 2층");
        assertThat(response.teacher().employmentStatus()).isEqualTo(EmploymentStatus.ACTIVE);
    }

    @Test
    @DisplayName("관리자: 공통 정보만 채워지고 역할별 상세·사진은 모두 null")
    void admin_commonOnly() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, "관리자", UserRole.ADMIN)));

        MyProfileResponse response = myProfileService.getMyProfile(USER_ID, UserRole.ADMIN);

        assertThat(response.name()).isEqualTo("관리자");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.student()).isNull();
        assertThat(response.parent()).isNull();
        assertThat(response.teacher()).isNull();
    }

    @Test
    @DisplayName("학생 정보가 없으면 STUDENT_NOT_FOUND")
    void student_notFound() {
        given(studentRepository.findByIdWithUserAndHomeroom(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> myProfileService.getMyProfile(USER_ID, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);
    }
}
