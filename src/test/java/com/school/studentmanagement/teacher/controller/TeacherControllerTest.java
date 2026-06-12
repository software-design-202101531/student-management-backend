package com.school.studentmanagement.teacher.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.teacher.service.TeacherAssignmentService;
import com.school.studentmanagement.teacher.service.TeacherProfileService;
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

@WebMvcTest(TeacherController.class)
@Import(SecurityConfig.class)
class TeacherControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private TeacherProfileService teacherProfileService;
    @MockitoBean private TeacherAssignmentService teacherAssignmentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/teachers/me";

    @Test
    @DisplayName("내 프로필(교사): 인증 없으면 401")
    void me_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 프로필(교사): STUDENT는 403 (TEACHER 전용)")
    void me_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내 프로필(교사): TEACHER → 200, userId 전달")
    void me_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE).with(asTeacher(1L))).andExpect(status().isOk());
        verify(teacherProfileService).getMyProfile(1L);
    }

    @Test
    @DisplayName("내 담당(교사): TEACHER → 200, teacherId 전달")
    void assignments_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE + "/assignments").with(asTeacher(1L))).andExpect(status().isOk());
        verify(teacherAssignmentService).getMyAssignments(1L);
    }
}
