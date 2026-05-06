package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.student.dto.StudentMyGradeResponse;
import com.school.studentmanagement.student.dto.StudentMyRecordResponse;
import com.school.studentmanagement.student.service.StudentMyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/me")
public class StudentMyPageController {

    private final StudentMyPageService studentMyPageService;

    // 본인 성적 조회 (기본: 현재 학기, academicYear/semester 파라미터로 이전 학기 조회 가능)
    @GetMapping("/grades")
    public ResponseEntity<ApiResponse<StudentMyGradeResponse>> getMyGrades(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester
    ) {
        Long studentId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                studentMyPageService.getMyGrades(studentId, academicYear, semester)
        ));
    }

    // 본인 세특·행특 조회 (기본: 현재 학기, academicYear/semester 파라미터로 이전 학기 조회 가능)
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<StudentMyRecordResponse>> getMyRecords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester
    ) {
        Long studentId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                studentMyPageService.getMyRecords(studentId, academicYear, semester)
        ));
    }
}
