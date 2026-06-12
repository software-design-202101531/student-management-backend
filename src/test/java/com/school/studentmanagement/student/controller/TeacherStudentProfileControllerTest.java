package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.student.service.TeacherStudentProfileService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeacherStudentProfileController.class)
@Import(SecurityConfig.class)
class TeacherStudentProfileControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private TeacherStudentProfileService teacherStudentProfileService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/teachers/students/10/profile";

    @Test
    @DisplayName("학생 프로필(교사): 인증 없으면 401")
    void get_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 프로필(교사): STUDENT는 403 (TEACHER 전용)")
    void get_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학생 프로필(교사): TEACHER → 200, studentId·teacherId 전달")
    void get_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE).with(asTeacher(1L))).andExpect(status().isOk());
        verify(teacherStudentProfileService).getProfile(10L, 1L);
    }

    @Test
    @DisplayName("연락처 수정(교사): TEACHER → 200, 위임")
    void updateContact_asTeacher_ok() throws Exception {
        mvc.perform(patch(BASE).with(asTeacher(1L)).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(teacherStudentProfileService).updateContact(eq(10L), eq(1L), any());
    }
}
