package com.school.studentmanagement.notification.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.notification.dto.NotificationResponse;
import com.school.studentmanagement.notification.dto.UnreadCountResponse;
import com.school.studentmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    // 페이지 크기 상한 (거대 페이지 요청 차단)
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    // 본인 알림 목록 (최신순, 페이지네이션)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable clamped = clampPageSize(pageable);
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getMyNotifications(userDetails.getUserId(), clamped)
        ));
    }

    // 미확인 알림 개수 (뱃지 갱신용)
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long count = notificationService.getUnreadCount(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(UnreadCountResponse.of(count)));
    }

    // 단건 확인 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userDetails.getUserId(), notificationId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 전체 확인 처리
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAllAsRead(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private Pageable clampPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }
}
