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
        @DisplayName("성공: User 계정 활성화 및 Student 상세 정보 동시 업데이트")
        void activateStudentAccount_Success() {
            User pendingUser = User.builder().id(1L).name("홍길동").gender(Gender.MALE)
                    .role(UserRole.STUDENT).status(UserStatus.PENDING).build();
            Student student = Student.builder().id(1L).user(pendingUser).enrollmentYear(2026).build();
            StudentActivationRequest request = buildRequest(1L, "hong2026", "pass1234!", "서울시 강남구", "01012345678");

            given(userRepository.findById(1L)).willReturn(Optional.of(pendingUser));
            given(studentRepository.findById(1L)).willReturn(Optional.of(student));
            given(passwordEncoder.encode("pass1234!")).willReturn("encodedPass");

            studentService.activateStudentAccount(request);

            assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(pendingUser.getLoginId()).isEqualTo("hong2026");
            assertThat(student.getAddress()).isEqualTo("서울시 강남구");
            assertThat(student.getPhoneNumber()).isEqualTo("01012345678");
        }

        @Test
        @DisplayName("실패: 이미 활성화된 계정 → BusinessException")
        void activateStudentAccount_Fail_AlreadyActive() {
            User activeUser = User.builder().id(1L).name("홍길동").gender(Gender.MALE)
                    .role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
            Student student = Student.builder().id(1L).user(activeUser).enrollmentYear(2026).build();
            StudentActivationRequest request = buildRequest(1L, "id", "pw", null, null);

            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(studentRepository.findById(1L)).willReturn(Optional.of(student));

            assertThatThrownBy(() -> studentService.activateStudentAccount(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("이미 활성화된 계정입니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 User ID → BusinessException")
        void activateStudentAccount_Fail_UserNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.activateStudentAccount(buildRequest(999L, "id", "pw", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 사용자입니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 Student ID → BusinessException")
        void activateStudentAccount_Fail_StudentNotFound() {
            User pendingUser = User.builder().id(1L).name("홍길동").gender(Gender.MALE)
                    .role(UserRole.STUDENT).status(UserStatus.PENDING).build();

            given(userRepository.findById(1L)).willReturn(Optional.of(pendingUser));
            given(studentRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.activateStudentAccount(buildRequest(1L, "id", "pw", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 학생 정보입니다");
        }
    }

    private StudentActivationRequest buildRequest(Long id, String loginId, String pw, String address, String phone) {
        StudentActivationRequest req = new StudentActivationRequest();
        ReflectionTestUtils.setField(req, "id", id);
        ReflectionTestUtils.setField(req, "loginId", loginId);
        ReflectionTestUtils.setField(req, "password", pw);
        ReflectionTestUtils.setField(req, "address", address);
        ReflectionTestUtils.setField(req, "phoneNumber", phone);
        return req;
    }
}
