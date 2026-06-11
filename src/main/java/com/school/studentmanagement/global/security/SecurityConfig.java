package com.school.studentmanagement.global.security;

import com.school.studentmanagement.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
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
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    // 단방향 암호화 메서드
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 필터 단계(시큐리티) 에러를 GlobalExceptionHandler와 동일한 JSON 형식으로 직렬화.
    private static void writeJsonError(HttpServletResponse response, ErrorCode errorCode) throws java.io.IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(errorCode.getStatus());
        response.getWriter().write(
                "{\"success\":false,\"error\":{\"code\":\"" + errorCode.getCode()
                        + "\",\"message\":\"" + errorCode.getMessage() + "\"}}");
    }

    // HTTP 요청에 대한 보안 규칙 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JWT/권한 에러 시 HTML 대신 JSON 반환
                .exceptionHandling(ex -> ex
                        // 코드/메시지는 ErrorCode 단일 출처에서 가져온다(GlobalExceptionHandler 응답과 형식 일치).
                        .authenticationEntryPoint((request, response, e) ->
                                writeJsonError(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, e) ->
                                writeJsonError(response, ErrorCode.ACCESS_DENIED))
                )

                // 경로별 출입 통제
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 배포 후 생존 확인용 헬스체크 — 인증 없이 접근 허용
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/api/user/**", "/api/parents/**", "/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 알림은 인증된 모든 역할(학생/학부모/교사) 본인 것만 — 수신자 필터는 서비스에서 userId 기준
                        .requestMatchers("/api/notifications/**").authenticated()
                        // 보고서/엑셀 내보내기는 교사·관리자 전용 (담임/과목담당/공개범위 검증은 재사용 서비스에서 수행)
                        .requestMatchers("/api/exports/**").hasAnyRole("TEACHER", "ADMIN")
                        // 분석(OLAP): ETL 수동 실행은 관리자, 대시보드 조회는 교사·관리자
                        .requestMatchers("/api/analytics/etl/**").hasRole("ADMIN")
                        .requestMatchers("/api/analytics/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/student/me/**").hasRole("STUDENT")
                        .requestMatchers("/api/parent/me/**").hasRole("PARENT")
                        .requestMatchers("/api/teachers/me", "/api/teachers/me/**").hasRole("TEACHER")
                        // 피드백 작성/수정/발행은 교사 전용 (목록 조회 GET은 인증된 모든 권한 허용 후 서비스에서 분기)
                        .requestMatchers(HttpMethod.POST, "/api/feedbacks").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/feedbacks/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/feedbacks/*/publish").hasRole("TEACHER")
                        // 상담 내역: 작성은 교사 전용, 조회/공개범위 변경은 교사·관리자 (세부 권한은 서비스에서 검증)
                        .requestMatchers(HttpMethod.POST, "/api/consultations").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/students/*/consultations").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/consultations/search").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/consultations/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/consultations/*/visibility").hasAnyRole("TEACHER", "ADMIN")
                        // 학생 프로필 사진 등록/수정은 교사 전용 (담임 검증은 서비스에서 수행)
                        .requestMatchers(HttpMethod.POST, "/api/students/*/profile-image").hasRole("TEACHER")
                        // 행특/과세특 작성·조회는 교사 전용 (담임·과목담당 세부 권한은 서비스에서 검증)
                        .requestMatchers("/api/students/*/records/**").hasRole("TEACHER")
                        .requestMatchers("/api/classrooms/*/subjects/*/students/*/records/**").hasRole("TEACHER")
                        // 특정 학생 성적 검색은 교사 전용 (담임·과목담당 세부 권한은 서비스에서 검증)
                        .requestMatchers(HttpMethod.GET, "/api/students/*/grades/search").hasRole("TEACHER")
                        // 성적: 시험 메타 등록/공개는 교사·관리자, 학기 마감/재개방은 관리자 전용,
                        //       그 외 성적 조회/입력은 교사 (담임·과목담당 세부 권한은 서비스에서 검증)
                        .requestMatchers("/api/grades/semesters/**").hasRole("ADMIN")
                        .requestMatchers("/api/grades/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/exams/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/teachers/students/*/grades/**").hasRole("TEACHER")
                        // 학생 단위 출결 조회 — 담임·과목담당 검증은 서비스에서
                        .requestMatchers("/api/teachers/students/*/attendance/**").hasRole("TEACHER")
                        // 학생 학생부 기본 프로필 조회/수정은 담임 교사 전용 (담임 검증은 서비스에서 수행)
                        .requestMatchers("/api/teachers/students/*/profile").hasRole("TEACHER")
                        .requestMatchers("/api/classrooms/*/subjects/*/grades/**").hasRole("TEACHER")
                        // 과제 부여/조회/제출현황은 과목 담당 교사 전용 (세부 권한은 서비스에서 검증)
                        .requestMatchers("/api/classrooms/*/subjects/*/assignments/**").hasRole("TEACHER")
                        .requestMatchers("/api/classrooms/*/grades/**").hasRole("TEACHER")
                        // 학급 학생 명단 조회는 교사 전용 (담임·과목담당 세부 권한은 서비스에서 검증)
                        .requestMatchers("/api/classrooms/*/students").hasRole("TEACHER")
                        // 학급 출결 조회/입력은 교사 전용 (담임 검증은 서비스에서 수행)
                        .requestMatchers("/api/classrooms/*/attendance/**").hasRole("TEACHER")
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

                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 상세 설정 Bean(Preflight 통과)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
