package com.school.studentmanagement.report.excel;

import com.school.studentmanagement.consultation.dto.ConsultationResponse;
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

// 상담 내역 목록을 단일 시트 엑셀로 작성.
@Component
public class ConsultationExcelWriter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] write(List<ConsultationResponse> list) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("상담내역");
            CellStyle header = PoiSupport.headerStyle(wb);
            PoiSupport.writeHeader(s, header, "상담일시", "작성교사", "공개범위", "내용", "다음계획", "작성일", "수정일");
            PoiSupport.setColumnWidths(s, 18, 12, 14, 50, 40, 18, 18);

            int r = 1;
            for (ConsultationResponse c : list) {
                Row row = s.createRow(r++);
                PoiSupport.text(row, 0, fmt(c.getConsultationDate()));
                PoiSupport.text(row, 1, c.getTeacherName());
                PoiSupport.text(row, 2, c.getVisibilityLabel());
                PoiSupport.text(row, 3, c.getContent());
                PoiSupport.text(row, 4, c.getNextPlan());
                PoiSupport.text(row, 5, fmt(c.getCreatedAt()));
                PoiSupport.text(row, 6, fmt(c.getUpdatedAt()));
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
