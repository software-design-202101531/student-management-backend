package com.school.studentmanagement.record.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.record.service.StudentRecordService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentRecordController.class)
@Import(SecurityConfig.class)
class StudentRecordControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentRecordService studentRecordService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/students/10/records/behavior";

    @Test
    @DisplayName("행특 조회: 인증 없으면 401")
    void get_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("행특 조회: STUDENT는 403 (TEACHER 전용)")
    void get_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("행특 조회: TEACHER → 200, studentId·teacherId 전달")
    void get_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE).with(asTeacher(1L))).andExpect(status().isOk());
        verify(studentRecordService).getBehaviorRecord(10L, 1L);
    }

    @Test
    @DisplayName("행특 저장: TEACHER → 200, 위임")
    void save_asTeacher_ok() throws Exception {
        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON)
                        .content("{\"content\":\"성실한 학생\"}"))
                .andExpect(status().isOk());
        verify(studentRecordService).saveBehaviorRecord(eq(10L), eq(1L), any());
    }

    @Test
    @DisplayName("행특 저장: content 누락이면 400")
    void save_blankContent_badRequest() throws Exception {
        mvc.perform(post(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
