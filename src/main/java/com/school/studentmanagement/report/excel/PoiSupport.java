package com.school.studentmanagement.report.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

// 엑셀 작성기 공통 헬퍼 (헤더 스타일/행 작성). 작성기 패키지 내부 전용.
final class PoiSupport {

    private PoiSupport() {
    }

    static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    static void writeHeader(Sheet sheet, CellStyle style, String... cols) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
        }
    }

    // 컬럼 폭을 글자 수 기준으로 일괄 지정 (POI 단위 = 1/256 글자폭)
    static void setColumnWidths(Sheet sheet, int... charWidths) {
        for (int i = 0; i < charWidths.length; i++) {
            sheet.setColumnWidth(i, charWidths[i] * 256);
        }
    }

    static void text(Row row, int col, String value) {
        row.createCell(col).setCellValue(value != null ? value : "");
    }

    // null이면 빈 셀로 둔다(숫자 0과 구분).
    static void number(Row row, int col, Double value) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    static void number(Row row, int col, Integer value) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
    }
}
