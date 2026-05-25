package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.student.dto.StudentMyAttendanceResponse;
import com.school.studentmanagement.student.service.StudentAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/me/attendance")
public class StudentMyAttendanceController {

    private final StudentAttendanceService studentAttendanceService;

    // 본인 월간 출결 조회 (특이사항 + 합계 + 휴일)
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<StudentMyAttendanceResponse>> getMyMonthlyAttendance(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(
                studentAttendanceService.getMyMonthlyAttendance(userDetails.getUserId(), year, month)
        ));
    }
}
