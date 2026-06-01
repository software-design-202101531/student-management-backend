package com.school.studentmanagement.student.service;

import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.student.dto.StudentActivationRequest;
import com.school.studentmanagement.student.dto.VerifyStudentRequest;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @InjectMocks private StudentService studentService;
    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentAffiliationRepository affiliationRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("학생 정보 검증 (verifyStudent)")
    class VerifyStudentTest {

        @Test
        @DisplayName("성공: 일치하는 대기 학생이 있으면 userId 반환")
        void verifyStudent_Success() {
            User pendingUser = User.builder().id(1L).name("홍길동").gender(Gender.MALE)
                    .role(UserRole.STUDENT).status(UserStatus.PENDING).build();
            given(affiliationRepository.findPendingStudentUser(any(), any(), any(), any(), any()))
                    .willReturn(Optional.of(pendingUser));

            Long result = studentService.verifyStudent(new VerifyStudentRequest());

            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패: 일치하는 대기 학생이 없으면 BusinessException")
        void verifyStudent_Fail_NoMatchingUser() {
            given(affiliationRepository.findPendingStudentUser(any(), any(), any(), any(), any()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.verifyStudent(new VerifyStudentRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("입력하신 정보와 일치하는 가입 대기 정보가 없습니다");
        }
    }

    @Nested
    @DisplayName("학생 계정 활성화 (activateStudentAccount)")
    class ActivateStudentAccountTest {

        @Test
        @DisplayName("성공: 신원 재검증 후 User 활성화 및 Student 상세 정보 동시 업데이트")
        void activateStudentAccount_Success() {
            User pendingUser = User.builder().id(1L).name("홍길동").gender(Gender.MALE)
                    .role(UserRole.STUDENT).status(UserStatus.PENDING).build();
            Student student = Student.builder().id(1L).user(pendingUser).enrollmentYear(2026).build();
            StudentActivationRequest request = buildRequest("hong2026", "pass1234!", "서울시 강남구", "01012345678");

            given(affiliationRepository.findPendingStudentUser(any(), any(), any(), any(), any()))
                    .willReturn(Optional.of(pendingUser));
            given(studentRepository.findById(1L)).willReturn(Optional.of(student));
            given(userRepository.findByLoginId("hong2026")).willReturn(Optional.empty());
            given(passwordEncoder.encode("pass1234!")).willReturn("encodedPass");

            studentService.activateStudentAccount(request);

            assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(pendingUser.getLoginId()).isEqualTo("hong2026");
            assertThat(student.getAddress()).isEqualTo("서울시 강남구");
            assertThat(student.getPhoneNumber()).isEqualTo("01012345678");
        }

        @Test
        @DisplayName("실패: 신원과 일치하는 가입대기 학생이 없으면(또는 이미 활성) → STUDENT_VERIFY_FAILED")
        void activateStudentAccount_Fail_NoMatchingPendingStudent() {
            given(affiliationRepository.findPendingStudentUser(any(), any(), any(), any(), any()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.activateStudentAccount(
                    buildRequest("hong2026", "pass1234!", "서울시 강남구", "01012345678")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("입력하신 정보와 일치하는 가입 대기 정보가 없습니다");
        }

        @Test
        @DisplayName("실패: 대기 User는 있으나 Student 정보가 없으면 → STUDENT_NOT_FOUND")
        void activateStudentAccount_Fail_StudentNotFound() {
            User pendingUser = User.builder().id(1L).name("홍길동").gender(Gender.MALE)
                    .role(UserRole.STUDENT).status(UserStatus.PENDING).build();

            given(affiliationRepository.findPendingStudentUser(any(), any(), any(), any(), any()))
                    .willReturn(Optional.of(pendingUser));
            given(studentRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.activateStudentAccount(
                    buildRequest("hong2026", "pass1234!", "서울시 강남구", "01012345678")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 학생 정보입니다");
        }

        @Test
        @DisplayName("실패: 이미 사용 중인 로그인 아이디 → LOGIN_ID_DUPLICATED")
        void activateStudentAccount_Fail_LoginIdDuplicated() {
            User pendingUser = User.builder().id(1L).name("홍길동").gender(Gender.MALE)
                    .role(UserRole.STUDENT).status(UserStatus.PENDING).build();
            Student student = Student.builder().id(1L).user(pendingUser).enrollmentYear(2026).build();
            User existing = User.builder().id(2L).loginId("hong2026").name("기존").gender(Gender.MALE)
                    .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();

            given(affiliationRepository.findPendingStudentUser(any(), any(), any(), any(), any()))
                    .willReturn(Optional.of(pendingUser));
            given(studentRepository.findById(1L)).willReturn(Optional.of(student));
            given(userRepository.findByLoginId("hong2026")).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> studentService.activateStudentAccount(
                    buildRequest("hong2026", "pass1234!", "서울시 강남구", "01012345678")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("이미 사용 중인 아이디입니다");
        }
    }

    private StudentActivationRequest buildRequest(String loginId, String pw, String address, String phone) {
        StudentActivationRequest req = new StudentActivationRequest();
        ReflectionTestUtils.setField(req, "academicYear", 2026);
        ReflectionTestUtils.setField(req, "grade", 1);
        ReflectionTestUtils.setField(req, "classNum", 3);
        ReflectionTestUtils.setField(req, "studentNum", 15);
        ReflectionTestUtils.setField(req, "name", "홍길동");
        ReflectionTestUtils.setField(req, "loginId", loginId);
        ReflectionTestUtils.setField(req, "password", pw);
        ReflectionTestUtils.setField(req, "address", address);
        ReflectionTestUtils.setField(req, "phoneNumber", phone);
        return req;
    }
}
