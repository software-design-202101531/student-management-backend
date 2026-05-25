package com.school.studentmanagement.grade.controller;

import com.school.studentmanagement.global.response.ApiResponse;
import com.school.studentmanagement.grade.dto.ExamCreateRequest;
import com.school.studentmanagement.grade.dto.ExamResponse;
import com.school.studentmanagement.grade.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // 시험 메타 등록 (교사 공통 — 학교 단위 메타이므로 권한 세분화는 추후 과제)
    @PostMapping
    public ResponseEntity<ApiResponse<ExamResponse>> create(@RequestBody @Valid ExamCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(examService.createExam(request)));
    }

    // 학기 시험 목록 (시험일자 오름차순)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamResponse>>> list(
            @RequestParam Integer academicYear,
            @RequestParam Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(examService.listExams(academicYear, semester)));
    }

    // 공개 처리: 학생/학부모 조회 가능 상태로 전환
    @PostMapping("/{examId}/publish")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable Long examId) {
        examService.publish(examId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/{examId}/unpublish")
    public ResponseEntity<ApiResponse<Void>> unpublish(@PathVariable Long examId) {
        examService.unpublish(examId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
