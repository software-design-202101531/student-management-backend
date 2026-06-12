package com.school.studentmanagement.parent.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.parent.service.ParentAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ParentAuthController(/api/parents) — 가입 전 단계라 permitAll. 검증·서비스 위임 확인.
 */
@WebMvcTest(ParentAuthController.class)
@Import(SecurityConfig.class)
class ParentAuthControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ParentAuthService parentAuthService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String VERIFY = "/api/parents/verify";
    private static final String VALID =
            "{\"year\":2026,\"grade\":1,\"classNum\":4,\"studentNum\":1,"
            + "\"studentName\":\"홍길동\",\"parentPhone\":\"01012345678\"}";

    @Test
    @DisplayName("학부모 검증: permitAll → 인증 없이 200, 서비스 위임")
    void verify_ok() throws Exception {
        mvc.perform(post(VERIFY).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());
        verify(parentAuthService).verifyParent(any());
    }

    @Test
    @DisplayName("학부모 검증: studentName 누락이면 400")
    void verify_missingName_badRequest() throws Exception {
        String invalid = "{\"year\":2026,\"grade\":1,\"classNum\":4,\"studentNum\":1,"
                + "\"parentPhone\":\"01012345678\"}";
        mvc.perform(post(VERIFY).contentType(APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }
}
