package com.school.studentmanagement.record.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.record.service.SubjectRecordService;
import com.school.studentmanagement.record.dto.SubjectRecordRequest;
import com.school.studentmanagement.record.dto.SubjectRecordResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classrooms/{classroomId}/subjects/{subjectId}/students/{studentId}/records")
@RequiredArgsConstructor
public class SubjectRecordController {

    private final SubjectRecordService subjectRecordService;

    // 과세특 상세 조회 API
    @GetMapping
    public ResponseEntity<ApiResponse<SubjectRecordResponse>> getSubjectRecord(
            @PathVariable("classroomId") Long classroomId,
            @PathVariable("subjectId") Long subjectId,
            @PathVariable("studentId") Long studentId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long teacherId = customUserDetails.getUserId();
        SubjectRecordResponse response = subjectRecordService.getSubjectRecord(classroomId, studentId, subjectId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 과세특 저장 및 수정
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveSubjectRecord(
            @PathVariable("classroomId") Long classroomId,
            @PathVariable("subjectId") Long subjectId,
            @PathVariable("studentId") Long studentId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody SubjectRecordRequest request
    ) {
        Long teacherId = customUserDetails.getUserId();
        subjectRecordService.saveSubjectRecord(classroomId, studentId, subjectId, teacherId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
