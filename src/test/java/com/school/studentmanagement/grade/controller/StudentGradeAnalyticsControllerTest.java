package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.grade.service.ClassroomStatsService;
import com.school.studentmanagement.grade.service.GradeAnalyticsService;
import com.school.studentmanagement.grade.service.GradeSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asStudent;
import static com.school.studentmanagement.support.MockAuth.asTeacher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentGradeAnalyticsController.class)
@Import(SecurityConfig.class)
class StudentGradeAnalyticsControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private GradeAnalyticsService gradeAnalyticsService;
    @MockitoBean private ClassroomStatsService classroomStatsService;
    @MockitoBean private GradeSearchService gradeSearchService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/student/me/grades";

    @Test
    @DisplayName("레이더: 인증 없으면 401")
    void radar_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE + "/radar")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("레이더: TEACHER는 403 (STUDENT 전용)")
    void radar_asTeacher_forbidden() throws Exception {
        mvc.perform(get(BASE + "/radar").with(asTeacher(1L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("레이더: 학생 본인 → 200, userId 전달")
    void radar_asStudent_ok() throws Exception {
        mvc.perform(get(BASE + "/radar").with(asStudent(10L))).andExpect(status().isOk());
        verify(gradeAnalyticsService).getStudentRadar(eq(10L), any(), any());
    }

    @Test
    @DisplayName("성적검색: 학생 본인 → 200, userId 전달")
    void search_asStudent_ok() throws Exception {
        mvc.perform(get(BASE + "/search").param("subjectId", "3").with(asStudent(10L)))
                .andExpect(status().isOk());
        verify(gradeSearchService).searchForStudent(eq(10L), eq(3L), any(), any(), any(), any());
    }
}
