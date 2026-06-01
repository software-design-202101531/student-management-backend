package com.school.studentmanagement.report.excel;

import com.school.studentmanagement.feedback.dto.FeedbackResponse;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 피드백 요약 목록을 단일 시트 엑셀로 작성.
@Component
public class FeedbackExcelWriter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] write(List<FeedbackResponse> list) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("피드백");
            CellStyle header = PoiSupport.headerStyle(wb);
            PoiSupport.writeHeader(s, header, "작성일", "작성교사", "분류", "공개여부", "상태", "내용");
            PoiSupport.setColumnWidths(s, 18, 12, 12, 10, 10, 50);

            int r = 1;
            for (FeedbackResponse f : list) {
                Row row = s.createRow(r++);
                PoiSupport.text(row, 0, fmt(f.getCreatedAt()));
                PoiSupport.text(row, 1, f.getTeacherName());
                PoiSupport.text(row, 2, f.getCategoryLabel());
                PoiSupport.text(row, 3, f.isPublic() ? "공개" : "비공개");
                PoiSupport.text(row, 4, f.getStatus() != null ? f.getStatus().name() : "");
                PoiSupport.text(row, 5, f.getContent());
            }

            wb.write(out);
            // try-with-resources의 close()가 SXSSF 임시파일까지 정리한다(dispose()는 deprecated).
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.REPORT_EXPORT_FAILED);
        }
    }

    private static String fmt(LocalDateTime t) {
        return t != null ? t.format(DT) : "";
    }
}
