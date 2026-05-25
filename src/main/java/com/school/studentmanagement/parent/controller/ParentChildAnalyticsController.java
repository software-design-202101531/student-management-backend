package com.school.studentmanagement.parent.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.grade.dto.GradeTrendResponse;
import com.school.studentmanagement.grade.dto.RadarChartResponse;
import com.school.studentmanagement.grade.dto.StudentGradeWideRankingResponse;
import com.school.studentmanagement.grade.dto.StudentRankingResponse;
import com.school.studentmanagement.grade.service.ClassroomStatsService;
import com.school.studentmanagement.grade.service.GradeAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parent/me/children/{studentId}/grades")
@RequiredArgsConstructor
public class ParentChildAnalyticsController {

    private final GradeAnalyticsService gradeAnalyticsService;
    private final ClassroomStatsService classroomStatsService;

    @GetMapping("/radar")
    public ResponseEntity<ApiResponse<RadarChartResponse>> getChildRadar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeAnalyticsService.getChildRadar(userDetails.getUserId(), studentId, academicYear, semester)
        ));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<GradeTrendResponse>> getChildTrend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromSemester,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toSemester) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeAnalyticsService.getChildTrend(userDetails.getUserId(), studentId,
                        fromYear, fromSemester, toYear, toSemester)
        ));
    }

    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<StudentRankingResponse>> getChildRanking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                classroomStatsService.getChildRanking(userDetails.getUserId(), studentId, academicYear, semester)
        ));
    }

    @GetMapping("/grade-ranking")
    public ResponseEntity<ApiResponse<StudentGradeWideRankingResponse>> getChildGradeWideRanking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                classroomStatsService.getChildGradeWideRanking(userDetails.getUserId(), studentId, academicYear, semester)
        ));
    }
}
