package com.school.studentmanagement.feedback.controller;

import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.feedback.service.FeedbackService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students/{studentId}/feedbacks")
public class StudentFeedbackController {

    private final FeedbackService feedbackService;

    // 피드백 목록 조회 (교사/학생/학부모) — 권한에 따라 노출 범위가 서비스에서 분기됨
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getStudentFeedbacks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId
    ) {
        Long requesterId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                feedbackService.getStudentFeedbacks(studentId, requesterId, userDetails.getUser().getRole())
        ));
    }
}
