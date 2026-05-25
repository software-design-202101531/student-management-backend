package com.school.studentmanagement.consultation.controller;

import com.school.studentmanagement.consultation.dto.ConsultationResponse;
import com.school.studentmanagement.consultation.service.ConsultationService;
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
@RequestMapping("/api/students/{studentId}/consultations")
public class StudentConsultationController {

    private final ConsultationService consultationService;

    // 특정 학생 상담 내역 조회 (교사/관리자) — 권한 통과 조건에 맞는 데이터만 서비스에서 필터링
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsultationResponse>>> getStudentConsultations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId
    ) {
        Long requesterId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                consultationService.getStudentConsultations(studentId, requesterId, userDetails.getUser().getRole())
        ));
    }
}
