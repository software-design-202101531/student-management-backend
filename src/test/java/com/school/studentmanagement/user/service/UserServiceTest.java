package com.school.studentmanagement.user.service;


import com.school.studentmanagement.affiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.user.dto.ActivateAccountRequest;
import com.school.studentmanagement.user.dto.VerifyStudentRequest;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks // 가짜 객체들을 주입 받을 서비스
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentAffiliationRepository affiliationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("학생 자격 검증 성공: 일치하는 대기 학생이 있으면 ID 반환")
    void verifyStudent_Success() {
        // Given
        VerifyStudentRequest request = new VerifyStudentRequest();

        User pendingUser = User.builder().status(UserStatus.PENDING).build();

        given(affiliationRepository.findPendingStudentUser(any(), any(), any(), any(), any()))
                .willReturn(Optional.of(pendingUser));

        // when
        Long resultId = userService.verifyStudent(request);

        // Then
        assertThat(resultId).isEqualTo(pendingUser.getId());
    }

    @Test
    @DisplayName("계정 활성화 실패: 이미 활성화된 계정인 경우")
    void activateAccount_Fail() {
        // Given
        ActivateAccountRequest request = new ActivateAccountRequest();

        User activeUser = User.builder().status(UserStatus.ACTIVE).build();

        given(userRepository.findById(any()))
                .willReturn(Optional.of(activeUser));

        // When & Then
        assertThatThrownBy(() -> userService.activateAccount(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 활성화 된 계정입니다");

    }

    @Test
    @DisplayName("계정 활성화 성공")
    void verifyUser_Success() {
        // Given
        ActivateAccountRequest request = new ActivateAccountRequest();

        User pendingUser = User.builder().status(UserStatus.PENDING).build();

        given(userRepository.findById(any()))
                .willReturn(Optional.of(pendingUser));
        given(passwordEncoder.encode(any()))
                .willReturn("encodedPassword");

        // When
        userService.activateAccount(request);

        // Then
        // 암호화 로직이 한 번이라도 불렸는가?
        verify(passwordEncoder).encode(any());
    }
}
