package com.school.studentmanagement.support;

import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.user.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * @WebMvcTest 컨트롤러 슬라이스 테스트용 인증 헬퍼.
 *
 * <p>실제 {@link CustomUserDetails}(userId + role)를 SecurityContext에 심어, 보안 매처(hasRole)와
 * 컨트롤러의 {@code @AuthenticationPrincipal}·{@code getUser().getRole()} 를 진짜 경로로 태운다.
 * JWT 토큰은 보내지 않는다 — JwtAuthenticationFilter는 토큰이 없으면 no-op이라 이 컨텍스트가 유지된다.
 *
 * <p>사용: {@code mvc.perform(post("/api/...").with(asTeacher(1L)))}
 */
public final class MockAuth {

    private MockAuth() {
    }

    public static RequestPostProcessor asUser(long userId, UserRole role) {
        User user = User.builder()
                .id(userId)
                .role(role)
                .status(UserStatus.ACTIVE)
                .name("test")
                .gender(Gender.MALE)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities())); // ROLE_<role> 부여
    }

    public static RequestPostProcessor asTeacher(long userId) {
        return asUser(userId, UserRole.TEACHER);
    }

    public static RequestPostProcessor asStudent(long userId) {
        return asUser(userId, UserRole.STUDENT);
    }

    public static RequestPostProcessor asParent(long userId) {
        return asUser(userId, UserRole.PARENT);
    }

    public static RequestPostProcessor asAdmin(long userId) {
        return asUser(userId, UserRole.ADMIN);
    }
}
