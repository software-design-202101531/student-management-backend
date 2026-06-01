package com.school.studentmanagement.auth.service;

import com.school.studentmanagement.auth.dto.LoginRequest;
import com.school.studentmanagement.auth.dto.TokenResponse;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.security.JwtTokenProvider;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("로그인 성공: 유효한 계정 정보로 AccessToken/RefreshToken 반환")
    void login_Success() {
        User user = User.builder().id(1L).loginId("teacher01").password("encodedPw").name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();

        given(userRepository.findByLoginId("teacher01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("rawPw", "encodedPw")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, "TEACHER")).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L, "TEACHER")).willReturn("refresh-token");

        TokenResponse response = authService.login(buildLoginRequest("teacher01", "rawPw"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("로그인 실패: 존재하지 않는 아이디 → BusinessException")
    void login_Fail_UserNotFound() {
        given(userRepository.findByLoginId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(buildLoginRequest("unknown", "pw")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("아이디 혹은 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호 불일치 → BusinessException")
    void login_Fail_WrongPassword() {
        User user = User.builder().id(1L).loginId("teacher01").password("encodedPw").name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();

        given(userRepository.findByLoginId("teacher01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPw", "encodedPw")).willReturn(false);

        assertThatThrownBy(() -> authService.login(buildLoginRequest("teacher01", "wrongPw")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("아이디 혹은 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("로그인 실패: 활성화되지 않은 계정(PENDING) → BusinessException")
    void login_Fail_InactiveAccount() {
        User pendingUser = User.builder().id(1L).loginId("student01").password("encodedPw").name("홍길동")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.PENDING).build();

        given(userRepository.findByLoginId("student01")).willReturn(Optional.of(pendingUser));
        given(passwordEncoder.matches(any(), any())).willReturn(true);

        assertThatThrownBy(() -> authService.login(buildLoginRequest("student01", "rawPw")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("활성화 되지 않은 계정입니다. 관리자에게 문의하세요");
    }

    @Test
    @DisplayName("로그인 성공 시 리프레시 토큰을 저장소에 기록")
    void login_StoresRefreshToken() {
        User user = User.builder().id(1L).loginId("teacher01").password("encodedPw").name("최수학")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        given(userRepository.findByLoginId("teacher01")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("rawPw", "encodedPw")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, "TEACHER")).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L, "TEACHER")).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshExpirationMillis()).willReturn(1209600000L);

        authService.login(buildLoginRequest("teacher01", "rawPw"));

        verify(refreshTokenStore).save(1L, "refresh-token", 1209600000L);
    }

    @Test
    @DisplayName("리프레시 성공: 저장소와 일치하면 새 토큰 발급 + 회전 저장")
    void refresh_Success() {
        given(jwtTokenProvider.validateToken("rt")).willReturn(true);
        given(jwtTokenProvider.isRefreshToken("rt")).willReturn(true);
        given(jwtTokenProvider.getUserId("rt")).willReturn(1L);
        given(refreshTokenStore.matches(1L, "rt")).willReturn(true);
        given(jwtTokenProvider.getRole("rt")).willReturn("TEACHER");
        given(jwtTokenProvider.createAccessToken(1L, "TEACHER")).willReturn("new-access");
        given(jwtTokenProvider.createRefreshToken(1L, "TEACHER")).willReturn("new-refresh");
        given(jwtTokenProvider.getRefreshExpirationMillis()).willReturn(1209600000L);

        TokenResponse response = authService.refresh("rt");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenStore).save(1L, "new-refresh", 1209600000L);
    }

    @Test
    @DisplayName("리프레시 실패: 토큰 null → INVALID_REFRESH_TOKEN")
    void refresh_Fail_Null() {
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다. 다시 로그인해 주세요");
    }

    @Test
    @DisplayName("리프레시 실패: refresh 타입이 아니면 → INVALID_REFRESH_TOKEN")
    void refresh_Fail_WrongType() {
        given(jwtTokenProvider.validateToken("at")).willReturn(true);
        given(jwtTokenProvider.isRefreshToken("at")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("at"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다. 다시 로그인해 주세요");
    }

    @Test
    @DisplayName("리프레시 실패: 저장소와 불일치(회전/탈취) → 무효화 후 거부")
    void refresh_Fail_NotInStore() {
        given(jwtTokenProvider.validateToken("rt")).willReturn(true);
        given(jwtTokenProvider.isRefreshToken("rt")).willReturn(true);
        given(jwtTokenProvider.getUserId("rt")).willReturn(1L);
        given(refreshTokenStore.matches(1L, "rt")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("rt"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다. 다시 로그인해 주세요");
        verify(refreshTokenStore).delete(1L);
    }

    @Test
    @DisplayName("로그아웃: 저장된 리프레시 토큰 무효화")
    void logout_DeletesStoredToken() {
        given(jwtTokenProvider.validateToken("rt")).willReturn(true);
        given(jwtTokenProvider.getUserId("rt")).willReturn(1L);

        authService.logout("rt");

        verify(refreshTokenStore).delete(1L);
    }

    private LoginRequest buildLoginRequest(String loginId, String password) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "loginId", loginId);
        ReflectionTestUtils.setField(req, "password", password);
        return req;
    }
}
