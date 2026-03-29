package com.school.studentmanagement.user.service;


import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.user.dto.LoginRequest;
import com.school.studentmanagement.user.dto.TokenResponse;
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

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {

        // 유저 조회
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 혹은 비밀번호가 존재하지 않습니다"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 혹은 비밀번호가 존재하지 않습니다");
        }

        // 계정 상태 검증(ACTIVE 상태인지)
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("활성화 되지 않은 계정입니다, 관리자에게 문의 해주세요");
        }

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole().name());

        return new TokenResponse(accessToken, refreshToken);
    }
}
