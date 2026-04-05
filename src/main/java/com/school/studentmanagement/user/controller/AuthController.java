package com.school.studentmanagement.user.controller;

import com.school.studentmanagement.user.dto.LoginRequest;
import com.school.studentmanagement.user.dto.TokenResponse;
import com.school.studentmanagement.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.login(loginRequest);

        // Refresh Token은 HttpOnly 쿠키
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokenResponse.getRefreshToken())
                .maxAge(14 * 24 * 60 * 60)
                .path("/")
                .secure(false)
                .sameSite("Strict")
                .build();

        // Access Token은 헤더에
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getAccessToken());
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok().headers(headers).body("로그인에 성공하였습니다");

    }
}
