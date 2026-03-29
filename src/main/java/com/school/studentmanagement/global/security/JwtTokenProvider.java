package com.school.studentmanagement.global.security;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access_expiration_time}")
    private Long accessExpirationTime;

    @Value("${jwt.refresh_expiration_time}")
    private Long refreshExpirationTime;

    private final UserDetailsService userDetailsService;
    private SecretKey key;

    // 시크릿 키 Base64 인코딩 (HMAC-SHA 알고리즘 키 생성)
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 엑세스 토큰 생성
    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, accessExpirationTime);
    }

    // 리프레시 토큰 생성
    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, refreshExpirationTime);
    }

    // 토큰 생성 공용 메서드
    public String createToken(Long userId, String role, long valiTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + valiTime);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .signWith(key)
                .compact();
    }

    // 토큰에서 인증 정보 조회
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserIdAsString(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }


    // 서비스 로직에서 사용할 pk 추출 메서드
    public Long getUserId(String token) {
        return Long.parseLong(getUserIdAsString(token));
    }

    // 토큰 파싱 및 pk 추출
    private String getUserIdAsString(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }


    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch(SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다", e);
        } catch(ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다", e);
        } catch(UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다", e);
        } catch(IllegalArgumentException e) {
            log.info("JWT 토큰이 비어있거나 잘못되었습니다");
        }
        return false;
    }
}
