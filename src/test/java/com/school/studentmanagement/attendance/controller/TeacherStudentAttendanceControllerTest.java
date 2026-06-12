package com.school.studentmanagement.attendance.controller;

import com.school.studentmanagement.attendance.service.TeacherStudentAttendanceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeacherStudentAttendanceController.class)
@Import(SecurityConfig.class)
class TeacherStudentAttendanceControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private TeacherStudentAttendanceService teacherStudentAttendanceService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/teachers/students/10/attendance";

    @Test
    @DisplayName("학생 출결 범위(교사): 인증 없으면 401")
    void range_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE).param("from", "2026-03-01").param("to", "2026-05-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 출결 범위(교사): STUDENT는 403 (TEACHER 전용)")
    void range_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE).param("from", "2026-03-01").param("to", "2026-05-31").with(asStudent(10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학생 출결 범위(교사): TEACHER → 200, teacherId·studentId 전달")
    void range_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE).param("from", "2026-03-01").param("to", "2026-05-31").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(teacherStudentAttendanceService).getStudentAttendanceRange(eq(1L), eq(10L), any(), any());
    }
}
