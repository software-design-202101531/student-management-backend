package com.school.studentmanagement.consultation.controller;

import com.school.studentmanagement.consultation.service.ConsultationService;
import com.school.studentmanagement.global.enums.UserRole;
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

@WebMvcTest(StudentConsultationController.class)
@Import(SecurityConfig.class)
class StudentConsultationControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ConsultationService consultationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/students/10/consultations";

    @Test
    @DisplayName("학생 상담목록: 인증 없으면 401")
    void list_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 상담목록: STUDENT는 403 (TEACHER/ADMIN 전용)")
    void list_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학생 상담목록: TEACHER → 200, studentId·requesterId·role 전달")
    void list_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE).with(asTeacher(1L))).andExpect(status().isOk());
        verify(consultationService).getStudentConsultations(10L, 1L, UserRole.TEACHER);
    }
}
