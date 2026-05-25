package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.grade.dto.GradeHistoryResponse;
import com.school.studentmanagement.grade.service.GradeHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeHistoryController {

    private final GradeHistoryService gradeHistoryService;

    // 한 성적의 변경 이력 (최신순). 권한: 담임 또는 해당 과목 담당 교사
    @GetMapping("/{gradeId}/history")
    public ResponseEntity<ApiResponse<GradeHistoryResponse>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long gradeId) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeHistoryService.getHistory(gradeId, userDetails.getUserId())
        ));
    }
}
