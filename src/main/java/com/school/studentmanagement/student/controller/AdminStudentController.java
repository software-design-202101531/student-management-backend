package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.student.service.ExcelUploadService;
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
    public ResponseEntity<ApiResponse<Void>> uploadExcel(@RequestParam("file") MultipartFile file) {
        excelUploadService.uploadStudentExcel(file);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
