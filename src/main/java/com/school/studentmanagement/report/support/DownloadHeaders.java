package com.school.studentmanagement.report.support;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

// 파일 다운로드 응답 헤더 구성 유틸. 한글 파일명은 RFC 5987(filename*=UTF-8'')로 인코딩된다.
public final class DownloadHeaders {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private DownloadHeaders() {
    }

    public static HttpHeaders excel(String filename) {
        return attachment(XLSX, filename);
    }

    public static HttpHeaders pdf(String filename) {
        return attachment(MediaType.APPLICATION_PDF, filename);
    }

    private static HttpHeaders attachment(MediaType contentType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        // ContentDisposition가 charset을 받으면 filename*=UTF-8'' 형태로 직렬화 → 한글 파일명 깨짐 방지
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return headers;
    }
}
