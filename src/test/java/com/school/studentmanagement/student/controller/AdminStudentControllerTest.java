package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.student.service.ExcelUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asAdmin;
import static com.school.studentmanagement.support.MockAuth.asTeacher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStudentController.class)
@Import(SecurityConfig.class)
class AdminStudentControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ExcelUploadService excelUploadService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String UPLOAD = "/api/admin/students/excel-upload";

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("엑셀 업로드: 인증 없으면 401")
    void upload_noAuth_unauthorized() throws Exception {
        mvc.perform(multipart(UPLOAD).file(file())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("엑셀 업로드: TEACHER는 403 (ADMIN 전용)")
    void upload_asTeacher_forbidden() throws Exception {
        mvc.perform(multipart(UPLOAD).file(file()).with(asTeacher(1L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("엑셀 업로드: ADMIN → 200, file 위임")
    void upload_asAdmin_ok() throws Exception {
        mvc.perform(multipart(UPLOAD).file(file()).with(asAdmin(99L))).andExpect(status().isOk());
        verify(excelUploadService).uploadStudentExcel(any());
    }
}
