package com.school.studentmanagement.classroom.controller;

import com.school.studentmanagement.classroom.service.ClassroomStudentService;
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

@WebMvcTest(ClassroomController.class)
@Import(SecurityConfig.class)
class ClassroomControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClassroomStudentService classroomStudentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String STUDENTS = "/api/classrooms/200/students";

    @Test
    @DisplayName("학급 학생목록: 인증 없으면 401")
    void students_noAuth_unauthorized() throws Exception {
        mvc.perform(get(STUDENTS)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학급 학생목록: STUDENT는 403 (TEACHER 전용)")
    void students_asStudent_forbidden() throws Exception {
        mvc.perform(get(STUDENTS).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학급 학생목록: TEACHER → 200, classroomId·teacherId 전달")
    void students_asTeacher_ok() throws Exception {
        mvc.perform(get(STUDENTS).with(asTeacher(1L))).andExpect(status().isOk());
        verify(classroomStudentService).getStudentsInClassroom(200L, 1L);
    }
}
