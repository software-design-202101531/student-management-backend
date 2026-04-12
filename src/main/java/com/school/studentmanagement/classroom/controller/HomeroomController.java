package com.school.studentmanagement.classroom.controller;

import com.school.studentmanagement.classroom.dto.HomeroomStudentResponse;
import com.school.studentmanagement.classroom.service.HomeroomService;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classrooms")
public class HomeroomController {

    private final HomeroomService homeroomService;

    // 내 담임 반 학생 목록 조회 API
    @GetMapping("/my-homeroom/studetns")
    public ResponseEntity<List<HomeroomStudentResponse>> getMyHomeroomStudents(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
            ) {
        // 토큰에서 pk 추출
        Long teacherId = customUserDetails.getUserId();

        // 서비스 로직 실행
        List<HomeroomStudentResponse> responses = homeroomService.getMyHomeroomStudents(teacherId);

        // HTTP 200 OK와 함께 응답
        return ResponseEntity.ok(responses);
    }
}
