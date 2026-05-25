package com.school.studentmanagement.grade.controller;

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
@RequestMapping("/api/student/me/grades")
@RequiredArgsConstructor
public class StudentGradeAnalyticsController {

    private final GradeAnalyticsService gradeAnalyticsService;
    private final ClassroomStatsService classroomStatsService;

    // 본인 레이더 차트 (학기 과목별 점수 + 학급 평균)
    @GetMapping("/radar")
    public ResponseEntity<ApiResponse<RadarChartResponse>> getMyRadar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeAnalyticsService.getStudentRadar(userDetails.getUserId(), academicYear, semester)
        ));
    }

    // 본인 시계열 추이 (학기 범위)
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<GradeTrendResponse>> getMyTrend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromSemester,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toSemester) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeAnalyticsService.getStudentTrend(userDetails.getUserId(),
                        fromYear, fromSemester, toYear, toSemester)
        ));
    }

    // 본인 학급 석차
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<StudentRankingResponse>> getMyRanking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                classroomStatsService.getMyRanking(userDetails.getUserId(), academicYear, semester)
        ));
    }

    // 본인 학년 석차
    @GetMapping("/grade-ranking")
    public ResponseEntity<ApiResponse<StudentGradeWideRankingResponse>> getMyGradeWideRanking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                classroomStatsService.getMyGradeWideRanking(userDetails.getUserId(), academicYear, semester)
        ));
    }
}
