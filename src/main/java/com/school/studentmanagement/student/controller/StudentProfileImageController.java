package com.school.studentmanagement.student.controller;

import com.school.studentmanagement.global.dto.ProfileImageResponse;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.student.service.StudentProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentProfileImageController {

    private final StudentProfileImageService studentProfileImageService;

    // 담임 교사가 자기 반 학생의 프로필 사진 등록/수정 (담임 검증은 서비스에서 수행)
    @PostMapping(value = "/{studentId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileImageResponse>> updateProfileImage(
            @PathVariable Long studentId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long teacherId = customUserDetails.getUserId();
        ProfileImageResponse response = studentProfileImageService.updateProfileImage(studentId, teacherId, file);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
