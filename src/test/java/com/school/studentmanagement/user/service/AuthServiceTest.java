package com.school.studentmanagement.user.service;

import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.user.dto.LoginRequest;
import com.school.studentmanagement.user.dto.TokenResponse;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
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
class AuthServiceTest {

    @InjectMocks private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("로그인 성공: 유효한 계정 정보로 AccessToken/RefreshToken 반환")
    void login_Success() {
        // Given
        User user = User.builder().id(1L).loginId("teacher01").password("encodedPw").name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();

        given(userRepository.findByLoginId("teacher01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("rawPw", "encodedPw")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, "TEACHER")).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L, "TEACHER")).willReturn("refresh-token");

        // When
        TokenResponse response = authService.login(buildLoginRequest("teacher01", "rawPw"));

        // Then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("로그인 실패: 존재하지 않는 아이디 → IllegalArgumentException")
    void login_Fail_UserNotFound() {
        // Given
        given(userRepository.findByLoginId(any())).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(buildLoginRequest("unknown", "pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디 혹은 비밀번호가 존재하지 않습니다");
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호 불일치 → IllegalArgumentException")
    void login_Fail_WrongPassword() {
        // Given
        User user = User.builder().id(1L).loginId("teacher01").password("encodedPw").name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();

        given(userRepository.findByLoginId("teacher01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPw", "encodedPw")).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(buildLoginRequest("teacher01", "wrongPw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디 혹은 비밀번호가 존재하지 않습니다");
    }

    @Test
    @DisplayName("로그인 실패: 활성화되지 않은 계정(PENDING) → IllegalArgumentException")
    void login_Fail_InactiveAccount() {
        // Given
        User pendingUser = User.builder().id(1L).loginId("student01").password("encodedPw").name("홍길동")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.PENDING).build();

        given(userRepository.findByLoginId("student01")).willReturn(Optional.of(pendingUser));
        given(passwordEncoder.matches(any(), any())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(buildLoginRequest("student01", "rawPw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("활성화 되지 않은 계정입니다, 관리자에게 문의 해주세요");
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────────
    private LoginRequest buildLoginRequest(String loginId, String password) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "loginId", loginId);
        ReflectionTestUtils.setField(req, "password", password);
        return req;
    }
}
