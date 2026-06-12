package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.student.service.StudentAttendanceService;
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

@WebMvcTest(StudentMyAttendanceController.class)
@Import(SecurityConfig.class)
class StudentMyAttendanceControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentAttendanceService studentAttendanceService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String MONTHLY = "/api/student/me/attendance/monthly";

    @Test
    @DisplayName("내 월별 출결: 인증 없으면 401")
    void monthly_noAuth_unauthorized() throws Exception {
        mvc.perform(get(MONTHLY).param("year", "2026").param("month", "5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 월별 출결: TEACHER는 403 (STUDENT 전용)")
    void monthly_asTeacher_forbidden() throws Exception {
        mvc.perform(get(MONTHLY).param("year", "2026").param("month", "5").with(asTeacher(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내 월별 출결: 학생 본인 → 200, userId·year·month 전달")
    void monthly_asStudent_ok() throws Exception {
        mvc.perform(get(MONTHLY).param("year", "2026").param("month", "5").with(asStudent(10L)))
                .andExpect(status().isOk());
        verify(studentAttendanceService).getMyMonthlyAttendance(10L, 2026, 5);
    }
}
