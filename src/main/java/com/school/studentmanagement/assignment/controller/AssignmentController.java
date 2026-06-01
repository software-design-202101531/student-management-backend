package com.school.studentmanagement.assignment.controller;

import com.school.studentmanagement.assignment.dto.AssignmentCreateRequest;
import com.school.studentmanagement.assignment.dto.AssignmentResponse;
import com.school.studentmanagement.assignment.dto.SubmissionGradeRequest;
import com.school.studentmanagement.assignment.dto.SubmissionStatusResponse;
import com.school.studentmanagement.assignment.service.AssignmentService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 과제 부여/조회/제출현황 — 과목 담당 교사 전용 (세부 권한은 서비스에서 검증)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classrooms/{classroomId}/subjects/{subjectId}/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classroomId,
            @PathVariable Long subjectId,
            @Valid @RequestBody AssignmentCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                assignmentService.createAssignment(userDetails.getUserId(), classroomId, subjectId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classroomId,
            @PathVariable Long subjectId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                assignmentService.getAssignments(userDetails.getUserId(), classroomId, subjectId)));
    }

    @PutMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classroomId,
            @PathVariable Long subjectId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                assignmentService.updateAssignment(userDetails.getUserId(), classroomId, subjectId, assignmentId, request)));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classroomId,
            @PathVariable Long subjectId,
            @PathVariable Long assignmentId
    ) {
        assignmentService.deleteAssignment(userDetails.getUserId(), classroomId, subjectId, assignmentId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<SubmissionStatusResponse>> submissionStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classroomId,
            @PathVariable Long subjectId,
            @PathVariable Long assignmentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                assignmentService.getSubmissionStatus(userDetails.getUserId(), classroomId, subjectId, assignmentId)));
    }

    // 특정 학생의 제출물 채점(점수+선택 피드백)
    @PostMapping("/{assignmentId}/submissions/{studentId}/grade")
    public ResponseEntity<ApiResponse<Void>> grade(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classroomId,
            @PathVariable Long subjectId,
            @PathVariable Long assignmentId,
            @PathVariable Long studentId,
            @Valid @RequestBody SubmissionGradeRequest request
    ) {
        assignmentService.gradeSubmission(userDetails.getUserId(), classroomId, subjectId, assignmentId, studentId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
