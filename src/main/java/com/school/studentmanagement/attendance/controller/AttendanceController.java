package com.school.studentmanagement.attendance.controller;

import com.school.studentmanagement.attendance.dto.AttendanceDailyResponse;
import com.school.studentmanagement.attendance.dto.AttendanceMonthlyResponse;
import com.school.studentmanagement.attendance.dto.AttendanceSaveRequest;
import com.school.studentmanagement.attendance.service.AttendanceService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classrooms/{classroomId}/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 월간 출결 정보(휴일 정보 등) 조회 API
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<AttendanceMonthlyResponse>> getMonthlyAttendance(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("classroomId") Long classroomId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        Long teacherId = customUserDetails.getUserId();
        AttendanceMonthlyResponse response = attendanceService.getMonthlyAttendance(classroomId, teacherId, year, month);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 일간 출결 상세 조회 API
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<AttendanceDailyResponse>> getDailyAttendance(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("classroomId") Long classroomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long teacherId = customUserDetails.getUserId();
        AttendanceDailyResponse response = attendanceService.getDailyAttendance(classroomId, teacherId, date);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 일간 출결 정보 저장/수정 API
    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<Void>> saveDailyAttendance(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("classroomId") Long classroomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody AttendanceSaveRequest request
    ) {
        Long teacherId = customUserDetails.getUserId();
        attendanceService.saveDailyAttendance(classroomId, teacherId, date, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
