package com.school.studentmanagement.assignment.controller;

import com.school.studentmanagement.assignment.service.StudentAssignmentService;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
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

@WebMvcTest(StudentAssignmentController.class)
@Import(SecurityConfig.class)
class StudentAssignmentControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentAssignmentService studentAssignmentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/student/me/assignments";

    @Test
    @DisplayName("내 과제: 인증 없으면 401")
    void list_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 과제: TEACHER는 403 (STUDENT 전용)")
    void list_asTeacher_forbidden() throws Exception {
        mvc.perform(get(BASE).with(asTeacher(1L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내 과제: 학생 본인 → 200, userId 전달")
    void list_asStudent_ok() throws Exception {
        mvc.perform(get(BASE).with(asStudent(10L))).andExpect(status().isOk());
        verify(studentAssignmentService).getMyAssignments(10L);
    }
}
