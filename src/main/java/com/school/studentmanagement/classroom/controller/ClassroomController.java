package com.school.studentmanagement.classroom.controller;

import com.school.studentmanagement.classroom.dto.StudentListResponse;
import com.school.studentmanagement.classroom.service.ClassroomStudentService;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomStudentService classroomStudentService;

    // 내 담임 반 학생 목록 조회 API
    @GetMapping("/my-homeroom/students")
    public ResponseEntity<ApiResponse<List<StudentListResponse>>> getMyHomeroomStudents(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long teacherId = customUserDetails.getUserId();
        List<StudentListResponse> responses = classroomStudentService.getMyHomeroomStudents(teacherId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/{classroomId}/students")
    public ResponseEntity<ApiResponse<List<StudentListResponse>>> getStudentsInClassroom(
            @PathVariable Long classroomId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long teacherId = customUserDetails.getUserId();
        List<StudentListResponse> responses = classroomStudentService.getStudentsInClassroom(classroomId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
