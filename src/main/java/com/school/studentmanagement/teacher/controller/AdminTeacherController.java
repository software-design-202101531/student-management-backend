package com.school.studentmanagement.teacher.controller;

import com.school.studentmanagement.global.dto.ProfileImageResponse;
import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.teacher.service.TeacherProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
public class AdminTeacherController {

    private final TeacherProfileImageService teacherProfileImageService;

    // 관리자가 교사 프로필 사진 등록/수정 (ADMIN 권한은 /api/admin/** 경로에서 강제)
    @PostMapping(value = "/{teacherId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileImageResponse>> updateProfileImage(
            @PathVariable Long teacherId,
            @RequestParam("file") MultipartFile file
    ) {
        ProfileImageResponse response = teacherProfileImageService.updateProfileImage(teacherId, file);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
