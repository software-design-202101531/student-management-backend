package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.grade.dto.GradeTrendResponse;
import com.school.studentmanagement.grade.dto.RadarChartResponse;
import com.school.studentmanagement.grade.dto.StudentOverviewResponse;
import com.school.studentmanagement.grade.service.GradeAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers/students/{studentId}/grades")
@RequiredArgsConstructor
public class TeacherStudentGradeController {

    private final GradeAnalyticsService gradeAnalyticsService;

    // 학생 종합 뷰: 학기 통계 + 과목별 학기점수(학급 평균 포함) + 시험별 결과 + 학급 석차
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<StudentOverviewResponse>> getOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeAnalyticsService.getStudentOverviewForTeacher(userDetails.getUserId(), studentId,
                        academicYear, semester)
        ));
    }

    @GetMapping("/radar")
    public ResponseEntity<ApiResponse<RadarChartResponse>> getRadar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeAnalyticsService.getRadarForTeacher(userDetails.getUserId(), studentId,
                        academicYear, semester)
        ));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<GradeTrendResponse>> getTrend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromSemester,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toSemester) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeAnalyticsService.getTrendForTeacher(userDetails.getUserId(), studentId,
                        fromYear, fromSemester, toYear, toSemester)
        ));
    }
}
