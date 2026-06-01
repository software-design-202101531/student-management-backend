package com.school.studentmanagement.feedback.controller;

import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.feedback.service.FeedbackService;
import com.school.studentmanagement.global.enums.FeedbackCategory;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students/{studentId}/feedbacks")
public class StudentFeedbackController {

    // 검색 엔드포인트 페이지 크기 상한 (악의/실수성 거대 페이지 요청 차단)
    private static final int MAX_PAGE_SIZE = 100;

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

    // 피드백 검색 — 카테고리/기간 필터 + 페이지네이션. 권한별 노출 범위는 기본 조회와 동일.
    // 기본: page=0, size=20, sort=createdAt,desc. 페이지 크기는 MAX_PAGE_SIZE로 강제 제한.
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<FeedbackResponse>>> searchStudentFeedbacks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) FeedbackCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long requesterId = userDetails.getUserId();
        Pageable clamped = clampPageSize(pageable);
        return ResponseEntity.ok(ApiResponse.ok(
                feedbackService.searchStudentFeedbacks(
                        studentId, requesterId, userDetails.getUser().getRole(),
                        category, from, to, clamped)
        ));
    }

    private Pageable clampPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }
}
