package com.school.studentmanagement.report.excel;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.grade.dto.StudentOverviewResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

// 학생 성적 분석(StudentOverviewResponse)을 3개 시트(요약/과목별/시험별) 엑셀로 작성.
@Component
public class GradeOverviewExcelWriter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] write(StudentOverviewResponse o) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = PoiSupport.headerStyle(wb);
            writeSummary(wb, header, o);
            writeSubjectScores(wb, header, o);
            writeExamResults(wb, header, o);
            wb.write(out);
            // try-with-resources의 close()가 SXSSF 임시파일까지 정리한다(dispose()는 deprecated).
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.REPORT_EXPORT_FAILED);
        }
    }

    private void writeSummary(SXSSFWorkbook wb, CellStyle header, StudentOverviewResponse o) {
        Sheet s = wb.createSheet("요약");
        PoiSupport.writeHeader(s, header, "항목", "값");
        PoiSupport.setColumnWidths(s, 18, 40);
        int r = 1;
        r = kv(s, r, "학생", safe(o.getStudentName()) + " (#" + o.getStudentId() + ")");
        r = kv(s, r, "학급", n(o.getGrade()) + "학년 " + n(o.getClassNum()) + "반 " + n(o.getStudentNum()) + "번");
        r = kv(s, r, "학년도/학기", n(o.getAcademicYear()) + "학년도 " + n(o.getSemester()) + "학기");
        r = kv(s, r, "총점", d(o.getTotalScore()));
        r = kv(s, r, "평균", d(o.getAverageScore()));
        r = kv(s, r, "성취등급", o.getGradeLevel() != null ? o.getGradeLevel().name() : "");
        kv(s, r, "석차", o.getClassRank() != null ? (o.getClassRank() + " / " + n(o.getClassSize())) : "");
    }

    private void writeSubjectScores(SXSSFWorkbook wb, CellStyle header, StudentOverviewResponse o) {
        Sheet s = wb.createSheet("과목별 학기점수");
        PoiSupport.writeHeader(s, header, "과목", "학기점수", "반평균");
        PoiSupport.setColumnWidths(s, 20, 12, 12);
        int r = 1;
        if (o.getSubjectScores() != null) {
            for (StudentOverviewResponse.SubjectSemesterScoreDto ss : o.getSubjectScores()) {
                Row row = s.createRow(r++);
                PoiSupport.text(row, 0, ss.getSubjectName());
                PoiSupport.number(row, 1, ss.getSemesterScore());
                PoiSupport.number(row, 2, ss.getClassAverage());
            }
        }
    }

    private void writeExamResults(SXSSFWorkbook wb, CellStyle header, StudentOverviewResponse o) {
        Sheet s = wb.createSheet("시험별 성적");
        PoiSupport.writeHeader(s, header, "시험", "유형", "일자", "과목", "점수", "응시상태");
        PoiSupport.setColumnWidths(s, 18, 10, 14, 16, 10, 12);
        int r = 1;
        if (o.getExamResults() != null) {
            for (StudentOverviewResponse.ExamResultDto ex : o.getExamResults()) {
                if (ex.getSubjects() == null) continue;
                for (StudentOverviewResponse.SubjectScoreDto sub : ex.getSubjects()) {
                    Row row = s.createRow(r++);
                    PoiSupport.text(row, 0, ex.getExamName());
                    PoiSupport.text(row, 1, ex.getExamType() != null ? ex.getExamType().name() : "");
                    PoiSupport.text(row, 2, ex.getExamDate() != null ? ex.getExamDate().format(DATE) : "");
                    PoiSupport.text(row, 3, sub.getSubjectName());
                    PoiSupport.number(row, 4, sub.getRawScore());
                    PoiSupport.text(row, 5, sub.getAttendanceStatus() != null ? sub.getAttendanceStatus().name() : "");
                }
            }
        }
    }

    private int kv(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        PoiSupport.text(row, 0, label);
        PoiSupport.text(row, 1, value);
        return rowIdx + 1;
    }

    private static String safe(String v) {
        return v != null ? v : "";
    }

    private static String n(Integer v) {
        return v != null ? String.valueOf(v) : "-";
    }

    private static String d(Double v) {
        return v != null ? String.valueOf(v) : "-";
    }
}
