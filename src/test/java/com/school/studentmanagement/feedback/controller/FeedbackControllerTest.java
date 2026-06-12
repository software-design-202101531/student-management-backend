package com.school.studentmanagement.feedback.controller;

import com.school.studentmanagement.feedback.service.FeedbackService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
@Import(SecurityConfig.class)
class FeedbackControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private FeedbackService feedbackService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/feedbacks";
    private static final String VALID =
            "{\"studentId\":10,\"category\":\"BEHAVIOR\",\"content\":\"성실함\",\"isPublic\":true}";

    @Test
    @DisplayName("작성: 인증 없으면 401")
    void create_noAuth_unauthorized() throws Exception {
        mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("작성: STUDENT는 403 (TEACHER 전용)")
    void create_asStudent_forbidden() throws Exception {
        mvc.perform(post(BASE).with(asStudent(10L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("작성: TEACHER 정상 → 200, principal userId 전달")
    void create_asTeacher_ok() throws Exception {
        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());
        verify(feedbackService).createFeedback(eq(1L), any());
    }

    @Test
    @DisplayName("작성: content 누락이면 400")
    void create_blankContent_badRequest() throws Exception {
        String invalid = "{\"studentId\":10,\"category\":\"BEHAVIOR\",\"isPublic\":true}";
        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("발행: TEACHER 정상 → 200, feedbackId+userId 전달")
    void publish_asTeacher_ok() throws Exception {
        mvc.perform(patch(BASE + "/5/publish").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(feedbackService).publishFeedback(5L, 1L);
    }
}
