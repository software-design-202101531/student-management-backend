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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClassroomStatsController.class)
@Import(SecurityConfig.class)
class ClassroomStatsControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClassroomStatsService classroomStatsService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String STATS = "/api/classrooms/200/grades/stats";

    @Test
    @DisplayName("학급 통계: 인증 없으면 401")
    void stats_noAuth_unauthorized() throws Exception {
        mvc.perform(get(STATS).param("examId", "1").param("subjectId", "3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학급 통계: STUDENT는 403 (TEACHER 전용)")
    void stats_asStudent_forbidden() throws Exception {
        mvc.perform(get(STATS).param("examId", "1").param("subjectId", "3").with(asStudent(10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학급 통계: TEACHER → 200, classroom·teacher·exam·subject 전달")
    void stats_asTeacher_ok() throws Exception {
        mvc.perform(get(STATS).param("examId", "1").param("subjectId", "3").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(classroomStatsService).getClassroomStats(200L, 1L, 1L, 3L);
    }
}
