package com.school.studentmanagement.parent.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.grade.service.ClassroomStatsService;
import com.school.studentmanagement.grade.service.GradeAnalyticsService;
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

@WebMvcTest(ParentChildAnalyticsController.class)
@Import(SecurityConfig.class)
class ParentChildAnalyticsControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private GradeAnalyticsService gradeAnalyticsService;
    @MockitoBean private ClassroomStatsService classroomStatsService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String RADAR = "/api/parent/me/children/10/grades/radar";

    @Test
    @DisplayName("자녀 레이더: 인증 없으면 401")
    void radar_noAuth_unauthorized() throws Exception {
        mvc.perform(get(RADAR)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("자녀 레이더: TEACHER는 403 (PARENT 전용)")
    void radar_asTeacher_forbidden() throws Exception {
        mvc.perform(get(RADAR).with(asTeacher(1L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("자녀 레이더: PARENT → 200, parentId·studentId 전달")
    void radar_asParent_ok() throws Exception {
        mvc.perform(get(RADAR).with(asParent(20L))).andExpect(status().isOk());
        verify(gradeAnalyticsService).getChildRadar(eq(20L), eq(10L), any(), any());
    }
}
