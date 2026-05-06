package com.school.studentmanagement.parent.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.parent.dto.ChildInfoResponse;
import com.school.studentmanagement.parent.service.ParentChildViewService;
import com.school.studentmanagement.student.dto.StudentMyGradeResponse;
import com.school.studentmanagement.student.dto.StudentMyRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parent/me")
public class ParentChildViewController {

    private final ParentChildViewService parentChildViewService;

    // 내 자녀 목록 조회 (학년·반·번호 포함)
    @GetMapping("/children")
    public ResponseEntity<ApiResponse<List<ChildInfoResponse>>> getMyChildren(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long parentId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                parentChildViewService.getMyChildren(parentId)
        ));
    }

    // 자녀 성적 조회 (기본: 현재 학기, academicYear/semester 파라미터로 이전 학기 조회 가능)
    @GetMapping("/children/{studentId}/grades")
    public ResponseEntity<ApiResponse<StudentMyGradeResponse>> getChildGrades(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester
    ) {
        Long parentId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                parentChildViewService.getChildGrades(parentId, studentId, academicYear, semester)
        ));
    }

    // 자녀 세특·행특 조회 (기본: 현재 학기, academicYear/semester 파라미터로 이전 학기 조회 가능)
    @GetMapping("/children/{studentId}/records")
    public ResponseEntity<ApiResponse<StudentMyRecordResponse>> getChildRecords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester
    ) {
        Long parentId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                parentChildViewService.getChildRecords(parentId, studentId, academicYear, semester)
        ));
    }
}
