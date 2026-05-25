package com.school.studentmanagement.feedback.controller;

import com.school.studentmanagement.feedback.dto.FeedbackCreateRequest;
import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.feedback.dto.FeedbackUpdateRequest;
import com.school.studentmanagement.feedback.service.FeedbackService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    // 피드백 생성 (교사 전용) — 최초 status = DRAFT
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedbackCreateRequest request
    ) {
        Long teacherId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                feedbackService.createFeedback(teacherId, request)
        ));
    }

    // 피드백 수정 (작성자 본인만) — 본문/분류/공개옵션 변경
    @PutMapping("/{feedbackId}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackUpdateRequest request
    ) {
        Long teacherId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                feedbackService.updateFeedback(feedbackId, teacherId, request)
        ));
    }

    // 피드백 최종 발행 (작성자 본인만) — DRAFT -> PUBLISHED
    @PatchMapping("/{feedbackId}/publish")
    public ResponseEntity<ApiResponse<Void>> publishFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedbackId
    ) {
        Long teacherId = userDetails.getUserId();
        feedbackService.publishFeedback(feedbackId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
