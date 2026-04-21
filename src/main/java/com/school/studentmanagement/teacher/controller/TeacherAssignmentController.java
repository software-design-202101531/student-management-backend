package com.school.studentmanagement.teacher.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.teacher.dto.TeacherAssignmentResponse;
import com.school.studentmanagement.teacher.service.TeacherAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teachers/me")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService teacherAssignmentService;

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentResponse>>> getMyAssignments(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long teacherId = customUserDetails.getUserId();
        List<TeacherAssignmentResponse> responses = teacherAssignmentService.getMyAssignments(teacherId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
