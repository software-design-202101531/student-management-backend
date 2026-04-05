package com.school.studentmanagement.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity // 스프링 시큐리티 필터 체인 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    // 단방향 암호화 메서드
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // HTTP 요청에 대한 보안 규칙 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // CORS 설정 추가
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF 보호 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // Form 로그인 및 HTTP Basic 인증 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 세션 관리 상태를 STATELESS로 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))


                // 경로별 출입 통제
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 브라우저 사전 요청(OPTIONS) 허용
                        .requestMatchers("/api/user/**", "/api/parents/**", "/api/auth/**").permitAll()

                        // 관리자 API 설정
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/v3/api-docs"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                // JWT 필터 등록(시큐리티 기본 필터보다 먼저 요청이 거쳐간다)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }


    // CORS 상세 설정 Bean(Preflight 통과)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 주소 허용
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 모든 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));
        // 자격 증명 허용(쿠키, Authorization 헤더의 토큰 등)
        configuration.setAllowCredentials(true);

        // 자바 스크립트가 읽을 수 있게 허용(이거 안 하면 엑세스 토큰 못 읽음)
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 대해 CORS 설정 적용
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
