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

    // 토큰 종류 (access 토큰을 refresh로 악용하거나 그 반대를 막기 위한 type 클레임)
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    // 엑세스 토큰 생성
    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, accessExpirationTime, TYPE_ACCESS);
    }

    // 리프레시 토큰 생성
    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, refreshExpirationTime, TYPE_REFRESH);
    }

    // 토큰 생성 공용 메서드
    private String createToken(Long userId, String role, long valiTime, String type) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + valiTime);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    // 토큰에서 인증 정보 조회
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserIdAsString(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // 토큰에서 사용자 PK 추출
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    // 토큰에서 role 클레임 추출
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // 토큰이 access 타입인지 여부 (API 접근 필터에서 refresh 토큰 거부용)
    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(getClaims(token).get("type", String.class));
    }

    // 토큰이 refresh 타입인지 여부
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(getClaims(token).get("type", String.class));
    }

    // 리프레시 토큰 만료 시간(ms) — Redis 저장 TTL 용도
    public long getRefreshExpirationMillis() {
        return refreshExpirationTime;
    }

    // 토큰 파싱 및 pk 추출
    private String getUserIdAsString(String token) {
        return getClaims(token).getSubject();
    }

    // 서명 검증 후 클레임 추출
    private io.jsonwebtoken.Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
            // 스택트레이스/예외 메시지에 claim 일부가 노출될 수 있어 메시지만 남긴다. 디버깅 필요 시 DEBUG 레벨로 자세히.
            log.info("잘못된 JWT 서명입니다");
            log.debug("잘못된 JWT 서명 상세", e);
        } catch(ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다");
            log.debug("만료된 JWT 상세", e);
        } catch(UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다");
            log.debug("지원되지 않는 JWT 상세", e);
        } catch(IllegalArgumentException e) {
            log.info("JWT 토큰이 비어있거나 잘못되었습니다");
        }
        return false;
    }
}
