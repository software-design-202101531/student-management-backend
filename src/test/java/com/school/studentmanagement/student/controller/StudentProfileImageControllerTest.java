package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.student.service.StudentProfileImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asStudent;
import static com.school.studentmanagement.support.MockAuth.asTeacher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentProfileImageController.class)
@Import(SecurityConfig.class)
class StudentProfileImageControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentProfileImageService studentProfileImageService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String UPLOAD = "/api/students/10/profile-image";

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("학생 사진 업로드: 인증 없으면 401")
    void upload_noAuth_unauthorized() throws Exception {
        mvc.perform(multipart(UPLOAD).file(file())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 사진 업로드: STUDENT는 403 (TEACHER 전용)")
    void upload_asStudent_forbidden() throws Exception {
        mvc.perform(multipart(UPLOAD).file(file()).with(asStudent(10L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학생 사진 업로드: TEACHER → 200, studentId·teacherId·file 전달")
    void upload_asTeacher_ok() throws Exception {
        mvc.perform(multipart(UPLOAD).file(file()).with(asTeacher(1L))).andExpect(status().isOk());
        verify(studentProfileImageService).updateProfileImage(eq(10L), eq(1L), any());
    }
}
