package com.school.studentmanagement.consultation.controller;

import com.school.studentmanagement.consultation.dto.ConsultationCreateRequest;
import com.school.studentmanagement.consultation.dto.ConsultationResponse;
import com.school.studentmanagement.consultation.service.ConsultationService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultations")
public class ConsultationController {

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
