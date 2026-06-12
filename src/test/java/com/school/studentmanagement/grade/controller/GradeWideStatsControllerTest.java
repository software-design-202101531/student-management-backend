package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.grade.service.ClassroomStatsService;
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

@WebMvcTest(GradeWideStatsController.class)
@Import(SecurityConfig.class)
class GradeWideStatsControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClassroomStatsService classroomStatsService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String RANKING = "/api/grades/grade-ranking";

    @Test
    @DisplayName("학년 석차: 인증 없으면 401")
    void ranking_noAuth_unauthorized() throws Exception {
        mvc.perform(get(RANKING).param("grade", "1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학년 석차: STUDENT는 403 (TEACHER/ADMIN 전용)")
    void ranking_asStudent_forbidden() throws Exception {
        mvc.perform(get(RANKING).param("grade", "1").with(asStudent(10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학년 석차: TEACHER → 200, grade 전달")
    void ranking_asTeacher_ok() throws Exception {
        mvc.perform(get(RANKING).param("grade", "1").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(classroomStatsService).getGradeWideRanking(any(), any(), eq(1));
    }
}
