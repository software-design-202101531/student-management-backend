package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.student.dto.StudentActivationRequest;
import com.school.studentmanagement.student.dto.VerifyStudentRequest;
import com.school.studentmanagement.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/verify-student")
    public ResponseEntity<ApiResponse<Long>> verifyStudent(@Valid @RequestBody VerifyStudentRequest request) {
        Long userId = studentService.verifyStudent(request);
        return ResponseEntity.ok(ApiResponse.ok(userId));
    }

    @PostMapping("/activate-student")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@Valid @RequestBody StudentActivationRequest request) {
        studentService.activateStudentAccount(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
