package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.grade.dto.ClosePreviewResponse;
import com.school.studentmanagement.grade.dto.CloseSemesterRequest;
import com.school.studentmanagement.grade.dto.SemesterClosureResponse;
import com.school.studentmanagement.grade.service.SemesterClosureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grades/semesters/{year}/{semester}")
@RequiredArgsConstructor
public class SemesterClosureController {

    private final SemesterClosureService semesterClosureService;

    // 마감 상태 조회
    @GetMapping("/closure")
    public ResponseEntity<ApiResponse<SemesterClosureResponse>> getStatus(
            @PathVariable Integer year,
            @PathVariable Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(semesterClosureService.getStatus(year, semester)));
    }

    // 마감 미리보기 (어떤 학생의 어떤 시험·과목이 NOT_SUBMITTED 처리될지)
    @PostMapping("/preview-close")
    public ResponseEntity<ApiResponse<ClosePreviewResponse>> previewClose(
            @PathVariable Integer year,
            @PathVariable Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(semesterClosureService.preview(year, semester)));
    }

    // 학기 마감 (수동)
    @PostMapping("/close")
    public ResponseEntity<ApiResponse<SemesterClosureResponse>> close(
            @PathVariable Integer year,
            @PathVariable Integer semester,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) @Valid CloseSemesterRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                semesterClosureService.close(year, semester, userDetails.getUserId(), reason)
        ));
    }

    // 학기 재개방 (NOT_SUBMITTED row는 그대로, 마감 상태만 해제)
    @PostMapping("/reopen")
    public ResponseEntity<ApiResponse<Void>> reopen(
            @PathVariable Integer year,
            @PathVariable Integer semester) {
        semesterClosureService.reopen(year, semester);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
