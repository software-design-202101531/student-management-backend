package com.school.studentmanagement.subject.controller;

import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.subject.dto.TeacherAssignmentResponse;
import com.school.studentmanagement.subject.service.TeacherAssignmentService;
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

    // 내 담당 수업 목록 조회
    @GetMapping("/assignments")
    public ResponseEntity<List<TeacherAssignmentResponse>> getMyAssignments(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
            ) {
        Long teacherId = customUserDetails.getUserId();

        List<TeacherAssignmentResponse> responses = teacherAssignmentService.getMyAssignments(teacherId);

        return ResponseEntity.ok(responses);
    }
}
