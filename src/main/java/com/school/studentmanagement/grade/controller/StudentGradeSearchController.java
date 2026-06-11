package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.global.security.dto.CustomUserDetails;
import com.school.studentmanagement.grade.dto.GradeSearchResponse;
import com.school.studentmanagement.grade.service.GradeSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students/{studentId}/grades")
public class StudentGradeSearchController {

    private final GradeSearchService gradeSearchService;

    // 교사: 특정 학생 성적 검색 — 과목(subjectId)·학기 범위 필터(모두 선택). 담임/과목담당만, 미발행 포함.
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<GradeSearchResponse>> searchStudentGrades(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long studentId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromSemester,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toSemester
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeSearchService.searchForTeacher(
                        studentId, userDetails.getUserId(),
                        subjectId, fromYear, fromSemester, toYear, toSemester)
        ));
    }
}
