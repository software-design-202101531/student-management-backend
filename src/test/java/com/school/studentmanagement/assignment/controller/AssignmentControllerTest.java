package com.school.studentmanagement.assignment.controller;

import com.school.studentmanagement.assignment.service.AssignmentService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssignmentController.class)
@Import(SecurityConfig.class)
class AssignmentControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private AssignmentService assignmentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/classrooms/200/subjects/3/assignments";
    private static final String VALID = "{\"title\":\"과제1\",\"dueDate\":\"2026-05-01T10:00:00\"}";

    @Test
    @DisplayName("과제 생성: 인증 없으면 401")
    void create_noAuth_unauthorized() throws Exception {
        mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("과제 생성: STUDENT는 403 (TEACHER 전용)")
    void create_asStudent_forbidden() throws Exception {
        mvc.perform(post(BASE).with(asStudent(10L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("과제 생성: TEACHER → 200, userId·classroom·subject 전달")
    void create_asTeacher_ok() throws Exception {
        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());
        verify(assignmentService).createAssignment(eq(1L), eq(200L), eq(3L), any());
    }

    @Test
    @DisplayName("과제 생성: title 누락이면 400")
    void create_missingTitle_badRequest() throws Exception {
        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON)
                        .content("{\"dueDate\":\"2026-05-01T10:00:00\"}"))
                .andExpect(status().isBadRequest());
    }
}
