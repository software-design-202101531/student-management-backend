package com.school.studentmanagement.feedback.controller;

import com.school.studentmanagement.feedback.service.FeedbackService;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asParent;
import static com.school.studentmanagement.support.MockAuth.asStudent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentFeedbackController.class)
@Import(SecurityConfig.class)
class StudentFeedbackControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private FeedbackService feedbackService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/students/10/feedbacks";

    @Test
    @DisplayName("조회: 인증 없으면 401")
    void get_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("조회: 학부모 인증 → 200, studentId·requesterId·role 전달")
    void get_asParent_ok() throws Exception {
        mvc.perform(get(BASE).with(asParent(20L))).andExpect(status().isOk());
        verify(feedbackService).getStudentFeedbacks(10L, 20L, UserRole.PARENT);
    }

    @Test
    @DisplayName("검색: 학생 본인 인증 → 200")
    void search_asStudent_ok() throws Exception {
        given(feedbackService.searchStudentFeedbacks(eq(10L), eq(10L), any(), any(), any(), any(), any()))
                .willReturn(Page.empty());
        mvc.perform(get(BASE + "/search").with(asStudent(10L))).andExpect(status().isOk());
    }
}
