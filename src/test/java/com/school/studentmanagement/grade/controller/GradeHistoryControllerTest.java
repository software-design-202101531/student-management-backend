package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.grade.service.GradeHistoryService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GradeHistoryController.class)
@Import(SecurityConfig.class)
class GradeHistoryControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private GradeHistoryService gradeHistoryService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String HISTORY = "/api/grades/5/history";

    @Test
    @DisplayName("성적 이력: 인증 없으면 401")
    void history_noAuth_unauthorized() throws Exception {
        mvc.perform(get(HISTORY)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("성적 이력: STUDENT는 403 (TEACHER/ADMIN 전용)")
    void history_asStudent_forbidden() throws Exception {
        mvc.perform(get(HISTORY).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("성적 이력: TEACHER → 200, gradeId·teacherId 전달")
    void history_asTeacher_ok() throws Exception {
        mvc.perform(get(HISTORY).with(asTeacher(1L))).andExpect(status().isOk());
        verify(gradeHistoryService).getHistory(5L, 1L);
    }

    @Test
    @DisplayName("성적 이력: ADMIN도 허용 → 200")
    void history_asAdmin_ok() throws Exception {
        mvc.perform(get(HISTORY).with(asAdmin(99L))).andExpect(status().isOk());
    }
}
