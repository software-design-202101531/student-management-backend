package com.school.studentmanagement.parent.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.parent.service.ParentChildViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asParent;
import static com.school.studentmanagement.support.MockAuth.asTeacher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentChildViewController.class)
@Import(SecurityConfig.class)
class ParentChildViewControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ParentChildViewService parentChildViewService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String CHILDREN = "/api/parent/me/children";

    @Test
    @DisplayName("자녀 목록: 인증 없으면 401")
    void children_noAuth_unauthorized() throws Exception {
        mvc.perform(get(CHILDREN)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("자녀 목록: TEACHER는 403 (PARENT 전용)")
    void children_asTeacher_forbidden() throws Exception {
        mvc.perform(get(CHILDREN).with(asTeacher(1L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("자녀 목록: PARENT 정상 → 200, parentId 전달")
    void children_asParent_ok() throws Exception {
        mvc.perform(get(CHILDREN).with(asParent(20L))).andExpect(status().isOk());
        verify(parentChildViewService).getMyChildren(20L);
    }

    @Test
    @DisplayName("자녀 성적: PARENT 정상 → 200, parentId·studentId 전달")
    void childGrades_asParent_ok() throws Exception {
        mvc.perform(get(CHILDREN + "/10/grades").with(asParent(20L))).andExpect(status().isOk());
        verify(parentChildViewService).getChildGrades(eq(20L), eq(10L), any(), any());
    }

    @Test
    @DisplayName("자녀 월별 출결: 필수 year·month 전달 → 200")
    void childMonthlyAttendance_asParent_ok() throws Exception {
        mvc.perform(get(CHILDREN + "/10/attendance/monthly")
                        .param("year", "2026").param("month", "5").with(asParent(20L)))
                .andExpect(status().isOk());
        verify(parentChildViewService).getChildMonthlyAttendance(20L, 10L, 2026, 5);
    }
}
