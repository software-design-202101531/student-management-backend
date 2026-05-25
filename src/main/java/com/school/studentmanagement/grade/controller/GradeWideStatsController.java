package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.grade.dto.GradeWideRankingResponse;
import com.school.studentmanagement.grade.service.ClassroomStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeWideStatsController {

    private final ClassroomStatsService classroomStatsService;

    // 학년 단위 석차표 (교사 인증 필요. 학년 데이터는 모든 교사 열람 가능)
    @GetMapping("/grade-ranking")
    public ResponseEntity<ApiResponse<GradeWideRankingResponse>> getGradeWideRanking(
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester,
            @RequestParam Integer grade) {
        return ResponseEntity.ok(ApiResponse.ok(
                classroomStatsService.getGradeWideRanking(academicYear, semester, grade)
        ));
    }
}
