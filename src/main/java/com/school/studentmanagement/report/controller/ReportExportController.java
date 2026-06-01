package com.school.studentmanagement.report.controller;

import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.report.service.ReportExportService;
import com.school.studentmanagement.report.support.DownloadHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 교사·관리자용 보고서 엑셀 다운로드. 권한 세부 검증은 ReportExportService가 호출하는 도메인 서비스에서 수행.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exports")
public class ReportExportController {

    private final ReportExportService reportExportService;

    // 성적 분석 (학생 단위) — 요약/과목별/시험별 3개 시트
    @GetMapping("/students/{studentId}/grade-overview.xlsx")
    public ResponseEntity<byte[]> gradeOverview(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester
    ) {
        byte[] body = reportExportService.gradeOverviewXlsx(me.getUserId(), studentId, academicYear, semester);
        return ResponseEntity.ok()
                .headers(DownloadHeaders.excel("성적분석_학생" + studentId + ".xlsx"))
                .body(body);
    }

    // 상담 내역 (학생 단위)
    @GetMapping("/students/{studentId}/consultations.xlsx")
    public ResponseEntity<byte[]> consultations(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long studentId
    ) {
        byte[] body = reportExportService.consultationsXlsx(studentId, me.getUserId(), me.getUser().getRole());
        return ResponseEntity.ok()
                .headers(DownloadHeaders.excel("상담내역_학생" + studentId + ".xlsx"))
                .body(body);
    }

    // 피드백 요약 (학생 단위)
    @GetMapping("/students/{studentId}/feedbacks.xlsx")
    public ResponseEntity<byte[]> feedbacks(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long studentId
    ) {
        byte[] body = reportExportService.feedbacksXlsx(studentId, me.getUserId(), me.getUser().getRole());
        return ResponseEntity.ok()
                .headers(DownloadHeaders.excel("피드백_학생" + studentId + ".xlsx"))
                .body(body);
    }

    // ===== PDF =====

    @GetMapping("/students/{studentId}/grade-overview.pdf")
    public ResponseEntity<byte[]> gradeOverviewPdf(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester
    ) {
        byte[] body = reportExportService.gradeOverviewPdf(me.getUserId(), studentId, academicYear, semester);
        return ResponseEntity.ok()
                .headers(DownloadHeaders.pdf("성적분석_학생" + studentId + ".pdf"))
                .body(body);
    }

    @GetMapping("/students/{studentId}/consultations.pdf")
    public ResponseEntity<byte[]> consultationsPdf(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long studentId
    ) {
        byte[] body = reportExportService.consultationsPdf(studentId, me.getUserId(), me.getUser().getRole());
        return ResponseEntity.ok()
                .headers(DownloadHeaders.pdf("상담내역_학생" + studentId + ".pdf"))
                .body(body);
    }

    @GetMapping("/students/{studentId}/feedbacks.pdf")
    public ResponseEntity<byte[]> feedbacksPdf(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long studentId
    ) {
        byte[] body = reportExportService.feedbacksPdf(studentId, me.getUserId(), me.getUser().getRole());
        return ResponseEntity.ok()
                .headers(DownloadHeaders.pdf("피드백_학생" + studentId + ".pdf"))
                .body(body);
    }

    // ===== 반 전체 성적 (담임 전용 — 시험 단위) =====

    @GetMapping("/classrooms/{classroomId}/grades.xlsx")
    public ResponseEntity<byte[]> classroomGradesXlsx(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long classroomId,
            @RequestParam Long examId
    ) {
        byte[] body = reportExportService.classroomGradesXlsx(classroomId, me.getUserId(), examId);
        return ResponseEntity.ok()
                .headers(DownloadHeaders.excel("반성적_" + classroomId + "_시험" + examId + ".xlsx"))
                .body(body);
    }

    @GetMapping("/classrooms/{classroomId}/grades.pdf")
    public ResponseEntity<byte[]> classroomGradesPdf(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long classroomId,
            @RequestParam Long examId
    ) {
        byte[] body = reportExportService.classroomGradesPdf(classroomId, me.getUserId(), examId);
        return ResponseEntity.ok()
                .headers(DownloadHeaders.pdf("반성적_" + classroomId + "_시험" + examId + ".pdf"))
                .body(body);
    }
}
