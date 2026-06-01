package com.school.studentmanagement.user.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.user.dto.MyProfileResponse;
import com.school.studentmanagement.user.service.MyProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class MyProfileController {

    private final MyProfileService myProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MyProfileResponse response = myProfileService.getMyProfile(
                userDetails.getUserId(), userDetails.getUser().getRole());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
