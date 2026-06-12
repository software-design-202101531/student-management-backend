package com.school.studentmanagement.auth.controller;

import com.school.studentmanagement.auth.dto.LoginRequest;
import com.school.studentmanagement.auth.dto.TokenResponse;
import com.school.studentmanagement.auth.service.AuthService;
import com.school.studentmanagement.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(14);

    private final AuthService authService;

    // 운영(HTTPS)에서는 true, 로컬 개발(HTTP)에서는 false (환경변수 COOKIE_SECURE로 분기)
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return tokenResponse(authService.login(loginRequest));
    }

    // 리프레시 토큰(쿠키)으로 access/refresh 재발급 (회전)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        return tokenResponse(authService.refresh(refreshToken));
    }

    // 로그아웃 — 서버측 리프레시 토큰 무효화 + 쿠키 삭제
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString());
        return ResponseEntity.ok().headers(headers).body(ApiResponse.ok());
    }

    // access 토큰은 Authorization 헤더, refresh 토큰은 httpOnly 쿠키로 내려준다 (login/refresh 공통)
    private ResponseEntity<ApiResponse<Void>> tokenResponse(TokenResponse tokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie(tokens.getRefreshToken(), REFRESH_COOKIE_MAX_AGE).toString());
        return ResponseEntity.ok().headers(headers).body(ApiResponse.ok());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        // 교차 사이트(프론트=vercel.app, 백엔드=attune.asia)에서 쿠키가 전송되려면 SameSite=None 필요.
        // 단 None 은 Secure 가 전제라, HTTPS(운영)에서만 None, 로컬(HTTP)은 Strict 로 둔다.
        String sameSite = cookieSecure ? "None" : "Strict";
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .maxAge(maxAge)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .build();
    }
}
