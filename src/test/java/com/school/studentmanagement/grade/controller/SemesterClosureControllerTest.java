package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.grade.service.SemesterClosureService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asAdmin;
import static com.school.studentmanagement.support.MockAuth.asTeacher;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SemesterClosureController.class)
@Import(SecurityConfig.class)
class SemesterClosureControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private SemesterClosureService semesterClosureService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String CLOSURE = "/api/grades/semesters/2026/1/closure";

    @Test
    @DisplayName("학기마감 상태: 인증 없으면 401")
    void status_noAuth_unauthorized() throws Exception {
        mvc.perform(get(CLOSURE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학기마감 상태: TEACHER는 403 (ADMIN 전용)")
    void status_asTeacher_forbidden() throws Exception {
        mvc.perform(get(CLOSURE).with(asTeacher(1L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학기마감 상태: ADMIN → 200, year·semester 전달")
    void status_asAdmin_ok() throws Exception {
        mvc.perform(get(CLOSURE).with(asAdmin(99L))).andExpect(status().isOk());
        verify(semesterClosureService).getStatus(2026, 1);
    }
}
