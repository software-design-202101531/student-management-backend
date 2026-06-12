package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.student.service.StudentMyPageService;
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

@WebMvcTest(StudentMyPageController.class)
@Import(SecurityConfig.class)
class StudentMyPageControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentMyPageService studentMyPageService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String GRADES = "/api/student/me/grades";

    @Test
    @DisplayName("내 성적: 인증 없으면 401")
    void grades_noAuth_unauthorized() throws Exception {
        mvc.perform(get(GRADES)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 성적: TEACHER는 403 (STUDENT 전용)")
    void grades_asTeacher_forbidden() throws Exception {
        mvc.perform(get(GRADES).with(asTeacher(1L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내 성적: 학생 본인 → 200, userId 전달")
    void grades_asStudent_ok() throws Exception {
        mvc.perform(get(GRADES).with(asStudent(10L))).andExpect(status().isOk());
        verify(studentMyPageService).getMyGrades(eq(10L), any(), any());
    }
}
