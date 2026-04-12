package com.school.studentmanagement.user.controller;

import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.user.dto.TeacherProfileResponse;
import com.school.studentmanagement.user.repository.TeacherRepository;
import com.school.studentmanagement.user.service.TeacherProfileService;
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
    public ResponseEntity<TeacherProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        // UserId 가져오기
        Long userId = customUserDetails.getUserId();

        // 서비스 로직 실행
        TeacherProfileResponse response = teacherProfileService.getMyProfile(userId);

        // 정보 DTO 응답
        return ResponseEntity.ok(response);
    }
}
