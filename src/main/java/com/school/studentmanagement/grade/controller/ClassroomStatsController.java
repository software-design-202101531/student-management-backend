package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.grade.dto.ClassroomRankingResponse;
import com.school.studentmanagement.grade.dto.ClassroomStatsResponse;
import com.school.studentmanagement.grade.service.ClassroomStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classrooms/{classroomId}/grades")
@RequiredArgsConstructor
public class ClassroomStatsController {

    private final ClassroomStatsService classroomStatsService;

    // 학급 통계 (시험 + 과목 단위): 평균/표준편차/최고/최저/점수 분포
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ClassroomStatsResponse>> getStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classroomId,
            @RequestParam Long examId,
            @RequestParam Long subjectId) {
        return ResponseEntity.ok(ApiResponse.ok(
                classroomStatsService.getClassroomStats(classroomId, userDetails.getUserId(), examId, subjectId)
        ));
    }

    // 학급 석차 (담임 전용): 학기 평균 기준 RANK
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<ClassroomRankingResponse>> getRanking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classroomId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                classroomStatsService.getClassroomRanking(classroomId, userDetails.getUserId(),
                        academicYear, semester)
        ));
    }
}
