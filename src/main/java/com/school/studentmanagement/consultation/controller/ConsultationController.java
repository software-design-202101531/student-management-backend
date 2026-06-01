package com.school.studentmanagement.consultation.controller;

import com.school.studentmanagement.consultation.dto.ConsultationCreateRequest;
import com.school.studentmanagement.consultation.dto.ConsultationResponse;
import com.school.studentmanagement.consultation.dto.ConsultationUpdateRequest;
import com.school.studentmanagement.consultation.service.ConsultationService;
import com.school.studentmanagement.global.enums.ConsultationVisibility;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultations")
public class ConsultationController {

    // 검색 엔드포인트 페이지 크기 상한 (악의/실수성 거대 페이지 요청 차단)
    private static final int MAX_PAGE_SIZE = 100;

    private final ConsultationService consultationService;

    // 상담 내역 생성 (교사 전용) — visibility 미지정 시 RESTRICTED
    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationResponse>> createConsultation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConsultationCreateRequest request
    ) {
        Long teacherId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                consultationService.createConsultation(teacherId, request)
        ));
    }

    // 교사 간 상담 내역 검색 (TEACHER/ADMIN) — 권한 분기는 서비스/Repository에서 처리.
    // 기본: page=0, size=20, sort=consultationDate,desc. 페이지 크기는 MAX_PAGE_SIZE로 강제 제한.
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ConsultationResponse>>> searchConsultations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) ConsultationVisibility visibility,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "consultationDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long requesterId = userDetails.getUserId();
        Pageable clamped = clampPageSize(pageable);
        return ResponseEntity.ok(ApiResponse.ok(
                consultationService.searchConsultations(
                        requesterId, userDetails.getUser().getRole(),
                        studentId, teacherId, visibility, keyword, from, to, clamped)
        ));
    }

    private Pageable clampPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }

    // 상담 내역 수정 (작성자 본인만) — 본문/일시/계획/공개범위 갱신
    @PutMapping("/{consultationId}")
    public ResponseEntity<ApiResponse<ConsultationResponse>> updateConsultation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long consultationId,
            @Valid @RequestBody ConsultationUpdateRequest request
    ) {
        Long requesterId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                consultationService.updateConsultation(consultationId, requesterId, request)
        ));
    }

    // 공개 범위 토글 (작성자 본인 또는 관리자) — RESTRICTED <-> ALL_TEACHERS
    @PatchMapping("/{consultationId}/visibility")
    public ResponseEntity<ApiResponse<ConsultationResponse>> toggleVisibility(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long consultationId
    ) {
        Long requesterId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                consultationService.toggleVisibility(consultationId, requesterId, userDetails.getUser().getRole())
        ));
    }
}
