package com.school.studentmanagement.report.excel;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse;
import com.school.studentmanagement.report.support.ClassroomGradeTable;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 반 전체 성적을 과목 컬럼 매트릭스(학생 행)로 작성.
@Component
public class ClassroomGradeExcelWriter {

    public byte[] write(ClassroomGradeResponse data) {
        ClassroomGradeTable table = ClassroomGradeTable.from(data);
        try (SXSSFWorkbook wb = new SXSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("반 성적");
            CellStyle header = PoiSupport.headerStyle(wb);

            List<String> cols = new ArrayList<>();
            cols.add("번호");
            cols.add("이름");
            cols.addAll(table.getSubjectNames());
            cols.add("총점");
            cols.add("평균");
            cols.add("등급");
            PoiSupport.writeHeader(sheet, header, cols.toArray(new String[0]));

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 12 * 256);
            int subjectCount = table.getSubjectNames().size();
            for (int i = 0; i < subjectCount; i++) {
                sheet.setColumnWidth(2 + i, 10 * 256);
            }
            int tail = 2 + subjectCount;
            sheet.setColumnWidth(tail, 10 * 256);
            sheet.setColumnWidth(tail + 1, 10 * 256);
            sheet.setColumnWidth(tail + 2, 8 * 256);

            int r = 1;
            for (ClassroomGradeTable.Row row : table.getRows()) {
                Row excelRow = sheet.createRow(r++);
                int c = 0;
                PoiSupport.number(excelRow, c++, row.getStudentNum());
                PoiSupport.text(excelRow, c++, row.getStudentName());
                for (String score : row.getScores()) {
                    PoiSupport.text(excelRow, c++, score);
                }
                PoiSupport.text(excelRow, c++, row.getTotalScore());
                PoiSupport.text(excelRow, c++, row.getAverageScore());
                PoiSupport.text(excelRow, c, row.getGradeLevel());
            }

            wb.write(out);
            // try-with-resources의 close()가 SXSSF 임시파일까지 정리한다(dispose()는 deprecated).
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.REPORT_EXPORT_FAILED);
        }
    }
}
