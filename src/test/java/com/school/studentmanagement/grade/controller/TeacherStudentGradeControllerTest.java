package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.grade.service.GradeAnalyticsService;
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

@WebMvcTest(TeacherStudentGradeController.class)
@Import(SecurityConfig.class)
class TeacherStudentGradeControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private GradeAnalyticsService gradeAnalyticsService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String OVERVIEW = "/api/teachers/students/10/grades/overview";

    @Test
    @DisplayName("학생 개요(교사): 인증 없으면 401")
    void overview_noAuth_unauthorized() throws Exception {
        mvc.perform(get(OVERVIEW)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 개요(교사): STUDENT는 403 (TEACHER 전용)")
    void overview_asStudent_forbidden() throws Exception {
        mvc.perform(get(OVERVIEW).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학생 개요(교사): TEACHER → 200, teacherId·studentId 전달")
    void overview_asTeacher_ok() throws Exception {
        mvc.perform(get(OVERVIEW).with(asTeacher(1L))).andExpect(status().isOk());
        verify(gradeAnalyticsService).getStudentOverviewForTeacher(eq(1L), eq(10L), any(), any());
    }
}
