package com.school.studentmanagement.analytics.controller;

import com.school.studentmanagement.analytics.etl.AnalyticsEtlService;
import com.school.studentmanagement.analytics.service.AnalyticsDashboardService;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asAdmin;
import static com.school.studentmanagement.support.MockAuth.asStudent;
import static com.school.studentmanagement.support.MockAuth.asTeacher;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
class AnalyticsControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private AnalyticsDashboardService dashboardService;
    @MockitoBean private AnalyticsEtlService etlService;
    @MockitoBean private AcademicCalendarUtil academicCalendarUtil;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("학생 분석: 인증 없으면 401")
    void studentOverview_noAuth_unauthorized() throws Exception {
        mvc.perform(get("/api/analytics/students/10/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 분석: STUDENT는 403 (TEACHER/ADMIN 전용)")
    void studentOverview_asStudent_forbidden() throws Exception {
        mvc.perform(get("/api/analytics/students/10/overview").with(asStudent(10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학생 분석: TEACHER → 200 (기본 학기 보정 후 위임)")
    void studentOverview_asTeacher_ok() throws Exception {
        given(academicCalendarUtil.getCurrentAcademicYear()).willReturn(2026);
        given(academicCalendarUtil.getCurrentSemester()).willReturn(1);
        mvc.perform(get("/api/analytics/students/10/overview").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(dashboardService).getStudentOverview(10L, 2026, 1);
    }

    @Test
    @DisplayName("ETL 실행: TEACHER는 403 (ADMIN 전용)")
    void etl_asTeacher_forbidden() throws Exception {
        mvc.perform(post("/api/analytics/etl/run").with(asTeacher(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ETL 실행: ADMIN → 200, runAll 호출")
    void etl_asAdmin_ok() throws Exception {
        mvc.perform(post("/api/analytics/etl/run").with(asAdmin(99L))).andExpect(status().isOk());
        verify(etlService).runAll();
    }
}
