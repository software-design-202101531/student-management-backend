package com.school.studentmanagement.report.service;

import com.school.studentmanagement.consultation.service.ConsultationService;
import com.school.studentmanagement.feedback.service.FeedbackService;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.grade.service.GradeAnalyticsService;
import com.school.studentmanagement.grade.service.StudentGradeService;
import com.school.studentmanagement.consultation.dto.ConsultationResponse;
import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse;
import com.school.studentmanagement.grade.dto.StudentOverviewResponse;
import com.school.studentmanagement.report.excel.ClassroomGradeExcelWriter;
import com.school.studentmanagement.report.excel.ConsultationExcelWriter;
import com.school.studentmanagement.report.excel.FeedbackExcelWriter;
import com.school.studentmanagement.report.excel.GradeOverviewExcelWriter;
import com.school.studentmanagement.report.pdf.PdfRenderer;
import com.school.studentmanagement.report.support.ClassroomGradeTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 보고서(엑셀) 내보내기 오케스트레이션.
 * 데이터는 권한 검증이 포함된 기존 도메인 서비스를 그대로 호출해 얻고(담임/과목담당/공개범위 규칙 상속),
 * 작성기에 넘겨 바이트 배열로 굽기만 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportExportService {

    private final GradeAnalyticsService gradeAnalyticsService;
    private final StudentGradeService studentGradeService;
    private final ConsultationService consultationService;
    private final FeedbackService feedbackService;

    private final GradeOverviewExcelWriter gradeOverviewExcelWriter;
    private final ConsultationExcelWriter consultationExcelWriter;
    private final FeedbackExcelWriter feedbackExcelWriter;
    private final ClassroomGradeExcelWriter classroomGradeExcelWriter;
    private final PdfRenderer pdfRenderer;

    public byte[] gradeOverviewXlsx(Long teacherId, Long studentId, Integer academicYear, Integer semester) {
        return gradeOverviewExcelWriter.write(
                gradeAnalyticsService.getStudentOverviewForTeacher(teacherId, studentId, academicYear, semester));
    }

    public byte[] consultationsXlsx(Long studentId, Long requesterId, UserRole role) {
        return consultationExcelWriter.write(
                consultationService.getStudentConsultations(studentId, requesterId, role));
    }

    public byte[] feedbacksXlsx(Long studentId, Long requesterId, UserRole role) {
        return feedbackExcelWriter.write(
                feedbackService.getStudentFeedbacks(studentId, requesterId, role));
    }

    // ===== PDF =====

    public byte[] gradeOverviewPdf(Long teacherId, Long studentId, Integer academicYear, Integer semester) {
        StudentOverviewResponse overview =
                gradeAnalyticsService.getStudentOverviewForTeacher(teacherId, studentId, academicYear, semester);
        return pdfRenderer.render("reports/grade-overview", Map.of("overview", overview));
    }

    public byte[] consultationsPdf(Long studentId, Long requesterId, UserRole role) {
        List<ConsultationResponse> items = consultationService.getStudentConsultations(studentId, requesterId, role);
        return pdfRenderer.render("reports/consultations", Map.of("items", items, "studentId", studentId));
    }

    public byte[] feedbacksPdf(Long studentId, Long requesterId, UserRole role) {
        List<FeedbackResponse> items = feedbackService.getStudentFeedbacks(studentId, requesterId, role);
        return pdfRenderer.render("reports/feedbacks", Map.of("items", items, "studentId", studentId));
    }

    // ===== 반 전체 성적 (담임 검증은 getClassroomGrades에서 수행) =====

    public byte[] classroomGradesXlsx(Long classroomId, Long teacherId, Long examId) {
        return classroomGradeExcelWriter.write(
                studentGradeService.getClassroomGrades(classroomId, teacherId, examId));
    }

    public byte[] classroomGradesPdf(Long classroomId, Long teacherId, Long examId) {
        ClassroomGradeResponse data = studentGradeService.getClassroomGrades(classroomId, teacherId, examId);
        ClassroomGradeTable table = ClassroomGradeTable.from(data);
        String label = (data.getAcademicYear() != null ? data.getAcademicYear() : "?")
                + "학년도 " + (data.getSemester() != null ? data.getSemester() : "?") + "학기";
        return pdfRenderer.render("reports/classroom-grades", Map.of(
                "label", label,
                "examName", data.getExamName() != null ? data.getExamName() : "",
                "subjectNames", table.getSubjectNames(),
                "rows", table.getRows()));
    }
}
