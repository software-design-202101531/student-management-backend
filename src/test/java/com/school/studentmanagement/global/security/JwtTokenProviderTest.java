package com.school.studentmanagement.global.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock private UserDetailsService userDetailsService;
    private JwtTokenProvider provider;

    // application-test.yml 과 동일한 48바이트 Base64 시크릿(디코드 시 ≥256bit)
    private static final String SECRET = "I6KrhX/S+V7RXp3D+CCrubXmtS4+hFrMUsFzlQe0AhojDZHVaSNKvMkQc1ycKeRt";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(userDetailsService);
        ReflectionTestUtils.setField(provider, "secretKey", SECRET);
        ReflectionTestUtils.setField(provider, "accessExpirationTime", 3_600_000L);
        ReflectionTestUtils.setField(provider, "refreshExpirationTime", 86_400_000L);
        provider.init();
    }

    @Test
    @DisplayName("유효한 access 토큰: 검증 통과 + 타입/클레임 추출")
    void validAccessToken() {
        String token = provider.createAccessToken(10L, "TEACHER");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isTrue();
        assertThat(provider.isRefreshToken(token)).isFalse();
        assertThat(provider.getUserId(token)).isEqualTo(10L);
        assertThat(provider.getRole(token)).isEqualTo("TEACHER");
    }

    @Test
    @DisplayName("refresh 토큰: type=refresh — API 접근(access) 판별과 구분된다")
    void refreshTokenType() {
        String token = provider.createRefreshToken(10L, "STUDENT");

        assertThat(provider.isRefreshToken(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("위조/손상 토큰: 검증 거부")
    void tamperedToken_invalid() {
        // 다른 시크릿으로 서명된 토큰 → 우리 키로 서명 검증 실패
        JwtTokenProvider other = new JwtTokenProvider(userDetailsService);
        ReflectionTestUtils.setField(other, "secretKey",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"); // 48B 다른 키
        ReflectionTestUtils.setField(other, "accessExpirationTime", 3_600_000L);
        ReflectionTestUtils.setField(other, "refreshExpirationTime", 86_400_000L);
        other.init();
        String foreign = other.createAccessToken(10L, "TEACHER");

        assertThat(provider.validateToken(foreign)).isFalse();      // 위조 서명
        assertThat(provider.validateToken("not.a.jwt")).isFalse();  // 형식 오류
    }

    @Test
    @DisplayName("빈 토큰: 검증 거부")
    void emptyToken_invalid() {
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰: 검증 거부")
    void expiredToken_invalid() {
        ReflectionTestUtils.setField(provider, "accessExpirationTime", -10_000L); // 이미 만료되도록
        String expired = provider.createAccessToken(10L, "TEACHER");

        assertThat(provider.validateToken(expired)).isFalse();
    }

    @Test
    @DisplayName("getAuthentication: subject(userId)로 UserDetails 로딩 후 인증 객체 구성")
    void getAuthentication_loadsBySubject() {
        UserDetails details = User.withUsername("10").password("x").authorities("ROLE_TEACHER").build();
        given(userDetailsService.loadUserByUsername("10")).willReturn(details);
        String token = provider.createAccessToken(10L, "TEACHER");

        Authentication auth = provider.getAuthentication(token);

        assertThat(auth.getPrincipal()).isEqualTo(details);
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_TEACHER");
    }
}
