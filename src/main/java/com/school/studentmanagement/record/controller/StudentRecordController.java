package com.school.studentmanagement.record.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.record.dto.BehaviorRecordRequest;
import com.school.studentmanagement.record.dto.BehaviorRecordResponse;
import com.school.studentmanagement.record.service.StudentRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students/{studentId}/records/behavior")
public class StudentRecordController {

    private final StudentRecordService studentRecordService;

    // 상세 조회 API
    @GetMapping
    public ResponseEntity<ApiResponse<BehaviorRecordResponse>> getBehaviorRecord(
            @PathVariable Long studentId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long teacherId = customUserDetails.getUserId();
        BehaviorRecordResponse response = studentRecordService.getBehaviorRecord(studentId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 행특 저장 및 수정 API
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveBehaviorRecord(
            @PathVariable Long studentId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody BehaviorRecordRequest request
    ) {
        Long teacherId = customUserDetails.getUserId();
        studentRecordService.saveBehaviorRecord(studentId, teacherId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
