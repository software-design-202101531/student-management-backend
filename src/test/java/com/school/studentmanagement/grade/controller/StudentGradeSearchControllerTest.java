package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
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

@WebMvcTest(StudentGradeSearchController.class)
@Import(SecurityConfig.class)
class StudentGradeSearchControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private GradeSearchService gradeSearchService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/students/10/grades/search";

    @Test
    @DisplayName("교사 성적검색: 인증 없으면 401")
    void search_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("교사 성적검색: STUDENT는 403 (TEACHER 전용)")
    void search_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("교사 성적검색: TEACHER 정상 → 200, studentId·teacherId·subjectId 전달")
    void search_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE).param("subjectId", "3").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(gradeSearchService).searchForTeacher(eq(10L), eq(1L), eq(3L), any(), any(), any(), any());
    }
}
