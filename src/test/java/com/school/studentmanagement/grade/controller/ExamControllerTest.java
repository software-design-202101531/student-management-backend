package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.grade.service.ExamService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExamController.class)
@Import(SecurityConfig.class)
class ExamControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ExamService examService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/exams";
    private static final String VALID =
            "{\"academicYear\":2026,\"semester\":1,\"examType\":\"MIDTERM\",\"name\":\"중간고사\","
            + "\"maxScore\":100,\"weight\":0.3}";

    @Test
    @DisplayName("시험 생성: 인증 없으면 401")
    void create_noAuth_unauthorized() throws Exception {
        mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("시험 생성: STUDENT는 403 (TEACHER/ADMIN 전용)")
    void create_asStudent_forbidden() throws Exception {
        mvc.perform(post(BASE).with(asStudent(10L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("시험 생성: TEACHER 정상 → 200")
    void create_asTeacher_ok() throws Exception {
        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());
        verify(examService).createExam(any());
    }

    @Test
    @DisplayName("시험 생성: ADMIN도 허용 → 200")
    void create_asAdmin_ok() throws Exception {
        mvc.perform(post(BASE).with(asAdmin(99L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("시험 생성: name 누락이면 400")
    void create_missingName_badRequest() throws Exception {
        String invalid = "{\"academicYear\":2026,\"semester\":1,\"examType\":\"MIDTERM\","
                + "\"maxScore\":100,\"weight\":0.3}";
        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("시험 발행: TEACHER 정상 → 200")
    void publish_asTeacher_ok() throws Exception {
        mvc.perform(post(BASE + "/5/publish").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(examService).publish(5L);
    }
}
