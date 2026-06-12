package com.school.studentmanagement.attendance.controller;

import com.school.studentmanagement.attendance.service.AttendanceService;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@Import(SecurityConfig.class)
class AttendanceControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private AttendanceService attendanceService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/classrooms/200/attendance";

    @Test
    @DisplayName("월별 출결: 인증 없으면 401")
    void monthly_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE + "/monthly").param("year", "2026").param("month", "5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("월별 출결: STUDENT는 403 (TEACHER 전용)")
    void monthly_asStudent_forbidden() throws Exception {
        mvc.perform(get(BASE + "/monthly").param("year", "2026").param("month", "5").with(asStudent(10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("월별 출결: TEACHER → 200, classroom·teacher·year·month 전달")
    void monthly_asTeacher_ok() throws Exception {
        mvc.perform(get(BASE + "/monthly").param("year", "2026").param("month", "5").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(attendanceService).getMonthlyAttendance(200L, 1L, 2026, 5);
    }

    @Test
    @DisplayName("일별 출결 저장: attendanceData 비면 400")
    void saveDaily_emptyData_badRequest() throws Exception {
        mvc.perform(post(BASE + "/daily").param("date", "2026-05-01").with(asTeacher(1L))
                        .contentType(APPLICATION_JSON).content("{\"attendanceData\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
