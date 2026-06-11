package com.school.studentmanagement.consultation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.studentmanagement.consultation.dto.ConsultationCreateRequest;
import com.school.studentmanagement.consultation.dto.ConsultationResponse;
import com.school.studentmanagement.consultation.service.ConsultationService;
import com.school.studentmanagement.global.enums.ConsultationVisibility;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.Month;

import static com.school.studentmanagement.support.MockAuth.asAdmin;
import static com.school.studentmanagement.support.MockAuth.asStudent;
import static com.school.studentmanagement.support.MockAuth.asTeacher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ConsultationController 웹 슬라이스 테스트 (파일럿).
 * 실제 SecurityConfig를 태워 권한 매처(401/403)·요청 검증(400)·해피패스(200, 인자 전달)를 검증한다.
 * 서비스 계층은 @MockBean으로 대체(권한 세부분기·DB는 별도 서비스/통합 테스트 책임).
 */
@WebMvcTest(ConsultationController.class)
@Import(SecurityConfig.class) // 실제 보안 규칙(권한 매처·CSRF disable·JSON 401/403)을 슬라이스에 적용
class ConsultationControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @MockitoBean private ConsultationService consultationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider; // SecurityConfig 생성자 의존성 충족용(no-op)

    private static final String BASE = "/api/consultations";

    private String createBody() throws Exception {
        return om.writeValueAsString(ConsultationCreateRequest.builder()
                .studentId(10L)
                .consultationDate(LocalDateTime.of(2026, Month.MAY, 1, 10, 0))
                .content("상담 내용")
                .visibility(ConsultationVisibility.RESTRICTED)
                .build());
    }

    // ===== 인가(AuthZ) — SecurityConfig 매처 검증 =====

    @Test
    @DisplayName("POST 생성: 인증 없으면 401")
    void create_noAuth_unauthorized() throws Exception {
        mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(createBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST 생성: STUDENT는 403 (TEACHER 전용 매처)")
    void create_asStudent_forbidden() throws Exception {
        mvc.perform(post(BASE).with(asStudent(10L)).contentType(APPLICATION_JSON).content(createBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET 검색: STUDENT는 403 (TEACHER/ADMIN 전용 매처)")
    void search_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE + "/search").with(asStudent(10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET 검색: ADMIN은 200 (매처 통과)")
    void search_asAdmin_ok() throws Exception {
        given(consultationService.searchConsultations(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(Page.empty());

        mvc.perform(get(BASE + "/search").with(asAdmin(99L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ===== 해피패스 + 인자 전달 =====

    @Test
    @DisplayName("POST 생성: TEACHER 정상 → 200, principal의 userId가 서비스로 전달")
    void create_asTeacher_ok() throws Exception {
        given(consultationService.createConsultation(eq(1L), any()))
                .willReturn(ConsultationResponse.builder()
                        .consultationId(100L)
                        .studentId(10L)
                        .teacherId(1L)
                        .visibility(ConsultationVisibility.RESTRICTED)
                        .build());

        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content(createBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.consultationId").value(100L));

        verify(consultationService).createConsultation(eq(1L), any());
    }

    // ===== 요청 검증(@Valid) =====

    @Test
    @DisplayName("POST 생성: content 누락이면 400 (TEACHER 인증 통과 후 검증 실패)")
    void create_blankContent_badRequest() throws Exception {
        String invalid = om.writeValueAsString(ConsultationCreateRequest.builder()
                .studentId(10L)
                .consultationDate(LocalDateTime.of(2026, Month.MAY, 1, 10, 0))
                .content(null) // @NotBlank 위반
                .build());

        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }
}
