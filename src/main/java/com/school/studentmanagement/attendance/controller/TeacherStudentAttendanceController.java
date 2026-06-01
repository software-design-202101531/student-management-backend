package com.school.studentmanagement.attendance.controller;

import com.school.studentmanagement.attendance.dto.StudentAttendanceRangeResponse;
import com.school.studentmanagement.attendance.service.TeacherStudentAttendanceService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 교사가 학생 한 명의 기간 출결을 조회하는 엔드포인트.
 * 학급 단위 조회({@code /api/classrooms/...})는 담임만 가능하지만, 본 엔드포인트는
 * 담임 또는 과목 담당 교사면 조회 가능(피드백/상담 작성과 동일 권한 모델).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teachers/students/{studentId}/attendance")
public class TeacherStudentAttendanceController {

    private final TeacherStudentAttendanceService teacherStudentAttendanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<StudentAttendanceRangeResponse>> getStudentAttendanceRange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long teacherId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                teacherStudentAttendanceService.getStudentAttendanceRange(teacherId, studentId, from, to)
        ));
    }
}
