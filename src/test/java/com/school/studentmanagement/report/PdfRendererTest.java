package com.school.studentmanagement.report;

import com.school.studentmanagement.consultation.dto.ConsultationResponse;
import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.global.enums.ConsultationVisibility;
import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.global.enums.FeedbackCategory;
import com.school.studentmanagement.global.enums.FeedbackStatus;
import com.school.studentmanagement.global.enums.GradeLevel;
import com.school.studentmanagement.grade.dto.StudentOverviewResponse;
import com.school.studentmanagement.report.pdf.PdfRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// DB·전체 컨텍스트 없이 PDF 생성 파이프라인(Thymeleaf → jsoup → openhtmltopdf + 한글폰트)을 검증.
class PdfRendererTest {

    private PdfRenderer renderer;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        renderer = new PdfRenderer(engine);
    }

    private void assertIsPdf(byte[] pdf) {
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(1000);
        // PDF 매직 넘버 "%PDF-"
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("성적 분석 PDF: 중첩 시험/과목·한글 렌더링")
    void gradeOverviewPdf() {
        StudentOverviewResponse overview = StudentOverviewResponse.builder()
                .studentId(1L).studentName("홍길동").grade(2).classNum(3).studentNum(11)
                .academicYear(2026).semester(1)
                .totalScore(540.0).averageScore(90.0).gradeLevel(GradeLevel.A)
                .classRank(2).classSize(28)
                .subjectScores(List.of(
                        StudentOverviewResponse.SubjectSemesterScoreDto.builder()
                                .subjectId(10L).subjectName("수학").semesterScore(92.0).classAverage(78.5).build()))
                .examResults(List.of(
                        StudentOverviewResponse.ExamResultDto.builder()
                                .examId(100L).examType(ExamType.MIDTERM).examName("중간고사")
                                .examDate(LocalDate.of(2026, 4, 25)).maxScore(100).published(true)
                                .subjects(List.of(
                                        StudentOverviewResponse.SubjectScoreDto.builder()
                                                .gradeId(1000L).subjectName("수학").rawScore(92)
                                                .attendanceStatus(ExamAttendanceStatus.PRESENT).build()))
                                .build()))
                .build();

        assertIsPdf(renderer.render("reports/grade-overview", Map.of("overview", overview)));
    }

    @Test
    @DisplayName("피드백 PDF: isPublic() SpEL·목록 렌더링")
    void feedbacksPdf() {
        FeedbackResponse f = FeedbackResponse.builder()
                .feedbackId(1L).studentId(1L).teacherId(5L).teacherName("김철수")
                .category(FeedbackCategory.GRADE).categoryLabel("성적")
                .content("성실하게 학습에 임함").status(FeedbackStatus.PUBLISHED).isPublic(true)
                .createdAt(LocalDateTime.of(2026, 5, 30, 10, 15)).build();

        assertIsPdf(renderer.render("reports/feedbacks", Map.of("items", List.of(f), "studentId", 1L)));
    }

    @Test
    @DisplayName("상담 PDF: 목록 렌더링")
    void consultationsPdf() {
        ConsultationResponse c = ConsultationResponse.builder()
                .consultationId(1L).studentId(1L).teacherId(5L).teacherName("이영희")
                .consultationDate(LocalDateTime.of(2026, 5, 20, 14, 0))
                .content("진로 상담 진행").nextPlan("2학기 재상담")
                .visibility(ConsultationVisibility.RESTRICTED).visibilityLabel("제한 공개")
                .createdAt(LocalDateTime.of(2026, 5, 20, 14, 30)).build();

        assertIsPdf(renderer.render("reports/consultations", Map.of("items", List.of(c), "studentId", 1L)));
    }

    @Test
    @DisplayName("빈 목록도 정상 PDF 생성")
    void emptyListPdf() {
        assertIsPdf(renderer.render("reports/feedbacks", Map.of("items", List.of(), "studentId", 9L)));
    }

    @Test
    @DisplayName("반 전체 성적 PDF: 동적 과목 컬럼 렌더링")
    void classroomGradesPdf() {
        var table = com.school.studentmanagement.report.support.ClassroomGradeTable.from(
                com.school.studentmanagement.grade.dto.ClassroomGradeResponse.builder()
                        .examId(1L).academicYear(2026).semester(1).examName("중간고사")
                        .students(List.of(
                                com.school.studentmanagement.grade.dto.ClassroomGradeResponse.StudentAllGradesDto.builder()
                                        .studentId(1L).studentName("홍길동").studentNum(1)
                                        .totalScore(180.0).averageScore(90.0).gradeLevel(GradeLevel.A)
                                        .subjectScores(List.of(
                                                com.school.studentmanagement.grade.dto.ClassroomGradeResponse.SubjectScoreDto.builder()
                                                        .gradeId(1L).subjectName("수학").rawScore(92)
                                                        .attendanceStatus(ExamAttendanceStatus.PRESENT).build(),
                                                com.school.studentmanagement.grade.dto.ClassroomGradeResponse.SubjectScoreDto.builder()
                                                        .gradeId(2L).subjectName("국어").rawScore(88)
                                                        .attendanceStatus(ExamAttendanceStatus.PRESENT).build()))
                                        .build()))
                        .build());

        assertIsPdf(renderer.render("reports/classroom-grades", Map.of(
                "label", "2026학년도 1학기", "examName", "중간고사",
                "subjectNames", table.getSubjectNames(), "rows", table.getRows())));
    }
}
