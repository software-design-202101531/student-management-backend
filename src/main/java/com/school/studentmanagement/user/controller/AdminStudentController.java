package com.school.studentmanagement.user.controller;

import com.school.studentmanagement.user.service.ExcelUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/students")
public class AdminStudentController {

    private final ExcelUploadService excelUploadService;

    @PostMapping(value = "/excel-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        // 서비스 로직 호출
        excelUploadService.uploadStudentExcel(file);

        return ResponseEntity.ok("엑셀 데이터 저장을 완료했습니다");
    }
}
