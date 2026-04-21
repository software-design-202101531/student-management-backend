package com.school.studentmanagement.teacher.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.teacher.dto.TeacherProfileResponse;
import com.school.studentmanagement.teacher.service.TeacherProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherProfileService teacherProfileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TeacherProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long userId = customUserDetails.getUserId();
        TeacherProfileResponse response = teacherProfileService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
