package com.school.studentmanagement.user.controller;

import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.user.service.MyProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asStudent;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MyProfileController.class)
@Import(SecurityConfig.class)
class MyProfileControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private MyProfileService myProfileService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String ME = "/api/me";

    @Test
    @DisplayName("내 프로필: 인증 없으면 401")
    void me_noAuth_unauthorized() throws Exception {
        mvc.perform(get(ME)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 프로필: 인증 사용자(any role) → 200, userId·role 전달")
    void me_authenticated_ok() throws Exception {
        mvc.perform(get(ME).with(asStudent(10L))).andExpect(status().isOk());
        verify(myProfileService).getMyProfile(10L, UserRole.STUDENT);
    }
}
