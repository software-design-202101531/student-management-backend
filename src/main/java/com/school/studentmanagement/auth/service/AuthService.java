package com.school.studentmanagement.auth.service;

import com.school.studentmanagement.auth.dto.LoginRequest;
import com.school.studentmanagement.auth.dto.TokenResponse;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        return issueTokens(user.getId(), user.getRole().name());
    }

    // 리프레시 토큰으로 access/refresh 재발급 (회전: 새 refresh를 저장하고 이전 것은 무효화)
    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null
                || !jwtTokenProvider.validateToken(refreshToken)
                || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // 저장소의 현재 토큰과 다르면 이미 회전됐거나 로그아웃/탈취 재사용 → 무효화 후 거부
        if (!refreshTokenStore.matches(userId, refreshToken)) {
            refreshTokenStore.delete(userId);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return issueTokens(userId, jwtTokenProvider.getRole(refreshToken));
    }

    // 로그아웃 — 저장된 리프레시 토큰 무효화 (멱등; 토큰이 없거나 무효여도 성공 처리)
    public void logout(String refreshToken) {
        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            refreshTokenStore.delete(jwtTokenProvider.getUserId(refreshToken));
        }
    }

    // access/refresh 발급 후 refresh를 저장소에 기록 (사용자당 1개 유지 → 회전/단일세션)
    private TokenResponse issueTokens(Long userId, String role) {
        String accessToken = jwtTokenProvider.createAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, role);
        refreshTokenStore.save(userId, refreshToken, jwtTokenProvider.getRefreshExpirationMillis());
        return new TokenResponse(accessToken, refreshToken);
    }
}
