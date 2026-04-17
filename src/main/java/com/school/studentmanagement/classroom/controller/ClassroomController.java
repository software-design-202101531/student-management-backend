package com.school.studentmanagement.classroom.controller;

import com.school.studentmanagement.classroom.dto.StudentListResponse;
import com.school.studentmanagement.classroom.service.ClassroomStudentService;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomStudentService classroomStudentService;

    // 내 담임 반 학생 목록 조회 API
    @GetMapping("/my-homeroom/students")
    public ResponseEntity<List<StudentListResponse>> getMyHomeroomStudents(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
            ) {
        // 토큰에서 pk 추출
        Long teacherId = customUserDetails.getUserId();

        // 서비스 로직 실행
        List<StudentListResponse> responses = classroomStudentService.getMyHomeroomStudents(teacherId);

        // HTTP 200 OK와 함께 응답
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{classroomId}/students")
    public ResponseEntity<List<StudentListResponse>> getMyHomeroomStudents(
            @PathVariable Long classroomId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long teacherId = customUserDetails.getUserId();

        List<StudentListResponse> responses = classroomStudentService.getStudentsInClassroom(classroomId, teacherId);

        return ResponseEntity.ok(responses);
    }
}
