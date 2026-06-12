package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.grade.service.StudentGradeService;
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

@WebMvcTest(StudentGradeController.class)
@Import(SecurityConfig.class)
class StudentGradeControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentGradeService studentGradeService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String GRADES = "/api/classrooms/200/subjects/3/grades";
    private static final String VALID = "{\"examId\":1,\"scores\":[{\"studentId\":10,\"rawScore\":90}]}";

    @Test
    @DisplayName("입력: 인증 없으면 401")
    void save_noAuth_unauthorized() throws Exception {
        mvc.perform(post(GRADES).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("입력: STUDENT는 403 (TEACHER 전용)")
    void save_asStudent_forbidden() throws Exception {
        mvc.perform(post(GRADES).with(asStudent(10L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("입력: TEACHER 정상 → 200, classroom/subject/teacher 전달")
    void save_asTeacher_ok() throws Exception {
        mvc.perform(post(GRADES).with(asTeacher(1L)).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());
        verify(studentGradeService).saveGrades(eq(200L), eq(3L), eq(1L), any());
    }

    @Test
    @DisplayName("입력: scores 비어있으면 400")
    void save_emptyScores_badRequest() throws Exception {
        mvc.perform(post(GRADES).with(asTeacher(1L)).contentType(APPLICATION_JSON)
                        .content("{\"examId\":1,\"scores\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("과목 성적 조회: TEACHER 정상 → 200, examId 전달")
    void getSubjectGrades_asTeacher_ok() throws Exception {
        mvc.perform(get(GRADES).param("examId", "1").with(asTeacher(1L)))
                .andExpect(status().isOk());
        verify(studentGradeService).getSubjectGrades(200L, 3L, 1L, 1L);
    }
}
