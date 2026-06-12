package com.school.studentmanagement.notification.controller;

import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.global.security.SecurityConfig;
import com.school.studentmanagement.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.school.studentmanagement.support.MockAuth.asParent;
import static com.school.studentmanagement.support.MockAuth.asStudent;
import static com.school.studentmanagement.support.MockAuth.asTeacher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private NotificationService notificationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private static final String BASE = "/api/notifications";

    @Test
    @DisplayName("목록: 인증 없으면 401")
    void list_noAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("목록: 인증 사용자면 200, 본인 userId로 조회")
    void list_authenticated_ok() throws Exception {
        given(notificationService.getMyNotifications(eq(10L), any())).willReturn(Page.empty());
        mvc.perform(get(BASE).with(asStudent(10L))).andExpect(status().isOk());
        verify(notificationService).getMyNotifications(eq(10L), any());
    }

    @Test
    @DisplayName("미확인 개수: 인증 사용자면 200")
    void unreadCount_ok() throws Exception {
        mvc.perform(get(BASE + "/unread-count").with(asParent(20L))).andExpect(status().isOk());
        verify(notificationService).getUnreadCount(20L);
    }

    @Test
    @DisplayName("단건 읽음: 본인 userId + notificationId 전달")
    void markRead_ok() throws Exception {
        mvc.perform(patch(BASE + "/5/read").with(asTeacher(1L))).andExpect(status().isOk());
        verify(notificationService).markAsRead(1L, 5L);
    }

    @Test
    @DisplayName("전체 읽음: 본인 userId 전달")
    void markAllRead_ok() throws Exception {
        mvc.perform(patch(BASE + "/read-all").with(asStudent(10L))).andExpect(status().isOk());
        verify(notificationService).markAllAsRead(10L);
    }
}
