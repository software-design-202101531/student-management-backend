package com.school.studentmanagement.record.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.record.service.SubjectRecordService;
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

@WebMvcTest(SubjectRecordController.class)
@Import(SecurityConfig.class)
class SubjectRecordControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private SubjectRecordService subjectRecordService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    // 경로: classroom=200, subject=3, student=10
    private static final String BASE = "/api/classrooms/200/subjects/3/students/10/records";

    @Test
    @DisplayName("세특 조회: 인증 없으면 401")
    void get_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("세특 조회: STUDENT는 403 (TEACHER 전용)")
    void get_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("세특 조회: TEACHER → 200, (classroom, student, subject, teacher) 순서로 전달")
    void get_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE).with(asTeacher(1L))).andExpect(status().isOk());
        verify(subjectRecordService).getSubjectRecord(200L, 10L, 3L, 1L);
    }
}
