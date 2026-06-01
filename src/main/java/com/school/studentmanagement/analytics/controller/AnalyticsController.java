package com.school.studentmanagement.analytics.controller;

import com.school.studentmanagement.analytics.dto.StudentAnalyticsOverviewResponse;
import com.school.studentmanagement.analytics.dto.SubjectAnalyticsOverviewResponse;
import com.school.studentmanagement.analytics.etl.AnalyticsEtlService;
import com.school.studentmanagement.analytics.service.AnalyticsDashboardService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 분석 대시보드 조회 + ETL 수동 트리거. analytics 스키마만 읽는다(운영 부하 격리).
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsDashboardService dashboardService;
    private final AnalyticsEtlService etlService;
    private final AcademicCalendarUtil academicCalendarUtil;

    // 학생 학습현황 (교사/관리자). 학기 미지정 시 현재 학년도/학기.
    @GetMapping("/students/{studentId}/overview")
    public ResponseEntity<ApiResponse<StudentAnalyticsOverviewResponse>> studentOverview(
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester
    ) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getStudentOverview(studentId, year, sem)));
    }

    // 과목 학습현황 (교사/관리자). 여러 학생을 가로지르는 과목 평균/분포/제출률. 학기 미지정 시 현재 학년도/학기.
    @GetMapping("/subjects/{subjectId}/overview")
    public ResponseEntity<ApiResponse<SubjectAnalyticsOverviewResponse>> subjectOverview(
            @PathVariable Long subjectId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester
    ) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSubjectOverview(subjectId, year, sem)));
    }

    // ETL 수동 실행 (관리자) — 야간 배치 외 즉시 적재용
    @PostMapping("/etl/run")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runEtl() {
        int rows = etlService.runAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("rowsUpserted", rows)));
    }
}
