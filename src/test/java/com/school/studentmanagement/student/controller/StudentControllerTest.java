package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.student.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * StudentController(/api/user) — 회원가입 전 단계라 permitAll. 인증 없이 접근되며,
 * 검증(@Valid)·서비스 위임을 확인한다.
 */
@WebMvcTest(StudentController.class)
@Import(SecurityConfig.class)
class StudentControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentService studentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("학생 검증: permitAll → 인증 없이 200, 서비스 위임")
    void verifyStudent_ok() throws Exception {
        String body = "{\"academicYear\":2026,\"grade\":1,\"classNum\":4,\"studentNum\":1,\"name\":\"홍길동\"}";
        mvc.perform(post("/api/user/verify-student").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(studentService).verifyStudent(any());
    }

    @Test
    @DisplayName("학생 검증: name 누락이면 400")
    void verifyStudent_missingName_badRequest() throws Exception {
        String invalid = "{\"academicYear\":2026,\"grade\":1,\"classNum\":4,\"studentNum\":1}";
        mvc.perform(post("/api/user/verify-student").contentType(APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("학생 활성화: 유효 입력 → 200")
    void activateStudent_ok() throws Exception {
        String body = "{\"academicYear\":2026,\"grade\":1,\"classNum\":4,\"studentNum\":1,\"name\":\"홍길동\","
                + "\"loginId\":\"user1234\",\"password\":\"Pass123!\",\"address\":\"서울시\","
                + "\"phoneNumber\":\"01012345678\"}";
        mvc.perform(post("/api/user/activate-student").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(studentService).activateStudentAccount(any());
    }
}
