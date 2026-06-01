package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.student.dto.StudentContactUpdateRequest;
import com.school.studentmanagement.student.dto.StudentProfileResponse;
import com.school.studentmanagement.student.service.TeacherStudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 담임 교사용 학생 학생부 기본 프로필 API.
 * - GET : 학생 기본 정보 통합 조회
 * - PATCH: 주소/전화 부분 갱신 (담임 검증은 서비스에서 수행)
 */
@RestController
@RequestMapping("/api/teachers/students/{studentId}/profile")
@RequiredArgsConstructor
public class TeacherStudentProfileController {

    private final TeacherStudentProfileService teacherStudentProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId
    ) {
        Long teacherId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                teacherStudentProfileService.getProfile(studentId, teacherId)
        ));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<StudentProfileResponse>> updateContact(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @Valid @RequestBody StudentContactUpdateRequest request
    ) {
        Long teacherId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                teacherStudentProfileService.updateContact(studentId, teacherId, request)
        ));
    }
}
