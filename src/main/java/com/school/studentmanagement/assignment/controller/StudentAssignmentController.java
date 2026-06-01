package com.school.studentmanagement.assignment.controller;

import com.school.studentmanagement.assignment.dto.StudentAssignmentResponse;
import com.school.studentmanagement.assignment.dto.SubmissionCreateRequest;
import com.school.studentmanagement.assignment.service.StudentAssignmentService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 학생 본인 과제 조회/제출 (STUDENT 전용 — userId == studentId)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/me/assignments")
public class StudentAssignmentController {

    private final StudentAssignmentService studentAssignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentAssignmentResponse>>> myAssignments(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                studentAssignmentService.getMyAssignments(userDetails.getUserId())));
    }

    @PostMapping("/{assignmentId}/submission")
    public ResponseEntity<ApiResponse<Void>> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long assignmentId,
            @RequestBody SubmissionCreateRequest request
    ) {
        studentAssignmentService.submit(userDetails.getUserId(), assignmentId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
